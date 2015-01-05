package i2p.bote.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import i2p.bote.I2PBote;
import i2p.bote.android.util.AuthenticatedListFragment;
import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.MoveToDialogFragment;
import i2p.bote.android.util.MultiSelectionUtil;
import i2p.bote.android.util.MultiSwipeRefreshLayout;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;

public class EmailListFragment extends AuthenticatedListFragment implements
        LoaderManager.LoaderCallbacks<List<Email>>,
        MoveToDialogFragment.MoveToDialogListener,
        EmailListAdapter.EmailSelector, SwipeRefreshLayout.OnRefreshListener {
    public static final String FOLDER_NAME = "folder_name";

    private static final int EMAIL_LIST_LOADER = 1;

    OnEmailSelectedListener mCallback;

    private MultiSwipeRefreshLayout mSwipeRefreshLayout;
    private AsyncTask<Void, Void, Void> mCheckingTask;
    private TextView mEmptyText;
    private TextView mNumIncompleteEmails;

    private EmailListAdapter mAdapter;
    private EmailFolder mFolder;

    private ImageButton mNewEmail;
    private MenuItem mCheckEmail;

    // The Controller which provides CHOICE_MODE_MULTIPLE_MODAL-like functionality
    private MultiSelectionUtil.Controller mMultiSelectController;
    private ModalChoiceListener mModalChoiceListener;

    public static EmailListFragment newInstance(String folderName) {
        EmailListFragment f = new EmailListFragment();
        Bundle args = new Bundle();
        args.putString(FOLDER_NAME, folderName);
        f.setArguments(args);
        return f;
    }

    // Container Activity must implement this interface
    public interface OnEmailSelectedListener {
        public void onEmailSelected(String folderName, String messageId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnEmailSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnEmailSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create the list fragment's content view by calling the super method
        final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);

        String folderName = getArguments().getString(FOLDER_NAME);
        mFolder = BoteHelper.getMailFolder(folderName);
        boolean isInbox = BoteHelper.isInbox(mFolder);

        View v = inflater.inflate(
                isInbox ? R.layout.fragment_list_emails_with_refresh : R.layout.fragment_list_emails,
                container, false);
        FrameLayout listContainer = (FrameLayout) v.findViewById(R.id.list_container);
        listContainer.addView(listFragmentView);

        mNewEmail = (ImageButton) v.findViewById(R.id.promoted_action);
        mNewEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewEmail();
            }
        });

        if (isInbox) {
            mSwipeRefreshLayout = (MultiSwipeRefreshLayout) v;

            // Set up the empty view
            View emptyView = mSwipeRefreshLayout.findViewById(android.R.id.empty);
            ListView listView = (ListView) mSwipeRefreshLayout.findViewById(android.R.id.list);
            listView.setEmptyView(emptyView);
            mEmptyText = (TextView) mSwipeRefreshLayout.findViewById(R.id.empty_text);

            // Set up the MultiSwipeRefreshLayout
            mSwipeRefreshLayout.setColorSchemeResources(
                    R.color.primary, R.color.accent, R.color.primary, R.color.accent);
            mSwipeRefreshLayout.setSwipeableChildren(android.R.id.list, android.R.id.empty);
            mSwipeRefreshLayout.setOnRefreshListener(this);
        }

        return v;
    }

    @Override
    public void setEmptyText(CharSequence text) {
        if (mEmptyText == null)
            super.setEmptyText(text);
        else
            mEmptyText.setText(text);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new EmailListAdapter(getActivity(), this,
                BoteHelper.isOutbox(mFolder));

        setListAdapter(mAdapter);

        // Attach a MultiSelectionUtil.Controller to the ListView, giving it an instance of
        // ModalChoiceListener (see below)
        mModalChoiceListener = new ModalChoiceListener();
        mMultiSelectController = MultiSelectionUtil
                .attachMultiSelectionController(getListView(), (ActionBarActivity) getActivity(),
                        mModalChoiceListener);

        // Allow the Controller to restore itself
        mMultiSelectController.restoreInstanceState(savedInstanceState);

        if (mFolder == null) {
            setEmptyText(getResources().getString(
                    R.string.folder_does_not_exist));
            getActivity().setTitle(getResources().getString(R.string.app_name));
        } else {
            getActivity().setTitle(
                    BoteHelper.getFolderDisplayName(getActivity(), mFolder));
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mSwipeRefreshLayout != null) {
            boolean isChecking = I2PBote.getInstance().isCheckingForMail();
            mSwipeRefreshLayout.setRefreshing(isChecking);
            if (isChecking)
                onRefresh();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mCheckingTask != null) {
            mCheckingTask.cancel(true);
            mCheckingTask = null;
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * Start loading the list of emails from this folder.
     * Only called when we have a password cached, or no
     * password is required.
     */
    protected void onInitializeList() {
        if (mFolder == null)
            return;

        if (BoteHelper.isInbox(mFolder)) {
            int numIncompleteEmails = I2PBote.getInstance().getNumIncompleteEmails();
            if (numIncompleteEmails > 0) {
                mNumIncompleteEmails = (TextView) getActivity().getLayoutInflater().inflate(
                        R.layout.listitem_incomplete, getListView(), false);
                mNumIncompleteEmails.setText(getResources().getQuantityString(R.plurals.incomplete_emails,
                        numIncompleteEmails, numIncompleteEmails));
                getListView().addHeaderView(mNumIncompleteEmails, null, false);
            }
        }

        setListShown(false);
        setEmptyText(getResources().getString(
                R.string.folder_empty));
        getLoaderManager().initLoader(EMAIL_LIST_LOADER, null, this);
    }

    protected void onDestroyList() {
        if (mNumIncompleteEmails != null) {
            getListView().removeHeaderView(mNumIncompleteEmails);
            mNumIncompleteEmails = null;
        }

        getLoaderManager().destroyLoader(EMAIL_LIST_LOADER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Allow the Controller to save it's instance state so that any checked items are
        // stored
        if (mMultiSelectController != null)
            mMultiSelectController.saveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.email_list, menu);
        mCheckEmail = menu.findItem(R.id.action_check_email);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean passwordRequired = I2PBote.getInstance().isPasswordRequired();
        mNewEmail.setVisibility(passwordRequired ? View.GONE : View.VISIBLE);
        mCheckEmail.setVisible(mSwipeRefreshLayout != null && !passwordRequired);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(!passwordRequired);
            if (mSwipeRefreshLayout.isRefreshing()) {
                mCheckEmail.setTitle(R.string.checking_email);
                mCheckEmail.setEnabled(false);
            } else {
                mCheckEmail.setTitle(R.string.check_email);
                mCheckEmail.setEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_check_email:
                if (!mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(true);
                    onRefresh();
                    getActivity().supportInvalidateOptionsMenu();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startNewEmail() {
        Intent nei = new Intent(getActivity(), NewEmailActivity.class);
        startActivity(nei);
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        super.onListItemClick(parent, view, pos, id);
        final Email email = (Email) getListView().getItemAtPosition(pos);
        if (email != null)
            mCallback.onEmailSelected(mFolder.getName(), email.getMessageID());
    }

    private class ModalChoiceListener implements MultiSelectionUtil.MultiChoiceModeListener {
        private boolean areUnread;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            final ListView listView = getListView();
            int numChecked = 0;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                final SparseBooleanArray items = listView.getCheckedItemPositions();
                for (int i = 0; i < items.size(); i++)
                    if (items.valueAt(i))
                        numChecked++;
            } else
                numChecked = listView.getCheckedItemCount();

            mode.setTitle(getResources().getString(R.string.items_selected, numChecked));

            if (checked && numChecked == 1) { // This is the first checked item
                Email email = (Email) listView.getItemAtPosition(position);
                areUnread = email.isUnread();
                mode.invalidate();
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final ListView listView = getListView();
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.action_delete:
                    SparseBooleanArray toDelete = listView.getCheckedItemPositions();
                    if (toDelete.size() == 0)
                        return false;

                    for (int i = (toDelete.size() - 1); i >= 0; i--) {
                        if (toDelete.valueAt(i)) {
                            Email email = (Email) listView.getItemAtPosition(toDelete.keyAt(i));
                            BoteHelper.revokeAttachmentUriPermissions(
                                    getActivity(),
                                    mFolder.getName(),
                                    email);
                            // The Loader will update mAdapter
                            I2PBote.getInstance().deleteEmail(mFolder, email.getMessageID());
                        }
                    }
                    mode.finish();
                    return true;

                case R.id.action_mark_read:
                case R.id.action_mark_unread:
                    SparseBooleanArray selected = listView.getCheckedItemPositions();
                    for (int i = (selected.size() - 1); i >= 0; i--) {
                        if (selected.valueAt(i)) {
                            Email email = (Email) listView.getItemAtPosition(selected.keyAt(i));
                            try {
                                // The Loader will update mAdapter
                                mFolder.setNew(email, !areUnread);
                            } catch (PasswordException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (GeneralSecurityException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    areUnread = !areUnread;
                    mode.invalidate();
                    return true;

                case R.id.action_move_to:
                    DialogFragment f = MoveToDialogFragment.newInstance(mFolder);
                    f.show(getFragmentManager(), "moveTo");
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.email_list_context, menu);
            if (BoteHelper.isOutbox(mFolder)) {
                menu.findItem(R.id.action_mark_read).setVisible(false);
                menu.findItem(R.id.action_mark_unread).setVisible(false);
            }
            // Only allow moving from the trash
            // TODO change this when user folders are implemented
            if (!BoteHelper.isTrash(mFolder))
                menu.findItem(R.id.action_move_to).setVisible(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Here you can perform updates to the CAB due to
            // an invalidate() request
            if (!BoteHelper.isOutbox(mFolder)) {
                menu.findItem(R.id.action_mark_read).setVisible(areUnread);
                menu.findItem(R.id.action_mark_unread).setVisible(!areUnread);
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    }

    // Called by EmailListActivity.onFolderSelected()

    public void onFolderSelected(EmailFolder newFolder) {
        final ListView listView = getListView();
        SparseBooleanArray toMove = listView.getCheckedItemPositions();
        for (int i = (toMove.size() - 1); i >= 0; i--) {
            if (toMove.valueAt(i)) {
                Email email = (Email) listView.getItemAtPosition(toMove.keyAt(i));
                mFolder.move(email, newFolder);
            }
        }
        mMultiSelectController.finish();
    }

    // LoaderManager.LoaderCallbacks<List<Email>>

    public Loader<List<Email>> onCreateLoader(int id, Bundle args) {
        return new EmailListLoader(getActivity(), mFolder);
    }

    private static class EmailListLoader extends BetterAsyncTaskLoader<List<Email>> implements
            FolderListener {
        private EmailFolder mFolder;

        public EmailListLoader(Context context, EmailFolder folder) {
            super(context);
            mFolder = folder;
        }

        @Override
        public List<Email> loadInBackground() {
            List<Email> emails = null;
            try {
                emails = BoteHelper.getEmails(mFolder, null, true);
            } catch (PasswordException pe) {
                // XXX: Should not get here.
            }
            return emails;
        }

        protected void onStartMonitoring() {
            mFolder.addFolderListener(this);
        }

        protected void onStopMonitoring() {
            mFolder.removeFolderListener(this);
        }

        protected void releaseResources(List<Email> data) {
        }

        // FolderListener

        @Override
        public void elementAdded(String messageId) {
            onContentChanged();
        }

        @Override
        public void elementUpdated() {
            onContentChanged();
        }

        @Override
        public void elementRemoved(String messageId) {
            onContentChanged();
        }
    }

    public void onLoadFinished(Loader<List<Email>> loader,
                               List<Email> data) {
        // Clear recent flags
        for (Email email : data)
            try {
                email.setFlag(Flag.RECENT, false);
            } catch (MessagingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        mAdapter.setData(data);
        try {
            getActivity().setTitle(
                    BoteHelper.getFolderDisplayNameWithNew(getActivity(), mFolder));
        } catch (PasswordException e) {
            // Should not get here.
            Log log = I2PAppContext.getGlobalContext().logManager().getLog(EmailListFragment.class);
            if (log.shouldLog(Log.WARN))
                log.warn("Email list loader finished, but password is no longer cached", e);
        }

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public void onLoaderReset(Loader<List<Email>> loader) {
        mAdapter.setData(null);
        getActivity().setTitle(
                BoteHelper.getFolderDisplayName(getActivity(), mFolder));
    }

    // EmailListAdapter.EmailSelector

    public boolean inActionMode() {
        return mMultiSelectController.inActionMode();
    }

    public void select(View view) {
        final ListView listView = getListView();
        final int position = listView.getPositionForView(view);
        listView.setItemChecked(position, !listView.isItemChecked(position));
        view.performLongClick();
    }

    // SwipeRefreshLayout.OnRefreshListener

    public void onRefresh() {
        // If we are already checking, do nothing else
        if (mCheckingTask != null)
            return;

        I2PBote bote = I2PBote.getInstance();
        if (bote.isConnected()) {
            try {
                if (!bote.isCheckingForMail())
                    bote.checkForMail();

                mCheckingTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        while (I2PBote.getInstance().isCheckingForMail()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                            if (isCancelled()) {
                                break;
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        super.onPostExecute(result);

                        int numIncomingEmails = I2PBote.getInstance().getNumIncompleteEmails();
                        if (numIncomingEmails > 0) {
                            if (mNumIncompleteEmails == null) {
                                mNumIncompleteEmails = new TextView(getActivity());
                                getListView().addHeaderView(mNumIncompleteEmails);
                            }
                            mNumIncompleteEmails.setText(getResources().getQuantityString(
                                    R.plurals.incomplete_emails,
                                    numIncomingEmails, numIncomingEmails));
                        } else if (mNumIncompleteEmails != null) {
                            getListView().removeHeaderView(mNumIncompleteEmails);
                            mNumIncompleteEmails = null;
                        }

                        // Notify PullToRefreshLayout that the refresh has finished
                        mSwipeRefreshLayout.setRefreshing(false);
                        getActivity().supportInvalidateOptionsMenu();
                    }
                };
                mCheckingTask.execute();
            } catch (PasswordException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.bote_needs_to_be_connected, Toast.LENGTH_SHORT).show();
        }
    }
}

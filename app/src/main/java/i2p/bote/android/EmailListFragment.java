package i2p.bote.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.pnikosis.materialishprogress.ProgressWheel;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import i2p.bote.I2PBote;
import i2p.bote.android.util.AuthenticatedFragment;
import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.MoveToDialogFragment;
import i2p.bote.android.util.MultiSelectionUtil;
import i2p.bote.android.widget.DividerItemDecoration;
import i2p.bote.android.widget.LoadingRecyclerView;
import i2p.bote.android.widget.MultiSwipeRefreshLayout;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;

public class EmailListFragment extends AuthenticatedFragment implements
        LoaderManager.LoaderCallbacks<List<Email>>,
        MoveToDialogFragment.MoveToDialogListener,
        SwipeRefreshLayout.OnRefreshListener {
    public static final String FOLDER_NAME = "folder_name";

    private static final int EMAIL_LIST_LOADER = 1;

    OnEmailSelectedListener mCallback;

    private MultiSwipeRefreshLayout mSwipeRefreshLayout;
    private AsyncTask<Void, Void, Void> mCheckingTask;

    private LoadingRecyclerView mEmailsList;
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
    public View onCreateAuthenticatedView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String folderName = getArguments().getString(FOLDER_NAME);
        mFolder = BoteHelper.getMailFolder(folderName);
        boolean isInbox = BoteHelper.isInbox(mFolder);

        View v = inflater.inflate(
                isInbox ? R.layout.fragment_list_emails_with_refresh : R.layout.fragment_list_emails,
                container, false);

        mEmailsList = (LoadingRecyclerView) v.findViewById(R.id.emails_list);
        View empty = v.findViewById(R.id.empty);
        ProgressWheel loading = (ProgressWheel) v.findViewById(R.id.loading);
        mEmailsList.setLoadingView(empty, loading);

        mNewEmail = (ImageButton) v.findViewById(R.id.promoted_action);
        mNewEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewEmail();
            }
        });

        if (isInbox) {
            mSwipeRefreshLayout = (MultiSwipeRefreshLayout) v;

            // Set up the MultiSwipeRefreshLayout
            mSwipeRefreshLayout.setColorSchemeResources(
                    R.color.primary, R.color.accent, R.color.primary, R.color.accent);
            mSwipeRefreshLayout.setSwipeableChildren(R.id.emails_list);
            mSwipeRefreshLayout.setOnRefreshListener(this);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mEmailsList.setHasFixedSize(true);
        mEmailsList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        // Use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mEmailsList.setLayoutManager(mLayoutManager);

        // Set the adapter for the list view
        mAdapter = new EmailListAdapter(getActivity(), mFolder.getName(), mCallback);
        mEmailsList.setAdapter(mAdapter);

        // Attach a MultiSelectionUtil.Controller to the ListView, giving it an instance of
        // ModalChoiceListener (see below)
        mModalChoiceListener = new ModalChoiceListener();
        mMultiSelectController = MultiSelectionUtil
                .attachMultiSelectionController(mEmailsList, (AppCompatActivity) getActivity(),
                        mModalChoiceListener);

        // Allow the Controller to restore itself
        mMultiSelectController.restoreInstanceState(savedInstanceState);

        if (mFolder == null) {
            mFolder = I2PBote.getInstance().getInbox();
            Toast.makeText(getActivity(), R.string.folder_does_not_exist, Toast.LENGTH_SHORT).show();
        }

        getActivity().setTitle(
                BoteHelper.getFolderDisplayName(getActivity(), mFolder));
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
    protected void onInitializeFragment() {
        if (mFolder == null)
            return;

        if (BoteHelper.isInbox(mFolder)) {
            mAdapter.setIncompleteEmails(I2PBote.getInstance().getNumIncompleteEmails());
        }

        getLoaderManager().initLoader(EMAIL_LIST_LOADER, null, this);
    }

    protected void onDestroyFragment() {
        mAdapter.setIncompleteEmails(0);

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

    private class ModalChoiceListener implements MultiSelectionUtil.MultiChoiceModeListener {
        private boolean areUnread;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            int numChecked = mAdapter.getSelectedItemCount();

            mode.setTitle(getResources().getString(R.string.items_selected, numChecked));

            if (checked && numChecked == 1) { // This is the first checked item
                Email email = mAdapter.getEmail(position);
                areUnread = email.isUnread();
                mode.invalidate();
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.action_delete:
                    List<Integer> toDelete = mAdapter.getSelectedItems();
                    if (toDelete.size() == 0)
                        return false;

                    for (int i = (toDelete.size() - 1); i >= 0; i--) {
                        Email email = mAdapter.getEmail(toDelete.get(i));
                        BoteHelper.revokeAttachmentUriPermissions(
                                getActivity(),
                                mFolder.getName(),
                                email);
                        // The Loader will update mAdapter
                        I2PBote.getInstance().deleteEmail(mFolder, email.getMessageID());
                    }
                    mode.finish();
                    return true;

                case R.id.action_mark_read:
                case R.id.action_mark_unread:
                    List<Integer> selected = mAdapter.getSelectedItems();
                    for (int i = (selected.size() - 1); i >= 0; i--) {
                        Email email = mAdapter.getEmail(selected.get(i));
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
            MenuItem markRead = menu.findItem(R.id.action_mark_read);
            MenuItem markUnread = menu.findItem(R.id.action_mark_unread);
            MenuItem moveTo = menu.findItem(R.id.action_move_to);

            menu.findItem(R.id.action_delete).setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_delete));
            markRead.setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_drafts));
            markUnread.setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_markunread));
            moveTo.setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_folder));

            if (BoteHelper.isOutbox(mFolder)) {
                markRead.setVisible(false);
                markUnread.setVisible(false);
            }
            // Only allow moving from the trash
            // TODO change this when user folders are implemented
            if (!BoteHelper.isTrash(mFolder))
                moveTo.setVisible(false);
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

    // Called by EmailListActivity.onIdentitySelected()

    public void onIdentitySelected() {
        getLoaderManager().restartLoader(EMAIL_LIST_LOADER, null, this);
    }

    // Called by EmailListActivity.onFolderSelected()

    public void onFolderSelected(EmailFolder newFolder) {
        List<Integer> toMove = mAdapter.getSelectedItems();
        for (int i = (toMove.size() - 1); i >= 0; i--) {
            Email email = mAdapter.getEmail(toMove.get(i));
            mFolder.move(email, newFolder);
        }
        mMultiSelectController.finish();
    }

    // LoaderManager.LoaderCallbacks<List<Email>>

    public Loader<List<Email>> onCreateLoader(int id, Bundle args) {
        return new EmailListLoader(getActivity(), mFolder,
                getActivity().getSharedPreferences(Constants.SHARED_PREFS, 0)
                        .getString(Constants.PREF_SELECTED_IDENTITY, null));
    }

    private static class EmailListLoader extends BetterAsyncTaskLoader<List<Email>> implements
            FolderListener {
        private EmailFolder mFolder;
        private String mSelectedIdentityKey;

        public EmailListLoader(Context context, EmailFolder folder, String selectedIdentityKey) {
            super(context);
            mFolder = folder;
            mSelectedIdentityKey = selectedIdentityKey;
        }

        @Override
        public List<Email> loadInBackground() {
            List<Email> emails = null;
            try {
                List<Email> allEmails = BoteHelper.getEmails(mFolder, null, true);

                if (mSelectedIdentityKey != null) {
                    emails = new ArrayList<>();

                    for (Email email : allEmails) {
                        boolean add = false;
                        if (BoteHelper.isSentEmail(email)) {
                            String senderDest = BoteHelper.extractEmailDestination(email.getOneFromAddress());
                            if (mSelectedIdentityKey.equals(senderDest))
                                add = true;
                        } else {
                            for (Address recipient : email.getAllRecipients()) {
                                String recipientDest = BoteHelper.extractEmailDestination(recipient.toString());
                                if (mSelectedIdentityKey.equals(recipientDest)) {
                                    add = true;
                                    break;
                                }
                            }
                        }
                        if (add)
                            emails.add(email);
                    }
                } else
                    emails = allEmails;
            } catch (PasswordException pe) {
                // XXX: Should not get here.
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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
        mAdapter.setEmails(data);
        try {
            getActivity().setTitle(
                    BoteHelper.getFolderDisplayNameWithNew(getActivity(), mFolder));
        } catch (PasswordException e) {
            // Should not get here.
            Log log = I2PAppContext.getGlobalContext().logManager().getLog(EmailListFragment.class);
            if (log.shouldLog(Log.WARN))
                log.warn("Email list loader finished, but password is no longer cached", e);
        } catch (MessagingException | GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public void onLoaderReset(Loader<List<Email>> loader) {
        mAdapter.setEmails(null);
        getActivity().setTitle(
                BoteHelper.getFolderDisplayName(getActivity(), mFolder));
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

                        mAdapter.setIncompleteEmails(I2PBote.getInstance().getNumIncompleteEmails());

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

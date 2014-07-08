package i2p.bote.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import i2p.bote.I2PBote;
import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.MoveToDialogFragment;
import i2p.bote.android.util.MultiSelectionUtil;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class EmailListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<Email>>,
        MoveToDialogFragment.MoveToDialogListener,
        EmailListAdapter.EmailSelector, OnRefreshListener {
    public static final String FOLDER_NAME = "folder_name";

    private static final int EMAIL_LIST_LOADER = 1;

    OnEmailSelectedListener mCallback;

    private PullToRefreshLayout mPullToRefreshLayout;
    private TextView mNumIncompleteEmails;

    private EmailListAdapter mAdapter;
    private EmailFolder mFolder;

    // The Controller which provides CHOICE_MODE_MULTIPLE_MODAL-like functionality
    private MultiSelectionUtil.Controller mMultiSelectController;
    private ModalChoiceListener mModalChoiceListener;

    private EditText mPasswordInput;
    private TextView mPasswordError;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String folderName = getArguments().getString(FOLDER_NAME);
        mFolder = BoteHelper.getMailFolder(folderName);

        if (BoteHelper.isInbox(mFolder)) {
            // This is the View which is created by ListFragment
            ViewGroup viewGroup = (ViewGroup) view;

            // We need to create a PullToRefreshLayout manually
            mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());

            // We can now setup the PullToRefreshLayout
            ActionBarPullToRefresh.from(getActivity())

                    // We need to insert the PullToRefreshLayout into the Fragment's ViewGroup
                    .insertLayoutInto(viewGroup)

                            // We need to mark the ListView and it's Empty View as pullable
                            // This is because they are not dirent children of the ViewGroup
                    .theseChildrenArePullable(getListView(), getListView().getEmptyView())

                            // We can now complete the setup as desired
                    .listener(this)
                    .options(Options.create()
                            .refreshOnUp(true)
                            .build())
                    .setup(mPullToRefreshLayout);

            mPullToRefreshLayout.setRefreshing(I2PBote.getInstance().isCheckingForMail());

            int numIncompleteEmails = I2PBote.getInstance().getNumIncompleteEmails();
            if (numIncompleteEmails > 0) {
                mNumIncompleteEmails = new TextView(getActivity());
                mNumIncompleteEmails.setText(getResources().getString(R.string.incomplete_emails,
                        numIncompleteEmails));
                mNumIncompleteEmails.setPadding(16, 5, 16, 5);
                getListView().addHeaderView(mNumIncompleteEmails, null, false);
            }
        }
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
            if (I2PBote.getInstance().isPasswordRequired()) {
                // Request a password from the user.
                requestPassword();
            } else {
                // Password is cached, or not set.
                initializeList();
            }
        }
    }

    /**
     * Request the password from the user, and try it.
     */
    private void requestPassword() {
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptView = li.inflate(R.layout.dialog_password, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(promptView);
        mPasswordInput = (EditText) promptView.findViewById(R.id.passwordInput);
        mPasswordError = (TextView) promptView.findViewById(R.id.passwordError);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                InputMethodManager imm = (InputMethodManager) EmailListFragment.this
                        .getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mPasswordInput.getWindowToken(), 0);
                dialog.dismiss();
                new PasswordWaiter().execute();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                setEmptyText(getResources().getString(
                        R.string.not_authed));
                dialog.cancel();
            }
        });
        AlertDialog passwordDialog = builder.create();
        passwordDialog.show();
    }

    private class PasswordWaiter extends AsyncTask<Void, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(EmailListFragment.this.getActivity());

        protected void onPreExecute() {
            dialog.setMessage(getResources().getString(
                    R.string.checking_password));
            dialog.setCancelable(false);
            dialog.show();
        }

        protected String doInBackground(Void... params) {
            try {
                if (BoteHelper.tryPassword(mPasswordInput.getText().toString()))
                    return null;
                else {
                    cancel(false);
                    return getResources().getString(
                            R.string.password_incorrect);
                }
            } catch (IOException e) {
                cancel(false);
                return getResources().getString(
                        R.string.password_file_error);
            } catch (GeneralSecurityException e) {
                cancel(false);
                return getResources().getString(
                        R.string.password_file_error);
            }
        }

        protected void onCancelled(String result) {
            dialog.dismiss();
            requestPassword();
            mPasswordError.setText(result);
            mPasswordError.setVisibility(View.VISIBLE);
        }

        protected void onPostExecute(String result) {
            // Password is valid
            initializeList();
            dialog.dismiss();
        }
    }

    /**
     * Start loading the list of emails from this folder.
     * Only called when we have a password cached, or no
     * password is required.
     */
    private void initializeList() {
        setListShown(false);
        setEmptyText(getResources().getString(
                R.string.folder_empty));
        getLoaderManager().initLoader(EMAIL_LIST_LOADER, null, this);
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
        inflater.inflate(R.menu.email_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_email:
                Intent nei = new Intent(getActivity(), NewEmailActivity.class);
                startActivity(nei);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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

    public void select(View view) {
        // TODO temporarily disabled while broken, need to fix
        //final ListView listView = getListView();
        //final int position = listView.getPositionForView(view);
        //listView.setItemChecked(position, !listView.isItemChecked(position));
        //view.performLongClick();
    }

    // OnRefreshListener

    public void onRefreshStarted(View view) {
        I2PBote bote = I2PBote.getInstance();
        if (bote.isConnected()) {
            try {
                if (!bote.isCheckingForMail())
                    bote.checkForMail();

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        while (I2PBote.getInstance().isCheckingForMail()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
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
                            mNumIncompleteEmails.setText(getResources().getString(R.string.incomplete_emails,
                                    numIncomingEmails));
                        } else if (mNumIncompleteEmails != null) {
                            getListView().removeHeaderView(mNumIncompleteEmails);
                            mNumIncompleteEmails = null;
                        }

                        // Notify PullToRefreshLayout that the refresh has finished
                        mPullToRefreshLayout.setRefreshComplete();
                    }
                }.execute();
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
        } else
            mPullToRefreshLayout.setRefreshComplete();
    }
}

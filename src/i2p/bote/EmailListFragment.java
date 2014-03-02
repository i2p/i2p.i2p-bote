package i2p.bote;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;

import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import i2p.bote.util.BetterAsyncTaskLoader;
import i2p.bote.util.BoteHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class EmailListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<Email>> {
    public static final String FOLDER_NAME = "folder_name";

    private static final int EMAIL_LIST_LOADER = 1;

    OnEmailSelectedListener mCallback;

    private EmailListAdapter mAdapter;
    private EmailFolder mFolder;

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new EmailListAdapter(getActivity());
        String folderName = getArguments().getString(FOLDER_NAME);
        mFolder = BoteHelper.getMailFolder(folderName);

        setListAdapter(mAdapter);

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
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        super.onListItemClick(parent, view, pos, id);
        mCallback.onEmailSelected(
                mFolder.getName(), mAdapter.getItem(pos).getMessageID());
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
        public void elementAdded() {
            onContentChanged();
        }

        @Override
        public void elementRemoved() {
            onContentChanged();
        }
    }

    public void onLoadFinished(Loader<List<Email>> loader,
            List<Email> data) {
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
}

package i2p.bote;

import java.util.List;

import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import i2p.bote.util.BetterAsyncTaskLoader;
import i2p.bote.util.BoteHelper;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

public class EmailListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<Email>> {
    public static final String FOLDER_NAME = "folder_name";

    private static final int EMAIL_LIST_LOADER = 1;

    OnEmailSelectedListener mCallback;

    private EmailListAdapter mAdapter;
    private EmailFolder mFolder;

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
            setListShown(false);
            setEmptyText(getResources().getString(
                    R.string.folder_empty));
            try {
                getActivity().setTitle(
                        BoteHelper.getFolderDisplayName(getActivity(), mFolder, false));
            } catch (PasswordException e) {
                // TODO: Get password from user and retry
                getActivity().setTitle("ERROR: " + e.getMessage());
            }
            getLoaderManager().initLoader(EMAIL_LIST_LOADER, null, this);
        }
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
                // TODO: Handle this error properly (get user to log in)
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
                    BoteHelper.getFolderDisplayName(getActivity(), mFolder, true));
        } catch (PasswordException e) {
            // TODO: Get password from user and retry
            getActivity().setTitle("ERROR: " + e.getMessage());
        }

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public void onLoaderReset(Loader<List<Email>> loader) {
        mAdapter.setData(null);
        try {
            getActivity().setTitle(
                    BoteHelper.getFolderDisplayName(getActivity(), mFolder, false));
        } catch (PasswordException e) {
            // TODO: Get password from user and retry
            getActivity().setTitle("ERROR: " + e.getMessage());
        }
    }
}

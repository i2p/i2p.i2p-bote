package i2p.bote.android.addressbook;

import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;

import java.util.SortedSet;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class AddressBookFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<SortedSet<Contact>> {
    OnContactSelectedListener mCallback;
    private ContactAdapter mAdapter;

    // Container Activity must implement this interface
    public interface OnContactSelectedListener {
        public void onContactSelected(Contact contact);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnContactSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnContactSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new ContactAdapter(getActivity());

        setListAdapter(mAdapter);

        setListShown(false);
        setEmptyText(getResources().getString(
                R.string.address_book_empty));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.address_book_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_new_contact:
            // TODO
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        mCallback.onContactSelected(mAdapter.getItem(pos));
    }

    // LoaderManager.LoaderCallbacks<SortedSet<Contact>>

    public Loader<SortedSet<Contact>> onCreateLoader(int id, Bundle args) {
        return new AddressBookLoader(getActivity());
    }

    private static class AddressBookLoader extends BetterAsyncTaskLoader<SortedSet<Contact>> {
        public AddressBookLoader(Context context) {
            super(context);
        }

        @Override
        public SortedSet<Contact> loadInBackground() {
            SortedSet<Contact> contacts = null;
            try {
                contacts = I2PBote.getInstance().getAddressBook().getAll();
            } catch (PasswordException e) {
                // TODO handle, but should not get here
                e.printStackTrace();
            }
            return contacts;
        }

        @Override
        protected void onStartMonitoring() {
        }

        @Override
        protected void onStopMonitoring() {
        }

        @Override
        protected void releaseResources(SortedSet<Contact> data) {
        }
    }

    @Override
    public void onLoadFinished(Loader<SortedSet<Contact>> loader,
            SortedSet<Contact> data) {
        mAdapter.setData(data);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<SortedSet<Contact>> loader) {
        mAdapter.setData(null);
    }
}

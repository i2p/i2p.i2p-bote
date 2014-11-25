package i2p.bote.android.addressbook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;

import java.util.SortedSet;

import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.util.AuthenticatedListFragment;
import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;

public class AddressBookFragment extends AuthenticatedListFragment implements
        LoaderManager.LoaderCallbacks<SortedSet<Contact>> {
    OnContactSelectedListener mCallback;
    private ContactAdapter mAdapter;

    private View mPromotedActions;

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
        // Only so we can show/hide the FAM
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create the list fragment's content view by calling the super method
        final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_list_contacts, container, false);
        FrameLayout listContainer = (FrameLayout) v.findViewById(R.id.list_container);
        listContainer.addView(listFragmentView);

        mPromotedActions = v.findViewById(R.id.promoted_actions);

        ImageButton b = (ImageButton) v.findViewById(R.id.action_new_contact);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewContact();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new ContactAdapter(getActivity());

        setListAdapter(mAdapter);
    }

    /**
     * Start loading the address book.
     * Only called when we have a password cached, or no
     * password is required.
     */
    protected void onInitializeList() {
        setListShown(false);
        setEmptyText(getResources().getString(
                R.string.address_book_empty));
        getLoaderManager().initLoader(0, null, this);
    }

    protected void onDestroyList() {
        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean passwordRequired = I2PBote.getInstance().isPasswordRequired();
        mPromotedActions.setVisibility(passwordRequired ? View.GONE : View.VISIBLE);
    }

    private void startNewContact() {
        Intent nci = new Intent(getActivity(), EditContactActivity.class);
        getActivity().startActivityForResult(nci, AddressBookActivity.ALTER_CONTACT_LIST);
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        mCallback.onContactSelected(mAdapter.getItem(pos));
    }

    protected void updateContactList() {
        setListShown(false);
        getLoaderManager().restartLoader(0, null, this);
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

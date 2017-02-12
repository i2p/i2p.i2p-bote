package i2p.bote.android.addressbook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.google.zxing.integration.android.IntentIntegrator;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.SortedSet;

import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.util.AuthenticatedFragment;
import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.android.widget.DividerItemDecoration;
import i2p.bote.android.widget.LoadingRecyclerView;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;

public class AddressBookFragment extends AuthenticatedFragment implements
        LoaderManager.LoaderCallbacks<SortedSet<Contact>> {
    OnContactSelectedListener mCallback;
    private LoadingRecyclerView mContactsList;
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
    public View onCreateAuthenticatedView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_contacts, container, false);

        mContactsList = (LoadingRecyclerView) v.findViewById(R.id.contacts_list);
        View empty = v.findViewById(R.id.empty);
        ProgressWheel loading = (ProgressWheel) v.findViewById(R.id.loading);
        mContactsList.setLoadingView(empty, loading);
        mPromotedActions = v.findViewById(R.id.promoted_actions);

        ImageButton b = (ImageButton) v.findViewById(R.id.action_new_contact);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewContact();
            }
        });

        b = (ImageButton) v.findViewById(R.id.action_scan_qr_code);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanQrCode();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContactsList.setHasFixedSize(true);
        mContactsList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        // Use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mContactsList.setLayoutManager(mLayoutManager);

        // Set the adapter for the list view
        mAdapter = new ContactAdapter(getActivity(), mCallback);
        mContactsList.setAdapter(mAdapter);
    }

    /**
     * Start loading the address book.
     * Only called when we have a password cached, or no
     * password is required.
     */
    protected void onInitializeFragment() {
        getLoaderManager().initLoader(0, null, this);
    }

    protected void onDestroyFragment() {
        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.address_book, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean passwordRequired = I2PBote.getInstance().isPasswordRequired();
        menu.findItem(R.id.export_address_book).setVisible(!passwordRequired);
        menu.findItem(R.id.import_address_book).setVisible(!passwordRequired);
        mPromotedActions.setVisibility(passwordRequired ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_address_book:
                Intent ei = new Intent(getActivity(), AddressBookShipActivity.class);
                ei.putExtra(AddressBookShipActivity.EXPORTING, true);
                startActivity(ei);
                return true;
            case R.id.import_address_book:
                Intent ii = new Intent(getActivity(), AddressBookShipActivity.class);
                ii.putExtra(AddressBookShipActivity.EXPORTING, false);
                startActivity(ii);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startNewContact() {
        Intent nci = new Intent(getActivity(), EditContactActivity.class);
        getActivity().startActivityForResult(nci, AddressBookActivity.ALTER_CONTACT_LIST);
    }

    private void startScanQrCode() {
        IntentIntegrator integrator = new IntentIntegrator(getActivity());
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    protected void updateContactList() {
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
        mAdapter.setContacts(data);
    }

    @Override
    public void onLoaderReset(Loader<SortedSet<Contact>> loader) {
        mAdapter.setContacts(null);
    }
}

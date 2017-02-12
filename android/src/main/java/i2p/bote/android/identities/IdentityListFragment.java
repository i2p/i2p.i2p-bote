package i2p.bote.android.identities;

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

import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.util.AuthenticatedFragment;
import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.android.widget.DividerItemDecoration;
import i2p.bote.android.widget.LoadingRecyclerView;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.IdentitiesListener;
import i2p.bote.fileencryption.PasswordException;

public class IdentityListFragment extends AuthenticatedFragment implements
        LoaderManager.LoaderCallbacks<Collection<EmailIdentity>> {
    OnIdentitySelectedListener mCallback;
    private LoadingRecyclerView mIdentitiesList;
    private IdentityAdapter mAdapter;

    private View mNewIdentity;

    // Container Activity must implement this interface
    public interface OnIdentitySelectedListener {
        void onIdentitySelected(EmailIdentity identity);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnIdentitySelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnIdentitySelectedListener");
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
        View v = inflater.inflate(R.layout.fragment_list_identities, container, false);

        mIdentitiesList = (LoadingRecyclerView) v.findViewById(R.id.identities_list);
        View empty = v.findViewById(R.id.empty);
        ProgressWheel loading = (ProgressWheel) v.findViewById(R.id.loading);
        mIdentitiesList.setLoadingView(empty, loading);

        mNewIdentity = v.findViewById(R.id.action_new_identity);
        mNewIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewIdentity();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mIdentitiesList.setHasFixedSize(true);
        mIdentitiesList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        // Use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mIdentitiesList.setLayoutManager(mLayoutManager);

        // Set the adapter for the list view
        mAdapter = new IdentityAdapter(getActivity(), mCallback);
        mIdentitiesList.setAdapter(mAdapter);
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
        inflater.inflate(R.menu.identity_list, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean passwordRequired = I2PBote.getInstance().isPasswordRequired();
        menu.findItem(R.id.export_identities).setVisible(!passwordRequired);
        menu.findItem(R.id.import_identities).setVisible(!passwordRequired);
        mNewIdentity.setVisibility(passwordRequired ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_identities:
                Intent ei = new Intent(getActivity(), IdentityShipActivity.class);
                ei.putExtra(IdentityShipActivity.EXPORTING, true);
                startActivity(ei);
                return true;
            case R.id.import_identities:
                Intent ii = new Intent(getActivity(), IdentityShipActivity.class);
                ii.putExtra(IdentityShipActivity.EXPORTING, false);
                startActivity(ii);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startNewIdentity() {
        Intent nii = new Intent(getActivity(), EditIdentityActivity.class);
        getActivity().startActivity(nii);
    }

    protected void updateIdentityList() {
        getLoaderManager().restartLoader(0, null, this);
    }

    // LoaderManager.LoaderCallbacks<SortedSet<EmailIdentity>>

    public Loader<Collection<EmailIdentity>> onCreateLoader(int id, Bundle args) {
        return new IdentityLoader(getActivity());
    }

    private static class IdentityLoader extends BetterAsyncTaskLoader<Collection<EmailIdentity>> implements IdentitiesListener {
        public IdentityLoader(Context context) {
            super(context);
        }

        @Override
        public Collection<EmailIdentity> loadInBackground() {
            Collection<EmailIdentity> identities = null;
            try {
                identities = I2PBote.getInstance().getIdentities().getAll();
            } catch (PasswordException e) {
                // TODO handle, but should not get here
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return identities;
        }

        @Override
        protected void onStartMonitoring() {
            I2PBote.getInstance().getIdentities().addIdentitiesListener(this);
        }

        @Override
        protected void onStopMonitoring() {
            I2PBote.getInstance().getIdentities().removeIdentitiesListener(this);
        }

        @Override
        protected void releaseResources(Collection<EmailIdentity> data) {
        }

        // IdentitiesListener

        @Override
        public void identityAdded(String s) {
            onContentChanged();
        }

        @Override
        public void identityUpdated(String s) {
            onContentChanged();
        }

        @Override
        public void identityRemoved(String s) {
            onContentChanged();
        }
    }

    @Override
    public void onLoadFinished(Loader<Collection<EmailIdentity>> loader,
            Collection<EmailIdentity> data) {
        mAdapter.setIdentities(data);
    }

    @Override
    public void onLoaderReset(Loader<Collection<EmailIdentity>> loader) {
        mAdapter.setIdentities(null);
    }
}

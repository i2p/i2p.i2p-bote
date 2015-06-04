package i2p.bote.android.util;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import i2p.bote.I2PBote;
import i2p.bote.android.R;

public abstract class AuthenticatedFragment extends Fragment {
    private FrameLayout mAuthenticatedView;
    private MenuItem mLogIn;
    private MenuItem mClearPassword;
    private boolean mFragmentInitialized;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_authenticated, container, false);

        mAuthenticatedView = (FrameLayout) view.findViewById(R.id.authenticated_view);
        mAuthenticatedView.addView(onCreateAuthenticatedView(inflater, container, savedInstanceState));

        return view;
    }

    protected abstract View onCreateAuthenticatedView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    @Override
    public void onResume() {
        super.onResume();

        if (I2PBote.getInstance().isPasswordRequired()) {
            // Ensure any existing data is destroyed.
            destroyFragment();
        } else {
            // Password is cached, or not set.
            initializeFragment();
        }

        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.authenticated_fragment, menu);
        mLogIn = menu.findItem(R.id.action_log_in);
        mClearPassword = menu.findItem(R.id.action_log_out);

        mLogIn.setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_lock));
        mClearPassword.setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_lock_open));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mLogIn.setVisible(I2PBote.getInstance().isPasswordRequired());
        mClearPassword.setVisible(I2PBote.getInstance().isPasswordInCache());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_log_in:
                // Request a password from the user.
                BoteHelper.requestPassword(getActivity(), new BoteHelper.RequestPasswordListener() {
                    @Override
                    public void onPasswordVerified() {
                        initializeFragment();
                        getActivity().supportInvalidateOptionsMenu();
                    }

                    @Override
                    public void onPasswordCanceled() {
                    }
                });
                return true;

            case R.id.action_log_out:
                BoteHelper.clearPassword();
                destroyFragment();
                getActivity().supportInvalidateOptionsMenu();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initializeFragment() {
        if (mFragmentInitialized)
            return;

        onInitializeFragment();

        mAuthenticatedView.setVisibility(View.VISIBLE);

        mFragmentInitialized = true;
    }

    private void destroyFragment() {
        onDestroyFragment();

        mAuthenticatedView.setVisibility(View.GONE);

        mFragmentInitialized = false;
    }

    protected abstract void onInitializeFragment();
    protected abstract void onDestroyFragment();
}

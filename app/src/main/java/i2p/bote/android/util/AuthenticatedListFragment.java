package i2p.bote.android.util;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import i2p.bote.I2PBote;
import i2p.bote.android.R;

public abstract class AuthenticatedListFragment extends ListFragment {
    private MenuItem mLogIn;
    private MenuItem mClearPassword;
    private boolean mListInitialized;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (I2PBote.getInstance().isPasswordRequired()) {
            // Ensure any existing data is destroyed.
            destroyList();
        } else {
            // Password is cached, or not set.
            initializeList();
        }

        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.authenticated_list, menu);
        mLogIn = menu.findItem(R.id.action_log_in);
        mClearPassword = menu.findItem(R.id.action_log_out);
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
                        initializeList();
                        getActivity().supportInvalidateOptionsMenu();
                    }

                    @Override
                    public void onPasswordCanceled() {
                    }
                });
                return true;

            case R.id.action_log_out:
                BoteHelper.clearPassword();
                destroyList();
                getActivity().supportInvalidateOptionsMenu();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initializeList() {
        if (mListInitialized)
            return;

        onInitializeList();

        mListInitialized = true;
    }

    private void destroyList() {
        onDestroyList();

        setEmptyText(getResources().getString(
                R.string.touch_lock_to_log_in));

        mListInitialized = false;
    }

    protected abstract void onInitializeList();
    protected abstract void onDestroyList();
}

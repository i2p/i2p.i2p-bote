package i2p.bote.android.config;

import java.io.IOException;
import java.security.GeneralSecurityException;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ViewIdentityFragment extends Fragment {
    public static final String IDENTITY_KEY = "identity_key";

    private String mKey;
    private EmailIdentity mIdentity;

    TextView mNameField;
    TextView mDescField;
    TextView mCryptoField;
    TextView mKeyField;

    public static ViewIdentityFragment newInstance(String key) {
        ViewIdentityFragment f = new ViewIdentityFragment();
        Bundle args = new Bundle();
        args.putString(IDENTITY_KEY, key);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_identity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mKey = getArguments().getString(IDENTITY_KEY);
        if (mKey != null) {
            try {
                mIdentity = BoteHelper.getIdentity(mKey);
                mNameField = (TextView) view.findViewById(R.id.public_name);
                mDescField = (TextView) view.findViewById(R.id.description);
                mCryptoField = (TextView) view.findViewById(R.id.crypto_impl);
                mKeyField = (TextView) view.findViewById(R.id.key);
            } catch (PasswordException e) {
                // TODO Handle
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Handle
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                // TODO Handle
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIdentity != null) {
            mNameField.setText(mIdentity.getPublicName());
            mDescField.setText(mIdentity.getDescription());
            mCryptoField.setText(mIdentity.getCryptoImpl().getName());
            mKeyField.setText(mKey);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_identity, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_edit_identity:
            Intent ei = new Intent(getActivity(), EditIdentityActivity.class);
            ei.putExtra(EditIdentityFragment.IDENTITY_KEY, mKey);
            startActivity(ei);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}

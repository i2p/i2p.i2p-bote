package i2p.bote.android.intro;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import i2p.bote.android.BoteActivityBase;
import i2p.bote.android.R;
import i2p.bote.android.config.SetPasswordActivity;
import i2p.bote.android.identities.EditIdentityActivity;

public class SetupActivity extends BoteActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // If a user has chosen to enter the setup wizard, don't let them
        // accidentally exit it early.
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SetPasswordFragment())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        // If a user has chosen to enter the setup wizard, don't let them
        // accidentally exit it early.
        Toast.makeText(this, R.string.please_finish_setup, Toast.LENGTH_SHORT).show();
    }


    /**
     * Set password.
     */
    public static class SetPasswordFragment extends Fragment {
        private static final int SET_PASSWORD = 1;

        public SetPasswordFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_setup_set_password, container, false);
            rootView.findViewById(R.id.button_set_password).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(getActivity(), SetPasswordActivity.class);
                    startActivityForResult(i, SET_PASSWORD);
                }
            });
            rootView.findViewById(R.id.button_skip).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    nextPage();
                }
            });
            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == SET_PASSWORD) {
                if (resultCode == RESULT_OK) {
                    nextPage();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        private void nextPage() {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new CreateIdentityFragment())
                    .commit();
        }
    }

    /**
     * Create identity.
     */
    public static class CreateIdentityFragment extends Fragment {
        private static final int CREATE_IDENTITY = 1;

        public CreateIdentityFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_setup_create_identity, container, false);
            rootView.findViewById(R.id.button_set_password).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(getActivity(), EditIdentityActivity.class);
                    startActivityForResult(i, CREATE_IDENTITY);
                }
            });
            rootView.findViewById(R.id.button_skip).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    nextPage();
                }
            });
            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == CREATE_IDENTITY) {
                if (resultCode == RESULT_OK) {
                    nextPage();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        private void nextPage() {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new SetupFinishedFragment())
                    .commit();
        }
    }

    /**
     * Setup finished.
     */
    public static class SetupFinishedFragment extends Fragment {

        public SetupFinishedFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_setup_finished, container, false);
            rootView.findViewById(R.id.button_finish).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().setResult(RESULT_OK);
                    getActivity().finish();
                }
            });
            return rootView;
        }
    }
}

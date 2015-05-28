package i2p.bote.android.config;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import i2p.bote.android.EmailListActivity;
import i2p.bote.android.InitActivities;
import i2p.bote.android.R;
import i2p.bote.android.identities.IdentityListActivity;

public class SettingsActivity extends AppCompatActivity {
    public static final String PREFERENCE_CATEGORY = "preference_category";
    public static final String PREFERENCE_CATEGORY_GENERAL = "preference_category_general";
    public static final String PREFERENCE_CATEGORY_CHANGE_PASSWORD = "preference_category_change_password";
    public static final String PREFERENCE_CATEGORY_IDENTITIES = "preference_category_identities";


    //
    // Android lifecycle
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Fragment fragment;
        String category = getIntent().getStringExtra(PREFERENCE_CATEGORY);
        if (category != null)
            fragment = getFragmentForCategory(category);
        else
            fragment = new SettingsFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            Intent intent = new Intent(this, EmailListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        return true;
    }


    //
    // Settings pages
    //

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            findPreference(PREFERENCE_CATEGORY_GENERAL)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_GENERAL));
            findPreference(PREFERENCE_CATEGORY_CHANGE_PASSWORD)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_CHANGE_PASSWORD));
            findPreference(PREFERENCE_CATEGORY_IDENTITIES)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_IDENTITIES));
        }

        @Override
        public void onResume() {
            super.onResume();
            ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.action_settings);
        }

        private class CategoryClickListener implements Preference.OnPreferenceClickListener {
            private String category;

            public CategoryClickListener(String category) {
                this.category = category;
            }

            @Override
            public boolean onPreferenceClick(Preference preference) {
                switch (category) {
                    case PREFERENCE_CATEGORY_CHANGE_PASSWORD:
                        Intent spi = new Intent(getActivity(), SetPasswordActivity.class);
                        startActivity(spi);
                        break;

                    case PREFERENCE_CATEGORY_IDENTITIES:
                        Intent ili = new Intent(getActivity(), IdentityListActivity.class);
                        startActivity(ili);
                        break;

                    default:
                        Fragment fragment = getFragmentForCategory(category);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.container, fragment)
                                .addToBackStack(null)
                                .commit();
                }

                return true;
            }
        }
    }

    private static Fragment getFragmentForCategory(String category) {
        switch (category) {
            case PREFERENCE_CATEGORY_GENERAL:
                return new GeneralPreferenceFragment();
            default:
                throw new AssertionError();
        }
    }
}

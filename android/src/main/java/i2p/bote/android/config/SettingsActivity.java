package i2p.bote.android.config;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;

import i2p.bote.I2PBote;
import i2p.bote.android.BoteActivityBase;
import i2p.bote.android.EmailListActivity;
import i2p.bote.android.R;
import i2p.bote.android.identities.IdentityListActivity;
import i2p.bote.android.service.BoteService;
import i2p.bote.android.util.BoteHelper;

public class SettingsActivity extends BoteActivityBase implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREFERENCE_CATEGORY = "preference_category";
    public static final String PREFERENCE_CATEGORY_NETWORK = "preference_category_network";
    public static final String PREFERENCE_CATEGORY_IDENTITIES = "preference_category_identities";
    public static final String PREFERENCE_CATEGORY_PRIVACY = "preference_category_privacy";
    public static final String PREFERENCE_CATEGORY_APP_PROTECTION = "preference_category_app_protection";
    public static final String PREFERENCE_CATEGORY_APPEARANCE = "preference_category_appearance";
    public static final String PREFERENCE_CATEGORY_ADVANCED = "preference_category_advanced";


    //
    // Android lifecycle
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_language")) {
            notifyLocaleChanged();
            Intent intent = new Intent(BoteService.LOCAL_BROADCAST_LOCALE_CHANGED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }


    //
    // Settings pages
    //

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle paramBundle, String s) {
            addPreferencesFromResource(R.xml.settings);

            findPreference(PREFERENCE_CATEGORY_NETWORK)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_NETWORK));
            findPreference(PREFERENCE_CATEGORY_IDENTITIES)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_IDENTITIES));
            findPreference(PREFERENCE_CATEGORY_PRIVACY)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_PRIVACY));
            findPreference(PREFERENCE_CATEGORY_APP_PROTECTION)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_APP_PROTECTION));
            findPreference(PREFERENCE_CATEGORY_APPEARANCE)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_APPEARANCE));
            findPreference(PREFERENCE_CATEGORY_ADVANCED)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_ADVANCED));
        }

        @Override
        public void onResume() {
            super.onResume();
            //noinspection ConstantConditions
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
                    case PREFERENCE_CATEGORY_IDENTITIES:
                        Intent ili = new Intent(getActivity(), IdentityListActivity.class);
                        startActivity(ili);
                        break;

                    case PREFERENCE_CATEGORY_PRIVACY:
                    case PREFERENCE_CATEGORY_APP_PROTECTION:
                        if (I2PBote.getInstance().isPasswordRequired()) {
                            BoteHelper.requestPassword(getActivity(), new BoteHelper.RequestPasswordListener() {
                                @Override
                                public void onPasswordVerified() {
                                    loadCategory();
                                }

                                @Override
                                public void onPasswordCanceled() {
                                }
                            });
                        } else
                            loadCategory();
                        break;

                    default:
                        loadCategory();
                }

                return true;
            }

            private void loadCategory() {
                Fragment fragment = getFragmentForCategory(category);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    private static Fragment getFragmentForCategory(String category) {
        switch (category) {
            case PREFERENCE_CATEGORY_NETWORK:
                return new NetworkPreferenceFragment();
            case PREFERENCE_CATEGORY_PRIVACY:
                return new PrivacyPreferenceFragment();
            case PREFERENCE_CATEGORY_APP_PROTECTION:
                return new AppProtectionPreferenceFragment();
            case PREFERENCE_CATEGORY_APPEARANCE:
                return new AppearancePreferenceFragment();
            case PREFERENCE_CATEGORY_ADVANCED:
                return new AdvancedPreferenceFragment();
            default:
                throw new AssertionError();
        }
    }
}

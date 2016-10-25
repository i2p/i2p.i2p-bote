package i2p.bote.android.config;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;

import i2p.bote.android.R;

public class AppProtectionPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        addPreferencesFromResource(R.xml.settings_app_protection);
        setupAppProtectionSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label_app_protection);

        // Screen security only works from API 14
        Preference screenSecurityPreference = findPreference("pref_screen_security");
        if (screenSecurityPreference != null &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            getPreferenceScreen().removePreference(screenSecurityPreference);
    }

    private void setupAppProtectionSettings() {
        findPreference("pref_change_password").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), SetPasswordActivity.class));
                return true;
            }
        });
    }
}

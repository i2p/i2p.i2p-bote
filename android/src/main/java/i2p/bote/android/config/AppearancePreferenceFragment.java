package i2p.bote.android.config;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;

import i2p.bote.android.R;

public class AppearancePreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        addPreferencesFromResource(R.xml.settings_appearance);
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                (SettingsActivity) getActivity()
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label_appearance);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                (SettingsActivity) getActivity()
        );
    }
}

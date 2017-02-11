package i2p.bote.android.config;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

import java.util.Map;

import i2p.bote.Configuration;
import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.config.util.CustomPreferenceFragment;

public class PrivacyPreferenceFragment extends CustomPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle paramBundle, String s) {
        addPreferencesFromResource(R.xml.settings_privacy);
        setupPrivacySettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_title_privacy);
    }

    @Override
    public void onPause() {
        Configuration config = I2PBote.getInstance().getConfiguration();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Map<String, ?> all = prefs.getAll();
        for (String x : all.keySet()) {
            if ("hideLocale".equals(x))
                config.setHideLocale(prefs.getBoolean(x, true));
            else if ("includeSentTime".equals(x))
                config.setIncludeSentTime(prefs.getBoolean(x, true));
            else if ("numSendHops".equals(x))
                config.setNumStoreHops(Integer.parseInt(prefs.getString(x, "0")));
            else if ("relayMinDelay".equals(x))
                config.setRelayMinDelay(prefs.getInt(x, 5));
            else if ("relayMaxDelay".equals(x))
                config.setRelayMaxDelay(prefs.getInt(x, 40));
        }

        config.save();

        // Store the settings in Android
        super.onPause();
    }

    private void setupPrivacySettings() {
        ListPreference numSendHops = (ListPreference) findPreference("numSendHops");
        int value = Integer.valueOf(numSendHops.getValue());
        numSendHops.setSummary(getResources().getQuantityString(R.plurals.pref_summ_numHops,
                value, value));
        numSendHops.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.valueOf((String) newValue);
                preference.setSummary(getResources().getQuantityString(R.plurals.pref_summ_numHops,
                        value, value));
                return true;
            }
        });
    }
}

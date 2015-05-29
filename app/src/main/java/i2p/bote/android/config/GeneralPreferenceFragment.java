package i2p.bote.android.config;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.preference.PreferenceFragment;

import java.util.Map;

import i2p.bote.Configuration;
import i2p.bote.I2PBote;
import i2p.bote.android.R;

public class GeneralPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        addPreferencesFromResource(R.xml.settings_general);
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_title_general);
    }

    @Override
    public void onPause() {
        Configuration config = I2PBote.getInstance().getConfiguration();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Map<String, ?> all = prefs.getAll();
        for (String x : all.keySet()) {
            if ("autoMailCheckEnabled".equals(x))
                config.setAutoMailCheckEnabled(prefs.getBoolean(x, true));
            else if ("mailCheckInterval".equals(x))
                config.setMailCheckInterval(prefs.getInt(x, 30));
            else if ("deliveryCheckEnabled".equals(x))
                config.setDeliveryCheckEnabled(prefs.getBoolean(x, true));
        }

        config.save();

        // Store the settings in Android
        super.onPause();
    }
}

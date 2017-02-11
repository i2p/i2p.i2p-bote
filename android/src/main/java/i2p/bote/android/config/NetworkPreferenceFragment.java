package i2p.bote.android.config;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.Map;

import i2p.bote.Configuration;
import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.config.util.CustomPreferenceFragment;

public class NetworkPreferenceFragment extends CustomPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle paramBundle, String s) {
        addPreferencesFromResource(R.xml.settings_network);
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label_network);
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

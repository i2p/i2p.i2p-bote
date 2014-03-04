package i2p.bote;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {
    // Actions for legacy settings
    private static final String ACTION_PREFS_GENERAL = "i2p.bote.PREFS_GENERAL";

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action != null) {
            if (ACTION_PREFS_GENERAL.equals(action)) {
                addPreferencesFromResource(R.xml.settings_general);
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.settings_headers_legacy);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        // The resource com.android.internal.R.bool.preferences_prefer_dual_pane
        // has different definitions based upon screen size. At present, it will
        // be true for -sw720dp devices, false otherwise. For your curiosity, in
        // Nexus 7 it is false.
        loadHeadersFromResource(R.xml.settings_headers, target);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("settings");
            if ("general".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_general);
            }
        }
    }

    @Override
    protected void onPause() {
        Configuration config = I2PBote.getInstance().getConfiguration();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Map<String, ?> all = prefs.getAll();
        Iterator<String> iterator = all.keySet().iterator();
        while (iterator.hasNext()) {
            String x = iterator.next();
            if (x.startsWith("i2pbote.")) // Skip over Android-specific settings
                continue;
            else if ("autoMailCheckEnabled".equals(x))
                config.setAutoMailCheckEnabled(prefs.getBoolean(x, true));
            else if ("mailCheckInterval".equals(x))
                config.setMailCheckInterval(prefs.getInt(x, 30));
            else if ("deliveryCheckEnabled".equals(x))
                config.setDeliveryCheckEnabled(prefs.getBoolean(x, true));
            else if ("hideLocale".equals(x))
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
}

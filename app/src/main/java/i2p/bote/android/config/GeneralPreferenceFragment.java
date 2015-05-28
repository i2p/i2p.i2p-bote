package i2p.bote.android.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v4.preference.PreferenceFragment;

import java.util.Map;

import i2p.bote.Configuration;
import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.widget.SummaryEditTextPreference;

public class GeneralPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        addPreferencesFromResource(R.xml.settings_general);
        setupGeneralSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
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

    private void setupGeneralSettings() {
        addPreferencesFromResource(R.xml.settings_general);

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

        final PreferenceCategory i2pCat = (PreferenceCategory) findPreference("i2pCategory");
        CheckBoxPreference routerAuto = (CheckBoxPreference) findPreference("i2pbote.router.auto");

        if (!routerAuto.isChecked()) {
            setupI2PCategory(getActivity(), i2pCat);
        }

        routerAuto.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final Boolean checked = (Boolean) newValue;
                if (!checked) {
                    setupI2PCategory(getActivity(), i2pCat);
                } else {
                    Preference p1 = i2pCat.findPreference("i2pbote.router.use");
                    Preference p2 = i2pCat.findPreference("i2pbote.i2cp.tcp.host");
                    Preference p3 = i2pCat.findPreference("i2pbote.i2cp.tcp.port");
                    if (p1 != null)
                        i2pCat.removePreference(p1);
                    if (p2 != null)
                        i2pCat.removePreference(p2);
                    if (p3 != null)
                        i2pCat.removePreference(p3);
                }
                return true;
            }
        });
    }

    private static void setupI2PCategory(Context context, PreferenceCategory i2pCat) {
        final ListPreference routerChoice = createRouterChoice(context);
        final EditTextPreference hostField = createHostField(context);
        final EditTextPreference portField = createPortField(context);
        i2pCat.addPreference(routerChoice);
        i2pCat.addPreference(hostField);
        i2pCat.addPreference(portField);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            routerChoice.setSummary(routerChoice.getEntry());

        if ("remote".equals(routerChoice.getValue())) {
            hostField.setEnabled(true);
            portField.setEnabled(true);
        }

        routerChoice.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String val = newValue.toString();
                int index = routerChoice.findIndexOfValue(val);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    routerChoice.setSummary(routerChoice.getEntries()[index]);
                if (index == 2) {
                    hostField.setEnabled(true);
                    hostField.setText("127.0.0.1");
                    portField.setEnabled(true);
                    portField.setText("7654");
                } else {
                    hostField.setEnabled(false);
                    hostField.setText("internal");
                    portField.setEnabled(false);
                    portField.setText("internal");
                }
                return true;
            }
        });
    }

    private static ListPreference createRouterChoice(Context context) {
        ListPreference routerChoice = new ListPreference(context);
        routerChoice.setKey("i2pbote.router.use");
        routerChoice.setEntries(R.array.routerOptionNames);
        routerChoice.setEntryValues(R.array.routerOptions);
        routerChoice.setTitle(R.string.pref_title_router);
        routerChoice.setSummary("%s");
        routerChoice.setDialogTitle(R.string.pref_dialog_title_router);
        routerChoice.setDefaultValue("internal");
        return routerChoice;
    }

    private static EditTextPreference createHostField(Context context) {
        EditTextPreference p = new SummaryEditTextPreference(context);
        p.setKey("i2pbote.i2cp.tcp.host");
        p.setTitle(R.string.pref_title_i2cp_host);
        p.setSummary("%s");
        p.setDefaultValue("internal");
        p.setEnabled(false);
        return p;
    }

    private static EditTextPreference createPortField(Context context) {
        EditTextPreference p = new SummaryEditTextPreference(context);
        p.setKey("i2pbote.i2cp.tcp.port");
        p.setTitle(R.string.pref_title_i2cp_port);
        p.setSummary("%s");
        p.setDefaultValue("internal");
        p.setEnabled(false);
        return p;
    }
}

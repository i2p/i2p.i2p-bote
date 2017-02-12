package i2p.bote.android.util;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Locale;

public class LocaleManager {
    private static final String DEFAULT_LANGUAGE = "zz";

    private Locale currentLocale;

    public void onCreate(Activity activity) {
        currentLocale = getSelectedLocale(activity);
        setContextLocale(activity, currentLocale);
    }

    public void onResume(Activity activity) {
        // If the activity has the incorrect locale, restart it
        if (!currentLocale.equals(getSelectedLocale(activity))) {
            Intent intent = activity.getIntent();
            activity.finish();
            activity.overridePendingTransition(0, 0);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        }
    }

    public void updateServiceLocale(Service service) {
        currentLocale = getSelectedLocale(service);
        setContextLocale(service, currentLocale);
    }

    private static Locale getSelectedLocale(Context context) {
        String selectedLanguage = PreferenceManager.getDefaultSharedPreferences(context).getString(
                "pref_language", DEFAULT_LANGUAGE
        );
        String language[] = TextUtils.split(selectedLanguage, "_");

        if (language[0].equals(DEFAULT_LANGUAGE))
            return Resources.getSystem().getConfiguration().locale;
        else if (language.length == 2)
            return new Locale(language[0], language[1]);
        else
            return new Locale(language[0]);
    }

    private static void setContextLocale(Context context, Locale selectedLocale) {
        Configuration configuration = context.getResources().getConfiguration();
        if (!configuration.locale.equals(selectedLocale)) {
            configuration.locale = selectedLocale;
            context.getResources().updateConfiguration(
                    configuration,
                    context.getResources().getDisplayMetrics()
            );
        }
    }
}

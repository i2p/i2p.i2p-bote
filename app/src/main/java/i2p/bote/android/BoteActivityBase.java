package i2p.bote.android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import i2p.bote.android.util.LocaleManager;

public class BoteActivityBase extends AppCompatActivity {
    private final LocaleManager localeManager = new LocaleManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        localeManager.onCreate(this);
        super.onCreate(savedInstanceState);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        localeManager.onResume(this);
    }

    public void notifyLocaleChanged() {
        localeManager.onResume(this);
    }
}

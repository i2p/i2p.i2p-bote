package i2p.bote.android.config;

import i2p.bote.android.InitActivities;
import i2p.bote.android.R;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

public class SetPasswordActivity extends ActionBarActivity implements
        SetPasswordFragment.Callbacks {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // SetPasswordFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this, R.string.password_changed,
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}

package i2p.bote.android.config;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import i2p.bote.android.BoteActivityBase;
import i2p.bote.android.R;

public class SetPasswordActivity extends BoteActivityBase implements
        SetPasswordFragment.Callbacks {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        //noinspection ConstantConditions
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

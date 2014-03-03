package i2p.bote;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

public class SetPasswordActivity extends ActionBarActivity implements
        SetPasswordFragment.Callbacks {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // SetPasswordFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this, R.string.password_changed,
                Toast.LENGTH_SHORT).show();
        finish();
    }
}

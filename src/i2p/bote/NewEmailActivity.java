package i2p.bote;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class NewEmailActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            NewEmailFragment f = new NewEmailFragment();
            getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, f).commit();
        }
    }
}

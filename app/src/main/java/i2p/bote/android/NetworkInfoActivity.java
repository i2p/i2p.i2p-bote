package i2p.bote.android;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class NetworkInfoActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.compose);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            NetworkInfoFragment f = new NetworkInfoFragment();
            getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, f).commit();
        }
    }
}

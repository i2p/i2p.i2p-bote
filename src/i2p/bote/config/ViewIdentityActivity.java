package i2p.bote.config;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class ViewIdentityActivity extends ActionBarActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            String key = null;
            Bundle args = getIntent().getExtras();
            if (args != null)
                key = args.getString(ViewIdentityFragment.IDENTITY_KEY);
            ViewIdentityFragment f = ViewIdentityFragment.newInstance(key);
            getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, f).commit();
        }
    }
}

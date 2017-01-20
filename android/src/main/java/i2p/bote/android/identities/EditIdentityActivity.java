package i2p.bote.android.identities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import i2p.bote.android.BoteActivityBase;
import i2p.bote.android.R;

public class EditIdentityActivity extends BoteActivityBase implements
        EditIdentityFragment.Callbacks {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_identity);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            String key = null;
            Bundle args = getIntent().getExtras();
            if (args != null)
                key = args.getString(EditIdentityFragment.IDENTITY_KEY);
            if (key != null)
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            EditIdentityFragment f = EditIdentityFragment.newInstance(key);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.edit_identity_frag, f).commit();
        }
    }

    // EditIdentityFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this, R.string.identity_saved,
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}

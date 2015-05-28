package i2p.bote.android.identities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import i2p.bote.android.InitActivities;
import i2p.bote.android.R;

public class IdentityShipActivity extends AppCompatActivity implements
        IdentityShipFragment.Callbacks {
    public static final String EXPORTING = "exporting";

    boolean mExporting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_ship);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Bundle args = getIntent().getExtras();
            mExporting = args.getBoolean(EXPORTING);
            IdentityShipFragment f = IdentityShipFragment.newInstance(mExporting);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.identity_ship_frag, f).commit();
        }
    }

    // IdentityShipFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this,
                mExporting ?
                        R.string.identities_exported :
                        R.string.identities_imported,
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}

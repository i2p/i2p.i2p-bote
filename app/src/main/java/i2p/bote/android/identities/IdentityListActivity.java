package i2p.bote.android.identities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import i2p.bote.android.InitActivities;
import i2p.bote.android.R;
import i2p.bote.email.EmailIdentity;

public class IdentityListActivity extends AppCompatActivity implements
        IdentityListFragment.IdentityListListener {
    static final int ALTER_IDENTITY_LIST = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            IdentityListFragment f = new IdentityListFragment();
            getSupportFragmentManager().beginTransaction()
                .add(R.id.container, f).commit();
        }
    }

    @Override
    public void onNewIdentity() {
        Intent i = new Intent(this, EditIdentityActivity.class);
        startActivityForResult(i, ALTER_IDENTITY_LIST);
    }

    @Override
    public void onIdentitySelected(EmailIdentity identity) {
        Intent i = new Intent(this, ViewIdentityActivity.class);
        i.putExtra(ViewIdentityFragment.ADDRESS, identity.getKey());
        startActivityForResult(i, ALTER_IDENTITY_LIST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ALTER_IDENTITY_LIST) {
            if (resultCode == Activity.RESULT_OK) {
                IdentityListFragment f = (IdentityListFragment) getSupportFragmentManager().findFragmentById(R.id.container);
                f.updateContactList();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

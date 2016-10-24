package i2p.bote.android;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

public class NewEmailActivity extends BoteActivityBase implements
        NewEmailFragment.Callbacks {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            NewEmailFragment f;
            String quoteMsgFolder = null;
            String quoteMsgId = null;
            NewEmailFragment.QuoteMsgType quoteMsgType = null;
            Bundle args = getIntent().getExtras();
            if (args != null) {
                quoteMsgFolder = args.getString(NewEmailFragment.QUOTE_MSG_FOLDER);
                quoteMsgId = args.getString(NewEmailFragment.QUOTE_MSG_ID);
                quoteMsgType =
                        (NewEmailFragment.QuoteMsgType) args.getSerializable(NewEmailFragment.QUOTE_MSG_TYPE);
            }
            f = NewEmailFragment.newInstance(quoteMsgFolder, quoteMsgId, quoteMsgType);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.container, f).commit();
        }
    }

    @Override
    public void onBackPressed() {
        NewEmailFragment f = (NewEmailFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        f.onBackPressed();
    }

    // NewEmailFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this, R.string.email_queued_for_sending,
                Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressAllowed() {
        super.onBackPressed();
    }
}

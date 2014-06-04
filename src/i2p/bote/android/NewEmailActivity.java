package i2p.bote.android;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

public class NewEmailActivity extends ActionBarActivity implements
        NewEmailFragment.Callbacks {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.compose);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            String sender = null;
            String recipient = null;
            Bundle args = getIntent().getExtras();
            if (args != null) {
                sender = args.getString(NewEmailFragment.SENDER);
                recipient = args.getString(NewEmailFragment.RECIPIENT);
            }
            NewEmailFragment f = NewEmailFragment.newInstance(sender, recipient);
            getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, f).commit();
        }
    }

    // NewEmailFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this, R.string.email_queued_for_sending,
                Toast.LENGTH_SHORT).show();
        finish();
    }
}

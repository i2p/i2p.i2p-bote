package i2p.bote;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

public class EditIdentityActivity extends ActionBarActivity implements
        EditIdentityFragment.Callbacks {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_identity);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            EditIdentityFragment f = EditIdentityFragment.newInstance(
                    getIntent().getExtras().getString(EditIdentityFragment.IDENTITY_KEY));
            getSupportFragmentManager().beginTransaction()
                .add(R.id.edit_identity_frag, f).commit();
        }
    }

    // EditIdentityFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this, R.string.identity_saved,
                Toast.LENGTH_SHORT).show();
        finish();
    }
}

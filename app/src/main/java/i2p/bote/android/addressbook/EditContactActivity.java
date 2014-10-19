package i2p.bote.android.addressbook;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import i2p.bote.android.InitActivities;
import i2p.bote.android.R;

public class EditContactActivity extends ActionBarActivity {
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
            String destination = null;
            Bundle args = getIntent().getExtras();
            if (args != null)
                destination = args.getString(EditContactFragment.CONTACT_DESTINATION);
            EditContactFragment f = EditContactFragment.newInstance(destination);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, f).commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // NFC receive only works on API 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Check to see that the Activity started due to an Android Beam
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()) ||
                    NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())) {
                processIntent(getIntent());
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs == null || rawMsgs.length < 1)
            return; // TODO notify user?
        NdefMessage msg = (NdefMessage) rawMsgs[0];

        NdefRecord[] records = msg.getRecords();
        if (records.length != 2)
            return; // TODO notify user?
        String name = new String(records[0].getPayload());
        String destination = new String(records[1].getPayload());

        EditContactFragment f = EditContactFragment.newInstance(
                name, destination);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, f).commit();
    }
}

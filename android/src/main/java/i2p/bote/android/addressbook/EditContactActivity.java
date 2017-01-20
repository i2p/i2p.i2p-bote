package i2p.bote.android.addressbook;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.Toolbar;

import java.util.Arrays;

import i2p.bote.android.BoteActivityBase;
import i2p.bote.android.Constants;
import i2p.bote.android.R;

public class EditContactActivity extends BoteActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            EditContactFragment f;
            Bundle args = getIntent().getExtras();
            if (args != null) {
                String destination = args.getString(EditContactFragment.NEW_DESTINATION);
                if (destination != null) {
                    String name = args.getString(EditContactFragment.NEW_NAME);
                    f = EditContactFragment.newInstance(name, destination);
                } else {
                    destination = args.getString(EditContactFragment.CONTACT_DESTINATION);
                    f = EditContactFragment.newInstance(destination);
                }
                if (destination != null)
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
            } else
                f = EditContactFragment.newInstance(null);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, f).commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
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
    private void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs == null || rawMsgs.length < 1)
            return; // TODO notify user?
        NdefMessage msg = (NdefMessage) rawMsgs[0];

        NdefRecord[] records = msg.getRecords();
        if (records.length != 2 ||
                records[0].getTnf() != NdefRecord.TNF_EXTERNAL_TYPE ||
                !Arrays.equals(records[0].getType(), Constants.NDEF_LEGACY_TYPE_CONTACT.getBytes()) ||
                records[1].getTnf() != NdefRecord.TNF_EXTERNAL_TYPE ||
                !Arrays.equals(records[1].getType(), Constants.NDEF_LEGACY_TYPE_CONTACT_DESTINATION.getBytes()))
            return; // TODO notify user?
        String name = new String(records[0].getPayload());
        String destination = new String(records[1].getPayload());

        EditContactFragment f = EditContactFragment.newInstance(
                name, destination);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, f).commit();
    }
}

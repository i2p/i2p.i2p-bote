package i2p.bote.android.addressbook;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;

import i2p.bote.android.InitActivities;

public class EditContactActivity extends ActionBarActivity {
    NfcAdapter mNfcAdapter;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            String destination = null;
            Bundle args = getIntent().getExtras();
            if (args != null)
                destination = args.getString(EditContactFragment.CONTACT_DESTINATION);
            EditContactFragment f = EditContactFragment.newInstance(destination);
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, f).commit();
        }

        // NFC send only works on API 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mNfcAdapter != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                mNfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                    @Override
                    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
                        return getNdefMessage();
                    }
                }, this);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mNfcAdapter.enableForegroundNdefPush(this, getNdefMessage());
        }

        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    private NdefMessage getNdefMessage() {
        EditContactFragment f = (EditContactFragment) getSupportFragmentManager()
                .findFragmentById(android.R.id.content);
        return f.createNdefMessage();
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
                .replace(android.R.id.content, f).commit();
    }

    @SuppressLint("NewApi")
    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mNfcAdapter.disableForegroundNdefPush(this);
        }
    }
}

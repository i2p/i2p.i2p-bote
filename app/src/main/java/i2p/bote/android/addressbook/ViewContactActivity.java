package i2p.bote.android.addressbook;

import android.annotation.SuppressLint;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import i2p.bote.android.InitActivities;

public class ViewContactActivity extends ActionBarActivity {
    NfcAdapter mNfcAdapter;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        if (savedInstanceState == null) {
            String destination = null;
            Bundle args = getIntent().getExtras();
            if (args != null)
                destination = args.getString(ViewContactFragment.ADDRESS);

            if (destination == null) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            ViewContactFragment f = ViewContactFragment.newInstance(destination);
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
    }

    private NdefMessage getNdefMessage() {
        ViewContactFragment f = (ViewContactFragment) getSupportFragmentManager()
                .findFragmentById(android.R.id.content);
        return f.createNdefMessage();
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

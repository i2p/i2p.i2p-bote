package i2p.bote.android.identities;

import android.annotation.SuppressLint;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;

import i2p.bote.android.BoteActivityBase;

public class ViewIdentityActivity extends BoteActivityBase {
    NfcAdapter mNfcAdapter;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            String key = null;
            Bundle args = getIntent().getExtras();
            if (args != null)
                key = args.getString(ViewIdentityFragment.ADDRESS);

            if (key == null) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            ViewIdentityFragment f = ViewIdentityFragment.newInstance(key);
            getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, f).commit();
        }

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mNfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
                    return getNdefMessage();
                }
            }, this);
        }
    }

    @SuppressWarnings("deprecation")
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
        ViewIdentityFragment f = (ViewIdentityFragment) getSupportFragmentManager()
                .findFragmentById(android.R.id.content);
        return f.createNdefMessage();
    }

    @SuppressWarnings("deprecation")
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

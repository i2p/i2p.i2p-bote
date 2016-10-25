package i2p.bote.android.addressbook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import i2p.bote.android.BoteActivityBase;
import i2p.bote.android.Constants;
import i2p.bote.android.R;
import i2p.bote.packet.dht.Contact;

public class AddressBookActivity extends BoteActivityBase implements
        AddressBookFragment.OnContactSelectedListener {
    static final int ALTER_CONTACT_LIST = 1;

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
            AddressBookFragment f = new AddressBookFragment();
            getSupportFragmentManager().beginTransaction()
                .add(R.id.container, f).commit();
        }
    }

    @Override
    public void onContactSelected(Contact contact) {
        if (Intent.ACTION_PICK.equals(getIntent().getAction())) {
            Intent result = new Intent();
            result.putExtra(ViewContactFragment.ADDRESS, contact.getBase64Dest());
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            Intent i = new Intent(this, ViewContactActivity.class);
            i.putExtra(ViewContactFragment.ADDRESS, contact.getBase64Dest());
            startActivityForResult(i, ALTER_CONTACT_LIST);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            String content = scanResult.getContents();
            if (content != null && content.startsWith(Constants.EMAILDEST_SCHEME)) {
                String destination = content.substring(Constants.EMAILDEST_SCHEME.length() + 1);
                Intent nci = new Intent(this, EditContactActivity.class);
                nci.putExtra(EditContactFragment.NEW_DESTINATION, destination);
                startActivityForResult(nci, ALTER_CONTACT_LIST);
            }
        } else if (requestCode == ALTER_CONTACT_LIST) {
            if (resultCode == Activity.RESULT_OK) {
                AddressBookFragment f = (AddressBookFragment) getSupportFragmentManager().findFragmentById(R.id.container);
                f.updateContactList();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

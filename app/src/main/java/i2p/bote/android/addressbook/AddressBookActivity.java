package i2p.bote.android.addressbook;

import i2p.bote.android.InitActivities;
import i2p.bote.android.R;
import i2p.bote.packet.dht.Contact;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class AddressBookActivity extends ActionBarActivity implements
        AddressBookFragment.OnContactSelectedListener {
    static final int ALTER_CONTACT_LIST = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.address_book);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            AddressBookFragment f = new AddressBookFragment();
            getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, f).commit();
        }
    }

    @Override
    public void onContactSelected(Contact contact) {
        if (getIntent().getAction() == Intent.ACTION_PICK) {
            Intent result = new Intent();
            result.putExtra(ViewContactFragment.CONTACT_DESTINATION, contact.getBase64Dest());
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            Intent i = new Intent(this, ViewContactActivity.class);
            i.putExtra(ViewContactFragment.CONTACT_DESTINATION, contact.getBase64Dest());
            startActivityForResult(i, ALTER_CONTACT_LIST);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ALTER_CONTACT_LIST) {
            if (resultCode == Activity.RESULT_OK) {
                AddressBookFragment f = (AddressBookFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
                f.updateContactList();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

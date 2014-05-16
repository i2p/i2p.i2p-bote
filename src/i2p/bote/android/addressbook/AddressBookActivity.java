package i2p.bote.android.addressbook;

import i2p.bote.android.R;
import i2p.bote.packet.dht.Contact;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class AddressBookActivity extends ActionBarActivity implements
        AddressBookFragment.OnContactSelectedListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.address_book);

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
            result.putExtra(EditContactFragment.CONTACT_DESTINATION, contact.getBase64Dest());
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            Intent i = new Intent(this, EditContactActivity.class);
            i.putExtra(EditContactFragment.CONTACT_DESTINATION, contact.getBase64Dest());
            startActivity(i);
        }
    }
}

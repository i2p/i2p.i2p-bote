package i2p.bote.android.addressbook;

import i2p.bote.packet.dht.Contact;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class AddressBookActivity extends ActionBarActivity implements
        AddressBookFragment.OnContactSelectedListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        // TODO
    }
}

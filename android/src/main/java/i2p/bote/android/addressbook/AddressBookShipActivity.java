package i2p.bote.android.addressbook;

import android.widget.Toast;

import i2p.bote.android.R;
import i2p.bote.android.util.DataShipActivity;
import i2p.bote.android.util.DataShipFragment;

public class AddressBookShipActivity extends DataShipActivity {
    @Override
    protected DataShipFragment getDataShipFragment() {
        return AddressBookShipFragment.newInstance(mExporting);
    }

    // DataShipFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this,
                mExporting ?
                        R.string.address_book_exported:
                        R.string.address_book_imported,
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}

package i2p.bote.android.identities;

import android.widget.Toast;

import i2p.bote.android.R;
import i2p.bote.android.util.DataShipActivity;
import i2p.bote.android.util.DataShipFragment;

public class IdentityShipActivity extends DataShipActivity {
    @Override
    protected DataShipFragment getDataShipFragment() {
        return IdentityShipFragment.newInstance(mExporting);
    }

    // DataShipFragment.Callbacks

    public void onTaskFinished() {
        Toast.makeText(this,
                mExporting ?
                        R.string.identities_exported :
                        R.string.identities_imported,
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}

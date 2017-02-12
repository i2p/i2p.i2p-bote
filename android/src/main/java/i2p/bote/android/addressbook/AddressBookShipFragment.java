package i2p.bote.android.addressbook;

import android.os.Bundle;
import android.view.View;

import java.io.File;
import java.io.FileDescriptor;

import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.util.DataShipFragment;
import i2p.bote.android.util.RobustAsyncTask;
import i2p.bote.fileencryption.PasswordException;

public abstract class AddressBookShipFragment extends DataShipFragment {
    public static DataShipFragment newInstance(boolean exporting) {
        return exporting ?
                new ExportAddressBookFragment() :
                new ImportAddressBookFragment();
    }

    public static class ExportAddressBookFragment extends ExportDataFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mExportFilename.setText("addressBook");
        }

        @Override
        protected RobustAsyncTask<Object, String, String> getExportWaiter() {
            return new ExportWaiter();
        }

        @Override
        protected int getTitle() {
            return R.string.export_address_book;
        }

        private class ExportWaiter extends RobustAsyncTask<Object, String, String> {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    publishProgress(getResources().getString(R.string.exporting_address_book));
                    I2PBote.getInstance().getAddressBook().export(
                            (File) params[0],
                            (String) params[1]);
                    return null;
                } catch (Throwable e) {
                    cancel(false);
                    return e.getMessage();
                }
            }
        }
    }

    public static class ImportAddressBookFragment extends ImportDataFragment {
        @Override
        protected RobustAsyncTask<Object, String, String> getImportWaiter() {
            return new ImportWaiter();
        }

        @Override
        protected int getTitle() {
            return R.string.import_address_book;
        }

        private class ImportWaiter extends RobustAsyncTask<Object, String, String> {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    publishProgress(getResources().getString(R.string.importing_address_book));
                    boolean success = I2PBote.getInstance().getAddressBook().importFromFileDescriptor(
                            (FileDescriptor) params[0],
                            (String) params[1],
                            (Boolean) params[2],
                            (Boolean) params[3]);
                    if (success)
                        return null;
                    else {
                        cancel(false);
                        return (params[1] == null) ?
                                getResources().getString(R.string.no_contacts_found_maybe_encrypted) :
                                getResources().getString(R.string.no_contacts_found);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    cancel(false);
                    if (e instanceof PasswordException)
                        return getResources().getString(R.string.password_incorrect);
                    return e.getLocalizedMessage();
                }
            }
        }
    }
}
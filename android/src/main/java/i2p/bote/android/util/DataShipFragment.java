package i2p.bote.android.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;

import i2p.bote.android.R;

public abstract class DataShipFragment extends Fragment {
    private Callbacks mCallbacks = sDummyCallbacks;

    public interface Callbacks {
        void onTaskFinished();
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        public void onTaskFinished() {}
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks))
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    // Code to identify the fragment that is calling onActivityResult().
    static final int SHIP_WAITER = 0;
    // Tag so we can find the task fragment again, in another
    // instance of this fragment after rotation.
    static final String SHIP_WAITER_TAG = "shipWaiterTask";

    TextView mError;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ShipWaiterFrag f = (ShipWaiterFrag) getFragmentManager().findFragmentByTag(SHIP_WAITER_TAG);
        if (f != null)
            f.setTargetFragment(this, SHIP_WAITER);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mError = (TextView) view.findViewById(R.id.error);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(getTitle());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SHIP_WAITER) {
            if (resultCode == Activity.RESULT_OK) {
                mCallbacks.onTaskFinished();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                setInterfaceEnabled(true);
                mError.setText(data.getStringExtra("error"));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected abstract int getTitle();
    protected abstract void setInterfaceEnabled(boolean enabled);

    public static class ShipWaiterFrag extends TaskFragment<Object, String, String> {
        String currentStatus;
        TextView mStatus;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.dialog_status, container, false);
            mStatus = (TextView) v.findViewById(R.id.status);

            if (currentStatus != null && !currentStatus.isEmpty())
                mStatus.setText(currentStatus);

            return v;
        }

        @Override
        public void updateProgress(String... values) {
            currentStatus = values[0];
            mStatus.setText(currentStatus);
        }

        @Override
        public void taskFinished(String result) {
            super.taskFinished(result);

            if (getTargetFragment() != null) {
                Intent i = new Intent();
                i.putExtra("result", result);
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_OK, i);
            }
        }

        @Override
        public void taskCancelled(String error) {
            super.taskCancelled(error);

            if (getTargetFragment() != null) {
                Intent i = new Intent();
                i.putExtra("error", error);
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_CANCELED, i);
            }
        }
    }

    public static abstract class ExportDataFragment extends DataShipFragment {
        protected EditText mExportFilename;
        TextView mSuffix;
        CheckBox mEncrypt;
        View mPasswordEntry;
        EditText mPassword;
        EditText mConfirmPassword;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_export_data, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mExportFilename = (EditText) view.findViewById(R.id.export_filename);
            mSuffix = (TextView) view.findViewById(R.id.suffix);
            mEncrypt = (CheckBox) view.findViewById(R.id.encrypt);
            mPasswordEntry = view.findViewById(R.id.password_entry);
            mPassword = (EditText) view.findViewById(R.id.password);
            mConfirmPassword = (EditText) view.findViewById(R.id.password_confirm);

            mEncrypt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mSuffix.setText(b ? ".bote" : ".txt");
                    mPasswordEntry.setVisibility(b ? View.VISIBLE : View.GONE);
                }
            });

            view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String exportFilename = mExportFilename.getText().toString();
                    String suffix = mSuffix.getText().toString();
                    boolean encrypt = mEncrypt.isChecked();
                    String password = null;

                    if (encrypt) {
                        password = mPassword.getText().toString();
                        String confirmPassword = mConfirmPassword.getText().toString();

                        if (password.isEmpty()) {
                            mPassword.setError(getActivity().getString(R.string.this_field_is_required));
                            mPassword.requestFocus();
                            return;
                        } else
                            mPassword.setError(null);

                        if (confirmPassword.isEmpty()) {
                            mConfirmPassword.setError(getActivity().getString(R.string.this_field_is_required));
                            mConfirmPassword.requestFocus();
                            return;
                        } else if (!password.equals(confirmPassword)) {
                            mConfirmPassword.setError(getActivity().getString(R.string.passwords_do_not_match));
                            mConfirmPassword.requestFocus();
                            return;
                        } else
                            mConfirmPassword.setError(null);
                    }

                    File exportFile = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                    ), exportFilename + suffix);
                    if (exportFile.exists()) {
                        // TODO ask to rename or overwrite
                        mExportFilename.setError(getActivity().getString(R.string.file_exists));
                        mExportFilename.requestFocus();
                        return;
                    } else
                        mExportFilename.setError(null);

                    exportIdentities(exportFile, password);
                }
            });
        }

        protected abstract RobustAsyncTask<Object, String, String> getExportWaiter();

        private void exportIdentities(File exportFile, String password) {
            setInterfaceEnabled(false);
            mError.setText("");

            ExportWaiterFrag f = ExportWaiterFrag.newInstance(exportFile, password);
            f.setTask(getExportWaiter());
            f.setTargetFragment(ExportDataFragment.this, SHIP_WAITER);
            getFragmentManager().beginTransaction()
                    .replace(R.id.waiter_frag, f, SHIP_WAITER_TAG)
                    .commit();
        }

        @Override
        protected void setInterfaceEnabled(boolean enabled) {
            mExportFilename.setEnabled(enabled);
            mEncrypt.setEnabled(enabled);
            mPassword.setEnabled(enabled);
            mConfirmPassword.setEnabled(enabled);
        }

        public static class ExportWaiterFrag extends ShipWaiterFrag {
            static final String SHIP_FILE = "shipFile";
            static final String PASSWORD = "password";

            public static ExportWaiterFrag newInstance(File shipFile, String password) {
                ExportWaiterFrag f = new ExportWaiterFrag();
                Bundle args = new Bundle();
                args.putSerializable(SHIP_FILE, shipFile);
                args.putString(PASSWORD, password);
                f.setArguments(args);
                return f;
            }

            @Override
            public Object[] getParams() {
                Bundle args = getArguments();
                return new Object[]{
                        args.getSerializable(SHIP_FILE),
                        args.getString(PASSWORD),
                };
            }
        }
    }

    public static abstract class ImportDataFragment extends DataShipFragment {
        static final int SELECT_IMPORT_FILE = 1;

        EditText mPassword;
        CheckBox mOverwrite;
        CheckBox mReplace;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_import_data, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mPassword = (EditText) view.findViewById(R.id.password);
            mOverwrite = (CheckBox) view.findViewById(R.id.overwrite);
            mReplace = (CheckBox) view.findViewById(R.id.replace);

            mOverwrite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mReplace.setVisibility(b ? View.GONE : View.VISIBLE);
                }
            });

            view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    if (mPassword.getText().toString().isEmpty()) {
                        i.setType("text/plain");
                    } else {
                        i.setType("*/*");
                    }
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    try {
                        startActivityForResult(i, SELECT_IMPORT_FILE);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(getActivity(), R.string.please_install_a_file_manager,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == SELECT_IMPORT_FILE) {
                if (resultCode == Activity.RESULT_OK) {
                    Uri result = data.getData();
                    try {
                        ParcelFileDescriptor pfd = getActivity().getContentResolver().openFileDescriptor(result, "r");
                        String password = mPassword.getText().toString();
                        if (password.isEmpty())
                            password = null;
                        importIdentities(pfd, password, !mOverwrite.isChecked(), mReplace.isChecked());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        mError.setText(e.getLocalizedMessage());
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        protected abstract RobustAsyncTask<Object, String, String> getImportWaiter();

        private void importIdentities(ParcelFileDescriptor importFile, String password,
                                      boolean append, boolean replace) {
            setInterfaceEnabled(false);
            mError.setText("");

            ImportWaiterFrag f = ImportWaiterFrag.newInstance(
                    importFile, password, append, replace);
            f.setTask(getImportWaiter());
            f.setTargetFragment(ImportDataFragment.this, SHIP_WAITER);
            getFragmentManager().beginTransaction()
                    .replace(R.id.waiter_frag, f, SHIP_WAITER_TAG)
                    .commit();
        }

        @Override
        protected void setInterfaceEnabled(boolean enabled) {
            mPassword.setEnabled(enabled);
            mOverwrite.setEnabled(enabled);
        }

        public static class ImportWaiterFrag extends ShipWaiterFrag {
            static final String SHIP_FILE_DESCRIPTOR = "shipFile";
            static final String PASSWORD = "password";
            static final String APPEND = "append";
            static final String REPLACE = "replace";

            public static ImportWaiterFrag newInstance(ParcelFileDescriptor shipFile, String password,
                                                       boolean append, boolean replace) {
                ImportWaiterFrag f = new ImportWaiterFrag();
                Bundle args = new Bundle();
                args.putParcelable(SHIP_FILE_DESCRIPTOR, shipFile);
                args.putString(PASSWORD, password);
                args.putBoolean(APPEND, append);
                args.putBoolean(REPLACE, replace);
                f.setArguments(args);
                return f;
            }

            @Override
            public Object[] getParams() {
                Bundle args = getArguments();
                return new Object[]{
                        ((ParcelFileDescriptor) args.getParcelable(SHIP_FILE_DESCRIPTOR))
                                .getFileDescriptor(),
                        args.getString(PASSWORD),
                        args.getBoolean(APPEND),
                        args.getBoolean(REPLACE),
                };
            }
        }
    }
}
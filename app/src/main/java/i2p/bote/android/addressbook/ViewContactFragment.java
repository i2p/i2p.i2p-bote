package i2p.bote.android.addressbook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.GeneralSecurityException;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;

public class ViewContactFragment extends Fragment {
    public static final String CONTACT_DESTINATION = "contact_destination";

    private String mDestination;
    private Contact mContact;

    Toolbar mToolbar;
    ImageView mContactPicture;
    TextView mNameField;
    TextView mTextField;
    TextView mCryptoField;
    TextView mDestinationField;
    TextView mFingerprintField;

    public static ViewContactFragment newInstance(String destination) {
        ViewContactFragment f = new ViewContactFragment();
        Bundle args = new Bundle();
        args.putString(CONTACT_DESTINATION, destination);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mDestination = getArguments().getString(CONTACT_DESTINATION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_contact, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = (Toolbar) view.findViewById(R.id.main_toolbar);
        mContactPicture = (ImageView) view.findViewById(R.id.contact_picture);
        mNameField = (TextView) view.findViewById(R.id.contact_name);
        mTextField = (TextView) view.findViewById(R.id.text);
        mCryptoField = (TextView) view.findViewById(R.id.crypto_impl);
        mDestinationField = (TextView) view.findViewById(R.id.destination);
        mFingerprintField = (TextView) view.findViewById(R.id.fingerprint);

        if (mDestination != null) {
            try {
                mContact = BoteHelper.getContact(mDestination);
            } catch (PasswordException e) {
                // TODO Handle
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBarActivity activity = ((ActionBarActivity)getActivity());

        // Set the action bar
        activity.setSupportActionBar(mToolbar);

        // Enable ActionBar app icon to behave as action to go back
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mContact != null) {
            Bitmap picture = BoteHelper.decodePicture(mContact.getPictureBase64());
            if (picture != null)
                mContactPicture.setImageBitmap(picture);
            else  {
                ViewGroup.LayoutParams lp = mContactPicture.getLayoutParams();
                mContactPicture.setImageBitmap(BoteHelper.getIdenticonForAddress(mDestination, lp.width, lp.height));
            }

            mNameField.setText(mContact.getName());
            if (mContact.getText().isEmpty())
                mTextField.setVisibility(View.GONE);
            else {
                mTextField.setText(mContact.getText());
                mTextField.setVisibility(View.VISIBLE);
            }
            mCryptoField.setText(mContact.getDestination().getCryptoImpl().getName());
            mDestinationField.setText(mDestination);
            try {
                String locale = getActivity().getResources().getConfiguration().locale.getLanguage();
                mFingerprintField.setText(BoteHelper.getFingerprint(mContact, locale));
            } catch (GeneralSecurityException e) {
                // Could not get fingerprint
                mFingerprintField.setText(e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_contact, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_contact:
                Intent ei = new Intent(getActivity(), EditContactActivity.class);
                ei.putExtra(EditContactFragment.CONTACT_DESTINATION, mDestination);
                startActivity(ei);
                return true;

            case R.id.action_delete_contact:
                DialogFragment df = new DialogFragment() {
                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.delete_contact)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        try {
                                            String err = BoteHelper.deleteContact(mDestination);
                                            if (err == null) {
                                                getActivity().setResult(Activity.RESULT_OK);
                                                getActivity().finish();
                                            } else
                                                Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
                                        } catch (PasswordException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        } catch (GeneralSecurityException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                    }
                                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        return builder.create();
                    }
                };
                df.show(getActivity().getSupportFragmentManager(), "deletecontact");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public NdefMessage createNdefMessage() {
        NdefMessage msg = new NdefMessage(new NdefRecord[]{
                createNameRecord(),
                createDestinationRecord()
        });
        return msg;
    }

    private NdefRecord createNameRecord() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return new NdefRecord(
                    NdefRecord.TNF_EXTERNAL_TYPE,
                    "i2p.bote:contact".getBytes(),
                    new byte[0],
                    mContact.getName().getBytes()
            );
        else
            return NdefRecord.createExternal(
                    "i2p.bote", "contact", mContact.getName().getBytes()
            );
    }

    private NdefRecord createDestinationRecord() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return new NdefRecord(
                    NdefRecord.TNF_EXTERNAL_TYPE,
                    "i2p.bote:contactDestination".getBytes(),
                    new byte[0],
                    mContact.getDestination().getKey().getBytes()
            );
        else
            return NdefRecord.createExternal(
                    "i2p.bote", "contactDestination", mContact.getDestination().getKey().getBytes()
            );
    }
}

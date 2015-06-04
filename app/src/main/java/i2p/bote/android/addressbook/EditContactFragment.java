package i2p.bote.android.addressbook;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.EditPictureFragment;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;

public class EditContactFragment extends EditPictureFragment {
    public static final String CONTACT_DESTINATION = "contact_destination";
    public static final String NEW_NAME = "new_name";
    public static final String NEW_DESTINATION = "new_destination";

    static final int REQUEST_DESTINATION_FILE = 3;
    EditText mNameField;
    EditText mDestinationField;
    EditText mTextField;
    TextView mError;
    private String mDestination;

    public static EditContactFragment newInstance(String destination) {
        EditContactFragment f = new EditContactFragment();
        Bundle args = new Bundle();
        args.putString(CONTACT_DESTINATION, destination);
        f.setArguments(args);
        return f;
    }

    public static EditContactFragment newInstance(String name, String destination) {
        EditContactFragment f = new EditContactFragment();
        Bundle args = new Bundle();
        args.putString(NEW_NAME, name);
        args.putString(NEW_DESTINATION, destination);
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
        return inflater.inflate(R.layout.fragment_edit_contact, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNameField = (EditText) view.findViewById(R.id.contact_name);
        mDestinationField = (EditText) view.findViewById(R.id.destination);
        mTextField = (EditText) view.findViewById(R.id.text);
        mError = (TextView) view.findViewById(R.id.error);

        Button b = (Button) view.findViewById(R.id.import_destination_from_file);
        if (mDestination != null) {
            mDestinationField.setVisibility(View.GONE);
            b.setVisibility(View.GONE);
        } else
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.setType("text/plain");
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    try {
                        startActivityForResult(
                                Intent.createChooser(i,
                                        getResources().getString(R.string.select_email_destination_file)),
                                REQUEST_DESTINATION_FILE);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(getActivity(), R.string.please_install_a_file_manager,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

        if (I2PBote.getInstance().isPasswordRequired()) {
            // Request a password from the user.
            BoteHelper.requestPassword(getActivity(), new BoteHelper.RequestPasswordListener() {
                @Override
                public void onPasswordVerified() {
                    initializeContact();
                }

                @Override
                public void onPasswordCanceled() {
                    getActivity().setResult(Activity.RESULT_CANCELED);
                    getActivity().finish();
                }
            });
        } else {
            // Password is cached, or not set.
            initializeContact();
        }
    }

    private void initializeContact() {
        String newDest = getArguments().getString(NEW_DESTINATION);

        if (mDestination != null) {
            try {
                Contact contact = BoteHelper.getContact(mDestination);

                String pic = contact.getPictureBase64();
                if (pic != null && !pic.isEmpty()) {
                    setPictureB64(pic);
                }

                mNameField.setText(contact.getName());
                mTextField.setText(contact.getText());
            } catch (PasswordException e) {
                // TODO Handle
                e.printStackTrace();
            }
        } else if (newDest != null) {
            mNameField.setText(getArguments().getString(NEW_NAME));
            mDestinationField.setText(newDest);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_contact, menu);
        menu.findItem(R.id.action_save_contact).setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_save));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_contact:
                String picture = getPictureB64();
                String name = mNameField.getText().toString();
                String destination = mDestination == null ?
                        mDestinationField.getText().toString() : mDestination;
                String text = mTextField.getText().toString();

                // Check fields
                if (destination.isEmpty()) {
                    mDestinationField.setError(getActivity().getString(R.string.this_field_is_required));
                    mDestinationField.requestFocus();
                    return true;
                } else {
                    mDestinationField.setError(null);
                }

                mError.setText("");

                try {
                    String err = BoteHelper.saveContact(destination, name, picture, text);
                    if (err == null) {
                        if (mDestination == null) // Only set if adding new contact
                            getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                    } else {
                        if (err.startsWith("No Email Destination found in string:") ||
                                err.startsWith("Not a valid Email Destination:")) {
                            mDestinationField.setError(getActivity().getString(R.string.not_a_valid_bote_address));
                            mDestinationField.requestFocus();
                        } else {
                            mError.setText(err);
                        }
                    }
                } catch (PasswordException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    mError.setText(e.getLocalizedMessage());
                } catch (GeneralSecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    mError.setText(e.getLocalizedMessage());
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DESTINATION_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri result = data.getData();
                BufferedReader br;
                try {
                    ParcelFileDescriptor pfd = getActivity().getContentResolver().openFileDescriptor(result, "r");
                    br = new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(pfd.getFileDescriptor()))
                    );
                    try {
                        mDestinationField.setText(br.readLine());
                    } catch (IOException ioe) {
                        Toast.makeText(getActivity(), R.string.failed_to_read_email_destination_file,
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (FileNotFoundException fnfe) {
                    Toast.makeText(getActivity(), R.string.could_not_find_email_destination_file,
                            Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

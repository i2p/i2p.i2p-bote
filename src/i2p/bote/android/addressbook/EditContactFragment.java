package i2p.bote.android.addressbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class EditContactFragment extends Fragment {
    public static final String CONTACT_DESTINATION = "contact_destination";

    static final int REQUEST_DESTINATION_FILE = 1;

    private String mDestination;
    EditText mNameField;
    EditText mDestinationField;
    EditText mTextField;
    TextView mError;

    public static EditContactFragment newInstance(String destination) {
        EditContactFragment f = new EditContactFragment();
        Bundle args = new Bundle();
        args.putString(CONTACT_DESTINATION, destination);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_contact, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDestination = getArguments().getString(CONTACT_DESTINATION);

        mNameField = (EditText) view.findViewById(R.id.contact_name);
        mDestinationField = (EditText) view.findViewById(R.id.destination);
        mTextField = (EditText) view.findViewById(R.id.text);
        mError = (TextView) view.findViewById(R.id.error);

        if (mDestination != null) {
            try {
                Contact contact = BoteHelper.getContact(mDestination);
                mNameField.setText(contact.getName());
                mDestinationField.setText(mDestination);
                mTextField.setText(contact.getText());
            } catch (PasswordException e) {
                // TODO Handle
                e.printStackTrace();
            }
        }

        Button b = (Button) view.findViewById(R.id.import_destination_from_file);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("text/plain");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(
                            Intent.createChooser(i,"Select file containing Email Destination"),
                            REQUEST_DESTINATION_FILE);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), "Please install a File Manager.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_contact, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_save_contact:
            String name = mNameField.getText().toString();
            String destination = mDestinationField.getText().toString();
            String text = mTextField.getText().toString();

            mError.setText("");

            try {
                String err = BoteHelper.saveContact(destination, name, null, text);
                if (err == null)
                    getActivity().finish();
                else
                    mError.setText(err);
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
                String path = result.getPath();
                File file = new File(path);
                BufferedReader br;
                try {
                    br = new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(file)));
                    try {
                        mDestinationField.setText(br.readLine());
                    } catch (IOException ioe) {
                        Toast.makeText(getActivity(), "Failed to read Email Destination file.",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (FileNotFoundException fnfe) {
                    Toast.makeText(getActivity(), "Could not find Email Destination file.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

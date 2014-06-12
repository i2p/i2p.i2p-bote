package i2p.bote.android;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import com.tokenautocomplete.FilteredArrayAdapter;

import net.i2p.data.DataFormatException;
import i2p.bote.I2PBote;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.ContactsCompletionView;
import i2p.bote.android.util.Person;
import i2p.bote.email.Attachment;
import i2p.bote.email.Email;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class NewEmailFragment extends Fragment {
    private Callbacks mCallbacks = sDummyCallbacks;

    public interface Callbacks {
        public void onTaskFinished();
    }
    private static Callbacks sDummyCallbacks = new Callbacks() {
        public void onTaskFinished() {};
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

    public static final String QUOTE_MSG_FOLDER = "sender";
    public static final String QUOTE_MSG_ID = "recipient";

    private String mSenderKey;

    Spinner mSpinner;
    int mDefaultPos;
    ArrayAdapter<Person> mAdapter;
    ContactsCompletionView mRecipients;
    EditText mSubject;
    EditText mContent;

    public static NewEmailFragment newInstance(String quoteMsgFolder, String quoteMsgId) {
        NewEmailFragment f = new NewEmailFragment();
        Bundle args = new Bundle();
        args.putString(QUOTE_MSG_FOLDER, quoteMsgFolder);
        args.putString(QUOTE_MSG_ID, quoteMsgId);
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
        return inflater.inflate(R.layout.fragment_new_email, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String quoteMsgFolder = getArguments().getString(QUOTE_MSG_FOLDER);
        String quoteMsgId = getArguments().getString(QUOTE_MSG_ID);
        Email origEmail = null;
        String recipientAddr = null;
        try {
            origEmail = BoteHelper.getEmail(quoteMsgFolder, quoteMsgId);
            if (origEmail != null) {
                mSenderKey = BoteHelper.extractEmailDestination(
                        BoteHelper.getOneLocalRecipient(origEmail).toString());
                recipientAddr = BoteHelper.getNameAndDestination(
                        origEmail.getReplyAddress(I2PBote.getInstance().getIdentities()));
            }
        } catch (PasswordException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mSpinner = (Spinner) view.findViewById(R.id.sender_spinner);
        IdentityAdapter identities = new IdentityAdapter(getActivity());
        mSpinner.setAdapter(identities);
        mSpinner.setSelection(mDefaultPos);

        List<Person> contacts = new ArrayList<Person>();
        try {
            for (Contact contact : I2PBote.getInstance().getAddressBook().getAll()) {
                contacts.add(new Person(contact.getName(), contact.getBase64Dest()));
            }
        } catch (PasswordException e) {
            // TODO handle
            e.printStackTrace();
        }
        mAdapter = new FilteredArrayAdapter<Person>(getActivity(), android.R.layout.simple_list_item_1, contacts) {
            @Override
            protected boolean keepObject(Person obj, String mask) {
                mask = mask.toLowerCase(Locale.US);
                return obj.getName().toLowerCase(Locale.US).startsWith(mask) || obj.getAddress().toLowerCase(Locale.US).startsWith(mask);
            }
        };

        mRecipients = (ContactsCompletionView) view.findViewById(R.id.recipients);
        mRecipients.setAdapter(mAdapter);
        if (recipientAddr != null) {
            String name = BoteHelper.extractName(recipientAddr);
            String address = BoteHelper.extractEmailDestination(recipientAddr);
            if (address == null) { // Assume external address
                address = recipientAddr;
                if (name.isEmpty())
                    name = address;
            } else if (name.isEmpty()) // Dest with no name
                name = address.substring(0, 5);
            mRecipients.addObject(new Person(name, address));
        }

        mSubject = (EditText) view.findViewById(R.id.subject);
        mContent = (EditText) view.findViewById(R.id.message);

        if (savedInstanceState == null) {
            mRecipients.setPrefix(getResources().getString(R.string.email_to) + " ");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.new_email, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_send_email:
            if (sendEmail())
                mCallbacks.onTaskFinished();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private boolean sendEmail() {
        Email email = new Email(I2PBote.getInstance().getConfiguration().getIncludeSentTime());
        try {
            // Set sender
            EmailIdentity sender = (EmailIdentity) mSpinner.getSelectedItem();
            InternetAddress ia = new InternetAddress(
                    sender == null ? "Anonymous" :
                        BoteHelper.getNameAndDestination(sender.getKey()));
            email.setFrom(ia);
            // We must continue to set "Sender:" even with only one mailbox
            // in "From:", which is against RFC 2822 but required for older
            // Bote versions to see a sender (and validate the signature).
            email.setSender(ia);

            for (Object obj : mRecipients.getObjects()) {
                Person person = (Person) obj;
                email.addRecipient(Message.RecipientType.TO, new InternetAddress(
                        person.getAddress(), person.getName()));
            }

            // Check that we have someone to send to
            Address[] rcpts = email.getAllRecipients();
            if (rcpts == null || rcpts.length == 0) {
                // No recipients
                DialogFragment df = new DialogFragment() {
                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.add_one_recipient)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        return builder.create();
                    }
                };
                df.show(getActivity().getSupportFragmentManager(), "norecipients");
                return false;
            }

            email.setSubject(mSubject.getText().toString(), "UTF-8");

            // Set the text and add attachments
            email.setContent(mContent.getText().toString(), (List<Attachment>) null);

            // Send the email
            I2PBote.getInstance().sendEmail(email);
            return true;
        } catch (PasswordException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DataFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    private class IdentityAdapter extends ArrayAdapter<EmailIdentity> {
        private LayoutInflater mInflater;

        public IdentityAdapter(Context context) {
            super(context, android.R.layout.simple_spinner_item);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            try {
                Collection<EmailIdentity> identities = I2PBote.getInstance().getIdentities().getAll();
                mDefaultPos = 0;
                for (EmailIdentity identity : identities) {
                    add(identity);
                    if ((mSenderKey == null && identity.isDefaultIdentity()) ||
                            (mSenderKey != null && identity.getKey().equals(mSenderKey)))
                        mDefaultPos = getPosition(identity);
                }
            } catch (PasswordException e) {
                // TODO Handle
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Handle
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                // TODO Handle
                e.printStackTrace();
            }
        }

        @Override
        public EmailIdentity getItem(int position) {
            if (position > 0)
                return super.getItem(position - 1);
            else
                return null;
        }

        @Override
        public int getPosition(EmailIdentity item) {
            if (item != null)
                return super.getPosition(item) + 1;
            else
                return 0;
        }

        @Override
        public int getCount() {
            return super.getCount() + 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null)
                v = mInflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            else
                v = convertView;

            setViewText(v, position);
            return v;
        }

        @Override
        public View getDropDownView (int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null)
                v = mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            else
                v = convertView;

            setViewText(v, position);
            return v;
        }

        private void setViewText(View v, int position) {
            TextView text = (TextView) v.findViewById(android.R.id.text1);
            EmailIdentity identity = getItem(position);
            if (identity == null)
                text.setText("Anonymous");
            else
                text.setText(identity.getPublicName());
        }
    }
}

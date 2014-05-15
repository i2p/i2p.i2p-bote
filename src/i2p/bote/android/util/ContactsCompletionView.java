package i2p.bote.android.util;

import java.security.GeneralSecurityException;
import java.util.SortedSet;

import i2p.bote.I2PBote;
import i2p.bote.android.R;
import i2p.bote.email.EmailDestination;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tokenautocomplete.TokenCompleteTextView;

public class ContactsCompletionView extends TokenCompleteTextView {
    public ContactsCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        allowDuplicates(false);
    }

    @Override
    protected View getViewForObject(Object object) {
        Person person = (Person) object;

        LayoutInflater l = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        LinearLayout view = (LinearLayout)l.inflate(R.layout.contact_token, (ViewGroup)ContactsCompletionView.this.getParent(), false);
        ((TextView)view.findViewById(R.id.contact_name)).setText(person.getName());

        return view;
    }

    @Override
    protected Object defaultObject(String completionText) {
        // Stupid simple example of guessing if we have an email or not
        int index = completionText.indexOf('@');
        if (index == -1) {
            try {
                // Check if it is a known Destination
                Contact c = BoteHelper.getContact(completionText);
                if (c != null)
                    return new Person(c.getName(), c.getBase64Dest());

                // Check if it is a name
                SortedSet<Contact> contacts = I2PBote.getInstance().getAddressBook().getAll();
                for (Contact contact : contacts) {
                    if (contact.getName().startsWith(completionText))
                        return new Person(contact.getName(), contact.getBase64Dest());
                }

                // Try as a new Destination
                try {
                    new EmailDestination(completionText);
                    return new Person(completionText.substring(0, 5), completionText);
                } catch (GeneralSecurityException e) {
                    // Not a valid Destination
                    // Assume the user meant an external address
                    return new Person(completionText, completionText.replace(" ", "") + "@example.com", true);
                }
            } catch (PasswordException e) {
                // TODO handle
                return new Person(completionText, completionText.replace(" ", "") + "@example.com", true);
            }
        } else {
            return new Person(completionText.substring(0, index), completionText, true);
        }
    }
}

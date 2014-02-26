package i2p.bote;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.mail.MessagingException;

import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class EmailListAdapter extends ArrayAdapter<Email> {
    private final LayoutInflater mInflater;

    public EmailListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<Email> emails) {
        clear();
        if (emails != null) {
            for (Email email : emails) {
                add(email);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.listitem_email, parent, false);
        Email email = getItem(position);

        TextView subject = (TextView) v.findViewById(R.id.email_subject);
        TextView from = (TextView) v.findViewById(R.id.email_from);
        TextView content = (TextView) v.findViewById(R.id.email_content);
        try {
            subject.setText(email.getSubject());
            from.setText(BoteHelper.getNameAndShortDestination(email.getOneFromAddress()));
        } catch (MessagingException e) {
            subject.setText("ERROR: " + e.getMessage());
        } catch (PasswordException e) {
            subject.setText("ERROR: " + e.getMessage());
        } catch (IOException e) {
            subject.setText("ERROR: " + e.getMessage());
        } catch (GeneralSecurityException e) {
            subject.setText("ERROR: " + e.getMessage());
        }
        // TODO: Fix library bugs
        // The .jar files are getting classes stripped during dexing.
        //content.setText(email.getText());

        return v;
    }
}

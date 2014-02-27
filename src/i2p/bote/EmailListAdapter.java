package i2p.bote;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.List;

import javax.mail.MessagingException;

import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.util.BoteHelper;
import android.content.Context;
import android.graphics.Typeface;
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
        TextView sent = (TextView) v.findViewById(R.id.email_sent);

        try {
            subject.setText(email.getSubject());
            from.setText(BoteHelper.getNameAndShortDestination(
                    email.getOneFromAddress()));
            if (email.getSentDate() != null)
                sent.setText(DateFormat.getInstance().format(
                        email.getSentDate()));

            if (email.isNew()) {
                subject.setTypeface(Typeface.DEFAULT_BOLD);
                from.setTypeface(Typeface.DEFAULT_BOLD);
            }
        } catch (MessagingException e) {
            subject.setText("ERROR: " + e.getMessage());
        } catch (PasswordException e) {
            subject.setText("ERROR: " + e.getMessage());
        } catch (IOException e) {
            subject.setText("ERROR: " + e.getMessage());
        } catch (GeneralSecurityException e) {
            subject.setText("ERROR: " + e.getMessage());
        }
        content.setText(email.getText());

        return v;
    }
}

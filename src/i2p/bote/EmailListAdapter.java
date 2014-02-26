package i2p.bote;

import java.util.List;

import javax.mail.MessagingException;

import i2p.bote.email.Email;
import i2p.bote.folder.EmailFolder;
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

        TextView name = (TextView) v.findViewById(R.id.email_subject);
        try {
            name.setText(email.getSubject());
        } catch (MessagingException e) {
            name.setText("ERROR: " + e.getMessage());
        }

        return v;
    }
}

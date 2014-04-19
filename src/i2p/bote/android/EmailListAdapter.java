package i2p.bote.android;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.List;

import javax.mail.MessagingException;

import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class EmailListAdapter extends ArrayAdapter<Email> {
    private final LayoutInflater mInflater;
    private SparseBooleanArray mSelectedEmails;

    public EmailListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSelectedEmails = new SparseBooleanArray();
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

        ImageView picture = (ImageView) v.findViewById(R.id.contact_picture);
        TextView subject = (TextView) v.findViewById(R.id.email_subject);
        TextView from = (TextView) v.findViewById(R.id.email_from);
        TextView content = (TextView) v.findViewById(R.id.email_content);
        TextView sent = (TextView) v.findViewById(R.id.email_sent);

        try {
            String fromAddress = email.getOneFromAddress();

            Bitmap pic = BoteHelper.getPictureForAddress(fromAddress);
            if (pic != null)
                picture.setImageBitmap(pic);

            subject.setText(email.getSubject());
            from.setText(BoteHelper.getNameAndShortDestination(fromAddress));
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

    public void toggleSelection(int position) {
        selectView(position, !mSelectedEmails.get(position));
    }

    public void removeSelection() {
        mSelectedEmails = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    public void selectView(int position, boolean value) {
        if (value)
            mSelectedEmails.put(position, value);
        else
            mSelectedEmails.delete(position);
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return mSelectedEmails.size();
    }

    public SparseBooleanArray getSelectedIds() {
        return mSelectedEmails;
    }
}

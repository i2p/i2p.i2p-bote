package i2p.bote.android;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;

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
    private EmailSelector mSelector;
    private boolean mIsOutbox;

    public interface EmailSelector {
        public void select(int position);
    }

    public EmailListAdapter(Context context, EmailSelector selector, boolean isOutbox) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSelectedEmails = new SparseBooleanArray();
        mSelector = selector;
        mIsOutbox = isOutbox;
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
        final Email email = getItem(position);

        ImageView picture = (ImageView) v.findViewById(R.id.contact_picture);
        TextView subject = (TextView) v.findViewById(R.id.email_subject);
        TextView from = (TextView) v.findViewById(R.id.email_from);
        TextView content = (TextView) v.findViewById(R.id.email_content);
        TextView sent = (TextView) v.findViewById(R.id.email_sent);

        picture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mSelector.select(getPosition(email));
            }
        });

        if (mSelectedEmails.get(position)) {
            ((ImageView) v.findViewById(R.id.email_selected)).setVisibility(View.VISIBLE);
        }

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

            List<Part> parts = email.getParts();
            for (Part part : parts) {
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    ((ImageView) v.findViewById(
                            R.id.email_attachment)).setVisibility(View.VISIBLE);
                    break;
                }
            }

            if (email.isNew()) {
                subject.setTypeface(Typeface.DEFAULT_BOLD);
                from.setTypeface(Typeface.DEFAULT_BOLD);
            }

            TextView emailStatus = (TextView) v.findViewById(R.id.email_status);
            // Set email sending status if this is the outbox,
            // or set email delivery status if we sent it.
            if (mIsOutbox) {
                emailStatus.setText(BoteHelper.getEmailStatusText(
                        getContext(), email, false));
                emailStatus.setVisibility(View.VISIBLE);
            } else if (BoteHelper.isSentEmail(email)) {
                if (email.isDelivered())
                    emailStatus.setCompoundDrawablesWithIntrinsicBounds(
                            getContext().getResources().getDrawable(
                                    R.drawable.ic_navigation_accept),
                            null, null, null);
                else if (email.getDeliveryPercentage() > 0)
                    emailStatus.setText(email.getDeliveryPercentage() + "%");
                emailStatus.setVisibility(View.VISIBLE);
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

package i2p.bote.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;

import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;

public class EmailListAdapter extends ArrayAdapter<Email> {
    private final LayoutInflater mInflater;
    private EmailSelector mSelector;
    private boolean mIsOutbox;

    public interface EmailSelector {
        public boolean inActionMode();
        public void select(View view);
    }

    public EmailListAdapter(Context context, EmailSelector selector, boolean isOutbox) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        final View v = mInflater.inflate(R.layout.listitem_email, parent, false);
        final Email email = getItem(position);

        ImageView picture = (ImageView) v.findViewById(R.id.contact_picture);
        TextView subject = (TextView) v.findViewById(R.id.email_subject);
        TextView address = (TextView) v.findViewById(R.id.email_address);
        TextView content = (TextView) v.findViewById(R.id.email_content);
        TextView sent = (TextView) v.findViewById(R.id.email_sent);

        if (!mSelector.inActionMode())
            picture.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    mSelector.select(v);
                }
            });

        // TODO fix
        //if (mSelectedEmails.get(position)) {
        //    ((ImageView) v.findViewById(R.id.email_selected)).setVisibility(View.VISIBLE);
        //}

        try {
            String otherAddress;
            if (BoteHelper.isSentEmail(email))
                otherAddress = email.getOneRecipient();
            else
                otherAddress = email.getOneFromAddress();

            Bitmap pic = BoteHelper.getPictureForAddress(otherAddress);
            if (pic != null)
                picture.setImageBitmap(pic);

            subject.setText(email.getSubject());
            address.setText(BoteHelper.getNameAndShortDestination(otherAddress));
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

            if (email.isUnread()) {
                subject.setTypeface(Typeface.DEFAULT_BOLD);
                address.setTypeface(Typeface.DEFAULT_BOLD);
            }
            if (email.isAnonymous() && !BoteHelper.isSentEmail(email)) {
                if (email.isUnread())
                    address.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
                else
                    address.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
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
                else
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
}

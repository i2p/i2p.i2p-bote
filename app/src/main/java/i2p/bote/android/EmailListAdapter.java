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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.mail.Part;

import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.Email;

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

    private static class ViewHolder {
        ImageView picture;
        //View emailSelected;
        TextView subject;
        TextView address;
        TextView content;
        TextView sent;
        View emailAttachment;
        TextView emailStatus;
        View emailDelivered;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View view;

        if (convertView == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.listitem_email, parent, false);
            holder.picture = (ImageView) view.findViewById(R.id.contact_picture);
            //holder.emailSelected = view.findViewById(R.id.email_selected);
            holder.subject = (TextView) view.findViewById(R.id.email_subject);
            holder.address = (TextView) view.findViewById(R.id.email_address);
            holder.content = (TextView) view.findViewById(R.id.email_content);
            holder.sent = (TextView) view.findViewById(R.id.email_sent);
            holder.emailAttachment = view.findViewById(R.id.email_attachment);
            holder.emailStatus = (TextView) view.findViewById(R.id.email_status);
            holder.emailDelivered = view.findViewById(R.id.email_delivered);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

        final Email email = getItem(position);

        if (!mSelector.inActionMode())
            holder.picture.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    mSelector.select(view);
                }
            });

        // TODO fix
        //if (mSelectedEmails.get(position)) {
        //    holder.emailSelected.setVisibility(View.VISIBLE);
        //}

        try {
            String otherAddress;
            if (BoteHelper.isSentEmail(email))
                otherAddress = email.getOneRecipient();
            else
                otherAddress = email.getOneFromAddress();

            Bitmap pic = BoteHelper.getPictureForAddress(otherAddress);
            if (pic != null)
                holder.picture.setImageBitmap(pic);
            else if (BoteHelper.isSentEmail(email) || !email.isAnonymous()) {
                ViewGroup.LayoutParams lp = holder.picture.getLayoutParams();
                holder.picture.setImageBitmap(BoteHelper.getIdenticonForAddress(otherAddress, lp.width, lp.height));
            } else
                holder.picture.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.ic_contact_picture));

            holder.subject.setText(email.getSubject());
            holder.address.setText(BoteHelper.getNameAndShortDestination(otherAddress));

            Date date = email.getSentDate();
            if (date == null)
                date = email.getReceivedDate();
            if (date != null) {
                DateFormat df;
                Calendar boundary = Calendar.getInstance();
                boundary.set(Calendar.HOUR, 0);
                boundary.set(Calendar.MINUTE, 0);
                boundary.set(Calendar.SECOND, 0);
                if (date.before(boundary.getTime())) {
                    boundary.set(Calendar.MONTH, Calendar.JANUARY);
                    boundary.set(Calendar.DAY_OF_MONTH, 1);
                    if (date.before(boundary.getTime())) // Sent before this year
                        df = DateFormat.getDateInstance(DateFormat.MEDIUM);
                    else { // Sent this year before today
                        String yearlessPattern = ((SimpleDateFormat) SimpleDateFormat.getDateInstance(DateFormat.MEDIUM))
                                .toPattern().replaceAll(",?\\W?[Yy]+\\W?", "");
                        df = new SimpleDateFormat(yearlessPattern);
                    }
                } else // Sent today
                    df = DateFormat.getTimeInstance();
                holder.sent.setText(df.format(date));
                holder.sent.setVisibility(View.VISIBLE);
            } else
                holder.sent.setVisibility(View.GONE);

            holder.emailAttachment.setVisibility(View.GONE);
            List<Part> parts = email.getParts();
            for (Part part : parts) {
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    holder.emailAttachment.setVisibility(View.VISIBLE);
                    break;
                }
            }

            holder.subject.setTypeface(email.isUnread() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            holder.address.setTypeface(email.isUnread() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            if (email.isAnonymous() && !BoteHelper.isSentEmail(email)) {
                if (email.isUnread())
                    holder.address.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
                else
                    holder.address.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            }

            // Set email sending status if this is the outbox,
            // or set email delivery status if we sent it.
            if (mIsOutbox) {
                holder.emailStatus.setText(BoteHelper.getEmailStatusText(
                        getContext(), email, false));
                holder.emailStatus.setVisibility(View.VISIBLE);
            } else if (BoteHelper.isSentEmail(email)) {
                if (email.isDelivered()) {
                    holder.emailStatus.setVisibility(View.GONE);
                } else {
                    holder.emailStatus.setText(email.getDeliveryPercentage() + "%");
                    holder.emailStatus.setVisibility(View.VISIBLE);
                }
            }
            holder.emailDelivered.setVisibility(
                    !mIsOutbox && BoteHelper.isSentEmail(email) && email.isDelivered() ?
                            View.VISIBLE : View.GONE);
        } catch (Exception e) {
            holder.subject.setText("ERROR: " + e.getMessage());
        }
        holder.content.setText(email.getText());

        return view;
    }
}

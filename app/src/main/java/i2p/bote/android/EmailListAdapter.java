package i2p.bote.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.mail.Part;

import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.MultiSelectionUtil;
import i2p.bote.email.Email;

public class EmailListAdapter extends MultiSelectionUtil.SelectableAdapter<RecyclerView.ViewHolder> {
    private static final DateFormat DATE_BEFORE_THIS_YEAR = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private static final DateFormat DATE_THIS_YEAR = new SimpleDateFormat(
            ((SimpleDateFormat) SimpleDateFormat.getDateInstance(DateFormat.MEDIUM))
                    .toPattern().replaceAll(",?\\W?[Yy]+\\W?", "")
    );
    private static final DateFormat DATE_TODAY = DateFormat.getTimeInstance();

    private Calendar BOUNDARY_DAY;
    private Calendar BOUNDARY_YEAR;

    private Context mCtx;
    private String mFolderName;
    private EmailListFragment.OnEmailSelectedListener mListener;
    private boolean mIsOutbox;
    private List<Email> mEmails;
    private int mIncompleteEmails;

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class EmailViewHolder extends RecyclerView.ViewHolder {
        public ImageView picture;
        //public ImageView emailSelected;
        public TextView subject;
        public TextView address;
        public TextView content;
        public TextView sent;
        public ImageView emailAttachment;
        public TextView emailStatus;
        public ImageView emailDelivered;

        public EmailViewHolder(View itemView) {
            super(itemView);

            picture = (ImageView) itemView.findViewById(R.id.contact_picture);
            //emailSelected = view.findViewById(R.id.email_selected);
            subject = (TextView) itemView.findViewById(R.id.email_subject);
            address = (TextView) itemView.findViewById(R.id.email_address);
            content = (TextView) itemView.findViewById(R.id.email_content);
            sent = (TextView) itemView.findViewById(R.id.email_sent);
            emailAttachment = (ImageView) itemView.findViewById(R.id.email_attachment);
            emailStatus = (TextView) itemView.findViewById(R.id.email_status);
            emailDelivered = (ImageView) itemView.findViewById(R.id.email_delivered);
        }
    }

    public EmailListAdapter(Context context, String folderName,
                            EmailListFragment.OnEmailSelectedListener listener) {
        super();
        mCtx = context;
        mFolderName = folderName;
        mListener = listener;
        mIsOutbox = BoteHelper.isOutbox(folderName);
        mIncompleteEmails = 0;
        setHasStableIds(true);

        setDateBoundaries();
    }

    /**
     * Set up the boundaries for date display formats.
     * <p/>
     * TODO: call this method at midnight to refresh the UI
     */
    public void setDateBoundaries() {
        BOUNDARY_DAY = Calendar.getInstance();
        BOUNDARY_DAY.set(Calendar.HOUR, 0);
        BOUNDARY_DAY.set(Calendar.MINUTE, 0);
        BOUNDARY_DAY.set(Calendar.SECOND, 0);

        BOUNDARY_YEAR = Calendar.getInstance();
        BOUNDARY_YEAR.set(Calendar.MONTH, Calendar.JANUARY);
        BOUNDARY_YEAR.set(Calendar.DAY_OF_MONTH, 1);
        BOUNDARY_YEAR.set(Calendar.HOUR, 0);
        BOUNDARY_YEAR.set(Calendar.MINUTE, 0);
        BOUNDARY_YEAR.set(Calendar.SECOND, 0);

        if (mEmails != null)
            notifyDataSetChanged();
    }

    public void setEmails(List<Email> emails) {
        mEmails = emails;
        notifyDataSetChanged();
    }

    public Email getEmail(int position) {
        if (mIncompleteEmails > 0)
            position--;

        if (position < 0)
            return null;

        return mEmails.get(position);
    }

    public void setIncompleteEmails(int incompleteEmails) {
        if (incompleteEmails > 0) {
            if (mIncompleteEmails == 0) {
                mIncompleteEmails = incompleteEmails;
                notifyItemInserted(0);
            } else {
                mIncompleteEmails = incompleteEmails;
                notifyItemChanged(0);
            }
        } else if (mIncompleteEmails > 0) {
            mIncompleteEmails = 0;
            notifyItemRemoved(0);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mEmails == null || mEmails.isEmpty())
            return R.layout.listitem_empty;

        if (mIncompleteEmails > 0)
            position--;

        return position < 0 ? R.layout.listitem_incomplete : R.layout.listitem_email;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        switch (viewType) {
            case R.layout.listitem_email:
                return new EmailViewHolder(v);
            default:
                return new SimpleViewHolder(v);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case R.layout.listitem_empty:
                ((TextView) holder.itemView).setText(
                        mCtx.getResources().getString(R.string.folder_empty));
                break;

            case R.layout.listitem_incomplete:
                ((TextView) holder.itemView).setText(
                        mCtx.getResources().getQuantityString(R.plurals.incomplete_emails,
                                mIncompleteEmails, mIncompleteEmails));
                break;

            case R.layout.listitem_email:
                final EmailViewHolder evh = (EmailViewHolder) holder;
                final Email email = getEmail(position);

                evh.picture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selectEmail(evh.getAdapterPosition(), evh.getItemId(), true);
                    }
                });
                evh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selectEmail(evh.getAdapterPosition(), evh.getItemId(), false);
                    }
                });
                evh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        selectEmail(evh.getAdapterPosition(), evh.getItemId(), true);
                        return true;
                    }
                });

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    evh.itemView.setSelected(isSelected(position));
                else
                    evh.itemView.setActivated(isSelected(position));
                // TODO fix
                //holder.emailSelected.setVisibility(isSelected(position) ? View.VISIBLE : View.GONE);

                try {
                    boolean isSentEmail = BoteHelper.isSentEmail(email);
                    String otherAddress;
                    if (isSentEmail)
                        otherAddress = email.getOneRecipient();
                    else
                        otherAddress = email.getOneFromAddress();

                    Bitmap pic = BoteHelper.getPictureForAddress(otherAddress);
                    if (pic != null)
                        evh.picture.setImageBitmap(pic);
                    else if (isSentEmail || !email.isAnonymous()) {
                        ViewGroup.LayoutParams lp = evh.picture.getLayoutParams();
                        evh.picture.setImageBitmap(BoteHelper.getIdenticonForAddress(otherAddress, lp.width, lp.height));
                    } else
                        evh.picture.setImageDrawable(
                                mCtx.getResources().getDrawable(R.drawable.ic_contact_picture));

                    evh.subject.setText(email.getSubject());
                    evh.address.setText(BoteHelper.getNameAndShortDestination(otherAddress));

                    Date date = email.getSentDate();
                    if (date == null)
                        date = email.getReceivedDate();
                    if (date != null) {
                        DateFormat df;
                        if (date.before(BOUNDARY_DAY.getTime())) {
                            if (date.before(BOUNDARY_YEAR.getTime())) // Sent before this year
                                df = DATE_BEFORE_THIS_YEAR;
                            else // Sent this year before today
                                df = DATE_THIS_YEAR;
                        } else // Sent today
                            df = DATE_TODAY;
                        evh.sent.setText(df.format(date));
                        evh.sent.setVisibility(View.VISIBLE);
                    } else
                        evh.sent.setVisibility(View.GONE);

                    evh.emailAttachment.setVisibility(View.GONE);
                    for (Part part : email.getParts()) {
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            evh.emailAttachment.setVisibility(View.VISIBLE);
                            break;
                        }
                    }

                    evh.subject.setTypeface(email.isUnread() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    evh.address.setTypeface(email.isUnread() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    if (email.isAnonymous() && !isSentEmail) {
                        if (email.isUnread())
                            evh.address.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
                        else
                            evh.address.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                    }

                    // Set email sending status if this is the outbox,
                    // or set email delivery status if we sent it.
                    if (mIsOutbox) {
                        evh.emailStatus.setText(BoteHelper.getEmailStatusText(
                                mCtx, email, false));
                        evh.emailStatus.setVisibility(View.VISIBLE);
                    } else if (isSentEmail) {
                        if (email.isDelivered()) {
                            evh.emailStatus.setVisibility(View.GONE);
                        } else {
                            evh.emailStatus.setText(email.getDeliveryPercentage() + "%");
                            evh.emailStatus.setVisibility(View.VISIBLE);
                        }
                    }
                    evh.emailDelivered.setVisibility(
                            !mIsOutbox && isSentEmail && email.isDelivered() ?
                                    View.VISIBLE : View.GONE);
                } catch (Exception e) {
                    evh.subject.setText("ERROR: " + e.getMessage());
                }
                evh.content.setText(email.getText());
                break;

            default:
                break;
        }
    }

    private void selectEmail(int position, long id, boolean selectorOnly) {
        if (selectorOnly || getSelector().inActionMode()) {
            getSelector().selectItem(position, id);
        } else {
            final Email email = getEmail(position);
            mListener.onEmailSelected(mFolderName, email.getMessageID());
        }
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mEmails == null || mEmails.isEmpty())
            return 1;

        return mIncompleteEmails > 0 ? mEmails.size() + 1 : mEmails.size();
    }

    public long getItemId(int position) {
        if (mEmails == null || mEmails.isEmpty())
            return 0;

        Email email = getEmail(position);
        return email == null ? 1 : email.getMessageID().hashCode();
    }
}

package i2p.bote.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;

import javax.mail.Address;
import javax.mail.MessagingException;

import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;

public class ViewEmailFragment extends Fragment {
    private String mFolderName;
    private String mMessageId;

    private boolean mIsAnonymous;

    public static ViewEmailFragment newInstance(
            String folderName, String messageId) {
        ViewEmailFragment f = new ViewEmailFragment();

        Bundle args = new Bundle();
        args.putString("folderName", folderName);
        args.putString("messageId", messageId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mFolderName = getArguments() != null ? getArguments().getString("folderName") : "inbox";
        mMessageId = getArguments() != null ? getArguments().getString("messageId") : "1";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_view_email, container, false);

        try {
            Email e = BoteHelper.getEmail(mFolderName, mMessageId);
            if (e != null) {
                displayEmail(e, v);
            } else {
                TextView subject = (TextView) v.findViewById(R.id.email_subject);
                subject.setText(R.string.email_not_found);
            }
        } catch (PasswordException e) {
            // TODO: Handle
            e.printStackTrace();
        }

        return v;
    }

    private void displayEmail(Email email, View v) {
        TextView subject = (TextView) v.findViewById(R.id.email_subject);
        ImageView picture = (ImageView) v.findViewById(R.id.picture);
        TextView sender = (TextView) v.findViewById(R.id.email_sender);
        LinearLayout recipients = (LinearLayout) v.findViewById(R.id.email_recipients);
        TextView sent = (TextView) v.findViewById(R.id.email_sent);
        TextView received = (TextView) v.findViewById(R.id.email_received);
        TextView content = (TextView) v.findViewById(R.id.email_content);

        try {
            String fromAddress = email.getOneFromAddress();

            subject.setText(email.getSubject());

            Bitmap pic = BoteHelper.getPictureForAddress(fromAddress);
            if (pic != null)
                picture.setImageBitmap(pic);
            else if (!email.isAnonymous()) {
                ViewGroup.LayoutParams lp = picture.getLayoutParams();
                picture.setImageBitmap(BoteHelper.getIdenticonForAddress(fromAddress, lp.width, lp.height));
            }

            sender.setText(BoteHelper.getDisplayAddress(fromAddress));

            Address[] emailRecipients = email.getToAddresses();
            if (emailRecipients != null) {
                for (Address recipient : emailRecipients) {
                    TextView tv = new TextView(getActivity());
                    tv.setText(BoteHelper.getDisplayAddress(recipient.toString()));
                    recipients.addView(tv);
                }
            }

            if (email.getSentDate() != null)
                sent.setText(DateFormat.getInstance().format(
                        email.getSentDate()));

            if (email.getReceivedDate() != null)
                received.setText(DateFormat.getInstance().format(
                        email.getReceivedDate()));

            content.setText(email.getText());

            // Prepare fields for replying
            mIsAnonymous = email.isAnonymous();
        } catch (MessagingException e) {
            // TODO Handle
            e.printStackTrace();
        } catch (PasswordException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (BoteHelper.isOutbox(mFolderName)) {
            ((TextView) v.findViewById(R.id.email_status)).setText(
                    BoteHelper.getEmailStatusText(getActivity(), email, true));
            ((TableRow) v.findViewById(R.id.email_status_row)).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_email, menu);
        if (mIsAnonymous)
            menu.findItem(R.id.action_reply).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reply:
            case R.id.action_reply_all:
            case R.id.action_forward:
                Intent nei = new Intent(getActivity(), NewEmailActivity.class);
                nei.putExtra(NewEmailFragment.QUOTE_MSG_FOLDER, mFolderName);
                nei.putExtra(NewEmailFragment.QUOTE_MSG_ID, mMessageId);
                NewEmailFragment.QuoteMsgType type = null;
                switch (item.getItemId()) {
                    case R.id.action_reply:
                        type = NewEmailFragment.QuoteMsgType.REPLY;
                        break;
                    case R.id.action_reply_all:
                        type = NewEmailFragment.QuoteMsgType.REPLY_ALL;
                        break;
                    case R.id.action_forward:
                        type = NewEmailFragment.QuoteMsgType.FORWARD;
                }
                nei.putExtra(NewEmailFragment.QUOTE_MSG_TYPE, type);
                startActivity(nei);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

package i2p.bote;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;

import javax.mail.Address;
import javax.mail.MessagingException;

import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.util.BoteHelper;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewEmailFragment extends Fragment {
    private String mFolderName;
    private String mMessageId;

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
                subject.setText("Email not found");
            }
        } catch (PasswordException e) {
            // TODO: Handle
            e.printStackTrace();
        }

        return v;
    }

    private void displayEmail(Email email, View v) {
        TextView subject = (TextView) v.findViewById(R.id.email_subject);
        TextView sender = (TextView) v.findViewById(R.id.email_sender);
        LinearLayout recipients = (LinearLayout) v.findViewById(R.id.email_recipients);
        TextView sent = (TextView) v.findViewById(R.id.email_sent);
        TextView received = (TextView) v.findViewById(R.id.email_received);
        TextView content = (TextView) v.findViewById(R.id.email_content);

        try {
            subject.setText(email.getSubject());

            sender.setText(BoteHelper.getDisplayAddress(
                    email.getOneFromAddress()));

            for (Address recipient : email.getToAddresses()) {
                TextView tv = new TextView(getActivity());
                tv.setText(BoteHelper.getDisplayAddress(recipient.toString()));
                recipients.addView(tv);
            }

            if (email.getSentDate() != null)
                sent.setText(DateFormat.getInstance().format(
                        email.getSentDate()));

            if (email.getReceivedDate() != null)
                received.setText(DateFormat.getInstance().format(
                        email.getReceivedDate()));

            content.setText(email.getText());
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
    }
}

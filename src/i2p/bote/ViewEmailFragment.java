package i2p.bote;

import javax.mail.MessagingException;

import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.util.BoteHelper;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        TextView subject = (TextView) v.findViewById(R.id.email_subject);

        try {
            Email e = BoteHelper.getEmail(mFolderName, mMessageId);
            if (e != null) {
                subject.setText(e.getSubject());
            } else {
                subject.setText("Email not found");
            }
        } catch (PasswordException e) {
            // TODO: Handle
            e.printStackTrace();
        } catch (MessagingException e) {
            // TODO Handle
            e.printStackTrace();
        }

        return v;
    }

    public String getMessageId() {
        return mMessageId;
    }
}

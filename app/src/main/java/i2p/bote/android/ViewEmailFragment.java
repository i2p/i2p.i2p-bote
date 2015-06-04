package i2p.bote.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Part;

import i2p.bote.android.provider.AttachmentProvider;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.ContentAttachment;
import i2p.bote.email.Attachment;
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
        View sigInvalid = v.findViewById(R.id.signature_invalid);
        TextView subject = (TextView) v.findViewById(R.id.email_subject);
        ImageView picture = (ImageView) v.findViewById(R.id.picture);
        TextView sender = (TextView) v.findViewById(R.id.email_sender);
        LinearLayout toRecipients = (LinearLayout) v.findViewById(R.id.email_to);
        TextView sent = (TextView) v.findViewById(R.id.email_sent);
        TextView received = (TextView) v.findViewById(R.id.email_received);
        TextView content = (TextView) v.findViewById(R.id.email_content);
        LinearLayout attachments = (LinearLayout) v.findViewById(R.id.attachments);

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

            final String senderDisplay = BoteHelper.getDisplayAddress(fromAddress);

            if (!email.isSignatureValid() && !email.isAnonymous()) {
                sigInvalid.setVisibility(View.VISIBLE);
                sigInvalid.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getActivity(), getString(R.string.signature_invalid, senderDisplay), Toast.LENGTH_LONG).show();
                    }
                });
            }

            sender.setText(senderDisplay);
            if (email.isAnonymous() && !BoteHelper.isSentEmail(email))
                sender.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);

            Address[] emailToRecipients = email.getToAddresses();
            if (emailToRecipients != null) {
                for (Address recipient : emailToRecipients) {
                    TextView tv = new TextView(getActivity());
                    tv.setText(BoteHelper.getDisplayAddress(recipient.toString()));
                    tv.setTextAppearance(getActivity(), R.style.TextAppearance_AppCompat_Secondary);
                    toRecipients.addView(tv);
                }
            }

            Address[] emailCcRecipients = email.getCCAddresses();
            if (emailCcRecipients != null) {
                v.findViewById(R.id.email_cc_row).setVisibility(View.VISIBLE);
                LinearLayout ccRecipients = (LinearLayout) v.findViewById(R.id.email_cc);
                for (Address recipient : emailCcRecipients) {
                    TextView tv = new TextView(getActivity());
                    tv.setText(BoteHelper.getDisplayAddress(recipient.toString()));
                    tv.setTextAppearance(getActivity(), R.style.TextAppearance_AppCompat_Secondary);
                    ccRecipients.addView(tv);
                }
            }

            Address[] emailBccRecipients = email.getBCCAddresses();
            if (emailBccRecipients != null) {
                v.findViewById(R.id.email_bcc_row).setVisibility(View.VISIBLE);
                LinearLayout bccRecipients = (LinearLayout) v.findViewById(R.id.email_bcc);
                for (Address recipient : emailBccRecipients) {
                    TextView tv = new TextView(getActivity());
                    tv.setText(BoteHelper.getDisplayAddress(recipient.toString()));
                    tv.setTextAppearance(getActivity(), R.style.TextAppearance_AppCompat_Secondary);
                    bccRecipients.addView(tv);
                }
            }

            if (email.getSentDate() != null)
                sent.setText(DateFormat.getInstance().format(
                        email.getSentDate()));

            if (email.getReceivedDate() != null)
                received.setText(DateFormat.getInstance().format(
                        email.getReceivedDate()));

            content.setText(email.getText());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                content.setTextIsSelectable(true);

            List<Part> parts = email.getParts();
            for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
                Part part = parts.get(partIndex);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    final ContentAttachment attachment = new ContentAttachment(getActivity(), part);

                    View a = getActivity().getLayoutInflater().inflate(R.layout.listitem_attachment, attachments, false);
                    ((TextView) a.findViewById(R.id.filename)).setText(attachment.getFileName());
                    ((TextView) a.findViewById(R.id.size)).setText(attachment.getHumanReadableSize());

                    final ImageView action = (ImageView) a.findViewById(R.id.attachment_action);
                    action.setImageDrawable(new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_more_vert).colorRes(R.color.md_grey_600).sizeDp(24).paddingDp(4));
                    action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            PopupMenu popup = new PopupMenu(getActivity(), action);
                            popup.inflate(R.menu.attachment);
                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    switch (menuItem.getItemId()) {
                                        case R.id.save_attachment:
                                            saveAttachment(attachment);
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            });
                            popup.show();
                        }
                    });

                    final Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(AttachmentProvider.getUriForAttachment(mFolderName, mMessageId, partIndex));
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    a.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(i);
                        }
                    });

                    attachments.addView(a);
                }
            }

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
            v.findViewById(R.id.email_status_row).setVisibility(View.VISIBLE);
        }
    }

    private void saveAttachment(Attachment attachment) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = attachment.getFileName();
        int extInd = fileName.lastIndexOf('.');
        String name = fileName.substring(0, extInd);
        String ext = fileName.substring(extInd);
        File outFile = new File(downloadsDir, fileName);
        for (int i = 1; outFile.exists() && i < 32; i++) {
            fileName = name + "-" + i + ext;
            outFile = new File(downloadsDir, fileName);
        }
        if (outFile.exists()) {
            Toast.makeText(getActivity(), R.string.file_exists_in_downloads, Toast.LENGTH_SHORT).show();
            return;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            attachment.getDataHandler().writeTo(out);
            Toast.makeText(getActivity(),
                    getResources().getString(R.string.saved_to_downloads, fileName),
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.could_not_save_to_downloads, Toast.LENGTH_SHORT).show();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_email, menu);
        MenuItem reply = menu.findItem(R.id.action_reply);

        reply.setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_reply));
        menu.findItem(R.id.action_reply_all).setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_reply_all));
        menu.findItem(R.id.action_forward).setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_forward));

        if (mIsAnonymous)
            reply.setVisible(false);
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

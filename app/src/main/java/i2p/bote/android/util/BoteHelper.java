package i2p.bote.android.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.lambdaworks.codec.Base64;

import i2p.bote.android.R;
import i2p.bote.email.Email;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.Outbox.EmailStatus;
import i2p.bote.packet.dht.Contact;
import i2p.bote.util.GeneralHelper;

public class BoteHelper extends GeneralHelper {
    /**
     * Get the translated name of the folder.
     * Built-in folders are special-cased; other folders are created by the
     * user, so their name is already "translated".
     * @param ctx Android Context to get strings from.
     * @param folder The folder.
     * @return The name of the folder.
     * @throws PasswordException
     */
    public static String getFolderDisplayName(Context ctx, EmailFolder folder) {
        String name = folder.getName();
        if ("inbox".equals(name))
            return ctx.getResources().getString(R.string.folder_inbox);
        else if ("outbox".equals(name))
            return ctx.getResources().getString(R.string.folder_outbox);
        else if ("sent".equals(name))
            return ctx.getResources().getString(R.string.folder_sent);
        else if ("trash".equals(name))
            return ctx.getResources().getString(R.string.folder_trash);
        else
            return name;
    }

    /**
     * Get the translated name of the folder with the number of
     * new messages it contains appended.
     * @param ctx Android Context to get strings from.
     * @param folder The folder.
     * @return The name of the folder.
     * @throws PasswordException
     */
    public static String getFolderDisplayNameWithNew(Context ctx, EmailFolder folder) throws PasswordException {
        String displayName = getFolderDisplayName(ctx, folder);

        int numNew = folder.getNumNewEmails();
        if (numNew > 0)
            displayName = displayName + " (" + numNew + ")";

        return displayName;
    }

    public static Drawable getFolderIcon(Context ctx, EmailFolder folder) {
        String name = folder.getName();
        if ("inbox".equals(name))
            return ctx.getResources().getDrawable(R.drawable.ic_inbox_grey600_24dp);
        else if ("outbox".equals(name))
            return ctx.getResources().getDrawable(R.drawable.ic_cloud_upload_grey600_24dp);
        else if ("sent".equals(name))
            return ctx.getResources().getDrawable(R.drawable.ic_send_grey600_24dp);
        else if ("trash".equals(name))
            return ctx.getResources().getDrawable(R.drawable.ic_delete_grey600_24dp);
        else
            return null;
    }

    public static String getDisplayAddress(String address) throws PasswordException, IOException, GeneralSecurityException, MessagingException {
        String fullAdr = getNameAndDestination(address);
        String emailDest = extractEmailDestination(fullAdr);
        String name = extractName(fullAdr);

        return (emailDest == null ? address
                : (name.isEmpty() ? emailDest.substring(0, 10)
                        : name + " <" + emailDest.substring(0, 10) + "...>"));
    }

    /**
     * Get a Bitmap containing the picture for the contact or identity
     * corresponding to the given address.
     * @param address
     * @return a Bitmap, or null if no picture was found.
     * @throws PasswordException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static Bitmap getPictureForAddress(String address) throws PasswordException, IOException, GeneralSecurityException {
        String base64dest = extractEmailDestination(address);

        if (base64dest != null) {
            return getPictureForDestination(base64dest);
        }

        // Address not found anywhere, or found and has no picture
        return null;
    }

    /**
     * Get a Bitmap containing the picture for the contact or identity
     * corresponding to the given Destination.
     * @param base64dest
     * @return a Bitmap, or null if no picture was found.
     * @throws PasswordException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static Bitmap getPictureForDestination(String base64dest) throws PasswordException, IOException, GeneralSecurityException {
        // Address was found; try address book first
        Contact c = getContact(base64dest);
        if (c != null) {
            // Address is in address book
            String pic = c.getPictureBase64();
            if (pic != null) {
                return decodePicture(pic);
            }
        } else {
            // Address is an identity
            EmailIdentity i = getIdentity(base64dest);
            if (i != null) {
                String pic = i.getPictureBase64();
                if (pic != null) {
                    return decodePicture(pic);
                }
            }
        }
        // Address is not known
        return null;
    }

    public static Bitmap decodePicture(String picB64) {
        if (picB64 == null)
            return null;
        byte[] decodedPic = Base64.decode(picB64.toCharArray());
        return BitmapFactory.decodeByteArray(decodedPic, 0, decodedPic.length);
    }

    public static String encodePicture(Bitmap picture) {
        if (picture == null)
            return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        picture.compress(CompressFormat.PNG, 0, baos);
        return new String(Base64.encode(baos.toByteArray()));
    }

    public static boolean isSentEmail(Email email) throws PasswordException, IOException, GeneralSecurityException, MessagingException {
        // Is the sender anonymous and we are not the recipient?
        if (email.isAnonymous()) {
            Address[] recipients = email.getAllRecipients();
            for (int i = 0; i < recipients.length; i++) {
                String toDest = EmailDestination.extractBase64Dest(recipients[i].toString());
                if (toDest != null && getIdentity(toDest) != null)
                    // We are a recipient
                    return false;
            }
            // We are not a recipient
            return true;
        }

        // Are we the sender?
        String fromAddress = email.getOneFromAddress();
        String fromDest = EmailDestination.extractBase64Dest(fromAddress);
        if ((fromDest != null && getIdentity(fromDest) != null))
            return true;

        // We are not the sender
        return false;
    }

    public static String getEmailStatusText(Context ctx, Email email, boolean full) {
        Resources res = ctx.getResources();
        EmailStatus emailStatus = getEmailStatus(email);
        switch (emailStatus.getStatus()) {
        case QUEUED:
            return res.getString(R.string.queued);
        case SENDING:
            return res.getString(R.string.sending);
        case SENT_TO:
            if (full)
                return res.getString(R.string.sent_to,
                        (Integer) emailStatus.getParam1(), (Integer) emailStatus.getParam2());
            else
                return res.getString(R.string.sent_to_short,
                        (Integer) emailStatus.getParam1(), (Integer) emailStatus.getParam2());
        case EMAIL_SENT:
            return res.getString(R.string.email_sent);
        case GATEWAY_DISABLED:
            return res.getString(R.string.gateway_disabled);
        case NO_IDENTITY_MATCHES:
            if (full)
                return res.getString(R.string.no_identity_matches,
                        emailStatus.getParam1());
        case INVALID_RECIPIENT:
            if (full)
                return res.getString(R.string.invalid_recipient,
                        emailStatus.getParam1());
        case ERROR_CREATING_PACKETS:
            if (full)
                return res.getString(R.string.error_creating_packets,
                        emailStatus.getParam1());
        case ERROR_SENDING:
            if (full)
                return res.getString(R.string.error_sending,
                        emailStatus.getParam1());
        case ERROR_SAVING_METADATA:
            if (full)
                return res.getString(R.string.error_saving_metadata,
                        emailStatus.getParam1());
        default:
            // Short string for errors and unknown status
            return res.getString(R.string.error);
        }
    }

    public static boolean isInbox(EmailFolder folder) {
        return isInbox(folder.getName());
    }

    public static boolean isInbox(String folderName) {
        return "Inbox".equalsIgnoreCase(folderName);
    }

    public static boolean isOutbox(EmailFolder folder) {
        return isOutbox(folder.getName());
    }

    public static boolean isOutbox(String folderName) {
        return "Outbox".equalsIgnoreCase(folderName);
    }

    public static boolean isTrash(EmailFolder folder) {
        return isTrash(folder.getName());
    }

    public static boolean isTrash(String folderName) {
        return "Trash".equalsIgnoreCase(folderName);
    }

    public static List<Email> getRecentEmails(EmailFolder folder) throws PasswordException, MessagingException {
        List<Email> emails = folder.getElements();
        Iterator<Email> iter = emails.iterator();
        while (iter.hasNext()) {
            Email email = iter.next();
            if (!email.isSet(Flag.RECENT))
                iter.remove();
        }
        return emails;
    }

    public interface RequestPasswordListener {
        public void onPasswordVerified();
        public void onPasswordCanceled();
    }

    /**
     * Request the password from the user, and try it.
     */
    public static void requestPassword(final Context context, final RequestPasswordListener listener) {
        requestPassword(context, listener, null);
    }

    /**
     * Request the password from the user, and try it.
     *
     * @param error is pre-filled in the dialog if not null.
     */
    public static void requestPassword(final Context context, final RequestPasswordListener listener, String error) {
        LayoutInflater li = LayoutInflater.from(context);
        View promptView = li.inflate(R.layout.dialog_password, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(promptView);

        final EditText passwordInput = (EditText) promptView.findViewById(R.id.passwordInput);
        if (error != null) {
            TextView passwordError = (TextView) promptView.findViewById(R.id.passwordError);
            passwordError.setText(error);
            passwordError.setVisibility(View.VISIBLE);
        }

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordInput.getWindowToken(), 0);
                dialog.dismiss();
                new PasswordWaiter(context, listener).execute(passwordInput.getText().toString());
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                if (listener != null)
                    listener.onPasswordCanceled();
            }
        }).setCancelable(false);
        AlertDialog passwordDialog = builder.create();
        passwordDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        passwordDialog.show();
    }

    private static class PasswordWaiter extends AsyncTask<String, Void, String> {
        private final Context mContext;
        private final ProgressDialog mDialog;
        private final RequestPasswordListener mListener;

        public PasswordWaiter(Context context, RequestPasswordListener listener) {
            super();
            mContext = context;
            mDialog = new ProgressDialog(context);
            mListener = listener;
        }

        protected void onPreExecute() {
            mDialog.setMessage(mContext.getResources().getString(
                    R.string.checking_password));
            mDialog.setCancelable(false);
            mDialog.show();
        }

        protected String doInBackground(String... params) {
            try {
                if (BoteHelper.tryPassword(params[0]))
                    return null;
                else {
                    cancel(false);
                    return mContext.getResources().getString(
                            R.string.password_incorrect);
                }
            } catch (IOException e) {
                cancel(false);
                return mContext.getResources().getString(
                        R.string.password_file_error);
            } catch (GeneralSecurityException e) {
                cancel(false);
                return mContext.getResources().getString(
                        R.string.password_file_error);
            }
        }

        protected void onCancelled(String result) {
            mDialog.dismiss();
            requestPassword(mContext, mListener, result);
        }

        protected void onPostExecute(String result) {
            // Password is valid
            mDialog.dismiss();
            if (mListener != null)
                mListener.onPasswordVerified();
        }
    }
}

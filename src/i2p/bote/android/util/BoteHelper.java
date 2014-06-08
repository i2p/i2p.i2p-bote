package i2p.bote.android.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Base64;
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
     * @param showNew Should the name contain the number of new messages?
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
        String fullAdr = getNameAndDestination(address);

        if (!address.equals(fullAdr)) {
            // Address was found; try address book first
            String base64dest = EmailDestination.extractBase64Dest(fullAdr);
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
        }

        // Address not found anywhere, or found and has no picture
        return null;
    }

    public static Bitmap decodePicture(String picB64) {
        if (picB64 == null)
            return null;
        byte[] decodedPic = Base64.decode(picB64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedPic, 0, decodedPic.length);
    }

    public static String encodePicture(Bitmap picture) {
        if (picture == null)
            return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // TODO something is corrupting here
        picture.compress(CompressFormat.PNG, 0, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
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
                        emailStatus.getParam1(), emailStatus.getParam2());
            else
                return res.getString(R.string.sent_to_short,
                        emailStatus.getParam1(), emailStatus.getParam2());
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

    public static boolean isOutbox(EmailFolder folder) {
        return isOutbox(folder.getName());
    }

    public static boolean isOutbox(String folderName) {
        return "Outbox".equalsIgnoreCase(folderName);
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
}

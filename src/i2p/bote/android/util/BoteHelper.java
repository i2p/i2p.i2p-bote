package i2p.bote.android.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.mail.MessagingException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Base64;
import i2p.bote.android.R;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
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
}

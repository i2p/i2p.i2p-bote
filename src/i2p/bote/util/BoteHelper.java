package i2p.bote.util;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.mail.MessagingException;

import android.content.Context;

import i2p.bote.R;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;

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
}

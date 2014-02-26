package i2p.bote;

import java.security.GeneralSecurityException;
import java.util.List;

import android.content.Context;

import i2p.bote.email.AddressDisplayFilter;
import i2p.bote.email.Email;
import i2p.bote.email.EmailAttribute;
import i2p.bote.email.Identities;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;

public class BoteHelper {
    private static AddressDisplayFilter ADDRESS_DISPLAY_FILTER;

    /**
     * Get an EmailFolder. If the folder is not found, returns null.
     * @param folderName The folder to get.
     * @return the EmailFolder.
     */
    public static EmailFolder getMailFolder(String folderName) {
        List<EmailFolder> folders = I2PBote.getInstance().getEmailFolders();
        for (EmailFolder folder : folders) {
            if (folder.getName().equals(folderName))
                return folder;
        }
        return null;
    }

    /**
     * Get the translated name of the folder.
     * Built-in folders are special-cased; other folders are created by the
     * user, so their name is already "translated".
     * @param ctx Android Context to get strings from.
     * @param folder The folder.
     * @return The name of the folder.
     */
    public static String getFolderDisplayName(Context ctx, EmailFolder folder) {
        String displayName = "";

        String name = folder.getName();
        if ("inbox".equals(name))
            displayName = ctx.getResources().getString(R.string.folder_inbox);
        else if ("outbox".equals(name))
            displayName = ctx.getResources().getString(R.string.folder_outbox);
        else if ("sent".equals(name))
            displayName = ctx.getResources().getString(R.string.folder_sent);
        else if ("trash".equals(name))
            displayName = ctx.getResources().getString(R.string.folder_trash);
        else
            displayName = name;

        try {
            int numUnread = folder.getNumNewEmails();
            if (numUnread > 0)
                displayName = displayName + " (" + numUnread + ")";
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
        } catch (PasswordException e) {
            // TODO Auto-generated catch block
        }

        return displayName;
    }

    public static List<Email> getEmails(EmailFolder folder, EmailAttribute sortColumn, boolean descending) throws PasswordException {
        return folder.getElements(getAddressDisplayFilter(), sortColumn, descending);
    }

    private static AddressDisplayFilter getAddressDisplayFilter() throws PasswordException {
        Identities identities = I2PBote.getInstance().getIdentities();
        if (ADDRESS_DISPLAY_FILTER == null)
            ADDRESS_DISPLAY_FILTER = new AddressDisplayFilter(identities, I2PBote.getInstance().getAddressBook());
        return ADDRESS_DISPLAY_FILTER;
    }
}

package i2p.bote;

import java.security.GeneralSecurityException;
import android.content.Context;

import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
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
     */
    public static String getFolderDisplayName(Context ctx, EmailFolder folder, boolean showNew) {
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

        if (showNew) {
            try {
                int numNew = folder.getNumNewEmails();
                if (numNew > 0)
                    displayName = displayName + " (" + numNew + ")";
            } catch (GeneralSecurityException e) {
                // TODO Auto-generated catch block
            } catch (PasswordException e) {
                // TODO Auto-generated catch block
            }
        }

        return displayName;
    }
}

package i2p.bote.folder;

import java.io.File;

/**
 * Subclassed for distinction between the trash folder and non-trash folders.
 */
public class TrashFolder extends EmailFolder {

    public TrashFolder(File storageDir) {
        super(storageDir);
    }
}
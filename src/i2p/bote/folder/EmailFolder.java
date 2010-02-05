/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 * 
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 * 
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.folder;

import i2p.bote.UniqueId;
import i2p.bote.email.Email;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.mail.MessagingException;

import net.i2p.util.Log;

/**
 * Stores emails in a directory on the file system. Each email is stored in one file.
 * Filenames are in the format <code><N, O>_<message ID>.mail</code>, where
 * N is for new (unread) and O is for old (read).
 */
public class EmailFolder extends Folder<Email> {
    protected static final String EMAIL_FILE_EXTENSION = ".mail";
    
    private Log log = new Log(EmailFolder.class);
    
    public EmailFolder(File storageDir) {
        super(storageDir, EMAIL_FILE_EXTENSION);
    }
    
    // store an email file
    public void add(Email email) throws IOException, MessagingException {
        // write out the email file
        File emailFile = getEmailFile(email);
        log.info("Mail folder <" + storageDir + ">: storing email file: <" + emailFile.getAbsolutePath() + ">");
        OutputStream emailOutputStream = new FileOutputStream(emailFile);
        email.writeTo(emailOutputStream);
        emailOutputStream.close();
    }
    
    /**
     * Finds an <code>Email</code> by message id. If the <code>Email</code> is
     * not found, <code>null</code> is returned.
     * The <code>messageId</code> parameter must be a 44-character base64-encoded
     * {@link UniqueId}.
     * @param messageIdString
     * @return
     */
    public Email getEmail(String messageIdString) {
        UniqueId messageId = new UniqueId(messageIdString);
        
        File file = getEmailFile(messageId);
        try {
            return createFolderElement(file);
        }
        catch (Exception e) {
            log.error("Can't read email from file: <" + file.getAbsolutePath() + ">", e);
            return null;
        }
    }
    
    private File getEmailFile(Email email) {
        return getEmailFile(email.getUniqueID(), email.isNew());
    }

    /**
     * Returns a file for a given message ID, or <code>null</code> if there is none.
     * @param messageId
     * @return
     */
    private File getEmailFile(UniqueId messageId) {
        // try new email
        File newEmailFile = getEmailFile(messageId, true);
        if (newEmailFile.exists())
            return newEmailFile;
        
        // try old email
        File oldEmailFile = getEmailFile(messageId, false);
        if (oldEmailFile.exists())
            return oldEmailFile;
        
        return null;
    }
    
    private File getEmailFile(UniqueId messageId, boolean newIndicator) {
        return new File(storageDir, (newIndicator?'N':'O') + "_" + messageId.toBase64() + EMAIL_FILE_EXTENSION);
    }
    
    /**
     * @see Folder.getNumElements()
     * @return
     */
    public int getNumNewEmails() {
        int numNew = 0;
        for (File file: getFilenames())
            if (isNew(file))
                numNew++;
        
        return numNew;
    }
    
    private boolean isNew(File file) {
        switch (file.getName().charAt(0)) {
        case 'N':
            return true;
        case 'O':
            return false;
        default:
            throw new IllegalArgumentException("Illegal email filename, doesn't start with N or O: <" + file.getAbsolutePath() + ">");
        }
    }
    
    /**
     * Flags an email "new" (if <code>isNew</code> is <code>true</code>) or
     * "old" (if <code>isNew</code> is <code>false</code>).
     * @param messageIdString
     * @param isNew
     */
    public void setNew(String messageIdString, boolean isNew) {
        UniqueId messageId = new UniqueId(messageIdString);
        File file = getEmailFile(messageId);
        if (file != null) {
            char newIndicator = isNew?'N':'O';   // the new start character
            String newFilename = newIndicator + file.getName().substring(1);
            File newFile = new File(file.getParentFile(), newFilename);
            boolean success = file.renameTo(newFile);
            if (!success)
                log.error("Cannot rename <" + file.getAbsolutePath() + "> to <" + newFile.getAbsolutePath() + ">");
        }
        else
            log.error("No email found for message Id: <" + messageId + ">");
    }
    
    /**
     * Deletes an email with a given message ID.
     * @param messageIdString
     * @return <code>true</code> if the email was deleted, <code>false</code> otherwise
     */
    public boolean delete(String messageIdString) {
        UniqueId messageId = new UniqueId(messageIdString);
        File emailFile = getEmailFile(messageId);
        if (emailFile != null)
            return emailFile.delete();
        else
            return false;
    }
    
    public void delete(Email email) {
        if (!getEmailFile(email).delete())
            log.error("Cannot delete file: '" + getEmailFile(email) + "'");
    }

    @Override
    protected Email createFolderElement(File file) throws Exception {
        Email email = new Email(file);
        email.setNew(isNew(file));
        
        String messageIdString = file.getName().substring(2, 46);
        email.setMessageId(new UniqueId(messageIdString));
        
        return email;
    }
}
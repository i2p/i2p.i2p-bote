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
import i2p.bote.Util;
import i2p.bote.email.AddressDisplayFilter;
import i2p.bote.email.Email;
import i2p.bote.email.EmailAttribute;
import i2p.bote.email.EmailMetadata;
import i2p.bote.fileencryption.DerivedKey;
import i2p.bote.fileencryption.EncryptedInputStream;
import i2p.bote.fileencryption.EncryptedOutputStream;
import i2p.bote.fileencryption.FileEncryptionUtil;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordHolder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.mail.MessagingException;

import net.i2p.util.Log;
import net.i2p.util.SecureFileOutputStream;

/**
 * Stores emails in a directory on the file system.<br/>
 * Two files are stored for each email; one email file with the name
 * <code>&lt;message ID&gt;.mail</code>, and a metadata file with the name
 * <code>&lt;message ID&gt;.meta</code>.
 */
public class EmailFolder extends Folder<Email> {
    private static final String EMAIL_FILE_EXTENSION = ".mail";
    private static final String METADATA_FILE_EXTENSION = ".meta";
    
    private Log log = new Log(EmailFolder.class);
    private PasswordHolder passwordHolder;
    private Collection<FolderListener> folderListeners;
    
    public EmailFolder(File storageDir, PasswordHolder passwordHolder) {
        super(storageDir, EMAIL_FILE_EXTENSION);
        this.passwordHolder = passwordHolder;
        folderListeners = new ArrayList<FolderListener>();
    }

    /**
     * Stores an email in the folder. If an email with the same
     * message ID exists already, nothing happens.
     * @param email
     * @throws IOException
     * @throws MessagingException
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     */
    public void add(Email email) throws IOException, MessagingException, PasswordException, GeneralSecurityException {
        // check if an email exists already with that message id
        if (getEmailFile(email.getMessageID()).exists()) {
            log.debug("Not storing email because there is an existing one with the same message ID: <" + email.getMessageID()+ ">");
            return;
        }
        
        // write out the email file
        File emailFile = getEmailFile(email);
        log.info("Mail folder <" + storageDir + ">: storing email file: <" + emailFile.getAbsolutePath() + ">");
        OutputStream emailOutputStream = new BufferedOutputStream(new EncryptedOutputStream(new SecureFileOutputStream(emailFile), passwordHolder));
        try {
            email.writeTo(emailOutputStream);
        }
        finally {
            emailOutputStream.close();
        }
        
        saveMetadata(email);
        
        for (FolderListener listener: folderListeners)
            listener.elementAdded(email.getMessageID());
    }
    
    public void changePassword(byte[] oldPassword, DerivedKey newKey) throws FileNotFoundException, IOException, GeneralSecurityException, PasswordException {
        for (File emailFile: getFilenames()) {   // getFilenames() only returns email files but not metadata files
            FileEncryptionUtil.changePassword(emailFile, oldPassword, newKey);
            File metadataFile = getMetadataFile(emailFile);
            if (metadataFile.exists())
                FileEncryptionUtil.changePassword(metadataFile, oldPassword, newKey);
        }
    }
    
    /**
     * Returns all emails in the folder, in the order specified by <code>sortColumn</code>.
     * @throws PasswordException 
     */
    public List<Email> getElements(AddressDisplayFilter displayFilter, EmailAttribute sortColumn, boolean descending) throws PasswordException {
        Comparator<Email> comparator = new EmailComparator(sortColumn, displayFilter);
        if (descending)
            comparator = Collections.reverseOrder(comparator);
        
        List<Email> emails = getElements();
        Collections.sort(emails, comparator);
        return emails;
    }
    
    /**
     * A <code>Comparator</code> for sorting emails by a given {@link EmailAttribute}.
     * If <code>attribute</code> is <code>null</code>, the date field is used.
     */
    private class EmailComparator implements Comparator<Email> {
        private EmailAttribute attribute;
        private AddressDisplayFilter displayFilter;
        
        public EmailComparator(EmailAttribute attribute, AddressDisplayFilter displayFilter) {
            if (attribute == null)
                attribute = EmailAttribute.DATE;
            this.attribute = attribute;
            this.displayFilter = displayFilter;
        }
        
        @Override
        public int compare(Email email1, Email email2) {
            Comparable<?> value1;
            Object value2;
            
            try {
                switch(attribute) {
                case DATE:
                    // use the sent date if there is one, otherwise use the received date
                    value1 = email1.getSentDate();
                    if (value1 == null)
                        value1 = email1.getReceivedDate();
                    value2 = email2.getSentDate();
                    if (value2 == null)
                        value2 = email2.getReceivedDate();
                    break;
                case FROM:
                    value1 = displayFilter.getNameAndDestination(email1.getOneFromAddress());
                    value2 = displayFilter.getNameAndDestination(email2.getOneFromAddress());
                    break;
                case TO:
                    value1 = displayFilter.getNameAndDestination(email1.getOneRecipient());
                    value2 = displayFilter.getNameAndDestination(email2.getOneRecipient());
                    break;
                case CREATE_TIME:
                    value1 = email1.getCreateTime();
                    value2 = email2.getCreateTime();
                    break;
                case SUBJECT:
                    value1 = email1.getSubject();
                    value2 = email2.getSubject();
                    break;
                case DELIVERED:
                    value1 = email1.getDeliveryPercentage();
                    value2 = email2.getDeliveryPercentage();
                    break;
                default:
                    log.error("Unknown email attribute type: " + attribute);
                    return 0;
                }
                
                @SuppressWarnings("unchecked")
                int comp = nullSafeCompare((Comparable<Object>)value1, value2);
                return comp;
            }
            catch (MessagingException e) {
                log.error("Can't read the " + attribute + " attribute from an email.", e);
                return 0;
            } catch (PasswordException e) {
                log.error("Can't compare emails because the password is not available.", e);
                return 0;
            } catch (IOException e) {
                log.error("Can't compare emails.", e);
                return 0;
            } catch (GeneralSecurityException e) {
                log.error("Can't compare emails.", e);
                return 0;
            }
        }
        
        private <T> int nullSafeCompare(Comparable<T> value1, T value2) {
            if (value1 == null) {
                if (value2 == null)
                    return 0;
                else
                    return -1;
            }
            else {
                if (value2 == null)
                    return 1;
                else {
                    if (value1 instanceof String)
                        return ((String)value1).compareToIgnoreCase((String)value2);
                    else
                        return value1.compareTo(value2);
                }
            }
        }
    }
    
    /**
     * Finds an <code>Email</code> by message id. If the <code>Email</code> is
     * not found, <code>null</code> is returned.
     * The <code>messageId</code> parameter must be a 44-character base64-encoded
     * {@link UniqueId}.
     * @param messageId
     * @throws PasswordException
     */
    public Email getEmail(String messageId) throws PasswordException {
        File file = getEmailFile(messageId);
        if (!file.exists())
            return null;
        try {
            return createFolderElement(file);
        }
        catch (PasswordException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Can't read email from file: <" + file.getAbsolutePath() + ">", e);
            return null;
        }
    }

    /**
     * Moves an email from this folder to another. First the email file is
     * moved; if a metadata file exists, it is moved as well.
     * @param messageId
     * @param newFolder
     * @return <code>true</code> if successful, <code>false</code> if not
     */
    public boolean move(String messageId, EmailFolder newFolder) {
        File oldEmailFile = getEmailFile(messageId);
        if (!oldEmailFile.exists()) {
            log.error("Cannot move email with message ID " + messageId + " to folder [" + newFolder + "]: email file doesn't exist in directory <" + getStorageDirectory() + ">.");
            return false;
        }
        File newEmailFile = new File(newFolder.getStorageDirectory(), oldEmailFile.getName());
        boolean success;
        try {
            success = move(oldEmailFile, newEmailFile);
        }
        catch (IOException e) {
            log.error("Cannot move email file <" + oldEmailFile.getAbsolutePath() + "> to <" + newEmailFile.getAbsolutePath() + ">", e);
            success = false;
        }

        // We don't want to move the metadata file if we couldn't move the email file
        if (!success)
            return false;

        File oldMetaFile = getMetadataFile(oldEmailFile);
        if (oldMetaFile.exists()) {
            File newMetaFile = getMetadataFile(newEmailFile);
            try {
                success &= move(oldMetaFile, newMetaFile);
            }
            catch (IOException e) {
                log.error("Cannot move metadata file <" + oldMetaFile.getAbsolutePath() + "> to <" + newMetaFile.getAbsolutePath() + ">", e);
                success = false;
            }
        }

        if (success) {
            for (FolderListener listener: folderListeners)
                listener.elementRemoved(messageId);
            for (FolderListener listener: newFolder.folderListeners)
                listener.elementAdded(messageId);
        }

        return success;
    }
    
    /**
     * Moves a file.
     * @param from
     * @param to
     * @return <code>true</code> on success, <code>false</code> if the file can't be moved
     * @throws IOException
     */
    private boolean move(File from, File to) throws IOException {
        boolean success = from.renameTo(to);
        
        // renameTo doesn't always work, even when "from" and "to" are on the same partition,
        // so in those cases do copy+delete
        if (success)
            return true;
        else {
            Util.copy(from, to);
            return from.delete();
        }
    }
    
    /** @see #move(String, EmailFolder) */
    public boolean move(Email email, EmailFolder newFolder) {
        return move(email.getMessageID(), newFolder);
    }
    
    private File getEmailFile(Email email) {
        return getEmailFile(email.getMessageID());
    }

    /**
     * Returns the name of an email file in the file system for a given message ID.<br/>
     * The file may or may not exist.
     * @param messageId
     */
    private File getEmailFile(String messageId) {
        return new File(storageDir, messageId + EMAIL_FILE_EXTENSION);
    }
    
    private File getMetadataFile(String messageId) {
        return new File(storageDir, messageId + METADATA_FILE_EXTENSION);
    }
    
    private File getMetadataFile(File emailFile) {
        File parent = emailFile.getParentFile();
        String filename = emailFile.getName();
        filename = filename.substring(0, filename.length()-EMAIL_FILE_EXTENSION.length()) + METADATA_FILE_EXTENSION;
        return new File(parent, filename);
    }
    
    /**
     * Returns the metadata for an email. If the metadata file doesn't exist or cannot be read,
     * an empty {@link EmailMetadata} object is returned. This method never returns <code>null</code>.<br/>
     * The returned metadata object is not connected to the 
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     */
    private EmailMetadata getMetadata(String messageId) throws GeneralSecurityException, PasswordException {
        File file = getMetadataFile(messageId);
        if (!file.exists())
            return new EmailMetadata();
        try {
            return getMetadata(file);
        } catch (IOException e) {
            log.error("Can't read metadata file: <" + file.getAbsolutePath() + ">", e);
            return new EmailMetadata();
        }
    }

    private EmailMetadata getMetadata(File file) throws IOException, GeneralSecurityException, PasswordException {
        InputStream metadataStream = new BufferedInputStream(new EncryptedInputStream(new FileInputStream(file), passwordHolder));
        EmailMetadata metadata = new EmailMetadata(metadataStream);
        return metadata;
    }
    
    /**
     * Returns the number of <strong>new</strong> emails in the folder.
     * @throws PasswordException 
     * @see i2p.bote.folder.Folder#getNumElements()
     */
    public int getNumNewEmails() throws PasswordException {
        int numNew = 0;
        for (File emailFile: getFilenames()) {
            // getFilenames() only returns email files but not metadata files
            File metaFile = getMetadataFile(emailFile);
            if (metaFile.exists())
                try {
                    EmailMetadata metadata = getMetadata(metaFile);
                    if (metadata.isUnread())
                        numNew++;
                } catch (IOException e) {
                    log.error("Can't read metadata file: <" + metaFile.getAbsolutePath() + ">", e);
                } catch (GeneralSecurityException gse) {
                	log.error("Can't access metadata file: <" + metaFile.getAbsolutePath() + ">", gse);
                }
            else
                numNew++;
        }
        
        return numNew;
    }

    public Email getLatestUnreadEmail() throws PasswordException {
        for (Email email : getElements()) {
            if (email.isUnread())
                return email;
        }
        return null;
    }

    public void setRecent(String messageId, boolean isRecent) throws PasswordException, GeneralSecurityException {
        EmailMetadata metadata = getMetadata(messageId);
        metadata.setRecent(isRecent);
        try {
            saveMetadata(metadata, getMetadataFile(messageId));
        } catch (IOException e) {
            log.error("Can't read metadata file for message ID <" + messageId + ">", e);
        }
    }
    
    /**
     * Flags an email "new" (if <code>isNew</code> is <code>true</code>) or
     * "old" (if <code>isNew</code> is <code>false</code>).
     * @param messageId
     * @param isNew
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     */
    public void setNew(String messageId, boolean isNew) throws PasswordException, GeneralSecurityException {
        EmailMetadata metadata = getMetadata(messageId);
        metadata.setUnread(isNew);
        try {
            saveMetadata(metadata, getMetadataFile(messageId));
        } catch (IOException e) {
            log.error("Can't read metadata file for message ID <" + messageId + ">", e);
        }
    }
    
    public void setNew(Email email, boolean isNew) throws PasswordException, GeneralSecurityException {
        EmailMetadata metadata = email.getMetadata();
        metadata.setUnread(isNew);
        try {
            saveMetadata(email);
        } catch (IOException e) {
            log.error("Can't read metadata file for message ID <" + email.getMessageID() + ">", e);
        }
    }
    
    public void setReplied(String messageId, boolean replied) throws PasswordException, GeneralSecurityException {
        EmailMetadata metadata = getMetadata(messageId);
        metadata.setReplied(replied);
        try {
            saveMetadata(metadata, getMetadataFile(messageId));
        } catch (IOException e) {
            log.error("Can't read metadata file for message ID <" + messageId + ">", e);
        }
    }
    
    public void saveMetadata(Email email) throws PasswordException, FileNotFoundException, IOException, GeneralSecurityException {
        EmailMetadata metadata = email.getMetadata();
        File metaFile = getMetadataFile(email.getMessageID());
        saveMetadata(metadata, metaFile);
    }
    
    private void saveMetadata(EmailMetadata metadata, File file) throws PasswordException, FileNotFoundException, IOException, GeneralSecurityException {
        log.info("Mail folder <" + storageDir + ">: storing metadata file: <" + file.getAbsolutePath() + ">");
        OutputStream emailOutputStream = new BufferedOutputStream(new EncryptedOutputStream(new SecureFileOutputStream(file), passwordHolder));
        try {
            metadata.writeTo(emailOutputStream);
        } catch (IOException e) {
            log.error("Can't write metadata to file <" + file.getAbsolutePath() + ">", e);
            throw e;
        } finally {
            if (emailOutputStream != null)
                emailOutputStream.close();
        }

        for (FolderListener listener: folderListeners)
            listener.elementUpdated();
    }
    
    /**
     * Deletes an email with a given message ID. If a metadata file exists, it is also
     * deleted.
     * @param messageId
     * @return <code>true</code> if the email was deleted, <code>false</code> otherwise
     */
    public boolean delete(String messageId) {
        File metadataFile = getMetadataFile(messageId);
        if (metadataFile.exists())
            metadataFile.delete();
        
        File emailFile = getEmailFile(messageId);
        boolean deleted;
        if (emailFile != null)
            deleted = emailFile.delete();
        else
            deleted = false;
        
        for (FolderListener listener: folderListeners)
            listener.elementRemoved(messageId);
        
        return deleted;
    }
    
    public void addFolderListener(FolderListener folderListener) {
        folderListeners.add(folderListener);
    }

    public void removeFolderListener(FolderListener folderListener) {
        folderListeners.remove(folderListener);
    }
    
    @Override
    protected Email createFolderElement(File emailFile) throws Exception {
        InputStream emailStream = null;
        try {
            emailStream = new BufferedInputStream(new EncryptedInputStream(new FileInputStream(emailFile), passwordHolder));
            InputStream metadataStream = null;
            File metadataFile = getMetadataFile(emailFile);
            if (metadataFile.exists())
                metadataStream = new BufferedInputStream(new EncryptedInputStream(new FileInputStream(metadataFile), passwordHolder));
            Email email = new Email(emailStream, metadataStream, passwordHolder);
            
            String messageIdString = emailFile.getName().substring(0, 44);
            email.setMessageID(messageIdString);
            
            return email;
        } finally {
            if (emailStream != null)
                emailStream.close();
        }
    }
}
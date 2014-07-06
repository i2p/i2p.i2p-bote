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

package i2p.bote.email;

import i2p.bote.I2PBote;
import i2p.bote.Util;
import i2p.bote.crypto.KeyUpdateHandler;
import i2p.bote.fileencryption.DerivedKey;
import i2p.bote.fileencryption.EncryptedInputStream;
import i2p.bote.fileencryption.EncryptedOutputStream;
import i2p.bote.fileencryption.FileEncryptionUtil;
import i2p.bote.fileencryption.PasswordCache;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordHolder;
import i2p.bote.util.SortedProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import net.i2p.data.Hash;
import net.i2p.util.Log;

import com.lambdaworks.codec.Base64;

/**
 * Holds a set of {@link EmailIdentity} objects that are sorted by name.<br/>
 * The Email Identities can be written to, and read from, a password-encrypted file.
 */
public class Identities implements KeyUpdateHandler {
    private Log log = new Log(Identities.class);
    private File identitiesFile;
    private PasswordHolder passwordHolder;
    private SortedSet<EmailIdentity> identities;   // null until file has been read successfully
    private EmailIdentity defaultIdentity;

    /**
     * Constructs a new empty <code>Identities</code> object. The <code>identitiesFile</code>
     * is lazy-loaded.
     * @param identitiesFile
     * @param passwordHolder
     */
    public Identities(File identitiesFile, PasswordHolder passwordHolder) {
        this.identitiesFile = identitiesFile;
        this.passwordHolder = passwordHolder;
    }

    private void initializeIfNeeded() throws PasswordException, IOException, GeneralSecurityException {
        if (identities == null)
            readIdentities();
    }
    
    /**
     * Reads <code>Identities</code> from the encrypted identities file. Each identity
     * is defined by one line that contains two to four tab-separated fields:<br/>
     * Email Identity key, Public Name, Description, and Email Address.
     * The first two are mandatory, the last two are optional.<br/>
     * <p/>
     * Additionally, the file can set a default Email Identity by including a
     * line that starts with "Default ", followed by an Email Destination key.<br/>
     * The destination key must match one of the Email Identities defined in
     * the file.
     * <p/>
     * An Email Identity key consists of two public keys and two private keys, whereas
     * an Email Destination consists only of two public keys.
     * @throws PasswordException 
     * @throws IOException 
     * @throws GeneralSecurityException 
     */
    private void readIdentities() throws PasswordException, IOException, GeneralSecurityException {
        log.debug("Reading identities file: <" + identitiesFile.getAbsolutePath() + ">");
        
        if (!identitiesFile.exists()) {
            log.debug("Identities file does not exist: <" + identitiesFile.getAbsolutePath() + ">");
            identities = new TreeSet<EmailIdentity>(new IdentityComparator());
            return;
        }
        
        BufferedReader input = null;
        try {
            InputStream encryptedStream = new EncryptedInputStream(new FileInputStream(identitiesFile), passwordHolder);
            input = new BufferedReader(new InputStreamReader(encryptedStream));
            
            // No PasswordException occurred, so read the input stream
            Properties properties = new Properties();
            properties.load(new InputStreamReader(encryptedStream));

            loadFromProperties(properties, false, false);
        }
        finally {
            if (input != null)
                try {
                    input.close();
                }
                catch (IOException e) {
                    log.error("Error closing input stream.", e);
                }
        }
    }

    /**
     * Imports identities.
     * @param importFileDescriptor
     * @param password
     * @param append Set to false if existing identities should be dropped.
     * @param replace Set to false to ignore duplicates, true to import and replace existing.
     * @return true if the import succeeded, false if there were no identities found.
     * @throws PasswordException if the provided password is invalid.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public boolean importFromFileDescriptor(FileDescriptor importFileDescriptor, String password,
            boolean append, boolean replace) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();

        InputStream importStream = new FileInputStream(importFileDescriptor);
        if (password != null)
            importStream = new EncryptedInputStream(importStream, password.getBytes());

        try {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(importStream));

            if (loadFromProperties(properties, append, replace)) {
                // Save the new identities
                save();
                return true;
            } else // No identities found
                return false;
        } finally {
            importStream.close();
        }
    }

    private boolean loadFromProperties(Properties properties, boolean append, boolean replace) throws GeneralSecurityException {
        String defaultIdentityStr = properties.getProperty("default");
        if (identities == null || !append)
            identities = new TreeSet<EmailIdentity>(new IdentityComparator());
        int index = 0;
        while (true) {
            String prefix = "identity" + index + ".";
            String key = properties.getProperty(prefix + "key");
            if (key == null)
                break;

            EmailIdentity identity = new EmailIdentity(key);
            identity.setDescription(properties.getProperty(prefix + "description"));
            String pictureBase64 = properties.getProperty(prefix + "picture");
            if (pictureBase64!=null && !pictureBase64.isEmpty())
                identity.setPictureBase64(pictureBase64);
            identity.setText(properties.getProperty(prefix + "text"));
            identity.setPublished("true".equalsIgnoreCase(properties.getProperty(prefix + "published")));
            String name = properties.getProperty(prefix + "publicName");
            identity.setPublicName(name);
            String salt = properties.getProperty(prefix + "salt");
            if (salt != null) {
                Hash nameHash = EmailIdentity.calculateHash(name);
                Fingerprint fingerprint = new Fingerprint(nameHash, identity, Base64.decode(salt.toCharArray()));
                identity.setFingerprint(fingerprint);
            }
            if (append && replace && identities.contains(identity))
                identities.remove(identity);
            identities.add(identity);

            if (identity.getKey().equals(defaultIdentityStr)) {
                identity.setDefaultIdentity(true);
                defaultIdentity = identity;
            }

            index++;
        }
        return index > 0;
    }

    /** Saves all identities to file. */
    public void save() throws IOException, GeneralSecurityException, PasswordException {
        initializeIfNeeded();
            
        OutputStream encryptedStream = new EncryptedOutputStream(new FileOutputStream(identitiesFile), passwordHolder);
        try {
            Properties properties = saveToProperties();
            properties.store(new OutputStreamWriter(encryptedStream, "UTF-8"), null);
        } catch (IOException e) {
            log.error("Can't save email identities to file <" + identitiesFile.getAbsolutePath() + ">.", e);
            throw e;
        } finally {
            encryptedStream.close();
        }
        Util.makePrivate(identitiesFile);
    }

    public void export(File exportFile, String password) throws IOException, GeneralSecurityException, PasswordException {
        initializeIfNeeded();

        OutputStream exportStream = new FileOutputStream(exportFile);
        if (password != null) {
            // Use same salt and parameters as the on-disk files
            PasswordCache cache = new PasswordCache(I2PBote.getInstance().getConfiguration());
            cache.setPassword(password.getBytes());
            DerivedKey derivedKey = cache.getKey();
            exportStream = new EncryptedOutputStream(exportStream, derivedKey);
        }

        try {
            Properties properties = saveToProperties();
            properties.store(new OutputStreamWriter(exportStream, "UTF-8"), null);
        } catch (IOException e) {
            log.error("Can't export email identities to file <" + exportFile.getAbsolutePath() + ">.", e);
            throw e;
        } finally {
            exportStream.close();
        }
    }

    private Properties saveToProperties() throws GeneralSecurityException {
        SortedProperties properties = new SortedProperties();
        int index = 0;
        String defaultIdentityStr = null;
        for (EmailIdentity identity: identities) {
            if (identity == defaultIdentity)
                defaultIdentityStr = identity.getKey();

            String prefix = "identity" + index + ".";
            String name = identity.getPublicName();
            properties.setProperty(prefix + "publicName", name==null ? "" : name);
            properties.setProperty(prefix + "key", identity.getFullKey());
            Fingerprint fingerprint = identity.getFingerprint();
            byte[] salt = fingerprint==null ? null : fingerprint.getSalt();
            properties.setProperty(prefix + "salt", salt==null ? "" : new String(Base64.encode(salt)));
            String description = identity.getDescription();
            properties.setProperty(prefix + "description", (description==null ? "" : description));
            String pictureBase64 = identity.getPictureBase64();
            properties.setProperty(prefix + "picture", (pictureBase64==null ? "" : pictureBase64));
            String text = identity.getText();
            properties.setProperty(prefix + "text", (text==null ? "" : text));
            properties.setProperty(prefix + "published", identity.isPublished() ? "true" : "false");
            
            index++;
        }

        if (defaultIdentityStr != null)
            properties.setProperty("default", defaultIdentityStr);

        return properties;
    }
    
    public void add(EmailIdentity identity) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        
        if (identities.isEmpty())
            identity.setDefaultIdentity(true);
        identities.add(identity);
    }
    
    public void remove(String key) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        
        EmailIdentity identity = get(key);
        if (identity != null) {
            identities.remove(identity);
            
            // if we deleted the default identity, set a new default
            if (identity.isDefaultIdentity() && !identities.isEmpty())
                identities.iterator().next().setDefaultIdentity(true);
            
            // when the last identity is deleted, remove the file; see isEmpty()
            if (identities.isEmpty() && !identitiesFile.delete())
                log.error("Can't delete file: " + identitiesFile.getAbsolutePath());
        }
    }
    
    public void changePassword(byte[] oldPassword, DerivedKey newKey) throws FileNotFoundException, IOException, GeneralSecurityException, PasswordException {
        if (identitiesFile.exists())
            FileEncryptionUtil.changePassword(identitiesFile, oldPassword, newKey);
    }
    
    public void clearPasswordProtectedData() {
        // TODO overwrite private keys
        identities = null;
    }
    
    /**
     * Sets the default identity. Assumes this <code>Identities</code> already
     * contains <code>defaultIdentity</code>.
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public void setDefault(EmailIdentity defaultIdentity) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        
        // clear the old default
        for (EmailIdentity identity: identities)
            identity.setDefaultIdentity(false);
        
        defaultIdentity.setDefaultIdentity(true);

        this.defaultIdentity = defaultIdentity;
    }
    
    /**
     * Returns the default identity, or <code>null</code> if no default is set.
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public EmailIdentity getDefault() throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        
        if (identities == null)
            return null;
        
        for (EmailIdentity identity: identities)
            if (identity.isDefaultIdentity())
                return identity;
        
        return null;
    }
    
    /**
     * Looks up an {@link EmailIdentity} that has the same public encryption key and the
     * same public signing key as a given {@link EmailDestination}.<br/>
     * Returns <code>null</code> if nothing is found.
     * @param destination
     * @throws PasswordException
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public EmailIdentity get(EmailDestination destination) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        
        if (identities == null)
            return null;
        
        for (EmailIdentity identity: identities)
            if (identity.getPublicEncryptionKey().equals(destination.getPublicEncryptionKey())
                    && identity.getPublicSigningKey().equals(destination.getPublicSigningKey()))
                return identity;
        return null;
    }
    
    /**
     * Looks up an {@link EmailIdentity} by its Base64 key (the two public keys, to be
     * more precise).<br/>
     * Returns An <code>EmailIdentity</code>, or <code>null</code> if nothing is found.
     * @param key
     * @throws PasswordException
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public EmailIdentity get(String key) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        
        if (identities == null)
            return null;
        
        for (EmailIdentity identity: identities)
            if (identity.getKey().equals(key))
                return identity;
        return null;
    }
    
    public Collection<EmailIdentity> getAll() throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        return identities;
    }
    
    /**
     * This method relies on the fact that the identities file only exists when
     * there is at least one identity. It does not attempt to read the file and
     * does not throw {@link PasswordException}.
     * @return <code>true</code> if there are no identities, <code>false</code> otherwise
     */
    public boolean isNone() {
        if (identities != null)
            return identities.isEmpty();
        else
            return !identitiesFile.exists();
    }
    
    /**
     * Returns <code>true</code> if any of the <code>Identities</code> matches
     * the two public keys of a given {@link EmailDestination}
     * (Note that an <code>EmailIdentity</code> is an <code>EmailDestination</code>).
     * @param base64Dest A base64-encoded Email Destination
     * @throws PasswordException
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public boolean contains(String base64Dest) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        
        if (identities==null || base64Dest==null)
            return false;
        
        for (EmailIdentity identity: identities)
            if (base64Dest.equals(identity.toBase64()))
                return true;
        
        return false;
    }
    
    public int size() throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        return identities.size();
    }
    
    public int getSize() throws PasswordException, IOException, GeneralSecurityException {
        return size();
    }
    
    /**
     * Looks for a Base64-encoded Email Destination in a string and returns
     * the identity that matches the Email Destination. If no Email Destination
     * is found, or if it doesn't match any Email Identity, <code>null</code>
     * is returned.
     * @param address
     * @throws PasswordException
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public EmailIdentity extractIdentity(String address) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        
        String destinationStr = EmailDestination.extractBase64Dest(address);
        if (destinationStr != null)
            return get(destinationStr);
        else
            return null;
    }
    
    public Iterator<EmailIdentity> iterator() throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();
        if (identities == null)
            return null;
        return identities.iterator();
    }

    /**
     * Compares two email identities by name and email destination.
     */
    public static class IdentityComparator implements Comparator<EmailIdentity> {
        @Override
        public int compare(EmailIdentity identity1, EmailIdentity identity2) {
            int nameComparison = compareNames(identity1, identity2);
            if (nameComparison == 0) {
                // if the names are the same, compare destination keys
                String key1 = identity1.getKey();
                String key2 = identity2.getKey();
                return key1.compareTo(key2);
            }
            else
                return nameComparison;
        }
        
        /** Null-safe comparison of public names */
        private int compareNames(EmailIdentity identity1, EmailIdentity identity2) {
            String name1 = identity1.getPublicName();
            String name2 = identity2.getPublicName();
            if (name1 == null)
                return name2==null ? 0 : -1;
            else if (name2 == null)
                return 1;
            else
                return name1.compareToIgnoreCase(name2);
        }
    }

    @Override
    public void updateKey() throws GeneralSecurityException, PasswordException, IOException {
        save();
    }
}
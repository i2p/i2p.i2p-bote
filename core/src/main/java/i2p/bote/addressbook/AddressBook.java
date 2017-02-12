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

package i2p.bote.addressbook;

import i2p.bote.email.EmailDestination;
import i2p.bote.fileencryption.DerivedKey;
import i2p.bote.fileencryption.EncryptedInputStream;
import i2p.bote.fileencryption.EncryptedOutputStream;
import i2p.bote.fileencryption.FileEncryptionUtil;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordHolder;
import i2p.bote.packet.dht.Contact;
import i2p.bote.util.ExportableData;
import i2p.bote.util.SortedProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import net.i2p.util.Log;
import net.i2p.util.SecureFileOutputStream;

/**
 * Implements the private address book. Holds a set of {@link Contact}s
 * which are sorted by name.<br/>
 * Contacts can be written to, and read from, a password-encrypted file.
 */
public class AddressBook extends ExportableData {
    private Log log = new Log(AddressBook.class);
    private File addressFile;
    private PasswordHolder passwordHolder;
    private SortedSet<Contact> contacts;

    /**
     * Constructs a new empty <code>AddressBook</code>.
     * @param addressFile
     * @param passwordHolder
     */
    public AddressBook(File addressFile, PasswordHolder passwordHolder) {
        this.addressFile = addressFile;
        this.passwordHolder = passwordHolder;
    }
 
    protected void initializeIfNeeded() throws PasswordException {
        if (contacts == null)
            readContacts();
    }
    
    /**
     * Reads an <code>AddressBook</code> from the encrypted address book file.
     * Each contact is defined by one line that contains an Email Destination
     * and a name, separated by a tab character.
     * @throws PasswordException 
     */
    private void readContacts() throws PasswordException {
        if (!addressFile.exists()) {
            log.debug("Address file does not exist: <" + addressFile.getAbsolutePath() + ">");
            contacts = new TreeSet<Contact>(new ContactComparator());
            return;
        }
        
        log.debug("Reading address book from <" + addressFile.getAbsolutePath() + ">");
        BufferedReader input = null;
        try {
            InputStream encryptedStream = new EncryptedInputStream(new FileInputStream(addressFile), passwordHolder);
            input = new BufferedReader(new InputStreamReader(encryptedStream));
            
            // No PasswordException occurred, so parse the input stream
            Properties properties = new Properties();
            properties.load(new InputStreamReader(encryptedStream));
            
            loadFromProperties(properties, false, false);
        } catch (PasswordException e) {
            throw e;
        } catch (Exception e) {
            log.error("Can't read address book.", e);
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

    protected boolean loadFromProperties(Properties properties, boolean append, boolean replace) {
        if (contacts == null || !append) {
            contacts = new TreeSet<Contact>(new ContactComparator());
        }
        int index = 0;
        while (true) {
            String prefix = "contact" + index + ".";
            String name = properties.getProperty(prefix + "name");
            if (name == null)
                break;

            String destBase64 = properties.getProperty(prefix + "destination");
            if (destBase64 == null)
                continue;
            try {
                EmailDestination destination = new EmailDestination(destBase64);
                String pictureBase64 = properties.getProperty(prefix + "picture");
                String text = properties.getProperty(prefix + "text");
                Contact contact = new Contact(name, destination, pictureBase64, text);

                if (append && replace && contacts.contains(contact)) {
                    contacts.remove(contact);
                }
                contacts.add(contact);
            }
            catch (GeneralSecurityException e) {
                log.error("Not a valid Email Destination: <" + destBase64 + ">", e);
            }

            index++;
        }
        return index > 0;
    }
    
    public void save() throws IOException, PasswordException, GeneralSecurityException {
        initializeIfNeeded();
        
        OutputStream encryptedStream = new EncryptedOutputStream(new SecureFileOutputStream(addressFile), passwordHolder);
        try {
            Properties properties = saveToProperties();
            properties.store(new OutputStreamWriter(encryptedStream, "UTF-8"), null);
        } catch (IOException e) {
            log.error("Can't save email identities to file <" + addressFile.getAbsolutePath() + ">.", e);
            throw e;
        } finally {
            encryptedStream.close();
        }
    }

    protected Properties saveToProperties() {
        SortedProperties properties = new SortedProperties();
        int index = 0;
        for (Contact contact: contacts) {
            String prefix = "contact" + index + ".";
            properties.setProperty(prefix + "name", contact.getName());
            String base64Dest = contact.getDestination().toBase64();
            properties.setProperty(prefix + "destination", base64Dest);
            String pictureBase64 = contact.getPictureBase64();
            properties.setProperty(prefix + "picture", (pictureBase64==null ? "" : pictureBase64));
            String text = contact.getText();
            properties.setProperty(prefix + "text", (text==null ? "" : text));
            index++;
        }
        return properties;
    }
    
    public void add(Contact contact) throws PasswordException {
        initializeIfNeeded();
        contacts.add(contact);
    }
    
    public void remove(String destination) throws PasswordException {
        initializeIfNeeded();
        Contact contact = get(destination);
        if (contact != null)
            contacts.remove(contact);
    }
    
    public void changePassword(byte[] oldPassword, DerivedKey newKey) throws FileNotFoundException, IOException, GeneralSecurityException, PasswordException {
        if (addressFile.exists())
            FileEncryptionUtil.changePassword(addressFile, oldPassword, newKey);
    }
    
    public void clearPasswordProtectedData() {
        contacts = null;
    }
    
    /**
     * Looks up an {@link Contact} by its Base64 key. If none is found,
     * <code>null</code> is returned.
     * @param destination
     */
    public Contact get(String destination) throws PasswordException {
        initializeIfNeeded();
        
       if (contacts==null || destination==null || destination.isEmpty())
            return null;
        
        for (Contact contact: contacts)
            if (destination.equals(contact.getBase64Dest()))
                return contact;
        return null;
    }
    
    /**
     * Returns <code>true</code> if a given Base64-encoded Email Destination
     * is in the address book.
     * @param base64dest
     */
    public boolean contains(String base64dest) throws PasswordException {
        initializeIfNeeded();
        
        if (base64dest == null)
            return false;
        
        for (Contact contact: contacts)
            if (base64dest.equals(contact.getBase64Dest()))
                return true;
        return false;
    }
    
    public SortedSet<Contact> getAll() throws PasswordException {
        initializeIfNeeded();
        return contacts;
    }
    
    public int size() throws PasswordException {
        initializeIfNeeded();
       return contacts.size();
    }
    
    /**
     * Compares two contacts by name and email destination.
     */
    private class ContactComparator implements Comparator<Contact> {
        @Override
        public int compare(Contact contact1, Contact contact2) {
            int nameComparison = String.CASE_INSENSITIVE_ORDER.compare(contact1.getName(), contact2.getName());
            if (nameComparison == 0) {
                // if the names are the same, compare destination keys
                String key1 = contact1.getDestination().getKey();
                String key2 = contact2.getDestination().getKey();
                return key1.compareTo(key2);
            }
            else
                return nameComparison;
        }
    }
}
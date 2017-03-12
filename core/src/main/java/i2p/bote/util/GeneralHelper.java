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

package i2p.bote.util;

import i2p.bote.Configuration;
import i2p.bote.I2PBote;
import i2p.bote.status.StatusListener;
import i2p.bote.Util;
import i2p.bote.addressbook.AddressBook;
import i2p.bote.crypto.CryptoFactory;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.email.AddressDisplayFilter;
import i2p.bote.email.Email;
import i2p.bote.email.EmailAttribute;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Fingerprint;
import i2p.bote.email.Identities;
import i2p.bote.email.IllegalDestinationParametersException;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.Outbox.EmailStatus;
import i2p.bote.network.DhtException;
import i2p.bote.network.NetworkStatus;
import i2p.bote.packet.dht.Contact;
import i2p.bote.service.EmailChecker;
import i2p.bote.status.ChangeIdentityStatus;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.Address;
import javax.mail.MessagingException;

import net.i2p.data.Destination;
import net.i2p.util.Log;
import net.i2p.util.RandomSource;
import net.i2p.util.SystemVersion;

/**
 * General helper functions used by all UIs.
 */
public class GeneralHelper {
    private static AddressDisplayFilter ADDRESS_DISPLAY_FILTER;
    private static GeneralHelper instance;
    private static final boolean _isUnlimited;

    // TODO: Remove this and bump min-i2p-version to 0.9.23
    static {
        boolean unlimited = false;
        try {
            unlimited = Cipher.getMaxAllowedKeyLength("AES") >= 256;
        } catch (GeneralSecurityException gse) {
            // a NoSuchAlgorithmException
        } catch (NoSuchMethodError nsme) {
            // JamVM, gij
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                SecretKeySpec key = new SecretKeySpec(new byte[32], "AES");
                cipher.init(Cipher.ENCRYPT_MODE, key);
                unlimited = true;
            } catch (GeneralSecurityException gse) {
            }
        }
        _isUnlimited = unlimited;
    }

    public GeneralHelper() {
    }

    protected static GeneralHelper getInstance() {
        if (instance == null)
            instance = new GeneralHelper();
        return instance;
    }

    public NetworkStatus getNetworkStatus() {
        return I2PBote.getInstance().getNetworkStatus();
    }

    public Exception getConnectError() {
        return I2PBote.getInstance().getConnectError();
    }

    public Identities getIdentities() throws PasswordException {
        return I2PBote.getInstance().getIdentities();
    }

    public AddressBook getAddressBook() throws PasswordException {
        return I2PBote.getInstance().getAddressBook();
    }

    /**
     * Updates an email identity if <code>createNew</code> is <code>false</code>,
     * or adds a new identity if <code>createNew</code> is <code>true</code>.
     * @param createNew 
     * @param cryptoImplId The id value of the cryptographic algorithm set to use for the new identity; ignored if <code>createNew</code> is <code>false</code>
     * @param vanityPrefix
     * @param key A base64-encoded Email Destination key
     * @param description
     * @param publicName
     * @param emailAddress
     * @param setDefault If this is <code>true</code>, the identity becomes the new default identity. Otherwise, the default stays the same.
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     * @throws IOException 
     * @throws IllegalDestinationParametersException if <code>cryptoImpl</code> and <code>vanityPrefix</code> aren't compatible
     */
    public static void createOrModifyIdentity(boolean createNew, int cryptoImplId, String vanityPrefix, String key, String publicName, String description, String pictureBase64, String emailAddress, Properties config, boolean setDefault) throws GeneralSecurityException, PasswordException, IOException, IllegalDestinationParametersException {
        createOrModifyIdentity(createNew, cryptoImplId, vanityPrefix, key, publicName, description, pictureBase64, emailAddress, config, setDefault, new StatusListener<ChangeIdentityStatus>() {
            public void updateStatus(ChangeIdentityStatus status, String... args) {} // Do nothing
        });
    }

    /**
     * Updates an email identity if <code>createNew</code> is <code>false</code>,
     * or adds a new identity if <code>createNew</code> is <code>true</code>.
     * @param createNew 
     * @param cryptoImplId The id value of the cryptographic algorithm set to use for the new identity; ignored if <code>createNew</code> is <code>false</code>
     * @param vanityPrefix An alphanumeric string the destination should start with; ignored if <code>createNew==false</code>.
     * @param key A base64-encoded Email Destination key
     * @param description
     * @param publicName
     * @param picture
     * @param emailAddress
     * @param setDefault If this is <code>true</code>, the identity becomes the new default identity. Otherwise, the default stays the same.
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     * @throws IOException 
     * @throws IllegalDestinationParametersException if <code>cryptoImplId</code> and <code>vanityPrefix</code> aren't compatible
     */
    public static void createOrModifyIdentity(boolean createNew, int cryptoImplId, String vanityPrefix, String key, String publicName, String description, String pictureBase64, String emailAddress, Properties config, boolean setDefault, StatusListener<ChangeIdentityStatus> lsnr) throws GeneralSecurityException, PasswordException, IOException, IllegalDestinationParametersException {
        Log log = new Log(GeneralHelper.class);
        Identities identities = I2PBote.getInstance().getIdentities();
        EmailIdentity identity;

        if (createNew) {
            CryptoImplementation cryptoImpl = CryptoFactory.getInstance(cryptoImplId);
            if (cryptoImpl == null) {
                log.error("Invalid ID number for CryptoImplementation: " + cryptoImplId);
                throw new IllegalArgumentException("Invalid ID number for CryptoImplementation: " + cryptoImplId);
            }

            lsnr.updateStatus(ChangeIdentityStatus.GENERATING_KEYS);
            try {
                identity = new EmailIdentity(cryptoImpl, vanityPrefix);
            } catch (GeneralSecurityException e) {
                log.error("Can't generate email identity for CryptoImplementation: <" + cryptoImpl + "> with vanity prefix: <" + vanityPrefix + ">", e);
                throw e;
            }
        } else
            identity = identities.get(key);

        identity.setPublicName(publicName);
        identity.setDescription(description);
        identity.setPictureBase64(pictureBase64);
        identity.setEmailAddress(emailAddress);

        // update the identity config
        if (config != null)
            identity.loadConfig(config, "", false);

        if (createNew)
            identities.add(identity);
        else
            identities.identityUpdated(key);

        // update the default identity
        if (setDefault)
            identities.setDefault(identity);
    }

    public static void modifyIdentity(String key, String publicName, String description, String pictureBase64, String emailAddress, Properties config, boolean setDefault) throws GeneralSecurityException, PasswordException, IOException {
        try {
            createOrModifyIdentity(false, -1, null, key, publicName, description, pictureBase64, emailAddress, config, setDefault);
        } catch (IllegalDestinationParametersException e) {
            Log log = new Log(GeneralHelper.class);
            log.error("This shouldn't happen because no identity is being created here.", e);
        }
    }

    /**
     * Deletes an email identity.
     * @param key A base64-encoded email identity key
     * @return null if sucessful, or an error message if an error occured
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public static String deleteIdentity(String key) throws PasswordException, IOException, GeneralSecurityException {
        Identities identities = I2PBote.getInstance().getIdentities();
        identities.remove(key);

        try {
            identities.save();
            return null;
        }
        catch (PasswordException e) {
            throw e;
        }
        catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }

    public static String getFingerprint(Contact contact, String localeCode) throws GeneralSecurityException {
        String[] wordList = I2PBote.getInstance().getWordList(localeCode);
        Fingerprint fingerprint = contact.getFingerprint();
        return fingerprint==null ? null : fingerprint.getWords(wordList);
    }

    public static String getFingerprint(EmailIdentity identity, String localeCode) throws GeneralSecurityException {
        String[] wordList = I2PBote.getInstance().getWordList(localeCode);
        Fingerprint fingerprint = identity.getFingerprint();
        return fingerprint==null ? null : fingerprint.getWords(wordList);
    }

    /** @see {@link I2PBote#getWordListLocales() */
    public List<String> getWordListLocales() throws UnsupportedEncodingException, IOException, URISyntaxException {
        return I2PBote.getInstance().getWordListLocales();
    }

    public static EmailIdentity getIdentity(String key) throws PasswordException, IOException, GeneralSecurityException {
        return I2PBote.getInstance().getIdentities().get(key);
    }

    public static void publishDestination(String destination, byte[] picture, String text) throws PasswordException, IOException, GeneralSecurityException, DhtException, InterruptedException {
        I2PBote.getInstance().publishDestination(destination, picture, text);
    }

    public static Contact lookupInDirectory(String name) throws InterruptedException {
        return I2PBote.getInstance().lookupInDirectory(name);
    }

    public static CryptoImplementation getCryptoImplementation(int id) {
        return CryptoFactory.getInstance(id);
    }

    public List<CryptoImplementation> getCryptoImplementations() {
        return CryptoFactory.getInstances();
    }

    /**
     * Updates a contact in the address book if the Destination <code>destinationString</code> exists,
     * or adds a new contact to the address book.
     * @param destinationString A base64-encoded Email Destination key
     * @param name
     * @param pictureBase64
     * @param text
     * @return null if sucessful, or an error message if an error occured
     * @throws PasswordException
     * @throws GeneralSecurityException 
     */
    public static String saveContact(String destinationString, String name, String pictureBase64, String text) throws PasswordException, GeneralSecurityException {
        destinationString = Util.fixAddress(destinationString);

        AddressBook addressBook = getInstance().getAddressBook();
        Contact contact = addressBook.get(destinationString);

        if (contact != null) {
            contact.setName(name);
            contact.setPictureBase64(pictureBase64);
            contact.setText(text);
        }
        else {
            EmailDestination destination;
            try {
                destination = new EmailDestination(destinationString);
            } catch (GeneralSecurityException e) {
                Log log = new Log(GeneralHelper.class);
                log.error("Can't save contact to address book.", e);
                return e.getLocalizedMessage();
            }
            contact = new Contact(name, destination, pictureBase64, text);
            addressBook.add(contact);
        }

        try {
            addressBook.save();
            return null;
        }
        catch (IOException e) {
            return e.getLocalizedMessage();
        }
    }

    /**
     * Deletes a contact from the address book.
     * @param destination A base64-encoded email destination
     * @return null if sucessful, or an error message if an error occured
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     */
    public static String deleteContact(String destination) throws PasswordException, GeneralSecurityException {
        AddressBook addressBook = getInstance().getAddressBook();
        addressBook.remove(destination);

        try {
            addressBook.save();
            return null;
        }
        catch (IOException e) {
            return e.getLocalizedMessage();
        }
    }

    public static Contact getContact(String destination) throws PasswordException {
        return getInstance().getAddressBook().get(destination);
    }

    public boolean isCheckingForMail() {
        return I2PBote.getInstance().isCheckingForMail();
    }

    public static boolean isCheckingForMail(EmailIdentity identity) {
        return I2PBote.getInstance().isCheckingForMail(identity);
    }

    /**
     * @see EmailChecker#getLastMailCheckTime()
     */
    public Date getLastMailCheckTime() {
        return I2PBote.getInstance().getLastMailCheckTime();
    }

    public boolean isNewMailReceived() {
        return I2PBote.getInstance().newMailReceived();
    }

    public int getNumIncompleteEmails() {
        return I2PBote.getInstance().getNumIncompleteEmails();
    }

    public static EmailFolder getMailFolder(String folderName) {
        if ("Inbox".equalsIgnoreCase(folderName))
            return I2PBote.getInstance().getInbox();
        else if ("Outbox".equalsIgnoreCase(folderName))
            return I2PBote.getInstance().getOutbox();
        else if ("Sent".equalsIgnoreCase(folderName))
            return I2PBote.getInstance().getSentFolder();
        else if ("Trash".equalsIgnoreCase(folderName))
            return I2PBote.getInstance().getTrashFolder();
        else
            return null;
    }

    public static Email getEmail(String folderName, String messageId) throws PasswordException {
        EmailFolder folder = getMailFolder(folderName);
        if (folder == null)
            return null;
        else
            return folder.getEmail(messageId);
    }

    public static List<Email> getEmails(EmailFolder folder, EmailAttribute sortColumn, boolean descending) throws PasswordException {
        return folder.getElements(getAddressDisplayFilter(), sortColumn, descending);
    }

    public static String getShortSenderName(String sender, int maxLength) {
        if (sender == null)
            return null;
        else {
            int angBracketIndex = sender.indexOf('<');
            if (angBracketIndex > 0)
                sender = sender.substring(0, angBracketIndex-1);

            if (sender.length() > maxLength)
                return sender.substring(0, maxLength-3) + "...";
            else
                return sender;
        }
    }

    /**
     * Returns the recipient address for an email that has been received by
     * the local node.<br/>
     * If the email was sent to more than one local Email Destination, one
     * of them is returned.<br/>
     * If the email does not contain a local Email Destination, a non-local
     * recipient is returned.<br/>
     * If the email contains no recipients at all, or if an error occurred,
     * <code>null</code> is returned.
     * @param email
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public static Address getOneLocalRecipient(Email email) throws PasswordException, IOException, GeneralSecurityException {
        Address[] recipients;
        try {
            recipients = email.getAllRecipients();
        }
        catch (MessagingException e) {
            return null;
        }
        if (recipients == null)
            return null;

        Identities identities = I2PBote.getInstance().getIdentities();
        Iterator<EmailIdentity> iterator = identities.iterator();
        if (iterator == null)
            return null;
        while (iterator.hasNext()) {
            EmailDestination localDestination = iterator.next();
            String base64Dest = localDestination.toBase64();
            for (Address recipient: recipients)
                if (recipient.toString().contains(base64Dest))
                    return recipient;
        }

        if (recipients.length > 0)
            return recipients[0];
        else
            return null;
    }

    public static String getReplyAddress(Email email, Identities identities) throws PasswordException {
        try {
            return email.getReplyAddress(identities);
        }
        catch (PasswordException e) {
            throw e;
        }
        catch (Exception e) {
            new Log(GeneralHelper.class).error("Can't get address to reply to.", e);
            return null;
        }
    }

    public static EmailStatus getEmailStatus(Email email) {
        return I2PBote.getInstance().getOutbox().getStatus(email);
    }

    /**
     * Moves an email from the folder denoted by <code>folderName</code> to
     * the trash folder. If <code>folderName</code> is the name of the trash
     * folder, the email is deleted.
     * @param folderName
     * @param messageId The message ID of the email to delete / move to the trash
     */
    public static boolean deleteEmail(String folderName, String messageId) {
        EmailFolder folder = getMailFolder(folderName);
        return I2PBote.getInstance().deleteEmail(folder, messageId);
    }

    /**
     * Looks for an Email Destination in a sender address. If no Email Destination
     * is found, </code>null</code> is returned.
     * @param address
     */
    public static String extractEmailDestination(String address) {
        return EmailDestination.extractBase64Dest(address);
    }

    /**
     * Returns <code>true</code> if <code>address</code> contains a base64-encoded
     * email destination that is either in the address book or among the local
     * email identities.
     * @param address
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public static boolean isKnown(String address) throws PasswordException, IOException, GeneralSecurityException {
        String destination = extractEmailDestination(address);
        if (destination == null)
            return false;
        else if (getInstance().getAddressBook().contains(destination))
            return true;
        else return I2PBote.getInstance().getIdentities().contains(destination);
    }

    public static String machineDateFormat() {
        if (SystemVersion.isJava7() && !SystemVersion.isAndroid())
            return "yyyy-MM-dd HH:mmXXX";
        else
            return "yyyy-MM-dd HH:mmZZZZZ";
    }

    public static String getNameAndDestination(String address) throws PasswordException, IOException, GeneralSecurityException {
        return getAddressDisplayFilter().getNameAndDestination(address);
    }

    public static String getImapNameAndDestination(String address) throws PasswordException, IOException, GeneralSecurityException {
        return getAddressDisplayFilter().getImapNameAndDestination(address);
    }

    public static String getNameAndShortDestination(String address) throws PasswordException, IOException, GeneralSecurityException {
        return getAddressDisplayFilter().getNameAndShortDestination(address);
    }

    public static String getName(String address) throws PasswordException, IOException, GeneralSecurityException {
        String name =  getAddressDisplayFilter().getName(address);
        return name.isEmpty() ? extractName(address) : name;
    }

    private static AddressDisplayFilter getAddressDisplayFilter() throws PasswordException {
        Identities identities = I2PBote.getInstance().getIdentities();
        if (ADDRESS_DISPLAY_FILTER == null)
            ADDRESS_DISPLAY_FILTER = new AddressDisplayFilter(identities, getInstance().getAddressBook());
        return ADDRESS_DISPLAY_FILTER;
    }

    public Configuration getConfiguration() {
        return I2PBote.getInstance().getConfiguration();
    }

    public void setImapEnabled(boolean enabled) {
        I2PBote.getInstance().setImapEnabled(enabled);
    }

    public void setSmtpEnabled(boolean enabled) {
        I2PBote.getInstance().setSmtpEnabled(enabled);
    }

    public String getAppVersion() {
        return I2PBote.getAppVersion();
    }

    public String getLanguage() {
        return I2PBote.getLanguage();
    }

    /**
     * Tests whether a given string can be converted to an integer.
     * @param str
     */
    protected static boolean isNumeric(String str) {
        return Pattern.matches("\\d+", str);
    }

    /**
     * Looks for a name in a sender address. If the address contains no name
     * (i.e. only an Email Destination), an empty string is returned.
     * @param address
     */
    public static String extractName(String address) {
        if (address == null)
            return "";

        int ltIndex = address.indexOf('<');
        if (ltIndex >= 0)
            return address.substring(0, ltIndex).trim();
        else
            return "";
    }

    public long getRandomNumber() {
        return RandomSource.getInstance().nextLong();
    }

    public static boolean tryPassword(String password) throws IOException, GeneralSecurityException {
        byte[] passwordBytes = password.getBytes();
        try {
            I2PBote.getInstance().tryPassword(passwordBytes);
            return true;
        }
        catch (PasswordException e) {
            return false;
        }
    }

    public boolean isPasswordInCache() {
        return I2PBote.getInstance().isPasswordInCache();
    }

    public boolean isPasswordRequired() {
        return I2PBote.getInstance().isPasswordRequired();
    }

    public static void clearPassword() {
        I2PBote.getInstance().clearPassword();
    }

    public List<File> getUndecryptableFiles() throws PasswordException, IOException, GeneralSecurityException {
        return I2PBote.getInstance().getUndecryptableFiles();
    }

    public boolean getRequiredCryptoStrengthSatisfied() {
        return _isUnlimited;
    }

    public String getJREHome() {
        return System.getProperty("java.home");
    }
}

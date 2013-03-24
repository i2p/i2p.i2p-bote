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

package i2p.bote.web;

import i2p.bote.Configuration;
import i2p.bote.I2PBote;
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
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.TrashFolder;
import i2p.bote.network.DhtException;
import i2p.bote.network.NetworkStatus;
import i2p.bote.packet.dht.Contact;
import i2p.bote.service.EmailChecker;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import net.i2p.I2PAppContext;
import net.i2p.data.Destination;
import net.i2p.util.Log;
import net.i2p.util.RandomSource;
import net.i2p.util.Translate;

/**
 * Implements the JSP functions defined in the <code>i2pbote.tld</code> file,
 * and serves as a bean for JSPs.
 */
public class JSPHelper {
    private static AddressDisplayFilter ADDRESS_DISPLAY_FILTER;
    private static JSPHelper instance;

    public JSPHelper() {
    }
    
    private static JSPHelper getInstance() {
        if (instance == null)
            instance = new JSPHelper();
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
    
    public String getLocalDestination() {
        Destination dest = I2PBote.getInstance().getLocalDestination();
        if (dest != null)
            return Util.toBase32(dest);
        return Util._("Not set.");
    }
    
    /**
     * Updates an email identity if <code>createNew</code> is <code>false</code>,
     * or adds a new identity if <code>createNew</code> is <code>true</code>.
     * @param createNew 
     * @param cryptoImplId The id value of the cryptographic algorithm set to use for the new identity; ignored if <code>createNew</code> is <code>false</code>
     * @param key A base64-encoded Email Destination key
     * @param description
     * @param publicName
     * @param emailAddress
     * @param setDefault If this is <code>true</code>, the identity becomes the new default identity. Otherwise, the default stays the same.
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     * @throws IOException 
     */
    public static void createOrModifyIdentity(boolean createNew, int cryptoImplId, String key, String publicName, String description, String emailAddress, boolean setDefault) throws GeneralSecurityException, PasswordException, IOException {
        Log log = new Log(JSPHelper.class);
        Identities identities = I2PBote.getInstance().getIdentities();
        EmailIdentity identity = identities.get(key);
        
        if (createNew) {
            CryptoImplementation cryptoImpl = CryptoFactory.getInstance(cryptoImplId);
            if (cryptoImpl == null) {
                String errorMsg = Util._("Invalid ID number for CryptoImplementation: " + cryptoImplId);
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
                
            try {
                identity = new EmailIdentity(cryptoImpl);
            } catch (GeneralSecurityException e) {
                log.error("Can't create email identity from base64 string: <" + key + ">", e);
                throw e;
            }
            identity.setPublicName(publicName);
            identity.setDescription(description);
            identity.setEmailAddress(emailAddress);
            identities.add(identity);
        }
        else {
            identity.setPublicName(publicName);
            identity.setDescription(description);
            identity.setEmailAddress(emailAddress);
        }

        // update the default identity
        if (setDefault)
            identities.setDefault(identity);
    }

    public static void modifyIdentity(String key, String publicName, String description, String emailAddress, boolean setDefault) throws GeneralSecurityException, PasswordException, IOException {
        createOrModifyIdentity(false, -1, key, publicName, description, emailAddress, setDefault);
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
                Log log = new Log(JSPHelper.class);
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

    /**
     * @see EmailChecker#getLastMailCheckTime()
     */
    public Date getLastMailCheckTime() {
        return I2PBote.getInstance().getLastMailCheckTime();
    }
    
    public boolean isNewMailReceived() {
        return I2PBote.getInstance().newMailReceived();
    }
    
    public static EmailFolder getMailFolder(String folderName) {
        if ("Inbox".equals(folderName))
            return I2PBote.getInstance().getInbox();
        else if ("Outbox".equals(folderName))
            return I2PBote.getInstance().getOutbox();
        else if ("Sent".equals(folderName))
            return I2PBote.getInstance().getSentFolder();
        else if ("Trash".equals(folderName))
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
            new Log(JSPHelper.class).error("Can't get address to reply to.", e);
            return null;
        }
    }
    
    public static String getEmailStatus(Email email) {
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
        if (folder instanceof TrashFolder)
            return folder.delete(messageId);
        else
            return I2PBote.getInstance().moveToTrash(folder, messageId);
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
     * Returns a new <code>SortedMap<String, String></code> that contains only those
     * entries from the original map whose key is <code>"recipient"</code>, followed
     * by a whole number.
     * @param parameters
     * @return A map whose keys start with "recipient", sorted by key
     */
    public static SortedMap<String, String> getSortedRecipientParams(Map<String, String> parameters) {
        SortedMap<String, String> newMap = new TreeMap<String, String>();
        for (String key: parameters.keySet()) {
            if (key == null)
                continue;
            if (key.startsWith("recipient")) {
                String indexString = key.substring("recipient".length());
                if (isNumeric(indexString)) {
                    String value = parameters.get(key);
                    if (value ==null)
                        value = "";
                    newMap.put(key, value);
                }
            }
        }
        return newMap;
    }
    
    public static List<RecipientAddress> mergeRecipientFields(ServletRequest request) {
        Log log = new Log(JSPHelper.class);
        
        // Convert request.getParameterMap() to a Map<String, String>
        @SuppressWarnings("unchecked")
        Map<String, String[]> parameterArrayMap = request.getParameterMap();
        Map<String, String> parameterStringMap = new HashMap<String, String>();
        for (Map.Entry<String, String[]> parameter: parameterArrayMap.entrySet()) {
            String[] value = parameter.getValue();
            if (value!=null && value.length>0)
                parameterStringMap.put(parameter.getKey(), value[0]);
            else
                parameterStringMap.put(parameter.getKey(), "");
        }
        Map<String, String> oldAddresses = getSortedRecipientParams(parameterStringMap);
        
        String action = request.getParameter("action");
        int indexToRemove = -1;
        if (action!=null && action.startsWith("removeRecipient")) {
            String indexString = action.substring("removeRecipient".length());
            if (isNumeric(indexString))
                indexToRemove = Integer.valueOf(indexString);
        }
        
        // make an Iterator over the selectedContact values
        String[] newAddressesArray = request.getParameterValues("selectedContact");
        Iterator<String> newAddresses;
        if (newAddressesArray == null)
            newAddresses = new ArrayList<String>().iterator();
        else
            newAddresses = Arrays.asList(newAddressesArray).iterator();
        
        // make selectedContact values and oldAddresses into one List
        List<RecipientAddress> mergedAddresses = new ArrayList<RecipientAddress>();
        int i = 0;
        for (String address: oldAddresses.values()) {
            // don't add it if it needs to be removed
            if (i == indexToRemove) {
                i++;
                continue;
            }
            
            String typeKey = "recipientType" + i;
            String type;
            if (parameterStringMap.containsKey(typeKey))
                type = parameterStringMap.get(typeKey);
            else {
                log.error("Request contains a parameter named recipient" + i + ", but no parameter named recipientType" + i + ".");
                type = "to";
            }
            
            if (!address.trim().isEmpty())
                mergedAddresses.add(new RecipientAddress(type, address));
            // if an existing address field is empty and a selectedContact is available, put the selectedContact into the address field
            else if (newAddresses.hasNext())
                mergedAddresses.add(new RecipientAddress(type, newAddresses.next()));
            else
                mergedAddresses.add(new RecipientAddress(type, ""));
            
            i++;
        }
        
        // add any remaining selectedContacts
        while ((newAddresses.hasNext()))
            mergedAddresses.add(new RecipientAddress("to", newAddresses.next()));
            
        if ("addRecipientField".equalsIgnoreCase(action))
            mergedAddresses.add(new RecipientAddress("to", ""));
        // Make sure there is a blank recipient field at the end so all non-empty fields have a remove button next to them
        else if (mergedAddresses.isEmpty() || !mergedAddresses.get(mergedAddresses.size()-1).getAddress().isEmpty())
            mergedAddresses.add(new RecipientAddress("to", ""));
        
        return mergedAddresses;
    }
    
    public static class RecipientAddress {
        private String addressType;
        private String address;
        
        public RecipientAddress(String addressType, String address) {
            this.addressType = addressType;
            this.address = address;
        }

        public String getAddressType() {
            return addressType;
        }

        public String getAddress() {
            return address;
        }
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
    
    public static String getNameAndDestination(String address) throws PasswordException, IOException, GeneralSecurityException {
        return getAddressDisplayFilter().getNameAndDestination(address);
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
    private static boolean isNumeric(String str) {
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
    
    public static String escapeQuotes(String s) {
        if (s == null)
            return null;
        
        return s.replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
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
    
    public static String changePassword(String oldPassword, String newPassword, String confirmNewPassword) {
        try {
            return I2PBote.getInstance().changePassword(oldPassword.getBytes(), newPassword.getBytes(), confirmNewPassword.getBytes());
        }
        catch (Exception e) {
            Log log = new Log(JSPHelper.class);
            log.error("Error while changing password", e);
            return e.getLocalizedMessage();
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
    
    public boolean isUpdateAvailable() {
        return I2PBote.getInstance().isUpdateAvailable();
    }
    
    public static String getFileSize(String filename) {
        return Util.getHumanReadableSize(new File(filename));
    }
    
    public static String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            new Log(JSPHelper.class).error("UTF-8 not supported!", e);
            return input;
        }
    }
    
    public static String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            new Log(JSPHelper.class).error("UTF-8 not supported!", e);
            return input;
        }
    }
    
    /**
     * Inserts the current locale (<code>de</code>, <code>fr</code>, etc.) into a filename
     * if the locale is not <code>en</code>. For example, <code>FAQ.html</code> becomes
     * <code>FAQ_fr.html</code> in the French locale.
     * @param baseName The filename for the <code>en</code> locale
     * @param context To find out if a localized file exists
     * @return The name of the localized file, or <code>baseName</code> if no localized version exists
     */
    public static String getLocalizedFilename(String baseName, ServletContext context) {
        String language = Translate.getLanguage(I2PAppContext.getGlobalContext());
        
        if (language.equals("en"))
            return baseName;
        else {
            String localizedName;
            if (baseName.contains(".")) {
                int dotIndex = baseName.lastIndexOf('.');
                localizedName = baseName.substring(0, dotIndex) + "_" + language + baseName.substring(dotIndex);
            }
            else
                localizedName = baseName + "_" + language;
            
            try {
                if (context.getResource("/" + localizedName) != null)
                    return localizedName;
                else
                    return baseName;
            } catch (MalformedURLException e) {
                new Log(JSPHelper.class).error("Invalid URL: </" + localizedName + ">", e);
                return baseName;
            }
        }
    }
}
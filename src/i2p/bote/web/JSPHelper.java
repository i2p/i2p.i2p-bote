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
import i2p.bote.addressbook.Contact;
import i2p.bote.email.Email;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Field;
import i2p.bote.email.Identities;
import i2p.bote.folder.EmailFolder;
import i2p.bote.network.NetworkStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.MessagingException;

import net.i2p.data.DataFormatException;

/**
 * Implements the JSP functions defined in the <code>i2pbote.tld</code> file.
 */
public class JSPHelper {

    public JSPHelper() {
    }
    
    public static NetworkStatus getNetworkStatus() {
        return I2PBote.getInstance().getNetworkStatus();
    }
    
    public static Identities getIdentities() {
        return I2PBote.getInstance().getIdentities();
    }
    
    public static AddressBook getAddressBook() {
        return I2PBote.getInstance().getAddressBook();
    }
    
    public static String getLocalDestination() {
        return I2PBote.getInstance().getLocalDestination().calculateHash().toBase64();
    }
    
    /**
     * Updates an email identity if the Destination <code>key</code> exists, or adds a new identity.
     * @param key A base64-encoded Email Destination key
     * @param description
     * @param publicName
     * @param emailAddress
     * @param setDefault If this is <code>true</code>, the identity becomes the new default identity. Otherwise, the default stays the same.
     * @return null if sucessful, or an error message if an error occured
     */
    public static String saveIdentity(String key, String publicName, String description, String emailAddress, boolean setDefault) {
        Identities identities = getIdentities();
        EmailIdentity identity = identities.get(key);
        
        if (identity != null) {
            identity.setPublicName(publicName);
            identity.setDescription(description);
            identity.setEmailAddress(emailAddress);
        }
        else {
            identity = new EmailIdentity();
            identity.setPublicName(publicName);
            identity.setDescription(description);
            identity.setEmailAddress(emailAddress);
            identities.add(identity);
        }

        // update the default identity
        if (setDefault)
            identities.setDefault(identity);
        
        try {
            identities.save();
            return null;
        }
        catch (IOException e) {
            return e.getLocalizedMessage();
        }
    }

    /**
     * Deletes an email identity.
     * @param key A base64-encoded email identity key
     * @return null if sucessful, or an error message if an error occured
     */
    public static String deleteIdentity(String key) {
        Identities identities = getIdentities();
        identities.remove(key);

        try {
            identities.save();
            return null;
        }
        catch (IOException e) {
            return e.getLocalizedMessage();
        }
    }

    /**
     * Updates acontact in the address book if the Destination <code>destinationString</code> exists,
     * or adds a new contact to the address book.
     * @param destinationString A base64-encoded Email Destination key
     * @param name
     * @return null if sucessful, or an error message if an error occured
     */
    public static String saveContact(String destinationString, String name) {
        AddressBook addressBook = getAddressBook();
        Contact contact = addressBook.get(destinationString);
        
        if (contact != null)
            contact.setName(name);
        else {
            EmailDestination destination;
            try {
                destination = new EmailDestination(destinationString);
            } catch (DataFormatException e) {
                return e.getLocalizedMessage();
            }
            contact = new Contact(destination, name);
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
     * @param key A base64-encoded email destination
     * @return null if sucessful, or an error message if an error occured
     */
    public static String deleteContact(String destination) {
        AddressBook addressBook = getAddressBook();
        addressBook.remove(destination);

        try {
            addressBook.save();
            return null;
        }
        catch (IOException e) {
            return e.getLocalizedMessage();
        }
    }
    
    public static void checkForMail() {
        I2PBote.getInstance().checkForMail();
    }

    public static boolean isCheckingForMail() {
        return I2PBote.getInstance().isCheckingForMail();
    }
 
    public static boolean newMailReceived() {
        return I2PBote.getInstance().newMailReceived();
    }
    
    public static EmailFolder getMailFolder(String folderName) {
        if ("Inbox".equals(folderName))
            return I2PBote.getInstance().getInbox();
        else
            return null;
    }
    
    public static Email getEmail(String folderName, String messageId) {
        return getMailFolder(folderName).getEmail(messageId);
    }

    public static List<Email> getEmails(EmailFolder folder, Field sortColumn, boolean descending) {
        return folder.getElements(sortColumn, descending);
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
     * the local node.
     * If the email was sent to more than one local Email Destination, one
     * of them is returned.
     * If the email does not contain a local Email Destination, <code>null</code>
     * is returned.
     * @param email
     * @return
     */
    public static Address getOneLocalRecipient(Email email) {
        Address[] recipients;
        try {
            recipients = email.getAllRecipients();
        }
        catch (MessagingException e) {
            return null;
        }
        
        Identities identities = getIdentities();
        for (EmailDestination localDestination: identities) {
            String base64Dest = localDestination.toBase64();
            for (Address recipient: recipients)
                if (recipient.toString().contains(base64Dest))
                    return recipient;
        }
        
        return null;
    }
    
    public static boolean deleteEmail(String folderName, String messageId) {
        return getMailFolder(folderName).delete(messageId);
    }
    
    /**
     * Looks for an Email Destination in a sender address. If no Email Destination
     * is found, </code>null</code> is returned.
     * @param address
     * @return
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
    public static SortedMap<String, String> getRecipients(Map<String, String> parameters) {
        SortedMap<String, String> newMap = new TreeMap<String, String>();
        for (String key: parameters.keySet()) {
            if (key == null)
                continue;
            if (key.startsWith("recipient")) {
                String indexString = key.substring("recipient".length());
                if (isNumeric(indexString)) {
                    String value = parameters.get(key);
                    if (value!=null && !value.isEmpty())
                        newMap.put(key, value);
                }
            }
        }
        return newMap;
    }
    
    /**
     * Returns <code>true</code> if <code>address</code> contains a base64-encoded
     * email destination that is either in the address book or among the local
     * email identities.
     * @param address
     * @return
     */
    public static boolean isKnown(String address) {
        String destination = extractEmailDestination(address);
        if (destination == null)
            return false;
        else if (getAddressBook().contains(destination))
            return true;
        else return getIdentities().contains(destination);
    }
    
    public static String getNameAndDestination(Address address) {
        return Util.getNameAndDestination(getIdentities(), getAddressBook(), address);
    }
    
    public Configuration getConfiguration() {
        return I2PBote.getInstance().getConfiguration();
    }
    
    public String getAppVersion() {
        return I2PBote.getAppVersion();
    }
    
    private static boolean isNumeric(String str) {
        return Pattern.matches("\\d+", str);
    }
    
    /**
     * Looks for a name in a sender address. If the address contains no name
     * (i.e. only an Email Destination), an empty string is returned.
     * @param address
     * @return
     */
    public static String extractName(String address) {
        if (address == null)
            return "";
        
        int ltIndex = address.indexOf('<');
        if (ltIndex >= 0)
            return address.substring(0, ltIndex);
        else
            return "";
    }
}
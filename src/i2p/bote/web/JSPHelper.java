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
import i2p.bote.email.EmailAttribute;
import i2p.bote.email.Identities;
import i2p.bote.folder.EmailFolder;
import i2p.bote.network.NetworkStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.servlet.ServletRequest;

import net.i2p.data.DataFormatException;
import net.i2p.util.Log;

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
    
    public static EmailIdentity getIdentity(String key) {
        return getIdentities().get(key);
    }
    
    /**
     * Updates acontact in the address book if the Destination <code>destinationString</code> exists,
     * or adds a new contact to the address book.
     * @param destinationString A base64-encoded Email Destination key
     * @param name
     * @return null if sucessful, or an error message if an error occured
     */
    public static String saveContact(String destinationString, String name) {
        destinationString = Util.fixAddress(destinationString);
        
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
    
    public static String getContactName(String destination) {
        Contact contact = getAddressBook().get(destination);
        if (contact == null)
            return null;
        else
            return contact.getName();
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
        else if ("Outbox".equals(folderName))
            return I2PBote.getInstance().getOutbox();
        else
            return null;
    }
    
    public static Email getEmail(String folderName, String messageId) {
        return getMailFolder(folderName).getEmail(messageId);
    }

    public static List<Email> getEmails(EmailFolder folder, EmailAttribute sortColumn, boolean descending) {
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
    
    public static String getEmailStatus(Email email) {
        return I2PBote.getInstance().getOutbox().getStatus(email);
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
            
        // Make sure there is at least one recipient field
        if (mergedAddresses.isEmpty())
            mergedAddresses.add(new RecipientAddress("to", ""));
        
        if ("addRecipientField".equalsIgnoreCase(action))
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
    
    public static String getNameAndDestination(String address) {
        return Util.getNameAndDestination(getIdentities(), getAddressBook(), address);
    }
    
    public Configuration getConfiguration() {
        return I2PBote.getInstance().getConfiguration();
    }
    
    public String getAppVersion() {
        return I2PBote.getAppVersion();
    }
    
    /**
     * Tests whether a given string can be converted to an integer.
     * @param str
     * @return
     */
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
    
    public static String escapeQuotes(String s) {
        if (s == null)
            return null;
        
        return s.replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
    }
}
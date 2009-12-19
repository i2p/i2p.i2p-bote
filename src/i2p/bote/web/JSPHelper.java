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

import i2p.bote.I2PBote;
import i2p.bote.email.Email;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Identities;
import i2p.bote.folder.EmailFolder;
import i2p.bote.network.NetworkStatus;

import java.io.IOException;

/**
 * Implements the JSP functions defined in the <code>i2pbote.tld</code> file.
 */
public class JSPHelper {

    private JSPHelper() {
        throw new UnsupportedOperationException();
    }
    
    public static NetworkStatus getNetworkStatus() {
        return I2PBote.getInstance().getNetworkStatus();
    }
    
    public static Identities getIdentities() {
        return I2PBote.getInstance().getIdentities();
    }
    
    public String getLocalDestination() {
        return I2PBote.getInstance().getLocalDestination();
    }
    
    /**
     * Updates an email identity if <code>key</code> exists, or adds a new identity.
     * @param key A base64-encoded email identity key
     * @param description
     * @param publicName
     * @param emailAddress
     * @return null if sucessful, or an error message if an error occured
     */
    public static String saveIdentity(String key, String publicName, String description, String emailAddress) {
        Identities identities = JSPHelper.getIdentities();
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
        Identities identities = JSPHelper.getIdentities();
        identities.remove(key);

        try {
            identities.save();
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

    public static boolean deleteEmail(String folderName, String messageId) {
        return JSPHelper.getMailFolder(folderName).delete(messageId);
    }
}
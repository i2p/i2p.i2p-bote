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

import i2p.bote.addressbook.AddressBook;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * This class is used for adding/replacing names in email addresses with
 * local names (address book entries and email identities).
 */
public class AddressDisplayFilter {
    private Identities identities;
    private AddressBook addressBook;
    
    public AddressDisplayFilter(Identities identities, AddressBook addressBook) {
        this.identities = identities;
        this.addressBook = addressBook;
    }
    
    /**
     * Looks up the name associated with a Base64-encoded Email Destination
     * in the address book and the local identities, and returns a string
     * that contains the name and the Base64-encoded destination.<br/>
     * If <code>address</code> already contains a name, it is replaced with
     * the one from the address book or identities.<br/>
     * If no name is found in the address book or the identities, or if
     * <code>address</code> does not contain a valid Email Destination,
     * <code>address</code> is returned.
     * @param address A Base64-encoded Email Destination, and optionally a name
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public String getNameAndDestination(String address) throws PasswordException, IOException, GeneralSecurityException {
        String base64dest = EmailDestination.extractBase64Dest(address);
        if (base64dest != null) {
            // try the address book
            Contact contact = addressBook.get(base64dest);
            if (contact != null)
                return contact.getName() + " <" + contact.getBase64Dest() + ">";
            
            // if no address book entry, try the email identities
            EmailIdentity identity = identities.get(base64dest);
            if (identity != null)
                return identity.getPublicName() + " <" + identity.toBase64() + ">";
        }
        
        return address;
    }
    
    /**
     * Looks up the name associated with a Base64-encoded Email Destination
     * in the address book and the local identities, and returns a string
     * that contains the name and the Base64-encoded destination + <code>@bote</code>.<br/>
     * If <code>address</code> already contains a name, it is replaced with
     * the one from the address book or identities.<br/>
     * If no name is found in the address book or the identities, or if
     * <code>address</code> does not contain a valid Email Destination,
     * <code>[UNK] address</code> is returned.
     * @param address A Base64-encoded Email Destination, and optionally a name
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public String getImapNameAndDestination(String address) throws PasswordException, IOException, GeneralSecurityException {
        String base64dest = EmailDestination.extractBase64Dest(address);
        if (base64dest != null) {
            // try the address book
            Contact contact = addressBook.get(base64dest);
            if (contact != null)
                return contact.getName() + " <" + contact.getBase64Dest() + "@bote>"; // TODO: Make @bote optional
            
            // if no address book entry, try the email identities
            EmailIdentity identity = identities.get(base64dest);
            if (identity != null)
                return identity.getPublicName() + " <" + identity.toBase64() + "@bote>"; // TODO: Make @bote optional

            // not known - append [UNK] if a name was provided
            if (!base64dest.equals(address)) {
                int gtIndex = address.lastIndexOf('>');
                return "{UNK} " + address.substring(0, gtIndex) + "@bote>";
            }
        }

        if (address.indexOf('@') < address.indexOf('.'))
            return address; // External, will never be in Bote addressbook

        // A plain B64 address or something unknown
        return address + "@bote";
    }
    
    /**
     * Same as {@link #getNameAndDestination(String)} but base64 destinations are shortened to 4 characters
     * if they are in the address book or refer to an email identity.
     */ 
    public String getNameAndShortDestination(String address) throws PasswordException, IOException, GeneralSecurityException {
        String nameAndDest = getNameAndDestination(address);
        String base64dest = EmailDestination.extractBase64Dest(address);
        if (base64dest == null)
            return nameAndDest;
        if (nameAndDest.contains(base64dest) && nameAndDest.length()>base64dest.length())
            nameAndDest = nameAndDest.replace(base64dest, base64dest.substring(0, 4));
        return nameAndDest;
    }
    
    /**
     * Looks up the name associated with a Base64-encoded Email Destination
     * in the address book and the local identities, and returns a string
     * that contains the name.<br/>
     * If <code>address</code> already contains a name, it is replaced with
     * the one from the address book or identities.<br/>
     * If no name is found in the address book or the identities, or if
     * <code>address</code> does not contain a valid Email Destination,
     * an empty string is returned.
     * @param address A Base64-encoded Email Destination, and optionally a name
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public String getName(String address) throws PasswordException, IOException, GeneralSecurityException {
        String base64dest = EmailDestination.extractBase64Dest(address);
        if (base64dest != null) {
            // try the address book
            Contact contact = addressBook.get(base64dest);
            if (contact != null)
                return contact.getName();
            
            // if no address book entry, try the email identities
            EmailIdentity identity = identities.get(base64dest);
            if (identity != null)
                return identity.getPublicName();
        }

        return "";
    }
}

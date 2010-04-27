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
import i2p.bote.addressbook.Contact;

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
     * Looks up the name associated with a Base64-encoded Email Destination,
     * in the address book and the local identities, and returns a string
     * that contains the name and the Base64-encoded destination.
     * If <code>address</code> already contains a name, it is replaced with
     * the one from the address book or identities.
     * If no name is found in the address book or the identities, or if
     * <code>address</code> does not contain a valid Email Destination,
     * <code>address</code> is returned.
     * @param address A Base64-encoded Email Destination, and optionally a name
     * @return
     */
    public String getNameAndDestination(String address) {
        String base64dest = EmailDestination.extractBase64Dest(address);
        if (base64dest != null) {
            // try the address book
            Contact contact = addressBook.get(base64dest);
            if (contact != null)
                return contact.getName() + "<" + contact.toBase64() + ">";
            
            // if no address book entry, try the email identities
            EmailIdentity identity = identities.get(base64dest);
            if (identity != null)
                return identity.getPublicName() + "<" + identity.toBase64() + ">";
        }
        
        return address;
    }
}
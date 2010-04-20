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

package i2p.bote;

import i2p.bote.addressbook.AddressBook;
import i2p.bote.addressbook.Contact;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Identities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadFactory;

import javax.mail.Address;

import net.i2p.I2PAppContext;
import net.i2p.util.Translate;

public class Util {
	private static final String BUNDLE_NAME = "i2p.bote.locale.Messages";
	
	private Util() { }
	
    /**
     * Looks up the name associated with a Base64-encoded Email Destination,
     * in the address book and the local identities, and returns a string
     * that contains the name and the Base64-encoded destination.
     * If <code>address</code> already contains a name, it is replaced with
     * the one from the address book or identities.
     * If no name is found in the address book or the identities, or if
     * <code>address</code> does not contain a valid Email Destination,
     * <code>address</code> is returned.
     * @param identities
     * @param addressBook
     * @param address A Base64-encoded Email Destination, and optionally a name
     * @return
     */
    public static String getNameAndDestination(Identities identities, AddressBook addressBook, String address) {
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
    
    public static String getNameAndDestination(Identities identities, AddressBook addressBook, Address address) {
        if (address == null)
            return null;
        else
            return getNameAndDestination(identities, addressBook, address.toString());
    }
    
    public static byte[] readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[32*1024];
        while (true) {
            int bytesToRead = Math.min(inputStream.available(), buffer.length);
            if (bytesToRead <= 0)
                break;
            else {
                int bytesRead = inputStream.read(buffer, 0, bytesToRead);
                byteStream.write(buffer, 0, bytesRead);
            }
        }
        return byteStream.toByteArray();
    }

    public static ThreadFactory createThreadFactory(final String threadName, final int stackSize) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(Thread.currentThread().getThreadGroup(), runnable, threadName, stackSize);
            }
        };
    }
    
    /**
     * Creates a thread-safe <code>Iterable</code> from a thread-unsafe one.
     * Modifications to the old <code>Iterable</code> will not affect the
     * new one.
     * @param <E>
     * @param iterable
     * @return
     */
    public static <E> Iterable<E> synchronizedCopy(Iterable<E> iterable) {
        synchronized(iterable) {
            Collection<E> collection = new ArrayList<E>();
            for (E element: iterable)
                collection.add(element);
            return collection;
        }
    }

    /**
     * Returns the <code>i</code>-th element of a <code>Collection</code>'s <code>Iterator</code>.
     * @param <E>
     * @param collection
     * @param i
     * @return
     */
    public static <E> E get(Collection<E> collection, int i) {
        for (E element: collection) {
            if (i == 0)
                return element;
            i--;
        }
        return null;
    }
    
    public static String _(String messageKey) {
        return Translate.getString(messageKey, I2PAppContext.getGlobalContext(), BUNDLE_NAME);
    }
    
    public static String _(String messageKey, Object parameter) {
        return Translate.getString(messageKey, parameter, I2PAppContext.getGlobalContext(), BUNDLE_NAME);
    }
    
    /**
     * Removes all whitespace at the beginning and the end of a string,
     * and replaces multiple whitespace characters with a single space.
     * @param string
     * @return
     */
    public static String removeExtraWhitespace(String string) {
        if (string == null)
            return null;
        return string.trim().replaceAll("\\s+", " ");
    }
}
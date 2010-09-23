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

import i2p.bote.Util;
import i2p.bote.email.EmailDestination;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import net.i2p.util.Log;

/**
 * Implements the private address book. Holds a set of {@link Contact}s
 * which are sorted by name.
 */
public class AddressBook implements Iterable<Contact> {
    private Log log = new Log(AddressBook.class);
    private File addressFile;
    private SortedSet<Contact> contacts;

    /**
     * Reads an <code>AddressBook</code> from a text file. Each contact is defined
     * by one line that contains an Email Destination and a name, separated by a
     * tab character.
     * @param addressFile
     */
    public AddressBook(File addressFile) {
        this.addressFile = addressFile;
        contacts = new TreeSet<Contact>(new NameComparator());
        
        if (!addressFile.exists()) {
            log.debug("Address file does not exist: <" + addressFile.getAbsolutePath() + ">");
            return;
        }
        
        log.debug("Reading address book from <" + addressFile.getAbsolutePath() + ">");
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(addressFile));
            
            while (true) {
                String line = input.readLine();
                if (line == null)   // EOF
                    break;
                
                String[] fields = line.split("\\t", 2);
                try {
                    EmailDestination destination = new EmailDestination(fields[0]);
                    String name = null;
                    if (fields.length > 1)
                        name = fields[1];
                    contacts.add(new Contact(destination, name));
                }
                catch (GeneralSecurityException e) {
                    log.error("Not a valid Email Destination: <" + fields[0] + ">", e);
                }
            }
        } catch (IOException e) {
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
 
    public void save() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(addressFile));
        try {
            for (Contact contact: contacts) {
                writer.write(contact.toBase64());
                writer.write("\t");
                writer.write(contact.getName());
                writer.newLine();
            }
            Util.makePrivate(addressFile);
        }
        catch (IOException e) {
            log.error("Can't save address book to file <" + addressFile.getAbsolutePath() + ">.", e);
            throw e;
        }
        finally {
            writer.close();
        }
    }
    
    public void add(Contact contact) {
        contacts.add(contact);
    }
    
    public void remove(String destination) {
        Contact contact = get(destination);
        if (contact != null)
            contacts.remove(contact);
    }
    
    /**
     * Looks up an {@link Contact} by its Base64 key. If none is found,
     * <code>null</code> is returned.
     * @param destination
     */
    public Contact get(String destination) {
        if (destination==null || destination.isEmpty())
            return null;
        
        for (Contact contact: contacts)
            if (destination.equals(contact.toBase64()))
                return contact;
        return null;
    }
    
    /**
     * Returns <code>true</code> if a given Base64-encoded Email Destination
     * is in the address book.
     * @param base64dest
     */
    public boolean contains(String base64dest) {
        if (base64dest == null)
            return false;
        
        for (Contact contact: contacts)
            if (base64dest.equals(contact.toBase64()))
                return true;
        return false;
    }
    
    public SortedSet<Contact> getAll() {
        return contacts;
    }
    
    public int size() {
        return contacts.size();
    }
    
    @Override
    public Iterator<Contact> iterator() {
        return contacts.iterator();
    }

    private class NameComparator implements Comparator<Contact> {
        @Override
        public int compare(Contact contact1, Contact contact2) {
            return String.CASE_INSENSITIVE_ORDER.compare(contact1.getName(), contact2.getName());
        }
    }
}
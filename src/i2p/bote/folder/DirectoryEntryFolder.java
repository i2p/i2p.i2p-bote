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

package i2p.bote.folder;

import i2p.bote.packet.dht.Contact;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.File;
import java.security.GeneralSecurityException;

import net.i2p.util.Log;

/** Stores DHT packets of type {@link Contact}. */
public class DirectoryEntryFolder extends DhtPacketFolder<Contact> {
    private Log log = new Log(DirectoryEntryFolder.class);
    
    public DirectoryEntryFolder(File storageDir) {
        super(storageDir);
    }
    
    @Override
    public void store(DhtStorablePacket packetToStore) {
        File packetFile = findPacketFile(packetToStore.getDhtKey());
        if (packetFile != null)
            log.debug("Not storing directory packet with DHT key " + packetToStore.getDhtKey() + " because file exists.");
        else {
            if (!(packetToStore instanceof Contact))
                log.error("Expected class Contact, got " + packetToStore.getClass());
            else {
                Contact contact = (Contact)packetToStore;
                try {
                    if (!contact.verify())
                        log.debug("Not storing Contact because verification failed.");
                    else
                        super.store(packetToStore);
                } catch (GeneralSecurityException e) {
                    log.error("Can't verify Contact", e);
                }
            }
        }
    }
}
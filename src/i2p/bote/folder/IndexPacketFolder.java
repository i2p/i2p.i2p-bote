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

import i2p.bote.UniqueId;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.DeletionInfoPacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.IndexPacketDeleteRequest;
import i2p.bote.packet.IndexPacketEntry;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * This class uses Email Destination hashes for DHT keys.
 * It differs from {@link DhtPacketFolder} in the following ways:
 *  * It doesn't overwrite an existing packet when a new packet is stored under the same key,
 *    but merges the packets.
 *  * It retains DHT keys of deleted packets in a {@link DeletionInfoPacket} file named
 *    <code>DEL_<dht_key>.pkt</code>, where <dht_key> is the DHT key of the index packet.
 */
public class IndexPacketFolder extends DeletionAwareFolder<IndexPacket> implements PacketListener, ExpirationListener {
    private final Log log = new Log(IndexPacketFolder.class);

    public IndexPacketFolder(File storageDir) {
        super(storageDir);
    }

    /** Overridden to merge the packet with an existing one, and to set time stamps on the packet entries */
    @Override
    public synchronized void store(DhtStorablePacket packetToStore) {
        if (!(packetToStore instanceof IndexPacket))
            throw new IllegalArgumentException("This class only stores packets of type " + IndexPacket.class.getSimpleName() + ".");
        
        IndexPacket indexPacketToStore = (IndexPacket)packetToStore;
        DhtStorablePacket existingPacket = retrieve(packetToStore.getDhtKey());
        
        // If an index packet with the same key exists in the folder, merge the two packets.
        if (existingPacket instanceof IndexPacket) {
            packetToStore = new IndexPacket(indexPacketToStore, (IndexPacket)existingPacket);
            if (packetToStore.isTooBig())
                // TODO make two new index packets, put half the email packet keys in each one, store the two index packets on the DHT, and put the two index packet keys into the local index file (only keep those two).
                log.error("After merging, IndexPacket is too big for a datagram: size=" + packetToStore.getSize());
        }
        
        setTimeStamps(indexPacketToStore);
        super.store(packetToStore);
    }

    private void setTimeStamps(IndexPacket packet) {
        long currentTime = System.currentTimeMillis();
        for (IndexPacketEntry entry: packet)
            entry.storeTime = currentTime;
    }
    
    /** Overridden to erase time stamps because there is no need for other peers to see it. */
    @Override
    public DhtStorablePacket retrieve(Hash dhtKey) {
        DhtStorablePacket packet = super.retrieve(dhtKey);
        if (!(packet instanceof IndexPacket)) {
            if (packet != null)
                log.error("Packet of type " + packet.getClass().getSimpleName() + " found in " + getClass().getSimpleName());
            return null;
        }
        else {
            IndexPacket indexPacket = (IndexPacket)packet;
            for (IndexPacketEntry entry: indexPacket)
                entry.storeTime = 0;
            return indexPacket;
        }
    }
    
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof IndexPacketDeleteRequest) {
            IndexPacketDeleteRequest delRequest = (IndexPacketDeleteRequest)packet;
            process(delRequest);
        }
    }
    
    /**
     * Deletes all index packet entries that match the keys in a delete request,
     * and for which the request contains a valid delete authorization.
     * @param delRequest
     */
    public synchronized void process(IndexPacketDeleteRequest delRequest) {
        log.debug("Processing delete request: " + delRequest);
        Hash destHash = delRequest.getEmailDestHash();
        DhtStorablePacket storedPacket = retrieve(destHash);
        if (storedPacket instanceof IndexPacket) {
            IndexPacket indexPacket = (IndexPacket)storedPacket;
            Collection<Hash> keysToDelete = delRequest.getDhtKeys();
            
            for (Hash keyToDelete: keysToDelete) {
                // verify
                Hash expectedVerificationHash = indexPacket.getDeleteVerificationHash(keyToDelete);
                if (expectedVerificationHash == null)
                    log.debug("Email packet key " + keyToDelete + " from IndexPacketDeleteRequest not found in index packet for destination " + destHash);
                else {
                    UniqueId delAuthorization = delRequest.getDeleteAuthorization(keyToDelete);
                    Hash actualVerificationHash = new Hash(delAuthorization.toByteArray());
                    boolean valid = expectedVerificationHash.equals(actualVerificationHash);
                    if (valid)
                        remove(indexPacket, keyToDelete, delAuthorization);
                    else
                        log.debug("Invalid delete verification hash in IndexPacketDeleteRequest. Should be: <" + expectedVerificationHash.toBase64() + ">, is <" + actualVerificationHash.toBase64() +">");
                }
            }
        }
        else if (storedPacket != null)
            log.debug("IndexPacket expected for DHT key <" + destHash + ">, found " + storedPacket.getClass().getSimpleName());
    }

    /**
     * Deletes an entry from an {@link IndexPacket} and saves the packet to disk.
     * @param indexPacket
     * @param emailPacketKey The entry to delete
     */
    private synchronized void remove(IndexPacket indexPacket, Hash emailPacketKey, UniqueId delAuthorization) {
        log.debug("Removing DHT key " + emailPacketKey + " from Index Packet for Email Dest " + indexPacket.getDhtKey());
        indexPacket.remove(emailPacketKey);
        String delFileName = DEL_FILE_PREFIX + getFilename(indexPacket);
        addToDeletedPackets(delFileName, emailPacketKey, delAuthorization);
        super.store(indexPacket);   // don't merge, but overwrite the file with the entry removed
    }

    /**
     * Does not add a Deletion Record, just deletes the file.
     */
    @Override
    public synchronized void deleteExpired() {
        long currentTime = System.currentTimeMillis();
        for (IndexPacket indexPacket: this) {
            boolean removed = false;   // true if at least one entry was removed
            synchronized(indexPacket) {
                Iterator<IndexPacketEntry> iterator = indexPacket.iterator();
                while (iterator.hasNext()) {
                    IndexPacketEntry entry = iterator.next();
                    if (currentTime > entry.storeTime + EXPIRATION_TIME_MILLISECONDS) {
                        log.debug("Deleting expired index packet entry: file=<" + getFilename(indexPacket) + ">, emailPktKey=" + entry.emailPacketKey.toBase64());
                        iterator.remove();
                        removed = true;
                    }
                }
                if (removed)
                    super.store(indexPacket);   // don't merge, but overwrite the file with the entry/entries removed
            }
        }
    }
}
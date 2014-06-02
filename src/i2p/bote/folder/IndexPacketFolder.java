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
import i2p.bote.Util;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.dht.DeleteRequest;
import i2p.bote.packet.dht.DeletionInfoPacket;
import i2p.bote.packet.dht.DeletionRecord;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.IndexPacket;
import i2p.bote.packet.dht.IndexPacketDeleteRequest;
import i2p.bote.packet.dht.IndexPacketEntry;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * This class uses Email Destination hashes for DHT keys.<br/>
 * It differs from {@link DeletionAwareDhtFolder} in that it doesn't overwrite an existing
 * packet when a new packet is stored under the same key, but adds the new packet.
 */
public class IndexPacketFolder extends DeletionAwareDhtFolder<IndexPacket> implements PacketListener, ExpirationListener {
    private final Log log = new Log(IndexPacketFolder.class);

    public IndexPacketFolder(File storageDir) {
        super(storageDir);
    }

    /** Overridden to merge the packet with an existing one, and to set time stamps on the packet entries */
    @Override
    public synchronized void store(DhtStorablePacket packetToStore) {
        storeAndCreateDeleteRequest(packetToStore);
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
    
    /**
     * Deletes an entry from an {@link IndexPacket} and saves the packet to disk.
     * @param indexPacket
     * @param emailPacketKey The entry to delete
     */
    private synchronized void remove(IndexPacket indexPacket, Hash emailPacketKey, UniqueId delAuthorization) {
        log.debug("Removing DHT key " + emailPacketKey + " from Index Packet for Email Dest " + indexPacket.getDhtKey());
        indexPacket.remove(emailPacketKey);
        // DEL_FILE_PREFIX + getFilename(indexPacket)
        String delFileName = getDeletionFileName(indexPacket.getDhtKey());
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
                        log.debug("Deleting expired index packet entry: file=<" + getFilename(indexPacket.getDhtKey()) + ">, emailPktKey=" + entry.emailPacketKey.toBase64());
                        iterator.remove();
                        removed = true;
                    }
                }
                if (removed)
                    super.store(indexPacket);   // don't merge, but overwrite the file with the entry/entries removed
            }
        }
    }
    
    /** Overridden to put each index packet entry in its own index packet */
    @Override
    public Iterator<IndexPacket> individualPackets() {
        final Iterator<IndexPacket> bigPackets = super.individualPackets();
        
        return new Iterator<IndexPacket>() {
            IndexPacket currentPacket;
            Iterator<IndexPacketEntry> currentPacketIterator;
            
            @Override
            public boolean hasNext() {
                if (currentPacketIterator!=null && currentPacketIterator.hasNext())
                    return true;
                
                while (bigPackets.hasNext()) {
                    currentPacket = bigPackets.next();
                    currentPacketIterator = currentPacket.iterator();
                    if (currentPacketIterator.hasNext())
                        return true;
                }
                return false;
            }

            @Override
            public IndexPacket next() {
                if (currentPacketIterator==null || !currentPacketIterator.hasNext()) {
                    currentPacket = bigPackets.next();
                    currentPacketIterator = currentPacket.iterator();
                }
                
                IndexPacketEntry entry = currentPacketIterator.next();
                IndexPacket indexPacket = new IndexPacket(currentPacket.getDhtKey());
                indexPacket.put(entry);
                return indexPacket;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported!");
            }
        };
    }

    @Override
    public UniqueId getDeleteAuthorization(Hash dhtKey) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public DeleteRequest storeAndCreateDeleteRequest(DhtStorablePacket packetToStore) {
        if (!(packetToStore instanceof IndexPacket))
            throw new IllegalArgumentException("Invalid packet type: " + packetToStore.getClass().getSimpleName() + "; this folder only stores packets of type " + IndexPacket.class.getSimpleName() + ".");
        
        IndexPacket indexPacketToStore = (IndexPacket)packetToStore;
        setTimeStamps(indexPacketToStore);   // set the time stamps before merging so existing ones don't change
        DhtStorablePacket existingPacket = super.retrieve(packetToStore.getDhtKey());   // use super.retrieve() because we don't want to delete time stamps here
        
        // make a Delete Request that contains any known-to-be-deleted DHT keys from the store request
        String delFileName = getDeletionFileName(packetToStore.getDhtKey());
        DeletionInfoPacket delInfo = createDelInfoPacket(delFileName);
        IndexPacketDeleteRequest delRequest = null;
        if (delInfo != null) {
            delRequest = null;
            for (IndexPacketEntry entry: indexPacketToStore) {
                DeletionRecord delRecord = delInfo.getEntry(entry.emailPacketKey);
                if (delRecord != null) {
                    // leave delRequest null until a DeletionRecord is found
                    if (delRequest == null)
                        delRequest = new IndexPacketDeleteRequest(indexPacketToStore.getDhtKey());
                    delRequest.put(delRecord.dhtKey, delRecord.delAuthorization);
                }
            }
        }
        
        // remove deleted entries from indexPacketToStore so they are not re-stored
        if (delInfo!=null && !delInfo.isEmpty())
            for (DeletionRecord delRecord: delInfo)
                indexPacketToStore.remove(delRecord.dhtKey);
        
        // If an index packet with the same key exists in the folder, merge the two packets.
        // The resulting packet may be too big for a datagram but we don't split it until a peer asks for it.
        if (existingPacket instanceof IndexPacket)
            indexPacketToStore = new IndexPacket(indexPacketToStore, (IndexPacket)existingPacket);
        else if (existingPacket != null)
            log.error("Packet of type " + existingPacket.getClass().getSimpleName() + " found in IndexPacketFolder.");
        
        super.store(indexPacketToStore);   // don't merge, but overwrite
        return delRequest;
    }
        
    private void setTimeStamps(IndexPacket packet) {
        long currentTime = System.currentTimeMillis();
        for (IndexPacketEntry entry: packet)
            entry.storeTime = currentTime;
    }
    
    private String getDeletionFileName(Hash dhtKey) {
        return DEL_FILE_PREFIX + getFilename(dhtKey);
    }

    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof IndexPacketDeleteRequest) {
            IndexPacketDeleteRequest delRequest = (IndexPacketDeleteRequest)packet;
            process(delRequest);
        }
    }

    /**
     * Deletes index packet entries.
     * @param delRequest An instance of {@link IndexPacketDeleteRequest}
     */
    @Override
    public synchronized void process(DeleteRequest delRequest) {
        log.debug("Processing delete request: " + delRequest);
        if (!(delRequest instanceof IndexPacketDeleteRequest))
            log.error("Invalid type of delete request for IndexPacketFolder: " + delRequest.getClass());
        IndexPacketDeleteRequest indexPacketDelRequest = (IndexPacketDeleteRequest)delRequest;
        
        Hash destHash = indexPacketDelRequest.getEmailDestHash();
        DhtStorablePacket storedPacket = retrieve(destHash);
        if (storedPacket instanceof IndexPacket) {
            IndexPacket indexPacket = (IndexPacket)storedPacket;
            Collection<Hash> keysToDelete = indexPacketDelRequest.getDhtKeys();
            
            for (Hash keyToDelete: keysToDelete) {
                // verify
                Hash verificationHash = indexPacket.getDeleteVerificationHash(keyToDelete);
                if (verificationHash == null)
                    log.debug("Email packet key " + keyToDelete + " from IndexPacketDeleteRequest not found in index packet for destination " + destHash);
                else {
                    UniqueId delAuthorization = indexPacketDelRequest.getDeleteAuthorization(keyToDelete);
                    boolean valid = Util.isDeleteAuthorizationValid(verificationHash, delAuthorization);
                    if (valid)
                        remove(indexPacket, keyToDelete, delAuthorization);
                    else
                        log.debug("Invalid delete verification hash in IndexPacketDeleteRequest. Should be: <" + verificationHash.toBase64() + ">");
                }
            }
        }
        else if (storedPacket != null)
            log.debug("IndexPacket expected for DHT key <" + destHash + ">, found " + storedPacket.getClass().getSimpleName());
    }
}
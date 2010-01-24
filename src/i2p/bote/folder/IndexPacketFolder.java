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
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.IndexPacketDeleteRequest;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.File;
import java.util.Collection;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * This class uses Email Destination hashes for DHT keys.
 * It differs from {@link DhtPacketFolder} in two ways:
 *  * It doesn't overwrite an existing packet when a new packet is stored under the same key,
 *    but merges the packets.
 *  * It retains DHT keys of deleted packets in a file named <code>DEL_<dht_key>.pkt</code>
 *    for later reference. These files use the same format as Index Packet files.
 * 
 */
public class IndexPacketFolder extends DhtPacketFolder<IndexPacket> implements PacketListener {
    private static final String DEL_FILE_PREFIX = "DEL_";
    
    private final Log log = new Log(IndexPacketFolder.class);

    public IndexPacketFolder(File storageDir) {
        super(storageDir);
    }

    @Override
    public void store(DhtStorablePacket packetToStore) {
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
        
        super.store(packetToStore);
    }

    @Override
    public void delete(Hash dhtKey) {
        throw new UnsupportedOperationException("Index packets are never deleted. Use remove(Hash, Hash) to remove an entry from an index packet file.");
    }
    
    /**
     * Deletes an entry from an {@link IndexPacket} and saves the packet to disk.
     * @param indexPacket
     * @param emailPacketKey The entry to delete
     */
    public void remove(IndexPacket indexPacket, Hash emailPacketKey) {
        log.debug("Removing DHT key " + emailPacketKey + " from Index Packet for Email Dest " + indexPacket.getDhtKey());
        UniqueId deletionKey = indexPacket.getDeletionKey(emailPacketKey);
        if (deletionKey == null)
            log.debug("DHT key " + emailPacketKey + " not found in index packet " + indexPacket);
        else {
            indexPacket.remove(emailPacketKey);
            addToDeletedPackets(indexPacket, emailPacketKey, deletionKey);
        }
        super.store(indexPacket);   // don't merge, but overwrite the file with the key removed
    }
    
    /**
     * Adds a DHT key of an Email Packet to the list of deleted packets.
     * If the key is already on the list, nothing happens.
     * @param indexPacket
     * @param dhtKey
     * @param deletionKey
     */
    private void addToDeletedPackets(IndexPacket indexPacket, Hash dhtKey, UniqueId deletionKey) {
        String delFileName = DEL_FILE_PREFIX + getFilename(indexPacket);
        File delFile = new File(storageDir, delFileName);

        // read delete list from file or create a new one if file doesn't exist
        DhtStorablePacket delListPacket;
        if (!delFile.exists())
            delListPacket = new IndexPacket(indexPacket);
        else {
            delListPacket = DhtStorablePacket.createPacket(delFile);
            if (!(delListPacket instanceof IndexPacket)) {
                log.error("Not an Index Packet file: <" + delFile + ">");
                return;
            }
        }
        ((IndexPacket)delListPacket).put(dhtKey, deletionKey);
        add(delListPacket, delFileName);
    }

    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof IndexPacketDeleteRequest) {
            IndexPacketDeleteRequest delRequest = (IndexPacketDeleteRequest)packet;
            log.debug("IndexPacketDeleteRequest received. #entries=" + delRequest.getNumEntries());
            
            Hash dhtKey = delRequest.getEmailDestHash();
            DhtStorablePacket storedPacket = retrieve(dhtKey);
            if (storedPacket instanceof IndexPacket) {
                IndexPacket indexPacket = (IndexPacket)storedPacket;
                Collection<Hash> keysToDelete = delRequest.getDhtKeys();
            
                for (Hash keyToDelete: keysToDelete) {
                    UniqueId deletionKeyFromRequest = delRequest.getDeletionKey(keyToDelete);
                    UniqueId storedDeletionKey = indexPacket.getDeletionKey(keyToDelete);
                    if (storedDeletionKey.equals(deletionKeyFromRequest))
                        remove(indexPacket, keyToDelete);
                    else
                        log.debug("Deletion key in IndexPacketDeleteRequest does not match. Should be: <" + storedDeletionKey + ">, is <" + deletionKeyFromRequest +">");
                }
            }
            else
                log.debug("IndexPacket expected for DHT key <" + dhtKey + ">, found " + storedPacket.getClass().getSimpleName());
        }
    }
}
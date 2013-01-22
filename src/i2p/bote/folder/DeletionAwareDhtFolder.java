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
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.MalformedPacketException;
import i2p.bote.packet.dht.DeleteRequest;
import i2p.bote.packet.dht.DeletionInfoPacket;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * A <code>DhtPacketFolder</code> that keeps a record of DHT keys that have been deleted,
 * along with delete authorization keys.<br/>
 * For this reason, elements of this folder are only deleted via {@link #process(DeleteRequest)},
 * not {@link #delete(Hash)}.<br/>
 * Subclasses retain DHT keys of deleted packets in a {@link DeletionInfoPacket} file whose
 * name starts with <code>DEL_</code>.
 * @param <T>
 */
public abstract class DeletionAwareDhtFolder<T extends DhtStorablePacket> extends DhtPacketFolder<T> {
    protected static final String DEL_FILE_PREFIX = "DEL_";   // file name prefix for DeletionInfoPackets to distinguish them from regular DHT packets
    
    private Log log = new Log(DeletionAwareDhtFolder.class);
    
    public DeletionAwareDhtFolder(File storageDir) {
        super(storageDir);
    }

    /**
     * Adds a DHT key to the list of deleted packets and sets the delete authorization key.
     * If the key is already on the list, nothing happens.
     * @param delFileName
     * @param dhtKey
     * @param delAuthorization
     */
    protected void addToDeletedPackets(String delFileName, Hash dhtKey, UniqueId delAuthorization) {
        DeletionInfoPacket packet = createDelInfoPacket(delFileName);
        if (packet == null) {
            log.debug("Creating a new Deletion Info Packet file: <" + delFileName + ">");
            packet = new DeletionInfoPacket();
        }
        
        packet.put(dhtKey, delAuthorization);
        add(packet, delFileName);
    }
    
    /**
     * Creates a <code>DeletionInfoPacket</code> from a file. If the file
     * does not exist, or an error occurs, <code>null</code> is returned.
     * @param delFileName
     */
    protected DeletionInfoPacket createDelInfoPacket(String delFileName) {
        File delFile = new File(storageDir, delFileName);
        try {
            DataPacket dataPacket = DataPacket.createPacket(delFile);
            if (dataPacket instanceof DeletionInfoPacket)
                return (DeletionInfoPacket)dataPacket;
            else if (dataPacket == null)
                return null;
            else {
                log.error("Not a Deletion Info Packet file: <" + delFile.getAbsolutePath() + ">");
                return null;
            }
        } catch (MalformedPacketException e) {
            log.error("Cannot read Deletion Info Packet,: <" + delFile.getAbsolutePath() + ">", e);
            return null;
        }
    }
    
    /**
     * Returns a Delete Authorization for a given DHT key, or <code>null<code>
     * if the packet has never been deleted from this folder.
     * @param dhtKey
     * @return a {@link Hash}
     */
    public abstract UniqueId getDeleteAuthorization(Hash dhtKey);
    
    /**
     * Stores a packet and returns a <code>DeleteRequest</code> if the packet, or entries in the
     * packet, have been deleted.<br/>
     * The <code>DeleteRequest</code> contains a Delete Authorization key (or keys) and can be
     * sent to the peer that is trying to store the <code>DhtStorablePacket</code>.
     * @param packetToStore
     * @return a <code>DeleteRequest</code> or <code>null</code> if the <code>DhtStorablePacket</code>
     * contains no deleted data.
     */
    public abstract DeleteRequest storeAndCreateDeleteRequest(DhtStorablePacket packetToStore);
    
    /**
     * Deletes all DHT items that match the keys in a delete request,
     * and for which the request contains a valid delete authorization.
     * @param delRequest
     */
    public abstract void process(DeleteRequest delRequest);
    
    /** Overridden to only return real DHT packets, not Deletion Info Packets. */
    @Override
    protected File[] getFilenames() {
        List<File> filteredNames = new ArrayList<File>();
        for (File file: super.getFilenames())
            if (!file.getName().startsWith(DEL_FILE_PREFIX))
                filteredNames.add(file);
        return filteredNames.toArray(new File[0]);
    }
}
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
import i2p.bote.packet.DeletionInfoPacket;
import i2p.bote.packet.DeletionRecord;
import i2p.bote.packet.MalformedDataPacketException;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * A <code>DhtPacketFolder</code> that keeps a record of DHT keys that have been deleted.
 * @param <T>
 */
public abstract class DeletionAwareFolder<T extends DhtStorablePacket> extends DhtPacketFolder<T> {
    protected static final String DEL_FILE_PREFIX = "DEL_";   // file name prefix for DeletionInfoPackets to distinguish them from regular DHT packets
    
    private Log log = new Log(DeletionAwareFolder.class);
    
    public DeletionAwareFolder(File storageDir) {
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
        DeletionInfoPacket packet = createPacket(delFileName);
        if (packet == null) {
            log.debug("Creating a new Deletion Info Packet file: <" + delFileName + ">");
            packet = new DeletionInfoPacket();
        }
        
        packet.put(dhtKey, delAuthorization);
        add(packet, delFileName);
    }
    
    /**
     * Returns the Delete Authorization for a DHT key, or <code>null</code>
     * if the key was not found.
     * @param delFileName
     * @param dhtKey
     * @return
     */
    protected UniqueId getDeleteAuthorization(String delFileName, Hash dhtKey) {
        DeletionInfoPacket packet = createPacket(delFileName);
        if (packet == null)
            return null;
        else {
            DeletionRecord entry = packet.getEntry(dhtKey);
            if (entry == null)
                return null;
            else
                return entry.delAuthorization;
        }
    }
    
    private DeletionInfoPacket createPacket(String delFileName) {
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
        } catch (MalformedDataPacketException e) {
            log.error("Cannot read Deletion Info Packet,: <" + delFile.getAbsolutePath() + ">", e);
            return null;
        }
    }
    
    /** Overridden to only return real DHT packets, not Deletion Info Packets. */
    @Override
    public File[] getFilenames() {
        List<File> filteredNames = new ArrayList<File>();
        for (File file: super.getFilenames())
            if (!file.getName().startsWith(DEL_FILE_PREFIX))
                filteredNames.add(file);
        return filteredNames.toArray(new File[0]);
    }
}
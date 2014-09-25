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

import i2p.bote.network.DhtStorageHandler;
import i2p.bote.packet.MalformedPacketException;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;

import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * This class uses dht keys for file names.
 *
 * @param <T> The type of DHT data stored in this folder
 */
public class DhtPacketFolder<T extends DhtStorablePacket> extends PacketFolder<T> implements DhtStorageHandler {
    private Log log = new Log(DhtPacketFolder.class);

    public DhtPacketFolder(File storageDir) {
        super(storageDir);
    }
    
    @Override
    public void store(DhtStorablePacket packetToStore) {
        add(packetToStore, getFilename(packetToStore.getDhtKey()));
    }
    
    protected String getFilename(Hash dhtKey) {
        return dhtKey.toBase64() + PACKET_FILE_EXTENSION;
    }
    
    @Override
    public DhtStorablePacket retrieve(Hash dhtKey) {
        File packetFile = findPacketFile(dhtKey);
        if (packetFile != null)
            try {
                return DhtStorablePacket.createPacket(packetFile);
            }
            catch (MalformedPacketException e) {
                log.error("Cannot create packet from file: <" + packetFile.getAbsolutePath() + ">", e);
                return null;
            }
        else
            return null;
    }

    /**
     * Returns the file a packet is stored in, or <code>null</code> if the file doesn't exist.
     * @param dhtKey a packet key
     */
    protected File findPacketFile(Hash dhtKey) {
        final String base64Key = dhtKey.toBase64();
        
        File[] files = storageDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return filenameMatches(name, base64Key);
            }
        });
        
        if (files.length > 1)
            log.warn("More than one packet files found for DHT key " + dhtKey);
        if (files.length > 0) {
            File file = files[0];
            return file;
        }
        return null;
    }

    protected boolean filenameMatches(String filename, String base64DhtKey) {
        return filename.startsWith(base64DhtKey);
    }
    
    public void delete(Hash dhtKey) {
        File packetFile = findPacketFile(dhtKey);
        if (packetFile != null) {
            if (!packetFile.delete())
                log.warn("File cannot be deleted: <" + packetFile.getAbsolutePath() + ">");
        }
        else
            log.debug("No file found for DHT key: " + dhtKey);
    }
    
    /** Does the same as {@link #iterator()}. */
    @Override
    public Iterator<T> individualPackets() {
        return iterator();
    }
}
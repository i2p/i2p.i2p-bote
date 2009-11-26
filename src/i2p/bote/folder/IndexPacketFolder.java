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

import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.File;

import net.i2p.util.Log;

/**
 * This class differs from {@link DhtPacketFolder} in that it doesn't overwrite an existing
 * packet when a new packet is stored under the same key, but merges the packets.
 */
public class IndexPacketFolder extends DhtPacketFolder<IndexPacket> {
    private final Log log = new Log(I2PBotePacket.class);

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
}
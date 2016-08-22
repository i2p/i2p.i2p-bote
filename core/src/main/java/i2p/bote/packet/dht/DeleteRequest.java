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

package i2p.bote.packet.dht;

import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.I2PBotePacket;

import java.util.Collection;

import net.i2p.data.Hash;

/**
 * Superclass for delete requests. A delete request contains zero or more entries,
 * each consisting of a DHT key (of the DHT item that is to be deleted) and
 * authentication data (which is defined in subclasses).
 */
public abstract class DeleteRequest extends CommunicationPacket {

    protected DeleteRequest() {
    }
    
    protected DeleteRequest(byte[] data) {
        super(data);
    }
    
    public abstract Class<? extends I2PBotePacket> getDataType();

    /** Returns all DHT keys in the <code>DeleteRequest</code>. */
    public abstract Collection<Hash> getDhtKeys();
    
    /**
     * Creates a new <code>DeleteRequest</code> containing only one of the entries in this <code>DeleteRequest</code>.
     * @param dhtKey The DHT key of the entry to use
     * @return A new <code>DeleteRequest</code>, or <code>null</code> if the DHT key doesn't exist in the packet
     */
    public abstract DeleteRequest getIndividualRequest(Hash dhtKey);
}
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
import i2p.bote.packet.TypeCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * A request for {@link i2p.bote.network.kademlia.KademliaConstants#K} peers that
 * are closest to a given key.
 */
@TypeCode('F')
public class FindClosePeersPacket extends CommunicationPacket {
    private Log log = new Log(FindClosePeersPacket.class);
    private Hash key;

    public FindClosePeersPacket(Hash key) {
        this.key = key;
    }
    
    public FindClosePeersPacket(byte[] data) {
        super(data);
        
        byte[] hashData = new byte[Hash.HASH_LENGTH];
        System.arraycopy(data, CommunicationPacket.HEADER_LENGTH, hashData, 0, hashData.length);
        key = new Hash(hashData);
        
        int remaining = data.length - (CommunicationPacket.HEADER_LENGTH+hashData.length);
        if (remaining > 0)
            log.debug("Find Close Nodes Request packet has " + remaining + " extra bytes.");
    }
    public Hash getKey() {
        return key;
    }

    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            writeHeader(outputStream);
            outputStream.write(key.toByteArray());
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        
        return outputStream.toByteArray();
    }
    
    @Override
    public String toString() {
        return super.toString() + " key=" + key.toBase64().substring(0, 8) + "...";
    }
}
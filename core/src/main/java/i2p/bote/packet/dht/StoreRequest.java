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
import i2p.bote.packet.MalformedPacketException;
import i2p.bote.packet.TypeCode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import net.i2p.data.Hash;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

/**
 * A request to a peer to store a DHT data item.
 */
@TypeCode('S')
public class StoreRequest extends CommunicationPacket {
    private Log log = new Log(StoreRequest.class);
    private HashCash hashCash;
    private DhtStorablePacket packetToStore;

    public StoreRequest(DhtStorablePacket packetToStore) {
        try {
            hashCash = HashCash.mintCash("", 1);   // TODO
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create HashCash.", e);
        }
        this.packetToStore = packetToStore;
    }
    
    public StoreRequest(byte[] data) throws NoSuchAlgorithmException, MalformedPacketException {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        int hashCashLength = buffer.getShort() & 0xFFFF;
        byte[] hashCashData = new byte[hashCashLength];
        buffer.get(hashCashData);
        hashCash = new HashCash(new String(hashCashData));
        
        int dataLength = buffer.getShort() & 0xFFFF;
        byte[] storedData = new byte[dataLength];
        buffer.get(storedData);
        packetToStore = DhtStorablePacket.createPacket(storedData);
        
        if (buffer.hasRemaining())
            log.debug("Storage Request Packet has " + buffer.remaining() + " extra bytes.");
    }

    public Hash getKey() {
        return packetToStore.getDhtKey();
    }
    
    public DhtStorablePacket getPacketToStore() {
        return packetToStore;
    }

    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteArrayStream);

        try {
            writeHeader(dataStream);
            String hashCashString = hashCash.toString();
            dataStream.writeShort(hashCashString.length());
            dataStream.write(hashCashString.getBytes());
            byte[] dataToStore = packetToStore.toByteArray();
            dataStream.writeShort(dataToStore.length);
            dataStream.write(dataToStore);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteArrayStream.toByteArray();
    }
    
    @Override
    public String toString() {
        return super.toString() + ", PayldType=" + (packetToStore==null?"<null>":packetToStore.getClass().getSimpleName());
    }
}
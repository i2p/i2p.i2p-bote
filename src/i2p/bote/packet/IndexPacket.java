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

package i2p.bote.packet;

import i2p.bote.email.EmailDestination;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * This class is not thread-safe.
 */
@TypeCode('I')
public class IndexPacket extends DhtStorablePacket {
    private Log log = new Log(IndexPacket.class);
    private Collection<Hash> dhtKeys;   // DHT keys of email packets
    private Hash destinationHash;   // The DHT key of this packet
    
    public IndexPacket(byte[] data) {
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        if (dataBuffer.get() != getPacketTypeCode())
            log.error("Wrong type code for IndexPacket. Expected <" + getPacketTypeCode() + ">, got <" + (char)data[0] + ">");
        
        destinationHash = readHash(dataBuffer);

        int numKeys = dataBuffer.get();
        
        dhtKeys = new ArrayList<Hash>();
        for (int i=0; i<numKeys; i++) {
            Hash dhtKey = readHash(dataBuffer);
            dhtKeys.add(dhtKey);
        }
        
        // TODO catch BufferUnderflowException; warn if extra bytes in the array
    }

    public IndexPacket(Collection<EncryptedEmailPacket> emailPackets, EmailDestination emailDestination) {
        dhtKeys = new ArrayList<Hash>();
        for (EncryptedEmailPacket emailPacket: emailPackets)
            dhtKeys.add(emailPacket.getDhtKey());
        
        destinationHash = emailDestination.getHash();
    }
    
    /**
     * Merges the DHT keys of multiple index packets into one big index packet.
     * The DHT key of this packet is set to the DHT key of the first input packet.
     * @param indexPackets
     * @throws IllegalArgumentException If an empty <code>Collection</code> or <code>null</code> was passed in
     */
    public IndexPacket(Collection<IndexPacket> indexPackets) {
        if (indexPackets==null || indexPackets.isEmpty())
            throw new IllegalArgumentException("This method must be invoked with at least one index packet.");
        
        destinationHash = indexPackets.iterator().next().getDhtKey();
        dhtKeys = new HashSet<Hash>();
        for (IndexPacket packet: indexPackets)
            dhtKeys.addAll(packet.getDhtKeys());
    }
    
    /**
     * A varargs version of {@link IndexPacket(Collection<IndexPacket>)}.
     * @param indexPackets
     */
    public IndexPacket(IndexPacket... indexPackets) {
        this(Arrays.asList(indexPackets));
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write((byte)getPacketTypeCode());
        try {
            destinationHash.writeBytes(outputStream);
            outputStream.write((byte)dhtKeys.size());
            for (Hash dhtKey: dhtKeys)
                dhtKey.writeBytes(outputStream);
            // TODO in the unit test, verify that toByteArray().length = Hash.NUM_BYTES + 1 + dhtKeys.size()*Hash.NUM_BYTES
        } catch (DataFormatException e) {
            log.error("Invalid format for email destination.", e);
        } catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return outputStream.toByteArray();
    }

    /**
     * Returns the DHT keys of the {@link EncryptedEmailPacket}s referenced by this {@link IndexPacket}.
     * @return
     */
    public Collection<Hash> getDhtKeys() {
        return dhtKeys;
    }
    
    /**
     * Returns the DHT key of this packet.
     */
    @Override
    public Hash getDhtKey() {
        return destinationHash;
    }
}
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
import java.nio.ByteBuffer;

import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * A query to a peer about whether that peer has deleted a given email packet from its DHT store.
 */
@TypeCode('Y')
public class DeletionQuery extends CommunicationPacket {
    private Log log = new Log(DeletionQuery.class);
    private Hash dhtKey;

    /**
     * @param dhtKey The DHT key of the email packet
     */
    public DeletionQuery(Hash dhtKey) {
        this.dhtKey = dhtKey;
    }
    
    public DeletionQuery(byte[] data) {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        dhtKey = readHash(buffer);
        
        if (buffer.hasRemaining())
            log.debug("Deletion Query has " + buffer.remaining() + " extra bytes.");
    }
    
    /**
     * Returns the DHT key of the email packet.
     */
    public Hash getDhtKey() {
        return dhtKey;
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            writeHeader(outputStream);
            outputStream.write(dhtKey.toByteArray());
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        
        return outputStream.toByteArray();
    }
}
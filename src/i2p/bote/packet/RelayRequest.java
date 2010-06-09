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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

/**
 * A <code>RelayRequest</code> contains a {@link RelayDataPacket} or a {@link DhtStorablePacket}.
 */
@TypeCode('Y')
public class RelayRequest extends CommunicationPacket {
    private Log log = new Log(RelayRequest.class);
    private HashCash hashCash;
    private DataPacket payload;

    public RelayRequest(DataPacket payload) {
        try {
            hashCash = HashCash.mintCash("", 1);   // TODO
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create HashCash.", e);
        }
        this.payload = payload;
    }
    
    public RelayRequest(byte[] data) throws MalformedDataPacketException {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        int hashCashLength = buffer.getShort();
        byte[] hashCashData = new byte[hashCashLength];
        buffer.get(hashCashData);
        try {
            hashCash = new HashCash(new String(hashCashData));
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create HashCash.", e);
        }
        
        int payloadLength = buffer.getShort();
        byte[] payloadData = new byte[payloadLength];
        buffer.get(payloadData);
        payload = DataPacket.createPacket(payloadData);
        
        if (buffer.hasRemaining())
            log.debug("Storage Request Packet has " + buffer.remaining() + " extra bytes.");
    }

    public HashCash getHashCash() {
        return hashCash;
    }

    /**
     * Returns the payload packet, i.e. the data that is being relayed.
     * @return
     */
    public DataPacket getStoredPacket() {
        return payload;
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
            
            byte[] payloadBytes = payload.toByteArray();
            dataStream.writeShort(payloadBytes.length);
            dataStream.write(payloadBytes);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteArrayStream.toByteArray();
    }
}
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

import i2p.bote.UniqueId;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.xml.crypto.Data;

import net.i2p.util.Log;

@TypeCode('N')
public class ResponsePacket extends CommunicationPacket {
    private Log log = new Log(ResponsePacket.class);
    private StatusCode statusCode;
    private DataPacket payload;

    public ResponsePacket(DataPacket payload, StatusCode statusCode, UniqueId packetId) {
        super(packetId);
        this.payload = payload;
        this.statusCode = statusCode;
    }
    
    public ResponsePacket(byte[] data) {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);

        statusCode = StatusCode.values()[buffer.get()];

        int payloadLength = buffer.getShort();
        if (payloadLength > 0) {
            byte[] payloadData = new byte[payloadLength];
            buffer.get(payloadData);
            payload = DataPacket.createPacket(payloadData);
        }
        
        if (buffer.hasRemaining())
            log.debug("Response Packet has " + buffer.remaining() + " extra bytes.");
    }

    public DataPacket getPayload() {
        return payload;
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        
        try {
            writeHeader(dataStream);
            dataStream.write(statusCode.ordinal());
            
            byte[] payloadBytes = payload.toByteArray();
            dataStream.writeShort(payloadBytes.length);
            dataStream.write(payloadBytes);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        
        return byteStream.toByteArray();
    }
    
    @Override
    public String toString() {
        return super.toString() + ", status=" + statusCode + ", ploadType=" + payload.getClass().getSimpleName();
    }
}
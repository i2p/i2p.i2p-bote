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

import i2p.bote.Util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.Log;

@TypeCode('L')
public class RelayPacket extends DataPacket {
    public static final int XOR_KEY_LENGTH = 32;   // length of the XOR key in bytes
    
    private Log log = new Log(RelayPacket.class);
    private long earliestSendTime;
    private long latestSendTime;
    private byte[] xorKey;
    private Destination nextDestination;   // an I2P node to send the packet to
    private byte[] payload;   // can contain another Relay Packet, Email Packet, or Retrieve Request

    public RelayPacket(Destination nextDestination, long earliestSendTime, long latestSendTime) {
        this.nextDestination = nextDestination;
        this.earliestSendTime = earliestSendTime;
        this.latestSendTime = latestSendTime;
    }

    public RelayPacket(DataPacket dataPacket, Destination nextDestination, long earliestSendTime, long latestSendTime, byte[] xorKey) throws DataFormatException {
        // TODO
    }
    
    public RelayPacket(InputStream inputStream) throws IOException, DataFormatException {
        this(Util.readInputStream(inputStream));
    }
    
    public RelayPacket(byte[] data) throws DataFormatException {
        super(data);
        
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        earliestSendTime = buffer.getInt();
        latestSendTime = buffer.getInt();
        
        xorKey = new byte[XOR_KEY_LENGTH];
        buffer.get(xorKey);
        
        nextDestination = new Destination();
        byte[] destinationData = new byte[384];
        buffer.get(destinationData);
        nextDestination.readBytes(destinationData, 0);
        
        int payloadLength = buffer.getShort();
        payload = new byte[payloadLength];
        buffer.get(payload);
        
        if (buffer.hasRemaining())
            log.debug("Relay Packet has " + buffer.remaining() + " extra bytes.");
    }
    
    public Destination getNextDestination() {
        return nextDestination;
    }

    public long getEarliestSendTime() {
        return earliestSendTime;
    }

    public long getLatestSendTime() {
        return latestSendTime;
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(arrayOutputStream);
 
        try {
            writeHeader(dataStream);
            dataStream.writeInt((int)earliestSendTime);
            dataStream.writeInt((int)latestSendTime);
            dataStream.write(xorKey);
            dataStream.write(nextDestination.toByteArray());
            dataStream.writeShort(payload.length);
            dataStream.write(payload);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        
        return arrayOutputStream.toByteArray();
    }
}
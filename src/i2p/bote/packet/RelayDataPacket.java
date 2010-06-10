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
import i2p.bote.service.RelayPeerManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.Log;

/**
 * A <code>RelayDataPacket</code> contains a {@link RelayRequest},
 * an I2P destination to send it to, and a time window for the send time.
 */
@TypeCode('L')
public class RelayDataPacket extends DataPacket {
    private Log log = new Log(RelayDataPacket.class);
    private long minDelay;
    private long maxDelay;
    private long sendTime;
    private Destination nextDestination;
    private RelayRequest request;

    /**
     * 
     * @param nextDestination The I2P destination to send the packet to
     * @param minDelayMilliseconds In milliseconds
     * @param maxDelayMilliseconds In milliseconds
     * @param request
     */
    public RelayDataPacket(Destination nextDestination, long minDelayMilliseconds, long maxDelayMilliseconds, RelayRequest request) {
        this.nextDestination = nextDestination;
        this.minDelay = minDelayMilliseconds;
        this.maxDelay = maxDelayMilliseconds;
        this.request = request;
    }

    public RelayDataPacket(byte[] data) throws DataFormatException, MalformedDataPacketException {
        super(data);
        
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        minDelay = buffer.getInt() * 1000L;
        maxDelay = buffer.getInt() * 1000L;
        nextDestination = Util.createDestination(buffer);
        
        int requestDataLength = buffer.getShort();
        byte[] requestBytes = new byte[requestDataLength];
        buffer.get(requestBytes);
        request = new RelayRequest(requestBytes);
        
        if (buffer.hasRemaining())
            log.debug("Relay Packet has " + buffer.remaining() + " extra bytes.");
    }
    
    /**
     * Creates a <code>RelayDataPacket</code> containing <code>numHops</code> nested
     * <code>RelayRequest</code>s, each containing a <code>RelayDataPacket</code>.
     * Returns <code>null</code> if <code>numHops</code> is <code>0</code>.
     * 
     * If <code>numHops = 1</code>, the finished packet looks as follows (outermost to innermost):
     * 
     * Unencrypted RelayDataPacket
     * RelayRequest
     * Encrypted DataPacket
     * 
     * For <code>numHops = 2</code>:
     * 
     * Unencrypted RelayDataPacket
     * RelayRequest
     * Encrypted RelayDataPacket
     * RelayRequest
     * Encrypted DataPacket
     * 
     * Each additional hop adds an encrypted <code>RelayDataPacket</code> and a <code>RelayRequest</code>.
     * @param payload
     * @param peerManager
     * @param numHops
     * @param minDelayMilliseconds
     * @param maxDelayMilliseconds
     * @return
     */
    public static RelayDataPacket create(DataPacket payload, RelayPeerManager peerManager, int numHops, long minDelayMilliseconds, long maxDelayMilliseconds) {
        List<Destination> relayPeers = peerManager.getRandomPeers(numHops);
        
        Log log = new Log(RelayDataPacket.class);
        if (log.shouldLog(Log.DEBUG)) {
            StringBuilder debugMsg = new StringBuilder("Creating relay chain: [");
            for (int i=relayPeers.size()-1; i>=0; i--) {
                debugMsg.append(relayPeers.get(i).calculateHash().toBase64().substring(0, 8));
                if (i > 0)
                    debugMsg.append("... --> ");
            }
            debugMsg.append("...]");
            log.debug(debugMsg.toString());
        }
        
        DataPacket dataPacket = payload;
        for (Iterator<Destination> iterator=relayPeers.iterator(); iterator.hasNext();) {
            Destination relayPeer = iterator.next();
            RelayRequest request = new RelayRequest(dataPacket, relayPeer);
            dataPacket = new RelayDataPacket(relayPeer, minDelayMilliseconds, maxDelayMilliseconds, request);
        }
        return (RelayDataPacket)dataPacket;
    }
    
    public Destination getNextDestination() {
        return nextDestination;
    }

    /** Returns the minimum delay time for this packet in milliseconds */
    public long getMinimumDelay() {
        return minDelay;
    }

    /** Returns the maximum delay time for this packet in milliseconds */
    public long getMaximumDelay() {
        return maxDelay;
    }
    
    /**
     * @param The time the packet is scheduled for sending, in milliseconds since 1-1-1970
     */
    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }
    
    /**
     * @return The time the packet is scheduled for sending, in milliseconds since 1-1-1970
     */
    public long getSendTime() {
        return sendTime;
    }

    /**
     * Returns the <code>RelayRequest</code> this packet contains.
     * @return
     */
    public RelayRequest getRequest() {
        return request;
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(arrayOutputStream);
 
        try {
            writeHeader(dataStream);
            dataStream.writeInt((int)(minDelay/1000));
            dataStream.writeInt((int)(maxDelay/1000));
            // write the first 384 bytes (the two public keys)
            dataStream.write(nextDestination.toByteArray(), 0, 384);
            byte[] requestBytes = request.toByteArray();
            dataStream.writeShort(requestBytes.length);
            dataStream.write(requestBytes);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        
        return arrayOutputStream.toByteArray();
    }
}
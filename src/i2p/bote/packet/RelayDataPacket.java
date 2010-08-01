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
import java.util.Random;

import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.Log;

/**
 * A <code>RelayDataPacket</code> contains a {@link RelayRequest},
 * an I2P destination to send it to, and a delay before sending.
 */
@TypeCode('R')
public class RelayDataPacket extends DataPacket {
    private static Random random = new Random();
    
    private Log log = new Log(RelayDataPacket.class);
    private long delay;   // in milliseconds
    private long sendTime;
    private Destination nextDestination;
    private RelayRequest request;

    /**
     * @param nextDestination The I2P destination to send the packet to
     * @param delayMilliseconds The amount of time to wait before sending the packet
     * @param request
     */
    public RelayDataPacket(Destination nextDestination, long delayMilliseconds, RelayRequest request) {
        this.nextDestination = nextDestination;
        this.delay = delayMilliseconds;
        this.request = request;
        random = new Random();
    }

    public RelayDataPacket(byte[] data) throws DataFormatException, MalformedDataPacketException {
        super(data);
        
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        delay = buffer.getInt() * 1000L;
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
     * <code>RelayRequest</code>s, each containing a <code>RelayDataPacket</code>.<br/>
     * Returns <code>null</code> if <code>numHops</code> is <code>0</code>.
     * <p/>
     * If <code>numHops = 1</code>, the finished packet looks as follows (outermost to innermost):<br/>
     * <ul>
     *   <li/>Unencrypted RelayDataPacket<br/>
     *   <li/>RelayRequest<br/>
     *   <li/>Encrypted DataPacket
     * </ul>
     * <p/>
     * For <code>numHops = 2</code>:<br/>
     * <ul>
     *   <li/>Unencrypted RelayDataPacket<br/>
     *   <li/>RelayRequest<br/>
     *   <li/>Encrypted RelayDataPacket<br/>
     *   <li/>RelayRequest<br/>
     *   <li/>Encrypted DataPacket
     * </ul>
     * <p/>
     * For each additional hop, an encrypted <code>RelayDataPacket</code> and a <code>RelayRequest</code> is added.
     * @param payload
     * @param peerManager
     * @param numHops
     * @param minDelay The minimum delay in milliseconds
     * @param maxDelay The maximum delay in milliseconds
     */
    public static RelayDataPacket create(DataPacket payload, RelayPeerManager peerManager, int numHops, long minDelay, long maxDelay) {
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
            // generate a random time between minDelay and maxDelay in the future
            long delay;
            if (minDelay == maxDelay)
                delay = minDelay;
            else
                delay = minDelay + Math.abs(random.nextLong()) % Math.abs(maxDelay-minDelay);

            Destination relayPeer = iterator.next();
            RelayRequest request = new RelayRequest(dataPacket, relayPeer);
            dataPacket = new RelayDataPacket(relayPeer, delay, request);
        }
        return (RelayDataPacket)dataPacket;
    }
    
    public Destination getNextDestination() {
        return nextDestination;
    }

    /** Returns the delay time for this packet in milliseconds */
    public long getDelay() {
        return delay;
    }

    /**
     * @param sendTime The time the packet is scheduled for sending, in milliseconds since <code>1-1-1970</code>
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
            dataStream.writeInt((int)(delay/1000));
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
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

package i2p.bote.packet.relay;

import i2p.bote.Util;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.MalformedPacketException;
import i2p.bote.packet.TypeCode;
import i2p.bote.service.RelayPeerManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

import net.i2p.client.I2PSession;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.PrivateKey;
import net.i2p.data.PublicKey;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

/**
 * A <code>RelayRequest</code> tells the receiver to communicate with a peer, or peers,
 * on behalf of the sender.<br/>
 * It contains an encrypted {@link CommunicationPacket}, a {@link RelayRequest}, an I2P
 * destination to send it to, and a delay before sending and a {@link ReturnChain}
 * which can be empty.
 */
@TypeCode('R')
public class RelayRequest extends CommunicationPacket {
    private static Random random = new Random();
    
    private Log log = new Log(RelayRequest.class);
    private HashCash hashCash;
    private long delayMilliseconds;
    private long sendTime;
    private Destination nextDestination;
    private ReturnChain returnChain;
    private byte[] payload;   // an encrypted CommunicationPacket
    private int padBytes;

    /**
     * Creates a <code>RelayRequest</code> that contains an encrypted <code>CommunicationPacket</code>.
     * @param payload
     * @param nextDestination
     * @param delayMilliseconds The amount of time to wait before sending the packet
     * @param padBytes The number of zeros to add at the end of the packet
     */
    public RelayRequest(CommunicationPacket payload, Destination nextDestination, long delayMilliseconds, int padBytes) {
        try {
            hashCash = HashCash.mintCash("", 1);   // TODO
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create HashCash.", e);
        }
        this.nextDestination = nextDestination;
        this.delayMilliseconds = delayMilliseconds;
        returnChain = new ReturnChain();
        this.payload = encrypt(payload, nextDestination);
        this.padBytes = padBytes;
    }
    
    public RelayRequest(CommunicationPacket payload, Destination nextDestination, long delayMilliseconds, ReturnChain returnChain) {
        try {
            hashCash = HashCash.mintCash("", 1);   // TODO
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create HashCash.", e);
        }
        this.nextDestination = nextDestination;
        this.delayMilliseconds = delayMilliseconds;
        this.returnChain = returnChain;
        this.payload = encrypt(payload, nextDestination);
    }
    
    public RelayRequest(byte[] data) throws DataFormatException {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        int hashCashLength = buffer.getShort() & 0xFFFF;
        byte[] hashCashData = new byte[hashCashLength];
        buffer.get(hashCashData);
        try {
            hashCash = new HashCash(new String(hashCashData));
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create HashCash.", e);
        }
        
        delayMilliseconds = buffer.getInt() * 1000L;
        nextDestination = Util.createDestination(buffer);
        
        returnChain = new ReturnChain(buffer);
        
        int payloadLength = buffer.getShort() & 0xFFFF;
        payload = new byte[payloadLength];
        buffer.get(payload);
        
        // read padding (the actual data doesn't matter, only the length)
        padBytes = buffer.remaining();
    }

    /**
     * Creates a <code>RelayRequest</code> containing <code>numHops</code> nested
     * <code>RelayRequest</code>s<br/>
     * Returns <code>null</code> if <code>numHops</code> is <code>0</code>.
     * @param payload
     * @param peerManager For obtaining relay peers
     * @param numHops
     * @param minDelay The minimum delay in milliseconds
     * @param maxDelay The maximum delay in milliseconds
     */
    public static RelayRequest create(CommunicationPacket payload, RelayPeerManager peerManager, int numHops, long minDelay, long maxDelay) {
        List<Destination> relayPeers = peerManager.getRandomPeers(numHops);
        
        Log log = new Log(RelayRequest.class);
        if (log.shouldLog(Log.DEBUG)) {
            StringBuilder debugMsg = new StringBuilder("Creating relay chain: [");
            for (int i=relayPeers.size()-1; i>=0; i--) {
                debugMsg.append(Util.toShortenedBase32(relayPeers.get(i)));
                if (i > 0)
                    debugMsg.append(" --> ");
            }
            debugMsg.append("]");
            log.debug(debugMsg.toString());
        }
        
        // calculate the number of pad bytes necessary to pad the payload to the maximum size possible
        int maxSize = I2PBotePacket.MAX_DATAGRAM_SIZE - getMaxOverhead(numHops);
        int padBytes = maxSize - payload.getSize();
        if (padBytes < 0)
            padBytes = 0;
        
        CommunicationPacket request = payload;
        for (Destination relayPeer: relayPeers) {
            // generate a random time between minDelay and maxDelay in the future
            long delay;
            if (minDelay == maxDelay)
                delay = minDelay;
            else
                delay = minDelay + Math.abs(random.nextLong()) % Math.abs(maxDelay-minDelay);

            request = new RelayRequest(request, relayPeer, delay, padBytes);
            
            // only pad the innermost packet (the payload)
            padBytes = 0;
        }
        return (RelayRequest)request;
    }
    
    public static int getMaxOverhead(int numHops) {
        if (numHops <= 0)
            return 0;
        else
            return 1049 + (numHops-1)*1040;
    }
    
    public Destination getNextDestination() {
        return nextDestination;
    }

    /** Returns the delay time for this packet in milliseconds */
    public long getDelay() {
        return delayMilliseconds;
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

    public HashCash getHashCash() {
        return hashCash;
    }

    public byte[] getPayload() {
        return payload;
    }
    
    /**
     * Returns the payload packet, i.e. the data that is being relayed.
     * @param i2pSession An <code>I2PSession</code> that contains the private key necessary to decrypt the payload
     * @throws MalformedPacketException 
     * @throws DataFormatException 
     */
    public CommunicationPacket getStoredPacket(I2PSession i2pSession) throws DataFormatException, MalformedPacketException {
        return decrypt(i2pSession);
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
            
            dataStream.writeInt((int)(delayMilliseconds/1000));
            // write the first 384 bytes (the two public keys)
            // TODO This is NOT compatible with newer key types!
            dataStream.write(nextDestination.toByteArray(), 0, 384);
            
            returnChain.writeTo(dataStream);
            
            dataStream.writeShort(payload.length);
            dataStream.write(payload);
            
            byte[] padding = new byte[padBytes];
            dataStream.write(padding);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteArrayStream.toByteArray();
    }
    
    private byte[] encrypt(CommunicationPacket packet, Destination destination) {
        PublicKey publicKey = destination.getPublicKey();
        byte[] data = packet.toByteArray();
        return Util.encrypt(data, publicKey);
    }

    /**
     * Decrypts the <code>CommunicationPacket</code> inside this packet.
     * @throws DataFormatException 
     * @throws MalformedPacketException 
     */
    private CommunicationPacket decrypt(I2PSession i2pSession) throws DataFormatException, MalformedPacketException {
        PrivateKey privateKey = i2pSession.getDecryptionKey();
        byte[] decryptedData = Util.decrypt(payload, privateKey);
        return CommunicationPacket.createPacket(decryptedData);
    }
}
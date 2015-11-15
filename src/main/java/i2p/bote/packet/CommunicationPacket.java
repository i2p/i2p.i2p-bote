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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import net.i2p.util.Log;

/**
 * This is the abstract superclass for all packets that are sent between two
 * I2P-Bote nodes.<br/>
 * Some types of <code>CommunicationPacket</code>s contain a data packet.<br/>
 * The packet Id is only used by some subclasses.
 */
public abstract class CommunicationPacket extends I2PBotePacket {
    private static final byte[] PACKET_PREFIX = new byte[] {(byte)0x6D, (byte)0x30, (byte)0x52, (byte)0xE9};
    protected static final int HEADER_LENGTH = PACKET_PREFIX.length + 2 + UniqueId.LENGTH;   // length of the common packet header in the byte array representation; this is where subclasses start reading
    
    private Log log = new Log(CommunicationPacket.class);
    private UniqueId packetId;
    private CountDownLatch sentSignal;
    private long sentTime;

    protected CommunicationPacket() {
        this(new UniqueId());
    }
    
    protected CommunicationPacket(UniqueId packetId) {
        this.packetId = packetId;
        sentSignal = new CountDownLatch(1);
        sentTime = -1;
    }
    
    /**
     * Creates a <code>CommunicationPacket</code> from raw datagram data. The packet ID
     * is initialized from the input data.<br/>
     * Subclasses start reading at byte {@link #HEADER_LENGTH} after calling this constructor.
     * @param data
     */
    protected CommunicationPacket(byte[] data) {
        super(data[5]);   // byte 5 is the protocol version in a communication packet
        if (!isPrefixValid(data)) {
            byte[] prefix = Arrays.copyOf(data, PACKET_PREFIX.length);
            log.error("Packet prefix invalid. Expected: " + Arrays.toString(PACKET_PREFIX) + ", actual: " + Arrays.toString(prefix));
        }

        checkPacketType(data[4]);
        packetId = new UniqueId(data, 6);
        sentSignal = new CountDownLatch(1);
        sentTime = -1;
    }
    
    /**
     * Creates a packet object from its byte array representation. If there is an error,
     * <code>null</code> is returned.
     * @param data
     * @throws MalformedPacketException
     */
    public static CommunicationPacket createPacket(byte[] data) throws MalformedPacketException {
        if (data == null) {
            Log log = new Log(CommunicationPacket.class);
            log.error("Packet data is null");
            return null;
        }
        if (data.length < HEADER_LENGTH) {
            Log log = new Log(CommunicationPacket.class);
            log.error("Packet is too short to be a CommunicationPacket");
            return null;
        }

        char packetTypeCode = (char)data[4];   // byte 4 of a communication packet is the packet type code
        Class<? extends I2PBotePacket> packetType = decodePacketTypeCode(packetTypeCode);
        if (packetType==null || !CommunicationPacket.class.isAssignableFrom(packetType)) {
            Log log = new Log(CommunicationPacket.class);
            log.error("Type code is not a CommunicationPacket type code: <" + packetTypeCode + ">");
            return null;
        }
        
        Class<? extends CommunicationPacket> commPacketType = packetType.asSubclass(CommunicationPacket.class);
        try {
            return commPacketType.getConstructor(byte[].class).newInstance(data);
        }
        catch (Exception e) {
            if (e instanceof MalformedPacketException)
                throw (MalformedPacketException)e;
            else
                throw new MalformedPacketException("Can't instantiate packet for type code <" + packetTypeCode + ">", e);
        }
    }
    
    /**
     * Writes the Prefix, Version, Type, and Packet Id fields of a Communication Packet to
     * an {@link OutputStream}.
     * @param outputStream
     */
    protected void writeHeader(OutputStream outputStream) throws IOException {
        outputStream.write(PACKET_PREFIX);
        outputStream.write((byte)getPacketTypeCode());
        outputStream.write(getProtocolVersion());
        outputStream.write(packetId.toByteArray());
    }
    
    /**
     * Returns <code>true</code> if the packet has the correct packet prefix; <code>false</code> otherwise.
     * @param packet
     */
    static boolean isPrefixValid(byte[] packet) {
        if (packet == null || packet.length < PACKET_PREFIX.length)
            return false;
        for (int i=0; i<PACKET_PREFIX.length; i++)
            if (packet[i] != PACKET_PREFIX[i])
                return false;
        return true;
    }

    public void setPacketId(UniqueId packetId) {
        this.packetId = packetId;
    }
    
    public UniqueId getPacketId() {
        return packetId;
    }
    
    public synchronized void setSentTime(long sentTime) {
        this.sentTime = sentTime;
        sentSignal.countDown();
    }

    public synchronized long getSentTime() {
        return sentTime;
    }

    @Override
    public String toString() {
        return "Type=" + getClass().getSimpleName() + ", Id=" + (packetId==null?"<null>":packetId.toString().substring(0, 8)) + "...";
    }
}
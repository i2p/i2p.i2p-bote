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

import i2p.bote.I2PBote;
import i2p.bote.UniqueId;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.util.Log;

public abstract class CommunicationPacket extends I2PBotePacket {
    protected static final int HEADER_LENGTH = 6 + UniqueId.LENGTH;   // length of the common packet header in the byte array representation; this is where subclasses start reading
    private static final byte[] PACKET_PREFIX = new byte[] {(byte)0x6D, (byte)0x30, (byte)0x52, (byte)0xE9};
    private static Log static_log = new Log(CommunicationPacket.class);
    
    private Log log = new Log(CommunicationPacket.class);
    private UniqueId packetId;
    private CountDownLatch sentSignal;
    private long sentTime;

    protected CommunicationPacket() {
        this(new UniqueId());
    }
    
    protected CommunicationPacket(UniqueId packetId) {
        super(I2PBote.PROTOCOL_VERSION);
        this.packetId = packetId;
        sentSignal = new CountDownLatch(1);
        sentTime = -1;
    }
    
    /**
     * Creates a packet and initializes the header fields shared by all Communication Packets:
     * packet type, protocol version, and packet id.
     * Subclasses should start reading at byte <code>HEADER_LENGTH</code> after calling this constructor.
     * @param data
     */
    protected CommunicationPacket(byte[] data) {
        super(data[5]);   // byte 5 is the protocol version in a communication packet
        verifyHeader(data);
        checkPacketType(data[4]);
        packetId = new UniqueId(data, 6);
    }
    
    /**
     * Creates a packet object from its byte array representation. If there is an error,
     * <code>null</code> is returned.
     * @param data
     * @param log
     * @return
     * @throws MalformedCommunicationPacketException
     */
    public static CommunicationPacket createPacket(byte[] data) throws MalformedCommunicationPacketException {
        char packetTypeCode = (char)data[4];   // byte 4 of a communication packet is the packet type code
        Class<? extends I2PBotePacket> packetType = decodePacketTypeCode(packetTypeCode);
        if (packetType==null || !CommunicationPacket.class.isAssignableFrom(packetType)) {
            static_log.error("Type code is not a CommunicationPacket type code: <" + packetTypeCode + ">");
            return null;
        }
        
        Class<? extends CommunicationPacket> commPacketType = packetType.asSubclass(CommunicationPacket.class);
        try {
            return commPacketType.getConstructor(byte[].class).newInstance(data);
        }
        catch (Exception e) {
            if (e instanceof MalformedCommunicationPacketException)
                throw (MalformedCommunicationPacketException)e;
            else
                throw new MalformedCommunicationPacketException("Can't instantiate packet for type code <" + packetTypeCode + ">", e);
        }
    }
    
    /**
     * Checks that the packet has the correct packet prefix.
     * @param packet
     */
    private void verifyHeader(byte[] packet) {
        for (int i=0; i<PACKET_PREFIX.length; i++)
            if (packet[i] != PACKET_PREFIX[i])
                log.error("Packet prefix invalid at byte " + i + ". Expected = " + PACKET_PREFIX[i] + ", actual = " + packet[i]);
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

    public boolean hasBeenSent() {
        return sentTime > 0;
    }
    
    public boolean awaitSending(long timeout, TimeUnit unit) throws InterruptedException {
        return sentSignal.await(timeout, unit);
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
    
    @Override
    public String toString() {
        return "Type=" + getClass().getSimpleName() + ", Id=" + (packetId==null?"<null>":packetId.toString().substring(0, 8)) + "...";
    }
}
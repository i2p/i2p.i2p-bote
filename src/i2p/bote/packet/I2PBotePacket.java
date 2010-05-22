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
import i2p.bote.packet.dht.FindClosePeersPacket;
import i2p.bote.packet.dht.RetrieveRequest;
import i2p.bote.packet.dht.StoreRequest;

import java.nio.ByteBuffer;

import net.i2p.data.Hash;
import net.i2p.util.Log;

public abstract class I2PBotePacket {
    private static final int MAX_DATAGRAM_SIZE = 31 * 1024;
    private static final Log log = new Log(I2PBotePacket.class);
    @SuppressWarnings("unchecked")
    private static Class<? extends I2PBotePacket>[] ALL_PACKET_TYPES = new Class[] {
        RelayPacket.class, ResponsePacket.class, RetrieveRequest.class, StoreRequest.class, FindClosePeersPacket.class,
        PeerList.class, EncryptedEmailPacket.class, UnencryptedEmailPacket.class, IndexPacket.class,
        EmailPacketDeleteRequest.class, IndexPacketDeleteRequest.class
    };
    
    private int protocolVersion;
    
    /**
     * Creates a new <code>I2PBotePacket</code> with the current protocol version.
     */
    protected I2PBotePacket() {
        protocolVersion = I2PBote.PROTOCOL_VERSION;
    }
    
    protected I2PBotePacket(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
	public abstract byte[] toByteArray();
	
	/**
	 * Returns the size of the packet in bytes.
	 * @return
	 */
	// TODO rename to getPacketSize
	// TODO override in subclasses to avoid calling toByteArray by adding field lengths
	public int getSize() {
	    return toByteArray().length;
	}
    
    /**
     * Returns <code>false</code> if this packet can't fit into an I2P datagram.
     * @return
     */
    public boolean isTooBig() {
        return getSize() > MAX_DATAGRAM_SIZE;
    }
    
    protected char getPacketTypeCode(Class<? extends I2PBotePacket> dataType) {
        return dataType.getAnnotation(TypeCode.class).value();
    }
	
    public char getPacketTypeCode() {
        return getPacketTypeCode(getClass());
    }

	/**
	 * Logs an error if the packet type of the packet instance is not correct
	 * @param packetTypeCode
	 */
	protected void checkPacketType(char packetTypeCode) {
	    if (getPacketTypeCode() != packetTypeCode)
	        log.error("Packet type code of class " + getClass().getSimpleName() + " should be " + getPacketTypeCode() + ", is <" + packetTypeCode + ">");
	}
	
    protected void checkPacketType(byte packetTypeCode) {
        checkPacketType((char)packetTypeCode);
    }

    /**
     * Returns the version of the I2P-Bote network protocol used by this packet.
     * @return
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }
    
    /**
     * Creates a {@link Hash} from bytes read from a {@link ByteBuffer}.
     * No check is done to make sure the buffer has enough bytes available.
     */
    protected Hash readHash(ByteBuffer buffer) {
        byte[] bytes = new byte[Hash.HASH_LENGTH];
        buffer.get(bytes);
        return new Hash(bytes);
    }
	
    protected static Class<? extends I2PBotePacket> decodePacketTypeCode(char packetTypeCode) {
        for (Class<? extends I2PBotePacket> packetType: ALL_PACKET_TYPES)
            if (packetType.getAnnotation(TypeCode.class).value() == packetTypeCode)
                return packetType;
        
        log.debug("Invalid type code for I2PBotePacket: <" + packetTypeCode + ">");
        return null;
    }

    @Override
    public String toString() {
    	return "Type=" + getClass().getSimpleName() + ", code=<" + getPacketTypeCode() + ">, sizeBytes=" + getSize();
    }
}
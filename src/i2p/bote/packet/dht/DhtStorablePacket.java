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

import i2p.bote.packet.DataPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.MalformedPacketException;

import java.io.File;

import net.i2p.data.Hash;
import net.i2p.util.Log;

public abstract class DhtStorablePacket extends DataPacket {
    private static Log log = new Log(DhtStorablePacket.class);

    protected DhtStorablePacket() {
    }

    /**
     * @see i2p.bote.packet.DataPacket#DataPacket(byte[])
     */
    protected DhtStorablePacket(byte[] data) {
        super(data);
    }

    public abstract Hash getDhtKey();

    /**
     * Creates a {@link DhtStorablePacket} object from its byte array representation.
     * The type of packet depends on the packet type field in the byte array.
     * If there is an error, <code>null</code> is returned.
     * @param data
     * @throws MalformedPacketException
     */
    public static DhtStorablePacket createPacket(byte[] data) throws MalformedPacketException {
        DataPacket packet = DataPacket.createPacket(data);
        if (packet instanceof DhtStorablePacket)
            return (DhtStorablePacket)packet;
        else {
            log.error("Packet is not a DhtStorablePacket: " + packet);
            return null;
        }
    }

    public static Class<? extends DhtStorablePacket> decodePacketTypeCode(char packetTypeCode) {
        Class<? extends I2PBotePacket> packetType = I2PBotePacket.decodePacketTypeCode(packetTypeCode);
        if (packetType!=null && DhtStorablePacket.class.isAssignableFrom(packetType))
            return packetType.asSubclass(DhtStorablePacket.class);
        else {
            log.debug("Invalid type code for DhtStorablePacket: <" + packetTypeCode + ">");
            return null;
        }
    }
    
    /**
     * Loads a <code>DhtStorablePacket</code> from a file.<br/>
     * Returns <code>null</code> if the file doesn't exist, or if
     * an error occurred.
     * @param file
     * @throws MalformedPacketException
     */
    public static DhtStorablePacket createPacket(File file) throws MalformedPacketException {
        if (file==null || !file.exists())
            return null;
        
        DataPacket dataPacket;
        dataPacket = DataPacket.createPacket(file);
        if (dataPacket instanceof DhtStorablePacket)
            return (DhtStorablePacket)dataPacket;
        else {
            log.warn("Expected: DhtStorablePacket, got: " + (dataPacket==null?"<null>":dataPacket.getClass().getSimpleName()));
            return null;
        }
    }
}
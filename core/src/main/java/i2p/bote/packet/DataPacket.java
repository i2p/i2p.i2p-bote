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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.i2p.util.Log;

/**
 * The superclass of all "payload" packet types.
 */
public abstract class DataPacket extends I2PBotePacket {
    protected static final int HEADER_LENGTH = 2;   // length of the common packet header in the byte array representation; this is where subclasses start reading
    private static Log log = new Log(DataPacket.class);

    public DataPacket() {
    }

    /**
     * Creates an <code>DataPacket</code> from raw datagram data. The only thing that is initialized
     * is the protocol version. The packet type code is verified.<br/>
     * Subclasses start reading at byte <code>HEADER_LENGTH</code> after calling this constructor.
     * @param data
     */
    protected DataPacket(byte[] data) {
        super(data[1]);   // byte 1 is the protocol version in a data packet
        checkPacketType(data[0]);
        if (data[0] != getPacketTypeCode())
            log.error("Wrong type code for " + getClass().getSimpleName() + ". Expected <" + getPacketTypeCode() + ">, got <" + (char)data[0] + ">");
    }
    
    /**
     * Creates a {@link #DataPacket} object from a file, using the same format as the
     * {@link #createPacket(byte[])} method.
     * @param file
     * @throws MalformedPacketException
     */
    public static DataPacket createPacket(File file) throws MalformedPacketException {
        if (file==null || !file.exists())
            return null;
        
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            DataPacket packet = createPacket(Util.readBytes(inputStream));
            return packet;
        }
        catch (IOException e) {
            throw new MalformedPacketException("Can't read packet file: " + file.getAbsolutePath(), e);
        }
        finally {
            if (inputStream != null)
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    log.error("Can't close stream.", e);
                }
        }
    }
    
    /**
     * Creates a {@link DataPacket} object from its byte array representation.<br/>
     * @param data
     * @throws MalformedPacketException If the byte array does not contain a valid <code>DataPacket</code>.
     */
    public static DataPacket createPacket(byte[] data) throws MalformedPacketException {
        if (data == null)
            throw new MalformedPacketException("Packet data is null");
        if (data.length < HEADER_LENGTH)
            throw new MalformedPacketException("Packet is too short to be a DataPacket");

        char packetTypeCode = (char)data[0];   // first byte of a data packet is the packet type code
        Class<? extends I2PBotePacket> packetType = decodePacketTypeCode(packetTypeCode);
        if (packetType==null || !DataPacket.class.isAssignableFrom(packetType))
            throw new MalformedPacketException("Type code is not a DataPacket type code: <" + packetTypeCode + ">");
        
        Class<? extends DataPacket> dataPacketType = packetType.asSubclass(DataPacket.class);
        DataPacket packet = null;
        try {
            packet = dataPacketType.getConstructor(byte[].class).newInstance(data);
        }
        catch (Exception e) {
            throw new MalformedPacketException("Can't instantiate packet for type code <" + packetTypeCode + ">", e);
        }
        
        if (!packet.isProtocolVersionOk())
            throw new MalformedPacketException("Incorrect protocol version: " + packet.getProtocolVersion() + ", packet: " + packet);
        
        return packet;
    }

    /**
     * Writes the Type and Protocol Version fields of a Data Packet to
     * an {@link OutputStream}.
     * @param outputStream
     */
    protected void writeHeader(OutputStream outputStream) throws IOException {
        outputStream.write((byte)getPacketTypeCode());
        outputStream.write(getProtocolVersion());
    }
}
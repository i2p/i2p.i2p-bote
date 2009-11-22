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
import i2p.bote.folder.FolderElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.i2p.util.Log;

/**
 * The superclass of all "payload" packet types.
 */
public abstract class DataPacket extends I2PBotePacket implements FolderElement {
    private static Log log = new Log(DataPacket.class);

    private File file;
    
    public DataPacket() {
    }
    
    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(toByteArray());
    }
    
    public static DataPacket createPacket(File file) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            DataPacket packet = createPacket(Util.readInputStream(inputStream));
            return packet;
        }
        catch (IOException e) {
            log.error("Can't read packet file: " + file.getAbsolutePath(), e);
            return null;
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                log.error("Can't close stream.", e);
            }
        }
    }
    
    /**
     * Creates a {@link DataPacket} object from its byte array representation.
     * If there is an error, <code>null</code> is returned.
     * @param data
     * @return
     */
    public static DataPacket createPacket(byte[] data) {
        char packetTypeCode = (char)data[0];   // first byte of a data packet is the packet type code
        Class<? extends I2PBotePacket> packetType = decodePacketTypeCode(packetTypeCode);
        if (packetType==null || !DataPacket.class.isAssignableFrom(packetType)) {
            log.error("Type code is not a DataPacket type code: <" + packetTypeCode + ">");
            return null;
        }
        
        Class<? extends DataPacket> dataPacketType = packetType.asSubclass(DataPacket.class);
        try {
            return dataPacketType.getConstructor(byte[].class).newInstance(data);
        }
        catch (Exception e) {
            log.warn("Can't instantiate packet for type code <" + packetTypeCode + ">", e);
            return null;
        }
    }

    // FolderElement implementation
    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(File file) {
        this.file = file;
    }
}
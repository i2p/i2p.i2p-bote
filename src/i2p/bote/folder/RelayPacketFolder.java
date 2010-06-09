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

package i2p.bote.folder;

import i2p.bote.UniqueId;
import i2p.bote.packet.RelayDataPacket;

import java.io.File;
import java.util.Random;

import net.i2p.util.Log;

/**
 * A <code>PacketFolder</code> that uses filenames that consist of
 * the packet's scheduled send time and a random part.
 */
public class RelayPacketFolder extends PacketFolder<RelayDataPacket> {
    private final Log log = new Log(RelayPacketFolder.class);
    private Random random;

    public RelayPacketFolder(File storageDir) {
        super(storageDir);
        random = new Random();
    }

    /**
     * Stores a <code>RelayDataPacket</code> in the folder.
     * @param packet
     */
    public void add(RelayDataPacket packet) {
        while (true) {
            UniqueId id = new UniqueId();
            long minDelay = packet.getMinimumDelay();
            long maxDelay = packet.getMaximumDelay();
            // generate a random time between minDelay and maxDelay milliseconds in the future
            long sendTime;
            if (minDelay == maxDelay)
                sendTime = System.currentTimeMillis() + minDelay;
            else
                sendTime = System.currentTimeMillis() + minDelay + Math.abs(random.nextLong()) % Math.abs(maxDelay-minDelay);
            
            String filename = sendTime + "_" + id.toBase32() + PACKET_FILE_EXTENSION;
            
            File file = new File(storageDir, filename);
            if (!file.exists()) {
                add(packet, filename);
                return;
            }
        }
    }
    
    @Override
    protected RelayDataPacket createFolderElement(File file) throws Exception {
        RelayDataPacket packet = super.createFolderElement(file);
        try {
            long sendTime = getSendTime(file.getName());
            packet.setSendTime(sendTime);
        }
        catch (NumberFormatException e) {
            log.error("Invalid send time in filename: <" + file.getAbsolutePath() + ">", e);
        }
        return packet;
    }
    
    private long getSendTime(String filename) throws NumberFormatException {
        String[] parts = filename.split("_");
        return Long.valueOf(parts[0]);
    }
}
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

import i2p.bote.packet.relay.RelayRequest;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * A <code>PacketFolder</code> that uses filenames that consist of
 * the packet's scheduled send time and the SHA256 hash of the packet.
 */
public class RelayPacketFolder extends PacketFolder<RelayRequest> {
    private final Log log = new Log(RelayPacketFolder.class);

    public RelayPacketFolder(File storageDir) {
        super(storageDir);
    }

    /**
     * Stores a <code>RelayRequest</code> in the folder.
     * @param packet
     */
    public void add(RelayRequest packet) {
        // make the packet's hash part of the filename and don't save if a file with the same hash exists already
        byte[] bytes = packet.toByteArray();
        Hash packetHash = SHA256Generator.getInstance().calculateHash(bytes);
        String base64Hash = packetHash.toBase64();
        if (!fileExistsForHash(base64Hash)) {
            long sendTime = System.currentTimeMillis() + packet.getDelay();
            String filename = sendTime + "_" + base64Hash + PACKET_FILE_EXTENSION;
            add(packet, filename);
            return;
        }
    }
    
    private boolean fileExistsForHash(final String base64Hash) {
        File[] files = storageDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(base64Hash);
            }
        });
        
        return files.length > 0;
    }
    
    @Override
    protected RelayRequest createFolderElement(File file) throws IOException {
        RelayRequest packet = super.createFolderElement(file);
        if (packet != null) {
            try {
                long sendTime = getSendTime(file.getName());
                packet.setSendTime(sendTime);
            } catch (NumberFormatException e) {
                log.error("Invalid send time in filename: <" + file.getAbsolutePath() + ">", e);
            }
        }
        return packet;
    }
    
    private long getSendTime(String filename) throws NumberFormatException {
        String[] parts = filename.split("_");
        return Long.valueOf(parts[0]);
    }
}
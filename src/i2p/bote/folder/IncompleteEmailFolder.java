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
import i2p.bote.email.Email;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import net.i2p.util.Log;

/**
 * File name format: <message id>_<fragment index>.pkt
 */
public class IncompleteEmailFolder extends PacketFolder<UnencryptedEmailPacket> {
    private Log log = new Log(IncompleteEmailFolder.class);
    private EmailFolder inbox;

    public IncompleteEmailFolder(File storageDir, EmailFolder inbox) {
        super(storageDir);
        this.inbox = inbox;
    }
    
    @Override
    public void add(UnencryptedEmailPacket packetToStore) {
        addEmailPacket(packetToStore);
    }
    
    /**
     * Same as {@link add(UnencryptedEmailPacket)}, but returns <code>true</code>
     * if an email was completed as a result of adding the packet.
     * @param packetToStore
     * @return
     */
    public synchronized boolean addEmailPacket(UnencryptedEmailPacket packetToStore) {
        add(packetToStore, getFilename(packetToStore));
        
        // TODO possible optimization: if getNumFragments == 1, no need to check for other packet files
        File[] finishedPacketFiles = getAllMatchingFiles(packetToStore.getMessageId());
        
        // if all packets of the email are available, assemble them into an email
        if (finishedPacketFiles.length == packetToStore.getNumFragments()) {
            assemble(finishedPacketFiles);
            return true;
        }
        return false;
    }
    
    private String getFilename(UnencryptedEmailPacket packet) {
        String fragIndex = String.format("%03d", packet.getFragmentIndex());
        return packet.getMessageId() + "_" + fragIndex + PacketFolder.PACKET_FILE_EXTENSION;
    }

    private void assemble(File[] packetFiles) {
        // No need to do this in a separate thread, just call run()
        new AssembleTask(packetFiles, inbox).run();
    }
    
    /**
     * Returns all filenames that match a given message ID. Not to be confused with
     * the {@link #retrieve(net.i2p.data.Hash)} method, which takes a DHT key.
     * @param messageId
     * @return
     */
    private File[] getAllMatchingFiles(UniqueId messageId) {
        final String base64Id = messageId.toBase64();
        
        return storageDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(base64Id);
            }
        });
    }
    
    /**
     * Makes a set of {@link UnencryptedEmailPacket}s into an {@link Email}, stores the email in an {@link EmailFolder},
     * and deletes the packet files.
     */
    private class AssembleTask implements Runnable {
        File[] packetFiles;
        
        public AssembleTask(File[] packetFiles, EmailFolder inbox) {
            this.packetFiles = packetFiles;
        }

        @Override
        public void run() {
        	UnencryptedEmailPacket[] packets = getEmailPackets(packetFiles).toArray(new UnencryptedEmailPacket[0]);
            
            // sort by fragment index
            Arrays.sort(packets, new Comparator<UnencryptedEmailPacket>() {
                @Override
                public int compare(UnencryptedEmailPacket packet1, UnencryptedEmailPacket packet2) {
                    return new Integer(packet1.getFragmentIndex()).compareTo(packet2.getFragmentIndex());
                }
            });

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                for (UnencryptedEmailPacket packet: packets)
                    outputStream.write(packet.getContent());
                Email email = new Email(outputStream.toByteArray());
                inbox.add(email);
                
                // delete packets
                for (File file: packetFiles)
                    if (!file.delete())
                        log.warn("Email packet file not deleted: <" + file.getAbsolutePath() + ">");
            }
            catch (Exception e) {
                log.error("Error assembling/storing email, or deleting email packets. ", e);
                return;
            }
        }
        
        private Collection<UnencryptedEmailPacket> getEmailPackets(File[] files) {
            Collection<UnencryptedEmailPacket> packets = new ArrayList<UnencryptedEmailPacket>();
            for (File file: files) {
                I2PBotePacket packet = DataPacket.createPacket(file);
                if (packet instanceof UnencryptedEmailPacket)
                    packets.add((UnencryptedEmailPacket)packet);
                else
                    log.error("Non-Email Packet found in the IncompleteEmailFolder, file: <" + file.getAbsolutePath() + ">");
            }
            return packets;
        }
    }

    // FolderElement implementation
	@Override
	protected UnencryptedEmailPacket createFolderElement(File file)
			throws Exception {
        FileInputStream inputStream = new FileInputStream(file);
        return new UnencryptedEmailPacket(inputStream);
	}
}
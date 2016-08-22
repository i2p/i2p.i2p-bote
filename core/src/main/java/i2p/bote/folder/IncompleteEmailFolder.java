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
import i2p.bote.packet.MalformedPacketException;
import i2p.bote.packet.dht.UnencryptedEmailPacket;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import net.i2p.util.Log;

/**
 * File name format: <code>&lt;message id&gt;_&lt;fragment index&gt;.pkt</code>
 */
public class IncompleteEmailFolder extends PacketFolder<UnencryptedEmailPacket> {
    private Log log = new Log(IncompleteEmailFolder.class);
    private EmailFolder inbox;
    private MessageIdCache messageIdCache;
    private Collection<NewEmailListener> newEmailListeners;

    public IncompleteEmailFolder(File storageDir, MessageIdCache messageIdCache, EmailFolder inbox) {
        super(storageDir);
        this.inbox = inbox;
        this.messageIdCache = messageIdCache;
        newEmailListeners = new ArrayList<NewEmailListener>();
    }

    public synchronized int getNumIncompleteEmails() {
        Set<String> messages = new HashSet<String>();
        File[] packets = getFilenames();
        for (int i = 0; i < packets.length; i++) {
            File packet = packets[i];
            String messageId = packet.getName().split("_")[0];
            messages.add(messageId);
        }
        return messages.size();
    }
    
    /**
     * Stores an <code>UnencryptedEmailPacket</code> in the folder and returns <code>true</code>
     * if an email was completed as a result of adding the packet.
     * @param packetToStore
     * @see i2p.bote.folder.PacketFolder#add(I2PBotePacket, String)
     */
    public synchronized boolean addEmailPacket(UnencryptedEmailPacket packetToStore) {
        UniqueId messageId = packetToStore.getMessageId();
        // if a previously assembled (completed) email contained the message ID, ignore the email packet
        if (messageIdCache.contains(messageId)) {
            log.debug("Discarding email packet because the message ID matches a previously received email. Packet: " + packetToStore);
            return false;
        }
        
        add(packetToStore, getFilename(packetToStore));
        
        // TODO possible optimization: if getNumFragments == 1, no need to check for other packet files
        File[] finishedPacketFiles = getAllMatchingFiles(messageId);
        
        // if all packets of the email are available, assemble them into an email
        if (finishedPacketFiles.length == packetToStore.getNumFragments()) {
            assemble(finishedPacketFiles);
            messageIdCache.add(messageId);
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
     * Makes a set of {@link UnencryptedEmailPacket}s into an {@link Email}, stores the email in the inbox,
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
                    return Integer.valueOf(packet1.getFragmentIndex()).compareTo(packet2.getFragmentIndex());
                }
            });

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                for (UnencryptedEmailPacket packet: packets)
                    outputStream.write(packet.getContent());
                Email email = new Email(outputStream.toByteArray());
                email.setMessageID(packets[0].getMessageId());   // all packets in the array have the same message ID
                email.setSignatureFlag();   // incoming emails have no signature flag, so set it now; if it exists, don't trust but overwrite
                email.getMetadata().setReceivedDate(new Date());
                inbox.add(email);

                // notify listeners
                for (NewEmailListener listener : newEmailListeners)
                    listener.emailReceived(email.getMessageID());
                
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
                try {
                    I2PBotePacket packet = DataPacket.createPacket(file);
                    if (packet instanceof UnencryptedEmailPacket)
                        packets.add((UnencryptedEmailPacket)packet);
                    else
                        log.error("Non-Email Packet found in the IncompleteEmailFolder, file: <" + file.getAbsolutePath() + ">");
                } catch (MalformedPacketException e) {
                    log.error("Cannot create packet from file: <" + file.getAbsolutePath() + ">", e);
                }
            }
            return packets;
        }
    }

    // FolderElement implementation
    @Override
    protected UnencryptedEmailPacket createFolderElement(File file) throws IOException {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return new UnencryptedEmailPacket(inputStream);
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
    }

    public void addNewEmailListener(NewEmailListener newEmailListener) {
        newEmailListeners.add(newEmailListener);
    }

    public void removeNewEmailListener(NewEmailListener newEmailListener) {
        newEmailListeners.remove(newEmailListener);
    }
}
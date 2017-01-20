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
import i2p.bote.Util;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.dht.DeleteRequest;
import i2p.bote.packet.dht.DeletionInfoPacket;
import i2p.bote.packet.dht.DeletionRecord;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.EmailPacketDeleteRequest;
import i2p.bote.packet.dht.EncryptedEmailPacket;

import java.io.File;
import java.util.Iterator;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * A subclass of {@link DeletionAwareDhtFolder} that stores email packets and deletes them
 * upon {@link EmailPacketDeleteRequest}s.<br/>
 * When a packet is deleted, its DHT key is added to a {@link DeletionInfoPacket} file
 * whose file name starts with <code>DEL_</code>.
 * <p/>
 * Deletion Records are kept until they expire (see {@link ExpirationListener}), so
 * there will be a large number of them after a while. To keep the number of files to
 * a reasonable level, the records are grouped together in Deletion Info Packets
 * (unlike Email Packets, which are all stored in separate files).
 */
public class EmailPacketFolder extends DeletionAwareDhtFolder<EncryptedEmailPacket> implements PacketListener, ExpirationListener {
    private Log log = new Log(EmailPacketFolder.class);

    public EmailPacketFolder(File storageDir) {
        super(storageDir);
    }

    /** Overridden to set a time stamp on the packet */
    @Override
    public void store(DhtStorablePacket packetToStore) {
        if (!(packetToStore instanceof EncryptedEmailPacket))
            throw new IllegalArgumentException("Invalid packet type: " + packetToStore.getClass().getSimpleName() + "; this folder only stores packets of type " + EncryptedEmailPacket.class.getSimpleName() + ".");
        
        EncryptedEmailPacket emailPacket = (EncryptedEmailPacket)packetToStore;
        // If the packet didn't come with a time stamp, set it to the current time
        if (emailPacket.getStoreTime() == 0)
            emailPacket.setStoreTime(System.currentTimeMillis());
        super.store(packetToStore);
    }
    
    /** Overridden to erase the time stamp because there is no need for other peers to see it. */
    @Override
    public DhtStorablePacket retrieve(Hash dhtKey) {
        DhtStorablePacket packet = super.retrieve(dhtKey);
        if (packet == null)
            return null;
        else if (!(packet instanceof EncryptedEmailPacket)) {
            log.error("Packet of type " + packet.getClass().getSimpleName() + " found in " + getClass().getSimpleName());
            return null;
        }
        else {
            EncryptedEmailPacket emailPacket = (EncryptedEmailPacket)packet;
            emailPacket.setStoreTime(0);
            return emailPacket;
        }
    }
    
    /** Handles delete requests */
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof EmailPacketDeleteRequest) {
            EmailPacketDeleteRequest delRequest = (EmailPacketDeleteRequest)packet;
            process(delRequest);
        }
    }
    
    /**
     * Deletes email packets.
     * @param delRequest An instance of {@link EmailPacketDeleteRequest}
     */
    @Override
    public synchronized void process(DeleteRequest delRequest) {
        log.debug("Processing delete request: " + delRequest);
        if (!(delRequest instanceof EmailPacketDeleteRequest))
            log.error("Invalid type of delete request for EmailPacketFolder: " + delRequest.getClass());
        EmailPacketDeleteRequest emailPacketDelRequest = (EmailPacketDeleteRequest)delRequest;
        
        // see if the packet exists
        Hash dhtKey = emailPacketDelRequest.getDhtKey();
        DhtStorablePacket storedPacket = retrieve(dhtKey);
        if (storedPacket instanceof EncryptedEmailPacket) {
            // verify
            Hash verificationHash = ((EncryptedEmailPacket)storedPacket).getDeleteVerificationHash();
            UniqueId delAuthorization = emailPacketDelRequest.getAuthorization();
            boolean valid = Util.isDeleteAuthorizationValid(verificationHash, delAuthorization);
        
            if (valid)
                delete(dhtKey, delAuthorization);
            else
                log.debug("Invalid Delete Authorization in EmailPacketDeleteRequest. Should be: <" + verificationHash.toBase64() + ">");
        }
        else if (storedPacket != null)
            log.debug("EncryptedEmailPacket expected for DHT key <" + dhtKey + ">, found " + storedPacket.getClass().getSimpleName());
    }
    
    /**
     * Deletes an Email Packet and adds its DHT key to the {@link DeletionInfoPacket} file.
     * @param dhtKey
     * @param delAuthorization
     */
    private void delete(Hash dhtKey, UniqueId delAuthorization) {
        delete(dhtKey);
        String delFileName = getDeletionFileName(dhtKey);
        addToDeletedPackets(delFileName, dhtKey, delAuthorization);
    }

    @Override
    public synchronized void deleteExpired() {
        long currentTimeMillis = System.currentTimeMillis();
        for (Iterator<EncryptedEmailPacket> iterator=iterator(); iterator.hasNext();) {
            EncryptedEmailPacket emailPacket = iterator.next();
            if (currentTimeMillis > emailPacket.getStoreTime() + EXPIRATION_TIME_MILLISECONDS) {
                log.debug("Deleting expired email packet: <" + emailPacket + ">");
                iterator.remove();
            }
        }
    }
    
    private String getDeletionFileName(Hash dhtKey) {
        // group deletion files by the first two base64 characters of the DHT key
        return DEL_FILE_PREFIX + dhtKey.toBase64().substring(0, 2) + PACKET_FILE_EXTENSION;
    }

    @Override
    public UniqueId getDeleteAuthorization(Hash dhtKey) {
        String delFileName = getDeletionFileName(dhtKey);
        DeletionInfoPacket delInfo = createDelInfoPacket(delFileName);
        if (delInfo != null) {
            DeletionRecord delRecord = delInfo.getEntry(dhtKey);
            if (delRecord != null)
                return delRecord.delAuthorization;
        }
        return null;
    }
    
    @Override
    public DeleteRequest storeAndCreateDeleteRequest(DhtStorablePacket packetToStore) {
        if (!(packetToStore instanceof EncryptedEmailPacket))
            throw new IllegalArgumentException("Invalid packet type: " + packetToStore.getClass().getSimpleName() + "; this folder only stores packets of type " + EncryptedEmailPacket.class.getSimpleName() + ".");
        
        DeleteRequest delRequest = null;
        
        // read the deletion info file for the email packet's DHT key
        Hash dhtKey = packetToStore.getDhtKey();
        String delFileName = getDeletionFileName(dhtKey);
        DeletionInfoPacket delInfo = createDelInfoPacket(delFileName);
        if (delInfo != null) {
            DeletionRecord delRecord = delInfo.getEntry(dhtKey);
            if (delRecord != null)
                delRequest = new EmailPacketDeleteRequest(delRecord.dhtKey, delRecord.delAuthorization);
        }
        else
            // if the DHT key has not been recorded as deleted, store the email packet
            store(packetToStore);
        
        return delRequest;
    }
}
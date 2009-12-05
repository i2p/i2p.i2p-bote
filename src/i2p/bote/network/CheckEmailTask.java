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

package i2p.bote.network;

import i2p.bote.UniqueId;
import i2p.bote.Util;
import i2p.bote.email.EmailIdentity;
import i2p.bote.folder.IncompleteEmailFolder;
import i2p.bote.packet.EncryptedEmailPacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.UnencryptedEmailPacket;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * Gets email packets from the DHT for one email identity. A separate thread is used for
 * each packet in order to speed things up, and because the packets are in different places
 * on the network.
 */
public class CheckEmailTask implements Callable<Boolean> {
    public static final int THREAD_STACK_SIZE = 64 * 1024;   // TODO find a safe low value (default in 64-bit Java 1.6 = 1MByte)
    private static final int MAX_THREADS = 50;
    private static final ThreadFactory EMAIL_PACKET_TASK_THREAD_FACTORY = Util.createThreadFactory("EmailPktTask", THREAD_STACK_SIZE);
    
    private Log log = new Log(CheckEmailTask.class);
    private EmailIdentity identity;
    private DHT dht;
    private PeerManager peerManager;
    private IncompleteEmailFolder incompleteEmailFolder;
    private ExecutorService executor;

    public CheckEmailTask(EmailIdentity identity, DHT dht, PeerManager peerManager, IncompleteEmailFolder incompleteEmailFolder) {
        this.identity = identity;
        this.dht = dht;
        this.peerManager = peerManager;
        this.incompleteEmailFolder = incompleteEmailFolder;
        executor = Executors.newFixedThreadPool(MAX_THREADS, EMAIL_PACKET_TASK_THREAD_FACTORY);
    }
    
    /**
     * Returns <code>true</code> if a new email was created in the inbox as a result
     * of receiving an email packet.
     */
    @Override
    public Boolean call() {
        Collection<Hash> emailPacketKeys = findEmailPacketKeys();

        Collection<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        for (Hash dhtKey: emailPacketKeys) {
            Future<Boolean> result = executor.submit(new EmailPacketTask(dhtKey));
            results.add(result);
        }
        
        boolean newEmail = false;
        for (Future<Boolean> result: results)
            try {
                if (result.get())
                    newEmail = true;
            } catch (ExecutionException e) {
                log.error("Can't retrieve email packet.", e);
            } catch (InterruptedException e) {
                log.error("Interrupted while checking for mail.", e);
            }
        return newEmail;
    }
    
    /**
     * Queries the DHT for new index packets and returns the DHT keys contained in them.
     * @return A <code>Collection</code> containing zero or more elements
     */
    private Collection<Hash> findEmailPacketKeys() {
        log.debug("Querying the DHT for index packets with key " + identity.getHash());
        Collection<DhtStorablePacket> packets = dht.findAll(identity.getHash(), IndexPacket.class);
        
        // build an Collection of index packets
        Collection<IndexPacket> indexPackets = new ArrayList<IndexPacket>();
        for (DhtStorablePacket packet: packets)
            if (packet instanceof IndexPacket)
                indexPackets.add((IndexPacket)packet);
            else
                log.error("DHT returned packet of class " + packet.getClass().getSimpleName() + ", expected IndexPacket.");
        
        IndexPacket mergedPacket = new IndexPacket(indexPackets);
        log.debug("Found " + mergedPacket.getDhtKeys().size() + " Email Packet keys.");
        return mergedPacket.getDhtKeys();
    }
    
    /**
     * Queries the DHT for an email packet, adds the packet to the {@link IncompleteEmailFolder},
     * and deletes the packet from the DHT.
     */
    private class EmailPacketTask implements Callable<Boolean> {
        private Hash dhtKey;
        
        /**
         * 
         * @param dhtKey The DHT key of the email packet to retrieve
         */
        public EmailPacketTask(Hash dhtKey) {
            this.dhtKey = dhtKey;
        }
        
        /**
         * Returns <code>true</code> if a new email was created in the inbox as a result
         * of receiving an email packet.
         */
        @Override
        public Boolean call() {
            boolean emailCompleted = false;
            DhtStorablePacket packet = dht.findOne(dhtKey, EncryptedEmailPacket.class);
            if (packet instanceof EncryptedEmailPacket) {
                EncryptedEmailPacket emailPacket = (EncryptedEmailPacket)packet;
                try {
                    UnencryptedEmailPacket decryptedPacket = emailPacket.decrypt(identity);
                    emailCompleted = incompleteEmailFolder.addEmailPacket(decryptedPacket);
                    sendDeleteRequest(dhtKey, decryptedPacket.getVerificationDeletionKey());
                }
                catch (DataFormatException e) {
                    log.error("Can't decrypt email packet: " + emailPacket, e);
                    // TODO propagate error message to UI
                }
            }
            else
                if (packet != null)
                    log.error("DHT returned packet of class " + packet.getClass().getSimpleName() + ", expected EmailPacket.");
            return emailCompleted;
        }
        
        /**
         * Sends a delete request to the DHT.
         * @param dhtKey The DHT key of the email packet that is to be deleted
         * @param deletionKey The deletion key for the email packet
         */
        private void sendDeleteRequest(Hash dhtKey, UniqueId deletionKey) {
            // TODO
        }
   }
}
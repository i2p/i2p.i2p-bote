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
import i2p.bote.packet.EmailPacketDeleteRequest;
import i2p.bote.packet.EncryptedEmailPacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.IndexPacketDeleteRequest;
import i2p.bote.packet.UnencryptedEmailPacket;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import net.i2p.I2PAppContext;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
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
    private I2PSendQueue sendQueue;
    private IncompleteEmailFolder incompleteEmailFolder;
    private I2PAppContext appContext;
    private ExecutorService executor;

    // TODO move appContext into EncryptedEmailPacket so there is one less parameter here
    public CheckEmailTask(EmailIdentity identity, DHT dht, PeerManager peerManager, I2PSendQueue sendQueue, IncompleteEmailFolder incompleteEmailFolder, I2PAppContext appContext) {
        this.identity = identity;
        this.dht = dht;
        this.peerManager = peerManager;
        this.sendQueue = sendQueue;
        this.incompleteEmailFolder = incompleteEmailFolder;
        this.appContext = appContext;
        executor = Executors.newFixedThreadPool(MAX_THREADS, EMAIL_PACKET_TASK_THREAD_FACTORY);
    }
    
    /**
     * Returns <code>true</code> if a new email was created in the inbox as a result
     * of receiving an email packet.
     */
    @Override
    public Boolean call() {
        // Use findAll rather than findOne because some peers might have an incomplete set of
        // Email Packet keys, and because we want to send IndexPacketDeleteRequests to all of them.
        DhtResults indexPacketResults = dht.findAll(identity.getHash(), IndexPacket.class);
        
        Collection<Hash> emailPacketKeys = findEmailPacketKeys(indexPacketResults.getPackets());

        Collection<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        for (Hash emailPacketKey: emailPacketKeys) {
            Future<Boolean> result = executor.submit(new EmailPacketTask(emailPacketKey, identity.getHash(), indexPacketResults.getPeers()));
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
     * @param indexPacketResults TODO comment
     * @return A <code>Collection</code> containing zero or more DHT keys
     */
    private Collection<Hash> findEmailPacketKeys(Collection<DhtStorablePacket> indexPacketResults) {
        log.debug("Querying the DHT for index packets with key " + identity.getHash());
        
        // build a Collection of index packets
        Collection<IndexPacket> indexPackets = new ArrayList<IndexPacket>();
        for (DhtStorablePacket packet: indexPacketResults)
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
        private Hash emailPacketKey;
        private Hash emailIdentityHash;
        private Set<Destination> indexPacketPeers;
        
        /**
         * 
         * @param emailPacketKey The DHT key of the email packet to retrieve
         * @param emailIdentityHash The DHT key of the packet recipient's email identity
         * @param indexPacketPeers All peers that are known to be storing an index packet
         *                         for the email destination the email packet is being sent to.
         */
        public EmailPacketTask(Hash emailPacketKey, Hash emailIdentityHash, Set<Destination> indexPacketPeers) {
            this.emailPacketKey = emailPacketKey;
            this.emailIdentityHash = emailIdentityHash;
            this.indexPacketPeers = indexPacketPeers;
        }
        
        /**
         * Returns <code>true</code> if a new email was created in the inbox as a result
         * of receiving an email packet.
         */
        @Override
        public Boolean call() {
            boolean emailCompleted = false;
            // Use findAll rather than findOne because after we receive an email packet, we want
            // to send delete requests to as many of the storage nodes as possible.
            DhtResults results = dht.findAll(emailPacketKey, EncryptedEmailPacket.class);
            
            EncryptedEmailPacket validPacket = null;   // stays null until a valid packet is found in the loop below
            IndexPacketDeleteRequest indexDelRequest = new IndexPacketDeleteRequest(emailIdentityHash);
            for (Destination peer: results.getPeers()) {
                DhtStorablePacket packet = results.getPacket(peer);
                if (packet instanceof EncryptedEmailPacket) {
                    EncryptedEmailPacket emailPacket = (EncryptedEmailPacket)packet;
                    try {
                        UnencryptedEmailPacket decryptedPacket = emailPacket.decrypt(identity, appContext);
                        if (validPacket == null) {
                            emailCompleted = incompleteEmailFolder.addEmailPacket(decryptedPacket);
                            validPacket = emailPacket;
                        }
                        UniqueId delKey = decryptedPacket.getVerificationDeletionKey();
                        sendDeleteRequest(emailPacketKey, delKey, peer);
                        indexDelRequest.put(emailPacketKey, delKey);
                    }
                    catch (DataFormatException e) {
                        log.error("Can't decrypt email packet: " + emailPacket, e);
                        // TODO propagate error message to UI
                    }
                }
                else
                    if (packet != null)
                        log.error("DHT returned packet of class " + packet.getClass().getSimpleName() + ", expected EmailPacket.");
            }
            
            if (indexDelRequest.getNumEntries() > 0)
                sendDeleteRequest(indexDelRequest, indexPacketPeers);
            
            return emailCompleted;
        }
        
        /**
         * Sends an Email Packet Delete Request to a peer.
         * @param dhtKey The DHT key of the email packet that is to be deleted
         * @param deletionKey The deletion key for the email packet
         * @param peer
         */
        private void sendDeleteRequest(Hash dhtKey, UniqueId deletionKey, Destination peer) {
            EmailPacketDeleteRequest packet = new EmailPacketDeleteRequest(dhtKey, deletionKey);
            log.debug("Sending an EmailPacketDeleteRequest for DHT key " + dhtKey + " to " + peer.calculateHash());
            sendQueue.send(packet, peer);
        }
        
        private void sendDeleteRequest(IndexPacketDeleteRequest indexDelRequest, Set<Destination> peers) {
            log.debug("Sending an IndexPacketDeleteRequest to " + peers.size() + " peers: " + indexDelRequest);
            for (Destination peer: peers)
                sendQueue.send(indexDelRequest, peer);
        }
   }
}
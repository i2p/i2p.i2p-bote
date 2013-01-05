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
import i2p.bote.folder.EmailPacketFolder;
import i2p.bote.folder.IncompleteEmailFolder;
import i2p.bote.folder.IndexPacketFolder;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.EmailPacketDeleteRequest;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.IndexPacket;
import i2p.bote.packet.dht.IndexPacketDeleteRequest;
import i2p.bote.packet.dht.IndexPacketEntry;
import i2p.bote.packet.dht.UnencryptedEmailPacket;
import i2p.bote.service.RelayPeerManager;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * Gets email packets from the DHT for one email identity. A separate thread is used for
 * each packet in order to speed things up, and because the packets are in different places
 * on the network.
 */
public class CheckEmailTask implements Callable<Boolean> {
    public static final int THREAD_STACK_SIZE = 256 * 1024;   // TODO find a safe low value (64k is too low, default in 64-bit Java 1.6 = 1MByte)
    private static final int MAX_THREADS = 50;
    private static final ThreadFactory EMAIL_PACKET_TASK_THREAD_FACTORY = Util.createThreadFactory("EmailPktTask", THREAD_STACK_SIZE);
    
    private Log log = new Log(CheckEmailTask.class);
    private EmailIdentity identity;
    private DHT dht;
    private RelayPeerManager peerManager;
    private I2PSendQueue sendQueue;
    private Destination localDestination;
    private IncompleteEmailFolder incompleteEmailFolder;
    private EmailPacketFolder emailPacketFolder;
    private IndexPacketFolder indexPacketFolder;
    private volatile boolean newEmail;   // EmailPacketTask sets this to true if an email was completed
    private IndexPacketDeleteRequest indexPacketDeleteRequest;   // EmailPacketTask populates this

    /**
     * 
     * @param identity The email identity to check emails for
     * @param dht For retrieving index packets and email packets
     * @param peerManager Unused; will be needed once emails can be retrieved via relays
     * @param sendQueue
     * @param incompleteEmailFolder For storing retrieved email packets
     * @param emailPacketFolder For accessing locally stored email packets directly (rather than sending a retrieve request)
     * @param indexPacketFolder For accessing locally stored index packets directly
     */
    public CheckEmailTask(EmailIdentity identity, DHT dht, RelayPeerManager peerManager, I2PSendQueue sendQueue,
            IncompleteEmailFolder incompleteEmailFolder, EmailPacketFolder emailPacketFolder, IndexPacketFolder indexPacketFolder) {
        this.identity = identity;
        this.dht = dht;
        this.peerManager = peerManager;
        this.sendQueue = sendQueue;
        localDestination = sendQueue.getLocalDestination();
        this.incompleteEmailFolder = incompleteEmailFolder;
        this.emailPacketFolder = emailPacketFolder;
        this.indexPacketFolder = indexPacketFolder;
    }
    
    /**
     * Returns <code>true</code> if a new email was created in the inbox as a result
     * of receiving an email packet.
     */
    @Override
    public Boolean call() throws InterruptedException, ExecutionException, TimeoutException, GeneralSecurityException {
        log.debug("Querying the DHT for index packets with key " + identity.getHash());
        // Use findAll rather than findOne because some peers might have an incomplete set of
        // Email Packet keys, and because we want to send IndexPacketDeleteRequests to all of them.
        DhtResults indexPacketResults = dht.findAll(identity.getHash(), IndexPacket.class);
        if (indexPacketResults.isEmpty())
            return false;
        
        Collection<IndexPacket> indexPackets = getIndexPackets(indexPacketResults.getPackets());
        IndexPacket mergedPacket = new IndexPacket(indexPackets);
        log.debug("Found " + mergedPacket.getNumEntries() + " Email Packet keys in " + indexPacketResults.getNumResults() + " Index Packets.");
        
        newEmail = false;
        indexPacketDeleteRequest = new IndexPacketDeleteRequest(identity.getHash());

        Collection<Future<?>> futureResults = new ArrayList<Future<?>>();
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS, EMAIL_PACKET_TASK_THREAD_FACTORY);
        for (IndexPacketEntry entry: mergedPacket) {
            Runnable task = new EmailPacketTask(entry.emailPacketKey);
            futureResults.add(executor.submit(task));
        }
        executor.shutdown();   // end all EmailPacketTask threads when tasks are finished
        
        // wait until all EmailPacketTasks are done
        try {
            for (Future<?> result: futureResults)
                result.get(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new InterruptedException("EmailPacketTask interrupted");
        }
        
        // delete index packets if all EmailPacketTasks finished without throwing an exception
        Set<Destination> indexPacketPeers = indexPacketResults.getPeers();
        if (indexPacketDeleteRequest.getNumEntries() > 0)
            send(indexPacketDeleteRequest, indexPacketPeers);
        
        return newEmail;
    }
    
    /**
     * Sends an Index Packet Delete Request (a request to delete one or more
     * email packet keys from an index packet) to a number of peers.
     * @param indexDelRequest
     * @param peers
     */
    private void send(IndexPacketDeleteRequest indexDelRequest, Set<Destination> peers) {
        log.debug("Sending an IndexPacketDeleteRequest to " + peers.size() + " peers: " + indexDelRequest);
        for (Destination peer: peers)
            // if the peer is us, delete locally; otherwise, send a delete request to the peer
            if (localDestination.equals(peer))
                indexPacketFolder.process(indexDelRequest);
            else
                sendQueue.send(indexDelRequest, peer);
    }
    
    /**
     * Returns all Index Packets in a <code>Collection</code> of {@link DhtStorablePacket}s.
     * @param dhtPackets Should only contain index packets; other packets are ignored
     * @return An <code>IndexPacket</code> containing all entries from the packets in the <code>Collection</code>
     */
    private Collection<IndexPacket> getIndexPackets(Collection<DhtStorablePacket> dhtPackets) {
        Collection<IndexPacket> indexPackets = new ArrayList<IndexPacket>();
        for (DhtStorablePacket packet: dhtPackets)
            if (packet instanceof IndexPacket)
                indexPackets.add((IndexPacket)packet);
            else
                log.error("DHT returned packet of class " + packet.getClass().getSimpleName() + ", expected IndexPacket.");

        return indexPackets;
    }

    /**
     * Queries the DHT for an email packet, adds the packet to the {@link IncompleteEmailFolder},
     * and deletes the packet from the DHT. If all went well, the index packet entry is also deleted from the DHT.
     */
    private class EmailPacketTask implements Runnable {
        private Hash emailPacketKey;
        
        /**
         * 
         * @param emailPacketKey The DHT key of the email packet to retrieve
         */
        public EmailPacketTask(Hash emailPacketKey) {
            this.emailPacketKey = emailPacketKey;
        }
        
        @Override
        public void run() {
            log.debug("Querying the DHT for email packets with key " + emailPacketKey.toBase64());
            boolean emailCompleted = false;
            // Use findAll rather than findOne because after we receive an email packet, we want
            // to send delete requests to as many of the storage nodes as possible.
            DhtResults results = null;
            try {
                results = dht.findAll(emailPacketKey, EncryptedEmailPacket.class);
            } catch (InterruptedException e) {
                log.debug("Interrupted during DHT.findAll()", e);
                Thread.currentThread().interrupt();
                return;
            }
            
            EncryptedEmailPacket validPacket = null;   // stays null until a valid packet is found in the loop below
            for (Destination peer: results.getPeers()) {
                DhtStorablePacket packet = results.getPacket(peer);
                if (packet instanceof EncryptedEmailPacket) {
                    EncryptedEmailPacket emailPacket = (EncryptedEmailPacket)packet;
                    // if the hash does not match the DHT key, throw the packet away
                    if (emailPacket.verifyPacketHash())
                        try {
                            UnencryptedEmailPacket decryptedPacket = emailPacket.decrypt(identity);
                            if (validPacket == null) {
                                emailCompleted = incompleteEmailFolder.addEmailPacket(decryptedPacket);
                                validPacket = emailPacket;
                                // successfully decrypted the email packet, add to delete requests
                                synchronized(indexPacketDeleteRequest) {
                                    indexPacketDeleteRequest.put(emailPacketKey, decryptedPacket.getDeleteAuthorization());
                                }
                            }
                            UniqueId delAuthorization = decryptedPacket.getDeleteAuthorization();
                            sendDeleteRequest(emailPacketKey, delAuthorization, peer);
                        }
                        catch (Exception e) {
                            log.error("Can't decrypt email packet: " + emailPacket, e);
                        }
                    else
                        log.error("Invalid hash for email packet: " + emailPacket + " Sender: " + Util.toShortenedBase32(peer));
                }
                else
                    if (packet != null)
                        log.error("DHT returned packet of class " + packet.getClass().getSimpleName() + ", expected EmailPacket.");
            }
            
            newEmail |= emailCompleted;
        }
        
        /**
         * Sends an Email Packet Delete Request to a peer. If the peer is the local node,
         * the Email Packet is deleted directly.
         * @param dhtKey The DHT key of the email packet that is to be deleted
         * @param delAuthorization The delete authorization key for the email packet
         * @param peer
         */
        private void sendDeleteRequest(Hash dhtKey, UniqueId delAuthorization, Destination peer) {
            EmailPacketDeleteRequest request = new EmailPacketDeleteRequest(dhtKey, delAuthorization);
            if (localDestination.equals(peer)) {
                log.debug("Handling email packet delete request locally. DHT key: " + dhtKey);
                emailPacketFolder.process(request);
            }
            else {
                log.debug("Sending an EmailPacketDeleteRequest for DHT key " + dhtKey + " to " + Util.toBase32(peer));
                sendQueue.send(request, peer);
            }
        }
   }
}
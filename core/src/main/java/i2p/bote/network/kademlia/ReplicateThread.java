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

package i2p.bote.network.kademlia;

import static i2p.bote.network.kademlia.KademliaConstants.REPLICATE_INTERVAL;
import static i2p.bote.network.kademlia.KademliaConstants.REPLICATE_VARIANCE;
import i2p.bote.network.DhtStorageHandler;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.dht.DeleteRequest;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.StoreRequest;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * Replicates locally stored DHT data on startup and every REPLICATE_INTERVAL seconds after that.<br/>
 * The basic algorithm goes like this:
 * <p/>
 * <ol>
 *   <li/>Do a lookup for the local node ID to refresh the s-bucket
 *   <li/>For each locally stored DHT item (index packet entries and email packets) that has not been deleted:
 *   <ol type="a">
 *     <li/>Store the entry on the k closest nodes, based on info from local buckets
 *     <li/>If at least one peer responds with a valid delete request,
 *       <ul>
 *         <li/>delete it locally (this is already handled by code outside this class), and
 *         <li/>send a delete request to the nodes that didn't respond (which they
 *           won't if they don't know the packet has been deleted)
 *       </ul>
 *     <li/>Otherwise, replication for that entry is finished.
 *   </ol>  
 * </ol>
 */
class ReplicateThread extends I2PAppThread implements PacketListener {
    private static final int SEND_TIMEOUT_MINUTES = 5;   // the maximum amount of time to wait for a store request to be sent
    private static final int WAIT_TIME_SECONDS = 5;   // amount of time to wait after sending a store request
    
    private final Log log = new Log(ReplicateThread.class);
    private Destination localDestination;
    private I2PSendQueue sendQueue;
    private I2PPacketDispatcher i2pReceiver;
    private BucketManager bucketManager;
    private Random rng;
    private long nextReplicationTime;
    private Set<DhtStorageHandler> dhtStores;
    private Set<Hash> keysToSkip;   // all DHT keys that have been re-stored since the last replication
    private Map<Hash, DeleteRequest> receivedDeleteRequests;   // Matching keys in this Map cause the delete request
                                                               // to be replicated instead of the DHT item.
    private volatile boolean replicationRunning;   // true when replication is active

    ReplicateThread(Destination localDestination, I2PSendQueue sendQueue, I2PPacketDispatcher i2pReceiver, BucketManager bucketManager) {
        super("ReplicateThd");
        this.localDestination = localDestination;
        this.sendQueue = sendQueue;
        this.i2pReceiver = i2pReceiver;
        this.bucketManager = bucketManager;
        rng = new Random();
        dhtStores = new ConcurrentHashSet<DhtStorageHandler>();
        keysToSkip = new ConcurrentHashSet<Hash>();
        receivedDeleteRequests = new ConcurrentHashMap<Hash, DeleteRequest>();
    }
    
    void addDhtStoreToReplicate(DhtStorageHandler dhtStore) {
        dhtStores.add(dhtStore);
    }
    
    private long randomTime(long min, long max) {
        if (min < max)
            return min + rng.nextLong() % (max-min);
        else
            return min;
    }

    private void replicate() throws InterruptedException {
        log.debug("Replicating DHT data...");
        
        replicationRunning = true;
        // refresh peers close to the local destination
        ClosestNodesLookupTask lookupTask = new ClosestNodesLookupTask(localDestination.calculateHash(), sendQueue, i2pReceiver, bucketManager);
        List<Destination> closestNodes = lookupTask.call();
        closestNodes.remove(localDestination);

        int numReplicated = 0;
        int numSkipped = 0;

        // Replicate all packets except keysToSkip, onto the known peers closest to the packet.
        // If a peer responds with a delete request, replicate the delete request instead.
        for (DhtStorageHandler dhtStore: dhtStores)
            for (Iterator<? extends DhtStorablePacket> packetIterator=dhtStore.individualPackets(); packetIterator.hasNext(); ) {
                DhtStorablePacket packet = packetIterator.next();
                Hash dhtKey = packet.getDhtKey();
                if (!keysToSkip.contains(dhtKey)) {
                    StoreRequest request = new StoreRequest(packet);
                    List<Destination> closestPeers = bucketManager.getClosestPeers(dhtKey, KademliaConstants.K);
                    for (Destination peer: closestPeers) {
                        // Send the store request and give the peer time to respond with a delete request
                        sendQueue.send(request, peer).await(SEND_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                        TimeUnit.SECONDS.sleep(WAIT_TIME_SECONDS);
                        
                        // If we received a delete request for the DHT item, notify the other close peers
                        DeleteRequest delRequest = receivedDeleteRequests.get(dhtKey);
                        if (delRequest != null) {
                            // KademliaDHT handles the delete request for local data, but we forward the
                            // request to the other nodes close to the DHT key.
                            // Note that the delete request contains only one entry, see packetReceived()
                            sendDeleteRequest(delRequest, closestPeers, peer);
                            break;
                        }
                    }
                    numReplicated++;
                }
                else
                    numSkipped++;
            }
        
        keysToSkip.clear();
        replicationRunning = false;
        receivedDeleteRequests.clear();
        
        log.debug("Replication finished. Replicated " + numReplicated + " packets, skipped: " + numSkipped);
    }
    
    /**
     * Sends a delete request to a number of peers. Does not send a request to <code>except</code>.
     * @param delRequest
     * @param peers
     * @param except
     */
    private void sendDeleteRequest(DeleteRequest delRequest, Collection<Destination> peers, Destination except) {
        for (Destination peer: peers)
            if (peer != except)
                sendQueue.send(delRequest, peer);
    }
    
    /**
     * Informs the <code>ReplicationThread</code> that a packet has been stored in a local folder.
     * @param folder
     * @param packet
     */
    public void packetStored(DhtStorageHandler folder, DhtStorablePacket packet) {
        keysToSkip.add(packet.getDhtKey());
    }
    
    @Override
    public void run() {
        nextReplicationTime = System.currentTimeMillis();
        
        while (!Thread.interrupted()) {
            try {
                replicate();
                long waitTime = randomTime(REPLICATE_INTERVAL-REPLICATE_VARIANCE, REPLICATE_INTERVAL+REPLICATE_VARIANCE);
                nextReplicationTime += waitTime;
                log.debug("Next replication at " + new Date(nextReplicationTime));
                TimeUnit.SECONDS.sleep(waitTime);
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.debug("Exception caught in ReplicateThread loop", e);
            }
        }
        
        log.debug("ReplicateThread interrupted, exiting.");
    }

    /**
     * PacketListener implementation.<br/>
     * Listens for delete requests and adds them to <code>receivedDeleteRequests</code> if a replication
     * is running. If not, this method does nothing.
     */
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof DeleteRequest && replicationRunning) {
            DeleteRequest delRequest = (DeleteRequest)packet;
            Collection<Hash> dhtKeys = delRequest.getDhtKeys();
            // create a DHT key - delete request mapping for each DHT key in the delete request
            for (Hash dhtKey: dhtKeys) {
                DeleteRequest indivRequest = delRequest.getIndividualRequest(dhtKey);
                receivedDeleteRequests.put(dhtKey, indivRequest);
            }
        }
    }
}
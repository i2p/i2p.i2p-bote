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
import i2p.bote.packet.DeleteRequest;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.StoreRequest;
import i2p.bote.service.I2PBoteThread;

import java.security.NoSuchAlgorithmException;
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
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

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
public class ReplicateThread extends I2PBoteThread implements PacketListener {
    private static final int WAIT_TIME_SECONDS = 5;   // amount of time to wait after sending <number of nodes> store requests
    
    private final Log log = new Log(ReplicateThread.class);
    private Destination localDestination;
    private I2PSendQueue sendQueue;
    private I2PPacketDispatcher i2pReceiver;
    private BucketManager bucketManager;
    private Random rng;
    private long nextReplicationTime;
    private Map<DhtStorageHandler, Set<Hash>> upToDateKeys;   // all DHT keys that have been re-stored since the last replication
    private Map<Destination, DeleteRequest> receivedDeleteRequests;   // null when not replicating

    public ReplicateThread(Destination localDestination, I2PSendQueue sendQueue, I2PPacketDispatcher i2pReceiver, BucketManager bucketManager) {
        super("ReplicateThd");
        this.localDestination = localDestination;
        this.sendQueue = sendQueue;
        this.i2pReceiver = i2pReceiver;
        this.bucketManager = bucketManager;
        rng = new Random();
        upToDateKeys = new ConcurrentHashMap<DhtStorageHandler, Set<Hash>>();
    }
    
    void addDhtStoreToReplicate(DhtStorageHandler dhtStore) {
        upToDateKeys.put(dhtStore, new ConcurrentHashSet<Hash>());
    }
    
    private long randomTime(long min, long max) {
        if (min < max)
            return min + rng.nextLong() % (max-min);
        else
            return min;
    }

    private void replicate() throws InterruptedException {
        log.debug("Replicating DHT data...");
        
        // refresh peers close to the local dest (essentially a refresh of the s-bucket)
        ClosestNodesLookupTask lookupTask = new ClosestNodesLookupTask(localDestination.calculateHash(), sendQueue, i2pReceiver, bucketManager);
        lookupTask.run();
        List<Destination> closestNodes = lookupTask.getResults();
        closestNodes.remove(localDestination);

        HashCash hashCash;
        try {
            hashCash = HashCash.mintCash("", 1);   // TODO
        }
        catch (NoSuchAlgorithmException e) {
            log.error("Can't generate HashCash for replication requests, aborting replication.", e);
            return;
        }
        
        receivedDeleteRequests = new ConcurrentHashMap<Destination, DeleteRequest>();
        
        // 3 possible ways of sending out the store requests:
        // 
        // 1. Send the first store request to all k nodes, send the next store request to all k nodes, etc.
        //    Downside: If the first node responds with a delete request, all other store requests are sent unnecessarily;
        //    this can be fixed by waiting after each request, but replication will take much longer
        //    (numNodes*numPackets*waitTime).
        // 2. Send all packets to the first node, wait for responses, send all packets to the second node, wait, etc.
        //    Downside: Nodes are bombarded with packets
        // 3. Send packet 0 to node 0, send packet 1 to node 1, etc. until packet k-1 is sent to node k-1. Wait for
        //    responses, then send packet 0 to node 1, send packet 1 to node 2, etc. until packet k-1 is sent to node k.
        //    If the node index would exceed the number of nodes, subtract the number of nodes.
        //    Repeat [number of nodes] times.
        //    This method avoids the downsides of (1) and (2).
        // 
        // For now, method (2) is used because it is easier to implement than (3).
        // 
        int numReplicated = 0;
        int numSkipped = 0;
        boolean shouldCount = true;
        for (Destination node: closestNodes) {
            // note that upToDateKeys always contains all DhtStorageHandlers
            for (DhtStorageHandler dhtStore: upToDateKeys.keySet()) {
                
                Set<Hash> keysToSkip = upToDateKeys.get(dhtStore);
                Iterator<? extends DhtStorablePacket> packetIterator = dhtStore.individualPackets();
                while (packetIterator.hasNext()) {
                    DhtStorablePacket packet = packetIterator.next();
                    if (!keysToSkip.contains(packet.getDhtKey())) {
                        StoreRequest request = new StoreRequest(hashCash, packet);
                        sendQueue.send(request, node);
                        if (shouldCount)
                            numReplicated++;
                    }
                    else if (shouldCount)
                        numSkipped++;
                }
            }
            // wait for responses
            TimeUnit.SECONDS.sleep(WAIT_TIME_SECONDS);
            
            DeleteRequest delRequest = receivedDeleteRequests.get(node);
            if (delRequest != null)
                // send the delete request to all other nodes
                for (Destination otherNode: closestNodes)
                    sendQueue.send(delRequest, otherNode);
            
            // avoid double counting of packets, only count on the first closestNodes iteration
            shouldCount = false;
        }
        
        for (DhtStorageHandler dhtStore: upToDateKeys.keySet())
            upToDateKeys.get(dhtStore).clear();
        receivedDeleteRequests = null;
        
        log.debug("Replication finished. Replicated " + numReplicated + " packets, skipped: " + numSkipped);
    }
    
    /**
     * Informs the <code>ReplicationThread</code> that a packet has been stored in a local folder.
     * @param folder
     * @param packet
     */
    public void packetStored(DhtStorageHandler folder, DhtStorablePacket packet) {
        Set<Hash> keys = upToDateKeys.get(folder);
        keys.add(packet.getDhtKey());
    }
    
    @Override
    public void postStartup() {
        nextReplicationTime = System.currentTimeMillis();
    }
    
    @Override
    public void doStep() throws InterruptedException {
        replicate();
        long waitTime = randomTime(REPLICATE_INTERVAL-REPLICATE_VARIANCE, REPLICATE_INTERVAL+REPLICATE_VARIANCE);
        nextReplicationTime += waitTime;
        log.debug("Next replication at " + new Date(nextReplicationTime));
        awaitShutdownRequest(waitTime, TimeUnit.SECONDS);
    }

    // PacketListener implementation
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof DeleteRequest && receivedDeleteRequests!=null)
            receivedDeleteRequests.put(sender, (DeleteRequest)packet);
    }
}
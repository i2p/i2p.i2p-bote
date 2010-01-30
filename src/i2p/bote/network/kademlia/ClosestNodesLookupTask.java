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

import i2p.bote.UniqueId;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.MalformedCommunicationPacket;
import i2p.bote.packet.PeerList;
import i2p.bote.packet.ResponsePacket;
import i2p.bote.packet.dht.FindClosePeersPacket;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

public class ClosestNodesLookupTask implements Runnable {
    private static final int REQUEST_TIMEOUT = 30 * 1000;
    private static final int CLOSEST_NODES_LOOKUP_TIMEOUT = 2 * 60 * 1000;   // the maximum amount of time a FIND_CLOSEST_NODES can take
    
    private Log log = new Log(ClosestNodesLookupTask.class);
    private Hash key;
    private I2PPacketDispatcher i2pReceiver;
    private BucketManager bucketManager;
    private Random randomNumberGenerator;
    private I2PSendQueue sendQueue;
    private Set<Destination> responses;
    private Set<Destination> notQueriedYet;
    private Map<Destination, FindClosePeersPacket> pendingRequests;
    private long startTime;
    
    public ClosestNodesLookupTask(Hash key, I2PSendQueue sendQueue, I2PPacketDispatcher i2pReceiver, BucketManager bucketManager) {
        this.key = key;
        this.sendQueue = sendQueue;
        this.i2pReceiver = i2pReceiver;
        this.bucketManager = bucketManager;
        randomNumberGenerator = new Random(getTime());
        responses = Collections.synchronizedSet(new TreeSet<Destination>(new HashDistanceComparator(key)));   // nodes that have responded to a query; sorted by distance to the key
        notQueriedYet = new ConcurrentHashSet<Destination>();   // peers we haven't contacted yet
        pendingRequests = new ConcurrentHashMap<Destination, FindClosePeersPacket>();   // outstanding queries
    }
    
    @Override
    public void run() {
        log.debug("Looking up nodes closest to " + key);
        
        PacketListener packetListener = new IncomingPacketHandler();
        i2pReceiver.addPacketListener(packetListener);
        
        // prepare a list of close nodes (might need more than alpha if they don't all respond)
        notQueriedYet.addAll(bucketManager.getClosestPeers(key, KademliaConstants.S));
        
        startTime = getTime();
        do {
            // send new requests if less than alpha are pending
            while (pendingRequests.size()<KademliaConstants.ALPHA && !notQueriedYet.isEmpty()) {
                Destination peer = selectRandom(notQueriedYet);
                notQueriedYet.remove(peer);
                FindClosePeersPacket packet = new FindClosePeersPacket(key);
                pendingRequests.put(peer, packet);
                sendQueue.send(packet, peer);
            }

            // handle timeouts
            for (Map.Entry<Destination, FindClosePeersPacket> request: pendingRequests.entrySet())
                if (hasTimedOut(request.getValue(), REQUEST_TIMEOUT)) {
                    Destination peer = request.getKey();
                    bucketManager.incrementStaleCounter(peer);   // resetting is also done in BucketManager
                    pendingRequests.remove(peer);
                }
            
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.warn("Interrupted while doing a closest nodes lookup.", e);
            }
        } while (!isDone());
        log.debug(responses.size() + " nodes found (may include local node).");
        for (Destination node: responses)
            log.debug("  Node: " + node.calculateHash());
        
        i2pReceiver.removePacketListener(packetListener);
    }

    private boolean isDone() {
        if (pendingRequests.isEmpty() && notQueriedYet.isEmpty())   // if there are no more requests to send, and no more responses to wait for, we're finished
            return true;
        if (responses.size() >= KademliaConstants.S)   // if we have received enough responses, we're also finished
            return true;
        if (hasTimedOut(startTime, CLOSEST_NODES_LOOKUP_TIMEOUT)) {
            log.error("Lookup for closest nodes timed out.");
            return true;
        }
        return false;
    }
    
    private Destination selectRandom(Collection<Destination> collection) {
        Destination[] array = new Destination[collection.size()];
        int index = randomNumberGenerator.nextInt(array.length);
        return collection.toArray(array)[index];
    }
    
    private long getTime() {
        return System.currentTimeMillis();
    }
    
    private boolean hasTimedOut(long startTime, long timeout) {
        return getTime() > startTime + timeout;
    }
    
    private boolean hasTimedOut(CommunicationPacket request, long timeout) {
        long sentTime = request.getSentTime();
        return sentTime>0 && hasTimedOut(sentTime, timeout);
    }
    
    /**
     * Returns up to <code>s</code> peers. If no peers were found, an empty
     * <code>List</code> is returned.
     * @return
     */
    public List<Destination> getResults() {
        List<Destination> resultsList = new ArrayList<Destination>();
        for (Destination destination: responses)
            resultsList.add(destination);
        Collections.sort(resultsList, new HashDistanceComparator(key));
        
        // trim the list to the k closest nodes
        if (resultsList.size() > KademliaConstants.S)
            resultsList = resultsList.subList(0, KademliaConstants.S);
        return resultsList;
    }

    // compares two Destinations in terms of closeness to <code>reference</code>
    private class HashDistanceComparator implements Comparator<Destination> {
        private Hash reference;
        
        public HashDistanceComparator(Hash reference) {
            this.reference = reference;
        }
        
        public int compare(Destination dest1, Destination dest2) {
            BigInteger dest1Distance = KademliaUtil.getDistance(dest1.calculateHash(), reference);
            BigInteger dest2Distance = KademliaUtil.getDistance(dest2.calculateHash(), reference);
            return dest1Distance.compareTo(dest2Distance);
        }
    };
    
    private class IncomingPacketHandler implements PacketListener {
        @Override
        public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
            if (packet instanceof ResponsePacket) {
                ResponsePacket responsePacket = (ResponsePacket)packet;
                FindClosePeersPacket request = getPacketById(pendingRequests.values(), responsePacket.getPacketId());   // find the request the node list is in response to
                
                // if the packet is in response to a pending request, update the three Sets
                if (request != null) {
                    responses.add(sender);
                    DataPacket payload = responsePacket.getPayload();
                    if (payload instanceof PeerList)
                        updatePeers((PeerList)payload, sender, receiveTime);
                    
                    pendingRequests.remove(sender);
                }
                else
                    log.debug("No Find Close Nodes packet found for Response Packet: " + responsePacket);
            }
            else if (packet instanceof MalformedCommunicationPacket)
                pendingRequests.remove(sender);   // since it is not generally possible to tell if an invalid comm packet is in response to a certain request, always remove invalid packets from the pending list
        }
        
        /**
         * Updates the <code>notQueriedYet</code> set with the peers from a <code>peerListPacket</code>,
         * and adds the <code>sender</code> to <code>responses</code>.
         * @param peerListPacket
         * @param sender
         * @param receiveTime
         */
        private void updatePeers(PeerList peerListPacket, Destination sender, long receiveTime) {
            log.debug("Peer List Packet received: #peers=" + peerListPacket.getPeers().size() + ", sender="+ sender.calculateHash());
            for (Destination peer: peerListPacket.getPeers())
                log.debug("  Peer: " + peer.calculateHash());

            // TODO make responseReceived and pendingRequests a parameter in the constructor?
            responses.add(sender);
            Collection<Destination> peersReceived = peerListPacket.getPeers();
            
            // add all peers from the PeerList, excluding those that we have already queried
            // TODO don't add local dest
            for (Destination peer: peersReceived)
                if (!pendingRequests.containsKey(peer) && !responses.contains(peer))
                    notQueriedYet.add(peer);   // this won't create duplicates because notQueriedYet is a Set
        }

        /**
         * Returns a packet that matches a given {@link UniqueId} from a {@link Collection} of packets, or
         * <code>null</code> if no match.
         * @param packets
         * @param packetId
         * @return
         */
        private FindClosePeersPacket getPacketById(Collection<FindClosePeersPacket> packets, UniqueId packetId) {
            for (FindClosePeersPacket packet: packets)
                if (packetId.equals(packet.getPacketId()))
                    return packet;
            return null;
        }
    };
}

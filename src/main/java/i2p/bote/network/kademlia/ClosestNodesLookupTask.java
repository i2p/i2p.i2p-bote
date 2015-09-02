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

import static i2p.bote.network.kademlia.KademliaConstants.K;
import i2p.bote.UniqueId;
import i2p.bote.Util;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * Queries the DHT for the {@link i2p.bote.network.kademlia.KademliaConstants#K}
 * peers closest to a given key.<br/>
 * The results are sorted by distance from the key.
 * <p/>
 * The number of pending requests never exceeds {@link KademliaConstants#ALPHA}.
 * According to the <code>infocom-2006-kad.pdf</code> paper (see
 * {@link i2p.bote.network.kademlia.KademliaDHT}), this is the most efficient.
 */
public class ClosestNodesLookupTask implements Callable<List<Destination>> {
    private static final int REQUEST_TIMEOUT = 30 * 1000;
    private static final int CLOSEST_NODES_LOOKUP_TIMEOUT = 5 * 60 * 1000;   // the maximum amount of time a FIND_CLOSEST_NODES can take
    
    private Log log = new Log(ClosestNodesLookupTask.class);
    private Hash key;
    private I2PPacketDispatcher i2pReceiver;
    private BucketManager bucketManager;
    private I2PSendQueue sendQueue;
    private Destination localDestination;   // The I2P destination of the local node
    private Comparator<Destination> peerComparator;
    private SortedSet<Destination> responses;   // sorted by distance to the key to look up
    private SortedSet<Destination> notQueriedYet;   // peers that are yet to be queried; sorted by distance to the key to look up
    private Map<Destination, FindClosePeersPacket> pendingRequests;
    private long startTime;
    
    /**
     * @param key The DHT key to look up
     * @param sendQueue For sending I2P packets
     * @param i2pReceiver For receiving I2P packets
     * @param bucketManager For looking up peers, and updating them
     */
    public ClosestNodesLookupTask(Hash key, I2PSendQueue sendQueue, I2PPacketDispatcher i2pReceiver, BucketManager bucketManager) {
        this.key = key;
        this.sendQueue = sendQueue;
        localDestination = sendQueue.getLocalDestination();
        this.i2pReceiver = i2pReceiver;
        this.bucketManager = bucketManager;
        
        peerComparator = new HashDistanceComparator(key);
        responses = Collections.synchronizedSortedSet(new TreeSet<Destination>(peerComparator));   // nodes that have responded to a query
        notQueriedYet = Collections.synchronizedSortedSet(new TreeSet<Destination>(peerComparator));   // peers we haven't contacted yet
        pendingRequests = new ConcurrentHashMap<Destination, FindClosePeersPacket>();   // outstanding queries
    }
    
    public List<Destination> call() throws InterruptedException {
        log.debug("Looking up nodes closest to " + key);
        
        PacketListener packetListener = new IncomingPacketHandler();
        i2pReceiver.addPacketListener(packetListener);
        
        try {
            // get a list of all unlocked peers (we don't how many we really need because some may not respond)
            notQueriedYet.addAll(bucketManager.getAllUnlockedPeers());
            logStatus();
            
            startTime = getTime();
            do {
                // send new requests if less than alpha are pending
                while (pendingRequests.size()<KademliaConstants.ALPHA && !notQueriedYet.isEmpty()) {
                    Destination peer = notQueriedYet.first();   // query the closest unqueried peer
                    notQueriedYet.remove(peer);
                    // if the peer is us, do a local lookup; otherwise, send a request to the peer
                    if (localDestination.equals(peer))
                        addLocalResults(key);
                    else {
                        FindClosePeersPacket packet = new FindClosePeersPacket(key);
                        pendingRequests.put(peer, packet);
                        sendQueue.send(packet, peer);
                    }
                    logStatus();
                }
    
                // handle timeouts
                for (Map.Entry<Destination, FindClosePeersPacket> request: pendingRequests.entrySet())
                    if (hasTimedOut(request.getValue(), REQUEST_TIMEOUT)) {
                        Destination peer = request.getKey();
                        log.debug("FindCloseNodes request to peer " + Util.toShortenedBase32(peer) + " timed out.");
                        bucketManager.noResponse(peer);
                        pendingRequests.remove(peer);
                    }
                
                TimeUnit.SECONDS.sleep(1);
            } while (!isDone());
            log.debug("Node lookup for " + key + " found " + responses.size() + " nodes (may include local node).");
            synchronized (responses) {
                Iterator<Destination> i = responses.iterator();
                while (i.hasNext())
                    log.debug("  Node: " + Util.toBase32(i.next()));
            }
        }
        finally {
            i2pReceiver.removePacketListener(packetListener);
        }
        
        return getResults();
    }
    
    private void logStatus() {
        log.debug("Lookup status for key " + key.toBase64().substring(0, 8) + "...: resp=" + responses.size() +" pend=" + pendingRequests.size() + " notQ=" + notQueriedYet.size());
    }
    
    private boolean isDone() {
        // if there are no more requests to send, and no more responses to wait for, we're finished
        if (pendingRequests.isEmpty() && notQueriedYet.isEmpty())
            return true;
        
        // if we have received responses from the k closest peers, we're also finished
        Destination kthClosestResult = null;
        synchronized(responses) {
            if (responses.size() >= K) {
                Destination[] responsesArr = responses.toArray(new Destination[0]);
                kthClosestResult = responsesArr[K-1];
            }
        }
        if (kthClosestResult != null) {
            Destination closestUnqueriedPeer = null;
            synchronized(notQueriedYet) {
                if (notQueriedYet.isEmpty())
                    return true;
                closestUnqueriedPeer = notQueriedYet.first();
            }
            if (peerComparator.compare(kthClosestResult, closestUnqueriedPeer) <= 0)
                return true;
        }
        
        if (hasTimedOut(startTime, CLOSEST_NODES_LOOKUP_TIMEOUT)) {
            log.debug("Lookup for closest nodes timed out.");
            return true;
        }
        
        // Check for thread interruption
        if (Thread.currentThread().isInterrupted())
            return true;
        
        return false;
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
     * Returns up to {@link i2p.bote.network.kademlia.KademliaConstants#K} peers,
     * sorted by distance from the key.<br/>
     * The list may contain the local node if it is among the <code>k</code>
     * closest.<br/>
     * If no peers were found, an empty <code>List</code> is returned.
     */
    private List<Destination> getResults() {
        List<Destination> resultsList = new ArrayList<Destination>();
        synchronized (responses) {
            Iterator<Destination> i = responses.iterator();
            while (i.hasNext()) {
                resultsList.add(i.next());
                if (resultsList.size() >= K)
                    break;
            }
        }
        return resultsList;
    }
    
    /**
     * Updates <code>notQueriedYet</code> with the <code>k</code> closest locally known peers.<br/>
     * This has the the same effect as sending a <code>FindClosePeersPacket</code> to the local destination,
     * but without the network round-trip.
     * @param key
     * @see IncomingPacketHandler#packetReceived(CommunicationPacket, Destination, long)
     */
    private void addLocalResults(Hash key) {
        log.debug("Adding local results for key " + key.toBase64());
        responses.add(localDestination);
        Collection<Destination> closestPeers = bucketManager.getClosestPeers(key, KademliaConstants.K);
        addPeersToBeQueried(closestPeers);
    }
    
    /**
     * Adds peers to <code>notQueriedYet</code> (the list of peers that need to be queried), excluding those
     * that have already responded.
     * @param peers
     */
    private void addPeersToBeQueried(Collection<Destination> peers) {
        for (Destination peer: peers)
            if (!pendingRequests.containsKey(peer) && !responses.contains(peer))
                notQueriedYet.add(peer);   // this won't create duplicates because notQueriedYet is a Set
    }
    
    // compares two Destinations in terms of closeness to <code>reference</code>
    private static class HashDistanceComparator implements Comparator<Destination> {
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
                synchronized(pendingRequests) {
                    FindClosePeersPacket request = getPacketById(pendingRequests.values(), responsePacket.getPacketId());   // find the request the node list is in response to
                    
                    // if the packet is in response to a pending request, update responses + notQueriedYet + pendingRequests
                    if (request != null) {
                        log.debug("Response to FindCloseNodesPacket received from " + Util.toShortenedBase32(sender));
                        responses.add(sender);
                        DataPacket payload = responsePacket.getPayload();
                        if (payload instanceof PeerList)
                            updatePeers((PeerList)payload, sender, receiveTime);
                        
                        pendingRequests.remove(sender);
                    }
                }
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
            log.debug("Peer List Packet received: #peers=" + peerListPacket.getPeers().size() + ", sender="+ Util.toShortenedBase32(sender));

            // update the list of peers to query
            Collection<Destination> peersReceived = peerListPacket.getPeers();
            addPeersToBeQueried(peersReceived);
        }

        /**
         * Returns a <code>FindClosePeersPacket</code> that matches a given {@link UniqueId}
         * from a {@link Collection} of packets, or <code>null</code> if no match.
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
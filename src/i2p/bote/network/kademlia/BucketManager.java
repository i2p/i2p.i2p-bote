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

import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

// TODO if a sibling times out, refill the sibling table
public class BucketManager implements PacketListener, Iterable<KBucket> {
    private Log log = new Log(BucketManager.class);
    private List<KBucket> kBuckets;
    private KBucket siblingBucket;   // TODO [ordered furthest away to closest in terms of hash distance to local node]
    private Hash localDestinationHash;

    public BucketManager(Hash localDestinationHash) {
        this.localDestinationHash = localDestinationHash;
        kBuckets = Collections.synchronizedList(new ArrayList<KBucket>());
        kBuckets.add(new KBucket(KBucket.MIN_HASH_VALUE, KBucket.MAX_HASH_VALUE, KademliaConstants.K, 0, true));   // this is the root bucket, so depth=0
        siblingBucket = new KBucket(KBucket.MIN_HASH_VALUE, KBucket.MAX_HASH_VALUE, KademliaConstants.S, 0, false);
    }
    
    public void addAll(Collection<KademliaPeer> nodes) {
        for (KademliaPeer node: nodes)
            addOrUpdate(node);
    }
    
    /**
     * Add a <code>{@link KademliaPeer}</code> to the sibling list or a bucket.
     * @param peer
     */
    public void addOrUpdate(KademliaPeer peer) {
        if (localDestinationHash.equals(peer.getDestinationHash())) {
            log.debug("Not adding local destination to bucket.");
            return;
        }
        
        Hash peerHash = peer.getDestinationHash();
        log.debug("Adding/updating peer: Hash = " + peerHash);

        peer.resetStaleCounter();
        
        synchronized(this) {
            if (!siblingBucket.isFull() || siblingBucket.contains(peer)) {
                siblingBucket.addOrUpdate(peer);
                getBucket(peerHash).remove(peer);
            }
            else if (isCloserSibling(peer)) {
                KademliaPeer ejectedPeer = siblingBucket.getMostDistantPeer(localDestinationHash);
                
                addToBucket(ejectedPeer);
                siblingBucket.remove(ejectedPeer);
                
                siblingBucket.addOrUpdate(peer);
                getBucket(peerHash).remove(peer);
            }
            else
                addToBucket(peer);
        }
        logBucketStats();
            
/*        KademliaPeer ejectedPeer = addSibling(peer);
        // if the peer was added as a sibling, it may need to be removed from a bucket
        if (ejectedPeer != peer)
            getBucket(peerHash).remove(peer);
        // if the peer didn't get added as a sibling, try a bucket
        else
            addToBucket(peer);
        // if adding the peer to the list of siblings replaced another sibling, add the old sibling to a bucket
        if (ejectedPeer != null)
            addToBucket(ejectedPeer);*/
        
/*TODO        synchronized(siblings) {
            if (siblings.isFull()) {
                KBucket bucket = getBucket(nodeHash);
                KademliaPeer mostDistantSibling = getMostDistantSibling();
                if (getDistance(node.getDestinationHash()) < getDistance(mostDistantSibling)) {
                    bucket.addOrUpdate(mostDistantSibling);
                    siblings.remove(mostDistantSibling);
                    siblings.add(node);
                }
                else
                    bucket.addOrUpdate(node);
            }
            else {
                siblings.add(node);
        }*/
        
    }

    private void logBucketStats() {
        int numBuckets = kBuckets.size();
        int numPeers = getAllPeers().size();
        int numSiblings = siblingBucket.size();
        
        log.debug("total #peers=" + numPeers + ", #siblings=" + numSiblings + ", #buckets=" + numBuckets + " (not counting the sibling bucket)");
    }
    
    /**
     * Add a <code>{@link KademliaPeer}</code> to the appropriate bucket.
     * @param peer
     */
    private void addToBucket(KademliaPeer peer) {
        Hash nodeHash = peer.getDestinationHash();
        KBucket bucket = getBucket(nodeHash);
        KBucket newBucket = bucket.addOrSplit(peer);
        if (newBucket != null)
            kBuckets.add(newBucket);
    }

    /**
     * Return <code>true</code> if a given peer is closer to the local node than at
     * least one sibling. In other words, test if <code>peer</code> should replace
     * an existing sibling.
     * @param peer
     * @return
     */
    private boolean isCloserSibling(KademliaPeer peer) {
        BigInteger peerDistance = KademliaUtil.getDistance(peer, localDestinationHash);
        for (KademliaPeer sibling: siblingBucket) {
            BigInteger siblingDistance = KademliaUtil.getDistance(sibling.getDestinationHash(), localDestinationHash);
            if (peerDistance.compareTo(siblingDistance) < 0)
                return true;
        }
        return false;
    }
    
    /**
     * Add a peer to the sibling list if the list is not full, or if there is a node that can be
     * replaced.
     * 
     * If <code>peer</code> replaced an existing sibling, that sibling is returned.
     * If <code>peer</code> could not be added to the list, <code>peer</code> is returned.
     * If the list was not full, <code>null</code> is returned.
     * @param peer
     * @return
     */
/*    private KademliaPeer addSibling(KademliaPeer peer) {
        // no need to handle a replacement cache because the sibling bucket has none.
        KademliaPeer mostDistantSibling = siblingBucket.getMostDistantPeer(localDestinationHash);
        if (!siblingBucket.isFull()) {
            siblingBucket.add(peer);
            return null;
        }
        else if (new PeerDistanceComparator(localDestinationHash).compare(peer, mostDistantSibling) < 0) {
            siblingBucket.remove(mostDistantSibling);
            siblingBucket.add(peer);
            return mostDistantSibling;
        }
        else
            return peer;
    }*/
    
    public void remove(KademliaPeer peer) {
        Hash nodeHash = peer.getDestinationHash();
        getBucket(nodeHash).remove(peer);
    }
    
    /**
     * Do a binary search for the index of the bucket whose key range contains a given {@link Hash}.
     * @param key
     * @return
     */
    private int getBucketIndex(Hash key) {
        // initially, the search interval is 0..n-1
        int lowEnd = 0;
        int highEnd = kBuckets.size();
        
        BigInteger keyValue = new BigInteger(key.getData());
        while (lowEnd < highEnd) {
            int centerIndex = (highEnd + lowEnd) / 2;
            if (keyValue.compareTo(kBuckets.get(centerIndex).getStartId()) < 0)
                highEnd = centerIndex;
            else if (keyValue.compareTo(kBuckets.get(centerIndex).getEndId()) > 0)
                lowEnd = centerIndex;
            else
                return centerIndex;
        }
     
        log.error("This should never happen! No k-bucket found for hash: " + key);
        return -1;
    }
    
    /**
     * Do a binary search for the bucket whose key range contains a given {@link Hash}.
     * @param key
     * @return
     */
    private KBucket getBucket(Hash key) {
        return kBuckets.get(getBucketIndex(key));
    }
    
    /**
     * Return the <code>count</code> peers that are closest to a given key.
     * Less than <code>count</code> peers may be returned if there aren't
     * enough peers in the k-buckets.
     * @param key
     * @param count
     * @return
     */
    public Collection<KademliaPeer> getClosestPeers(Hash key, int count) {
        Collection<KademliaPeer> closestPeers = new ConcurrentHashSet<KademliaPeer>();
        
        // TODO don't put all peers in one huge list, only use two buckets at a time
        KademliaPeer[] allPeers = getAllPeersSortedByDistance(key);
        
        for (int i=0; i<count && i<allPeers.length; i++)
            closestPeers.add(allPeers[i]);
        
        return closestPeers;
    }

    private KademliaPeer[] getAllPeersSortedByDistance(Hash key) {
        List<KademliaPeer> allPeers = getAllPeers();
        KademliaPeer[] peerArray = getAllPeers().toArray(new KademliaPeer[allPeers.size()]);
        Arrays.sort(peerArray, new PeerDistanceComparator(key));
        return peerArray;
    }
    
    public List<KademliaPeer> getAllPeers() {
        List<KademliaPeer> allPeers = new ArrayList<KademliaPeer>();
        for (KBucket bucket: kBuckets)
            allPeers.addAll(bucket.getNodes());
        allPeers.addAll(siblingBucket.getNodes());
        return allPeers;
    }
    
    /**
     * Return all siblings of the local node (siblings are an S/Kademlia feature).
     * @return
     */
    public List<KademliaPeer> getSiblings() {
        List<KademliaPeer> siblingDestinations = new ArrayList<KademliaPeer>();
        for (KademliaPeer sibling: siblingBucket)
            siblingDestinations.add(sibling);
        return siblingDestinations;
    }

    /**
     * Return the total number of known Kademlia peers.
     * @return
     */
    public int getPeerCount() {
        int count = 0;
        for (KBucket bucket: kBuckets)
            count += bucket.size();
        count += siblingBucket.size();
        return count;
    }
    
    // PacketListener implementation
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        // any type of incoming packet updates the peer's record in the bucket/sibling list, or adds the peer to the bucket/sibling list
        addOrUpdate(new KademliaPeer(sender, receiveTime));
  }

    private class PeerDistanceComparator implements Comparator<KademliaPeer> {
        private Hash reference;
        
        PeerDistanceComparator(Hash reference) {
            this.reference = reference;
        }
        
        @Override
        public int compare(KademliaPeer peer1, KademliaPeer peer2) {
            BigInteger distance1 = KademliaUtil.getDistance(peer1.getDestinationHash(), reference);
            BigInteger distance2 = KademliaUtil.getDistance(peer2.getDestinationHash(), reference);
            return distance1.compareTo(distance2);
        }
    }

    /**
     * Iterates over the k-buckets. Does not include the sibling bucket.
     * @return
     */
    @Override
    public Iterator<KBucket> iterator() {
        return kBuckets.iterator();
    }
}
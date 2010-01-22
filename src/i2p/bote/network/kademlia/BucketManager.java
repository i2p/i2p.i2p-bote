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
    private SBucket sBucket;   // The sibling bucket [TODO ordered furthest away to closest in terms of hash distance to local node]
    private Hash localDestinationHash;

    public BucketManager(Hash localDestinationHash) {
        this.localDestinationHash = localDestinationHash;
        kBuckets = Collections.synchronizedList(new ArrayList<KBucket>());
        kBuckets.add(new KBucket(AbstractBucket.MIN_HASH_VALUE, AbstractBucket.MAX_HASH_VALUE, KademliaConstants.K, 0));   // this is the root bucket, so depth=0
        sBucket = new SBucket(KademliaConstants.S);
    }
    
    /**
     * Calls <code>addOrUpdate(KademliaPeer)</code> for one or more peers.
     * @param peers
     */
    public void addAll(Collection<KademliaPeer> peers) {
        for (KademliaPeer node: peers)
            addOrUpdate(node);
    }
    
    /**
     * Adds a <code>{@link KademliaPeer}</code> to the s-bucket or a k-bucket,
     * depending on its distance to the local node.
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
            if (!sBucket.isFull() || sBucket.contains(peer)) {
                sBucket.addOrUpdate(peer);
                getKBucket(peerHash).remove(peer);
            }
            else if (isCloserSibling(peer)) {
                Destination ejectedPeer = sBucket.getMostDistantPeer(localDestinationHash);
                
                addToKBucket(ejectedPeer);
                sBucket.remove(ejectedPeer);
                
                sBucket.addOrUpdate(peer);
                getKBucket(peerHash).remove(peer);
            }
            else
                addToKBucket(peer);
        }
        logBucketStats();
    }

    private void addToKBucket(Destination destination) {
        KBucket bucket = getKBucket(destination);
        KBucket newBucket = bucket.addOrSplit(destination);
        if (newBucket != null)
            kBuckets.add(newBucket);
    }

    public void incrementStaleCounter(Destination destination) {
        AbstractBucket bucket = getBucket(destination);
        if (bucket != null) {
            KademliaPeer peer = bucket.getPeer(destination);
            if (peer != null) {
                peer.incrementStaleCounter();
                return;
            }
        }
        log.debug("Can't increment stale counter because peer not found in buckets: " + destination.calculateHash());
    }
    
    private void logBucketStats() {
        int numBuckets = kBuckets.size();
        int numPeers = getAllPeers().size();
        int numSiblings = sBucket.size();
        
        log.debug("total #peers=" + numPeers + ", #siblings=" + numSiblings + ", #buckets=" + numBuckets + " (not counting the sibling bucket)");
    }
    
    /**
     * Adds a <code>{@link Destination}</code> to the appropriate bucket.
     * @param destination
     */
    /**
     * Return <code>true</code> if a given peer is closer to the local node than at
     * least one sibling. In other words, test if <code>peer</code> should replace
     * an existing sibling.
     * @param peer
     * @return
     */
    private boolean isCloserSibling(KademliaPeer peer) {
        BigInteger peerDistance = KademliaUtil.getDistance(peer, localDestinationHash);
        for (KademliaPeer sibling: sBucket) {
            BigInteger siblingDistance = KademliaUtil.getDistance(sibling.getDestinationHash(), localDestinationHash);
            if (peerDistance.compareTo(siblingDistance) < 0)
                return true;
        }
        return false;
    }
    
    public void remove(KademliaPeer peer) {
        getBucket(peer).remove(peer);
    }
    
    /**
     * Does a binary search for the index of the k-bucket whose key range contains
     * a given {@link Hash}.
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
     * Does a binary search for the k-bucket whose key range contains a given
     * {@link Hash}.
     * The bucket may or may not contain a peer with that hash.
     * @param key
     * @return
     */
    private KBucket getKBucket(Hash key) {
        return kBuckets.get(getBucketIndex(key));
    }
    
    /**
     * Does a binary search for the k-bucket whose key range contains a given
     * {@link Destination}.
     * The bucket may or may not contain the peer.
     * @param key
     * @return
     */
    private KBucket getKBucket(Destination destination) {
        return getKBucket(destination.calculateHash());
    }
    
    /**
     * Returns the (s or k) bucket that contains a given {@link Destination}.
     * The s-bucket is checked first, then the k-buckets.
     * If no bucket contains the peer, <code>null</code> is returned.
     * @param destination
     * @return
     */
    private AbstractBucket getBucket(Destination destination) {
        if (sBucket.contains(destination))
            return sBucket;
        else {
            KBucket kBucket = getKBucket(destination.calculateHash());
            if (kBucket.contains(destination))
                return kBucket;
            else
                return null;
        }
    }
    
    /**
     * Return the <code>count</code> peers that are closest to a given key.
     * Less than <code>count</code> peers may be returned if there aren't
     * enough peers in the k-buckets and the s-bucket.
     * @param key
     * @param count
     * @return
     */
    public Collection<Destination> getClosestPeers(Hash key, int count) {
        Collection<Destination> closestPeers = new ConcurrentHashSet<Destination>();
        
        // TODO don't put all peers in one huge list, only use two buckets at a time
        Destination[] allPeers = getAllPeersSortedByDistance(key);
        
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
            allPeers.addAll(bucket.getPeers());
        allPeers.addAll(sBucket.getPeers());
        return allPeers;
    }
    
    /**
     * Return all siblings of the local node (siblings are an S/Kademlia feature).
     * @return
     */
    public List<KademliaPeer> getSiblings() {
        List<KademliaPeer> siblingDestinations = new ArrayList<KademliaPeer>();
        for (KademliaPeer sibling: sBucket)
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
        count += sBucket.size();
        return count;
    }
    
    // PacketListener implementation
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        // any type of incoming packet updates the peer's record in the bucket/sibling list, or adds the peer to the bucket/sibling list
        addOrUpdate(new KademliaPeer(sender, receiveTime));
  }

    private class PeerDistanceComparator implements Comparator<Destination> {
        private Hash reference;
        
        PeerDistanceComparator(Hash reference) {
            this.reference = reference;
        }
        
        @Override
        public int compare(Destination peer1, Destination peer2) {
            BigInteger distance1 = KademliaUtil.getDistance(peer1.calculateHash(), reference);
            BigInteger distance2 = KademliaUtil.getDistance(peer2.calculateHash(), reference);
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
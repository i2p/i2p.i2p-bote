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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * An {@link AbstractBucket} that:
 *  * can be split in two, and
 *  * maintains a replacement cache.
 *  
 * Peers are kept in an <code>ArrayList</code>. New peers are added at
 * index 0. When a peer is updated, it is moved to index 0.
 * When the bucket needs to make room for a new peer, the peer at the
 * highest index (the least recently seen one) is dropped.
 * This effectively sorts peers by the time of most recent communication.
 * 
 * The replacement cache is handled the same way.
 * 
 * TODO use peers from the replacement cache when the bucket is not full
 */
class KBucket extends AbstractBucket {
    private static final int REPLACEMENT_CACHE_MAX_SIZE = KademliaConstants.K;
    
    private Log log = new Log(KBucket.class);
    private BigInteger startId;
    private BigInteger endId;
    private List<KademliaPeer> replacementCache;
    private int depth;

    // capacity - The maximum number of peers the bucket can hold
    KBucket(BigInteger startId, BigInteger endId, int capacity, int depth) {
        super(capacity);
        this.startId = startId;
        this.endId = endId;
        replacementCache = Collections.synchronizedList(new ArrayList<KademliaPeer>());
        this.depth = depth;
    }
    
    synchronized BigInteger getStartId() {
        return startId;
    }
    
    synchronized BigInteger getEndId() {
        return endId;
    }
    
    synchronized void add(Destination destination) {
        if (isFull())
            log.error("Error: adding a node to a full k-bucket. Bucket needs to be split first. Size=" + size() + ", capacity=" + capacity);
        
        KademliaPeer peer = getPeer(destination);
        if (peer == null)
            peers.add(new KademliaPeer(destination));
    }
    
    /**
     * Adds a node to the bucket, splitting the bucket if necessary, or updates the node
     * if it exists in the bucket. If the bucket is split, the newly created bucket is
     * returned. Otherwise, <code>null</code> is returned.
     * 
     * If the bucket is full but cannot be split, the new node is added to the replacement
     * cache and <code>null</code> is returned.
     * @param destination
     * @return
     */
    synchronized KBucket addUpdateOrSplit(Destination destination) {
        if (!rangeContains(destination))
            log.error("Attempt to add a node whose hash is outside the bucket's range! Bucket start=" + startId + " Bucket end=" + endId + " peer hash=" + new BigInteger(1, destination.calculateHash().getData()));

        KademliaPeer peer = getPeer(destination);
        
        if (isFull() && peer==null) {   // if bucket full and bucket doesn't contain peer, split the bucket if possible
            if (canSplit(destination)) {
                KBucket newBucket = split();
                if (rangeContains(destination))
                    add(destination);
                else if (newBucket.rangeContains(destination))
                    newBucket.add(destination);
                else
                    log.error("After splitting a bucket, node is outside of both buckets' ranges.");
                return newBucket;
            }
            else {
                addOrUpdateReplacement(new KademliaPeer(destination));
                return null;
            }
        }
        else {   // no splitting needed
            addOrUpdate(destination);
            return null;
        }
    }
    
    /**
     * Updates a known peer, or adds the peer if it isn't known. If the bucket
     * is full, the oldest peer is removed before adding the new peer.
     * @param destination
     * @return The peer that was removed from the bucket, or <code>null</code>
     * if no peer was removed.
     */
    private Destination addOrUpdate(Destination destination) {
        // TODO log an error if peer outside bucket's range
        int index = getPeerIndex(destination);
        if (index >= 0) {
            KademliaPeer peer = peers.remove(index);
            peers.add(0, peer);
            peer.resetStaleCounter();
            return null;
        }
        else {
            KademliaPeer removedPeer = null;
            if (isFull())
                removedPeer = peers.remove(peers.size() - 1);
            peers.add(0, new KademliaPeer(destination));
            return removedPeer;
        }
    }

    private void addOrUpdateReplacement(KademliaPeer peer) {
        replacementCache.add(0, peer);
        while (replacementCache.size() > REPLACEMENT_CACHE_MAX_SIZE)
            replacementCache.remove(REPLACEMENT_CACHE_MAX_SIZE-1);
    }
    
    /**
     * Returns <code>true</code> if the bucket should be split in order to make room for a new peer.
     * @return
     */
    private boolean canSplit(Destination peer) {
        return depth%KademliaConstants.B!=0 || rangeContains(peer);
    }
    
    /**
     * Returns <code>true</code> if the bucket's Id range contains the hash of a given
     * peer, regardless if the bucket contains the peer; <code>false</code> if the hash
     * is outside the range.
     * @param peer
     * @return
     */
    private boolean rangeContains(Destination peer) {
        BigInteger peerHash = new BigInteger(1, peer.calculateHash().getData());
        return (startId.compareTo(peerHash)<=0 && endId.compareTo(peerHash)>=0);
    }
    
    /**
     * Splits the bucket in two by moving peers to a new bucket. The existing bucket
     * retains all peers whose keys have a 0 at the <code>depth</code>-th bit; the
     * new bucket will contain all peers for which the <code>depth</code>-th bit is 1.
     * In other words, the bucket is split into two sub-branches in the Kademlia
     * tree, with the old bucket representing the left branch and the new bucket
     * representing the right branch.
     * @return The new bucket
     * @see split(BigInteger)
     */
    KBucket split() {
        // pivot has the depth-th highest bit set to 1, all bit indices lower than depth set to 0
        BigInteger pivot = startId.setBit(Hash.HASH_LENGTH*8-1-depth);
        for (int i=0; i<depth; i++)
            pivot = pivot.clearBit(i);
        
        return split(pivot);
    }

    /**
     * Splits the bucket in two by keeping all peers with a DHT key less than
     * <code>pivot</code> in the existing bucket, and moving the rest into a new bucket.
     * @param pivot
     * @return The new bucket (which contains the higher IDs)
     */
    private KBucket split(BigInteger pivot) {
        depth++;
        KBucket newBucket = new KBucket(pivot, endId, capacity, depth);
        endId = pivot.subtract(BigInteger.ONE);
        for (int i=peers.size()-1; i>=0; i--) {
            KademliaPeer peer = peers.get(i);
            BigInteger nodeId = new BigInteger(1, peer.getDestinationHash().getData());
            if (nodeId.compareTo(pivot) >= 0) {
                newBucket.add(peer);
                remove(peer);
            }
        }
        return newBucket;
    }
}
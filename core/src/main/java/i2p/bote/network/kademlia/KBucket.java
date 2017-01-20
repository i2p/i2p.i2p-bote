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
import java.util.Date;
import java.util.List;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * An {@link AbstractBucket} that:
 *  * can be split in two,
 *  * has a start and end Kademlia ID,
 *  * knows its depth in the bucket tree, and
 *  * maintains a replacement cache.
 * 
 * Peers are kept in an <code>ArrayList</code>. Active peers are added
 * at index 0. When a peer is updated, it is moved to index 0.
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
    private List<KademliaPeer> replacementCache;   // Basically a FIFO. Peers are sorted most recently seen to least recently seen
    private volatile int depth;
    private volatile long lastLookupTime;

    KBucket(BigInteger startId, BigInteger endId, int depth) {
        super(KademliaConstants.K);
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
    
    /**
     * @param lastLookupTime
     * @see getLastLookupTime
     */
    public void setLastLookupTime(long lastLookupTime) {
        this.lastLookupTime = lastLookupTime;
    }

    /**
     * Returns the time at which a closest nodes lookup for a key in this
     * bucket's range was last performed.
     * @return
     */
    public long getLastLookupTime() {
        return lastLookupTime;
    }

    /**
     * Returns <code>true</code> if the bucket needs to, AND can be split
     * so a given <code>Destination</code> can be added.
     */
    boolean shouldSplit(Destination destination) {
        return isFull() && !contains(destination) && canSplit(destination);
    }
    
    /**
     * Returns <code>true</code> if the bucket can be split in order to make room for a new peer.
     * @return
     */
    private boolean canSplit(Destination destination) {
        return depth%KademliaConstants.B!=0 || rangeContains(destination);
    }
    
    /**
     * Updates a known peer, or adds the peer if it isn't known. If the bucket
     * is full and the replacement cache is not empty, the oldest peer is removed
     * before adding the new peer.
     * If the bucket is full and the replacement cache is empty, the peer is
     * added to the replacement cache.
     * @param peer
     */
    void addOrUpdate(KademliaPeer peer) {
        // TODO log an error if peer outside bucket's range
        int index = getPeerIndex(peer);
        if (index >= 0) {
            KademliaPeer existingPeer = peers.remove(index);
            existingPeer.responseReceived();
            add(existingPeer);
        }
        else {
            if (!isFull())
                add(peer);
            else
                addOrUpdateReplacement(peer);
        }
    }

    /**
     * Adds a peer to the tail of the bucket if it is locked, or to the
     * head of the bucket if it isn't locked.
     * The bucket cannot be full when calling this method.
     * @param peer
     */
    private void add(KademliaPeer peer) {
        if (isFull())
            log.error("Error: adding a node to a full k-bucket. Bucket needs to be split first. Size=" + size() + ", capacity=" + capacity);
        
        if (peer.isLocked())
            peers.add(peer);
        else
            peers.add(0, peer);
    }
    
    /**
     * Adds a peer to the head of the replacement cache, or makes
     * it the head if it exists in the replacement cache.
     * @param peer
     */
    private void addOrUpdateReplacement(KademliaPeer peer) {
        if (replacementCache.contains(peer))
            replacementCache.remove(peer);
        replacementCache.add(0, peer);
        
        while (replacementCache.size() > REPLACEMENT_CACHE_MAX_SIZE)
            replacementCache.remove(REPLACEMENT_CACHE_MAX_SIZE-1);
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
     * Splits the bucket in two equal halves (only in terms of ID range, the number
     * of elements in the two buckets may differ) and moves peers to the new bucket
     * if necessary.<br/>
     * The existing bucket retains the lower IDs; the new bucket will contain the
     * higher IDs.<br/>
     * In other words, the bucket is split into two sub-branches in the Kademlia
     * tree, with the old bucket representing the left branch and the new bucket
     * representing the right branch.
     * @return The new bucket
     * @see split(BigInteger)
     */
    KBucket split() {
        BigInteger pivot = startId.add(endId).divide(BigInteger.valueOf(2));
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
        KBucket newBucket = new KBucket(pivot, endId, depth);
        endId = pivot;
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
    
    /**
     * Returns the common prefix shared by the binary representation of all peers in the bucket.
     * @return
     */
    String getBucketPrefix() {
        if (depth == 0)
            return "(None)";
        
        String binary = startId.toString(2);
        while (binary.length() < Hash.HASH_LENGTH*8)
            binary = "0" + binary;
        return binary.substring(0, depth);
    }
    
    @Override
    public String toString() {
        return "K-Bucket (depth=" + depth + ", prefix=" + getBucketPrefix() + ", lastLookup=" + new Date(lastLookupTime) + ", start=" + startId + ", end=" + endId + ")";
    }
}
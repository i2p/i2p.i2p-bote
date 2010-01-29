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
import java.util.Collection;
import java.util.Set;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

class KBucket extends AbstractBucket {
    private static final int REPLACEMENT_CACHE_MAX_SIZE = KademliaConstants.K;
    
    private Log log = new Log(KBucket.class);
    private BigInteger startId;
    private BigInteger endId;
    private Set<KademliaPeer> replacementCache;
    private int depth;

    // capacity - The maximum number of peers the bucket can hold
    KBucket(BigInteger startId, BigInteger endId, int capacity, int depth) {
        super(capacity);
        this.startId = startId;
        this.endId = endId;
        replacementCache = new ConcurrentHashSet<KademliaPeer>();
        this.depth = depth;
    }
    
    BigInteger getStartId() {
        return startId;
    }
    
    BigInteger getEndId() {
        return endId;
    }
    
    void add(Destination destination) {
        if (isFull())
            log.error("Error: adding a node to a full k-bucket. Bucket needs to be split first. Size=" + size() + ", capacity=" + capacity);
        
        KademliaPeer peer = getPeer(destination);
        if (peer == null)
            peers.add(peer);
    }
    
    /**
     * Adds a node to the bucket, splitting the bucket if necessary. If the bucket is split,
     * the newly created bucket is returned. Otherwise, <code>null</code> is returned.
     * 
     * If the bucket is full but cannot be split, the new node is added to the replacement
     * cache and <code>null</code> is returned.
     * @param destination
     * @return
     */
    synchronized KBucket addOrSplit(Destination destination) {
        if (!rangeContains(destination))
            log.error("Attempt to add a node whose hash is outside the bucket's range! Bucket start=" + startId + " Bucket end=" + endId + " peer hash=" + new BigInteger(destination.calculateHash().getData()));

        KademliaPeer peer = getPeer(destination);
        
        if (isFull() && peer==null) {   // if bucket full and bucket doesn't contain peer, split the bucket if possible
            if (canSplit(destination)) {
                KBucket newBucket = split(destination.calculateHash());
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
        else {
            // if the peer exists in the bucket already, update
            if (peer == null)
                peers.add(new KademliaPeer(destination, System.currentTimeMillis()));
            else
                peer.setLastReception(System.currentTimeMillis());
            return null;
        }
    }
    
    private void addOrUpdateReplacement(KademliaPeer peer) {
        replacementCache.add(peer);
        while (replacementCache.size() > REPLACEMENT_CACHE_MAX_SIZE)
            removeOldest(replacementCache);
    }
    
    private void removeOldest(Collection<KademliaPeer> peers) {
        KademliaPeer oldestPeer = null;
        for (KademliaPeer peer: peers)
            if (oldestPeer==null || peer.getLastReception()<oldestPeer.getLastReception())
                oldestPeer = peer;
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
        BigInteger peerHash = new BigInteger(peer.calculateHash().getData());
        return (startId.compareTo(peerHash)<=0 || endId.compareTo(peerHash)>0);
    }
    
    /**
     * Splits the bucket in two by moving all peers with a DHT key less than <code>hash</code> into one
     * bucket, and the rest in the other.
     * @return The new bucket
     */
    KBucket split(Hash hash) {
        return split(new BigInteger(hash.toBase64()));
    }
    
    KBucket split(BigInteger pivot) {
        depth++;
        KBucket newBucket = new KBucket(startId, pivot.subtract(BigInteger.ONE), capacity, depth);
        startId = pivot;
        for (KademliaPeer peer: peers) {
            BigInteger nodeId = new BigInteger(peer.getDestinationHash().getData());
            if (nodeId.compareTo(pivot) >= 0) {
                newBucket.add(peer);
                remove(peer);
            }
        }
        return newBucket;
    }
}
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

import i2p.bote.Util;
import i2p.bote.network.BanList;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.DhtPeerStatsRenderer;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * The k-bucket tree isn't actually implemented as a tree, but a {@link List}.
 */
class BucketManager implements PacketListener, Iterable<KBucket> {
    private Log log = new Log(BucketManager.class);
    private List<KBucket> kBuckets;
    private SBucket sBucket;   // The sibling bucket
    private Hash localDestinationHash;

    public BucketManager(Hash localDestinationHash) {
        this.localDestinationHash = localDestinationHash;
        kBuckets = new CopyOnWriteArrayList<KBucket>();
        kBuckets.add(new KBucket(AbstractBucket.MIN_HASH_VALUE, AbstractBucket.MAX_HASH_VALUE, 0));   // this is the root bucket, so depth=0
        sBucket = new SBucket(localDestinationHash);
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
     * depending on its distance to the local node and how full the buckets are.
     * @param destination
     */
    public void addOrUpdate(KademliaPeer peer) {
        Hash destHash = peer.getDestinationHash();
        if (localDestinationHash.equals(destHash)) {
            log.debug("Not adding local destination to bucket.");
            return;
        }
        
        KademliaPeer removedOrNotAdded = sBucket.addOrUpdate(peer);
        if (removedOrNotAdded == null)
            getKBucket(destHash).remove(peer);   // if the peer was in a k-bucket, remove it because it is now in the s-bucket
        else
            addToKBucket(removedOrNotAdded);   // if a peer was removed from the s-bucket or didn't qualify as a sibling, add it to a k-bucket

        // log
        int numBuckets = kBuckets.size();
        int numPeers = getAllPeers().size();
        int numSiblings = sBucket.size();
        log.debug("Peer " + Util.toBase32(destHash) + " added/updated. Peers=" + numPeers + " sib=" + numSiblings + " buk=" + numBuckets + " (not counting the sibling bucket)");
    }

    /**
     * Adds a peer to the appropriate k-bucket, splitting the bucket if necessary, or updates the
     * peer if it exists in the bucket.
     * @param peer
     */
    private void addToKBucket(KademliaPeer peer) {
        int bucketIndex = getBucketIndex(peer.calculateHash());
        KBucket bucket = kBuckets.get(bucketIndex);
        
        if (bucket.shouldSplit(peer)) {
            KBucket newBucket = bucket.split();
            kBuckets.add(bucketIndex+1, newBucket);   // the new bucket is one higher than the old bucket
            
            // if all peers ended up in one bucket (overfilling it and leaving the other bucket empty), split again
            while (newBucket.isEmpty() || bucket.isEmpty())
                if (newBucket.isEmpty()) {
                    newBucket = bucket.split();
                    kBuckets.add(bucketIndex+1, newBucket);
                }
                else {   // if bucket.isEmpty()
                    bucketIndex++;
                    bucket = newBucket;
                    newBucket = newBucket.split();
                    kBuckets.add(bucketIndex+1, newBucket);
                }
            
            bucket = getKBucket(peer.calculateHash());
        }
        
        bucket.addOrUpdate(peer);
    }

    /**
     * Notifies the <code>BucketManager</code> that a peer didn't respond to
     * a request.
     * @param destination
     */
    public synchronized void noResponse(Destination destination) {
        KademliaPeer peer = getPeer(destination);
        if (peer != null)
            peer.noResponse();
        else
            log.debug("Peer not found in buckets: " + Util.toBase32(destination));   // this happens when a peer that was not yet known is contacted and fails to respond
    }
    
    public void remove(Destination peer) {
        AbstractBucket bucket = getBucket(peer);
        if (bucket != null) {
            bucket.remove(peer);
            if (bucket instanceof SBucket)
                refillSiblings();
        }
        else
            log.debug("Can't remove peer because no bucket contains it: " + Util.toBase32(peer));
    }
    
    /**
     * Moves peers from the k-buckets to the s-bucket until the s-bucket is full
     * or all k-buckets are empty.
     */
    private void refillSiblings() {
        // Sort all k-peers by distance to the local destination
        List<KademliaPeer> kPeers = new ArrayList<KademliaPeer>();
        for (KBucket kBucket: kBuckets)
            kPeers.addAll(kBucket.getPeers());
        Collections.sort(kPeers, new PeerDistanceComparator(localDestinationHash));
        
        while (!sBucket.isFull() && !kPeers.isEmpty()) {
            // move the closest k-peer to the s-bucket
            KademliaPeer peerToMove = kPeers.remove(0);
            int bucketIndex = getBucketIndex(peerToMove.getDestinationHash());
            kBuckets.get(bucketIndex).remove(peerToMove);
            sBucket.addOrUpdate(peerToMove);
        }
    }
    
    /**
     * Finds the index of the k-bucket whose key range contains a given {@link Hash}.
     * This method does a binary search "by hand" because <code>Collections.binarySearch()<code>
     * cannot be used to search for a <code>Hash</code> in a <code>List&lt;KBucket&gt;</code>.
     * @param key
     * @return
     */
    private int getBucketIndex(Hash key) {
        if (kBuckets.size() == 1)
            return 0;
        
        // initially, the search interval is 0..n-1
        int lowIndex = 0;
        int highIndex = kBuckets.size() - 1;
        
        BigInteger keyValue = new BigInteger(1, key.getData());
        while (lowIndex < highIndex) {
            int centerIndex = (highIndex + lowIndex) / 2;
            if (keyValue.compareTo(kBuckets.get(centerIndex).getStartId()) < 0)
                highIndex = centerIndex - 1;
            else if (keyValue.compareTo(kBuckets.get(centerIndex).getEndId()) >= 0)
                lowIndex = centerIndex + 1;
            else
                return centerIndex;
        }
     
        return lowIndex;
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
     * Looks up a <code>KademliaPeer</code> by I2P destination. If no bucket
     * (k or s-bucket) contains the peer, <code>null</code> is returned.
     * @param destination
     * @return
     */
    private KademliaPeer getPeer(Destination destination) {
        AbstractBucket bucket = getBucket(destination);
        if (bucket != null)
            return bucket.getPeer(destination);
        else
            return null;
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
     * Returns the <code>count</code> peers that are closest to a given key,
     * and which are not locked.
     * Less than <code>count</code> peers may be returned if there aren't
     * enough peers in the k-buckets and the s-bucket.
     * @param key
     * @param count
     * @return Up to <code>count</code> peers, sorted by distance to <code>key</code>.
     */
    public List<Destination> getClosestPeers(Hash key, int count) {
        // TODO don't put all peers in one huge list, only use two k-buckets and the s-bucket at a time
        List<Destination> peers = getAllUnlockedPeers();
        Collections.sort(peers, new PeerDistanceComparator(key));
        if (peers.size() < count)
            return peers;
        else
            return peers.subList(0, count);
    }

    /**
     * Returns all peers that are not locked.
     * @return
     */
    public synchronized List<Destination> getAllUnlockedPeers() {
        List<Destination> allPeers = new ArrayList<Destination>();
        
        for (KBucket bucket: kBuckets)
            for (KademliaPeer peer: bucket.getPeers())
                if (!peer.isLocked())
                    allPeers.add(peer);
        for (KademliaPeer peer: sBucket.getPeers())
            if (!peer.isLocked())
                allPeers.add(peer);
        
        return allPeers;
    }
    
    /**
     * Returns the total number of peers that are not locked.
     * @return
     */
    int getUnlockedPeerCount() {
        return getAllUnlockedPeers().size();
    }

    public synchronized List<KademliaPeer> getAllPeers() {
        List<KademliaPeer> allPeers = new ArrayList<KademliaPeer>();
        for (KBucket bucket: kBuckets)
            allPeers.addAll(bucket.getPeers());
        allPeers.addAll(sBucket.getPeers());
        return allPeers;
    }
    
    /**
     * Return the total number of known Kademlia peers (locked + unlocked peers).
     * @return
     */
    int getPeerCount() {
        int count = 0;
        for (KBucket bucket: kBuckets)
            count += bucket.size();
        count += sBucket.size();
        return count;
    }

    /**
     * @see KademliaDHT.getPeerStats()
     */
    DhtPeerStats getPeerStats(DhtPeerStatsRenderer renderer) {
        return new KademliaPeerStats(renderer, sBucket, kBuckets, localDestinationHash);
    }

    /**
     * Updates the time at which the k-bucket for a DHT key, and the s-bucket,
     * was last refreshed.
     * @param key
     */
    void updateLastLookupTime(Hash key) {
        long time = System.currentTimeMillis();
        getKBucket(key).setLastLookupTime(time);
        sBucket.setLastLookupTime(key, time);
    }
    
    // PacketListener implementation
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        BanList banList = BanList.getInstance();
        banList.update(sender, packet);
        if (!banList.isBanned(sender))
            // any type of incoming packet updates the peer's record in the bucket/sibling list, or adds the peer to the bucket/sibling list
            addOrUpdate(new KademliaPeer(sender));
        else
            remove(sender);
    }

    SBucket getSBucket() {
        return sBucket;
    }
    
    /**
     * Iterates over the k-buckets. Does not include the sibling bucket.
     * This method is not thread safe.
     * @return
     */
    @Override
    public Iterator<KBucket> iterator() {
        return kBuckets.iterator();
    }
}
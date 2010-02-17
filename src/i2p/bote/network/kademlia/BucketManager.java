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

import i2p.bote.I2PBote;
import i2p.bote.network.BanList;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

/**
 * The k-bucket tree isn't actually implemented as a tree, but a {@link List}.
 * TODO if a sibling times out, refill the sibling table
 */
class BucketManager implements PacketListener, Iterable<KBucket> {
    private Log log = new Log(BucketManager.class);
    private List<KBucket> kBuckets;
    private SBucket sBucket;   // The sibling bucket
    private Hash localDestinationHash;

    public BucketManager(Hash localDestinationHash) {
        this.localDestinationHash = localDestinationHash;
        kBuckets = Collections.synchronizedList(new ArrayList<KBucket>());
        kBuckets.add(new KBucket(AbstractBucket.MIN_HASH_VALUE, AbstractBucket.MAX_HASH_VALUE, KademliaConstants.K, 0));   // this is the root bucket, so depth=0
        sBucket = new SBucket(KademliaConstants.S, localDestinationHash);
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
     * Adds a <code>{@link Destination}</code> to the s-bucket or a k-bucket,
     * depending on its distance to the local node and how full the buckets are.
     * @param destination
     */
    public void addOrUpdate(Destination destination) {
        Hash destHash = destination.calculateHash();
        if (localDestinationHash.equals(destHash)) {
            log.debug("Not adding local destination to bucket.");
            return;
        }
        
        log.debug("Adding/updating peer: Hash = " + destHash);

        Destination removedOrNotAdded = sBucket.addOrUpdate(destination);
        if (removedOrNotAdded == null)
            getKBucket(destHash).remove(destination);   // if the peer was in a k-bucket, remove it because it is now in the s-bucket
        else
            addToKBucket(removedOrNotAdded);   // if a peer was removed from the s-bucket or didn't qualify as a sibling, add it to a k-bucket
        
        logBucketStats();
    }

    private void addToKBucket(Destination destination) {
        int bucketIndex = getBucketIndex(destination.calculateHash());
        KBucket bucket = kBuckets.get(bucketIndex);
        KBucket newBucket = bucket.addUpdateOrSplit(destination);
        if (newBucket != null)
            kBuckets.add(bucketIndex+1, newBucket);   // the new bucket is one higher than the old bucket
        
        // if all peers ended up in one bucket (overfilling it and leaving the other bucket empty), split again
        while (newBucket!=null && (newBucket.isEmpty() || bucket.isEmpty()))
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
    }

    public void incrementStaleCounter(Destination destination) {
        AbstractBucket bucket = getBucket(destination);
        if (bucket != null) {
            KademliaPeer peer = bucket.getPeer(destination);
            if (peer != null) {
                peer.incrementStaleCounter();
                if (peer.isDead())
                    bucket.remove(peer);
            }
        }
        else
            log.debug("Can't increment stale counter because peer not found in buckets: " + destination.calculateHash());
    }
    
    private void logBucketStats() {
        int numBuckets = kBuckets.size();
        int numPeers = getAllPeers().size();
        int numSiblings = sBucket.size();
        
        log.debug("total #peers=" + numPeers + ", #siblings=" + numSiblings + ", #buckets=" + numBuckets + " (not counting the sibling bucket)");
    }
    
    public void remove(Destination peer) {
        AbstractBucket bucket = getBucket(peer);
        if (bucket != null)
            bucket.remove(peer);
        else
            log.debug("Can't remove peer because no bucket contains it: " + peer.calculateHash().toBase64());
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
        
        BigInteger keyValue = new BigInteger(1, key.getData());   // 
        while (lowIndex < highIndex) {
            int centerIndex = (highIndex + lowIndex) / 2;
            if (keyValue.compareTo(kBuckets.get(centerIndex).getStartId()) < 0)
                highIndex = centerIndex - 1;
            else if (keyValue.compareTo(kBuckets.get(centerIndex).getEndId()) > 0)
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
    public KBucket getKBucket(Hash key) {
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
    
    public synchronized List<KademliaPeer> getAllPeers() {
        List<KademliaPeer> allPeers = new ArrayList<KademliaPeer>();
        for (KBucket bucket: kBuckets)
            allPeers.addAll(bucket.getPeers());
        allPeers.addAll(sBucket.getPeers());
        return allPeers;
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

    /**
     * @see KademliaDHT.getPeerStats()
     */
    DhtPeerStats getPeerStats() {
        return new KademliaPeerStats(sBucket, kBuckets, localDestinationHash);
    }
    
    // PacketListener implementation
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        boolean banned = false;
        
        KademliaPeer peer = getPeer(sender);
        if (peer != null) {
            if (packet.getProtocolVersion() != I2PBote.PROTOCOL_VERSION) {
                BanList.getInstance().ban(peer, "Wrong protocol version: " + packet.getProtocolVersion());
                remove(peer);
                banned = true;
            }
            else
                BanList.getInstance().unban(peer);
        }
        
        // any type of incoming packet updates the peer's record in the bucket/sibling list, or adds the peer to the bucket/sibling list
        if (!banned)
            addOrUpdate(sender);
    }

    SBucket getSBucket() {
        return sBucket;
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
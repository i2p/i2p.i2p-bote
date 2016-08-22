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
import java.util.Collections;

import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * An {@link AbstractBucket} that implements an S/Kademlia sibling list.<br/>
 * Peers are kept in an <code>ArrayList</code> sorted by XOR distance
 * from the local destination.<br/>
 * The closest peer is at index 0, the most distant peer is at index <code>n-1</code>.
 */
class SBucket extends AbstractBucket {
    private Log log = new Log(SBucket.class);
    private PeerDistanceComparator distanceComparator;
    private BucketSection[] sections;   // used for refreshing the s-bucket
    
    SBucket(Hash localDestinationHash) {
        super(KademliaConstants.S);
        distanceComparator = new PeerDistanceComparator(localDestinationHash);
        
        int numSections = (KademliaConstants.S - 1) / KademliaConstants.K + 1;
        if (numSections < 1)
            numSections = 1;
        sections = new BucketSection[numSections];
        for (int i=0; i<numSections; i++)
            sections[i] = new BucketSection(BigInteger.ZERO, BigInteger.ZERO, 0);
    }

    /**
     * Adds/updates a peer if there is room in the bucket, or if the peer is not
     * further away from the local destination than the furthest sibling in the
     * bucket.
     * @param peer
     * @return The peer that was removed to make room for the new peer, or
     * <code>null</code> if no peer was removed from the bucket. If
     * <code>peer</code> didn't exist in the bucket but was not added
     * because it was too far away from the local destination,
     * <code>peer</code> itself is returned.
     */
    KademliaPeer addOrUpdate(KademliaPeer peer) {
        synchronized(peers) {
            int index = Collections.binarySearch(peers, peer, distanceComparator);
            
            if (index >= 0) {   // destination is already in the bucket, so update it
                peers.get(index).responseReceived();
                return null;
            }
            else {
                int insertionPoint = -(index+1);
                if (isFull()) {
                    // insertionPoint can only be equal to or greater than size() at this point, see Collections.binarySearch javadoc
                    if (insertionPoint > size())
                        log.error("insertionPoint > size(), this shouldn't happen.");
                    if (insertionPoint < size()) {   // if destination is closer than an existing sibling, replace the furthest away sibling and return the removed sibling
                        peers.add(insertionPoint, new KademliaPeer(peer));
                        KademliaPeer removedPeer = peers.remove(size() - 1);
                        return removedPeer;
                    }
                    else   // insertionPoint==size(), this means the new peer is further away than all other siblings
                        return peer;
                }
                else {
                    peers.add(insertionPoint, peer);
                    return null;
                }
            }
        }
    }

    BucketSection[] getSections() {
        BigInteger minSiblingId;
        BigInteger maxSiblingId;
        if (peers.size() < 2) {
            // If there are less than two siblings, use the whole ID space. This avoids zero-length bucket sections.
            minSiblingId = MIN_HASH_VALUE;
            maxSiblingId = MAX_HASH_VALUE;
        }
        else {
            // find the minimum and the maximum Kademlia id of all siblings
            minSiblingId = MAX_HASH_VALUE;
            maxSiblingId = MIN_HASH_VALUE;
            for (KademliaPeer sibling: peers) {
                BigInteger id = new BigInteger(1, sibling.getDestinationHash().getData());
                minSiblingId = minSiblingId.min(id);
                maxSiblingId = maxSiblingId.max(id);
            }
        }
        
        // divide the interval [minSiblingId, maxSiblingId] in sections.length equal size parts
        BigInteger interval = maxSiblingId.subtract(minSiblingId);
        BigInteger numSections = BigInteger.valueOf(sections.length);
        sections[0].start = minSiblingId;
        for (int i=0; i<sections.length; i++) {
            if (i > 0)
                sections[i].start = sections[i-1].end;
            BigInteger iPlusOne = BigInteger.valueOf(i+1);
            sections[i].end = minSiblingId.add(interval.multiply(iPlusOne).divide(numSections));   // sections[i].end = minSiblingId + interval*(i+1)/numSections
        }
        
        return sections;
    }
    
    private BucketSection getSection(Hash key) {
        for (BucketSection section: sections)
            if (section.contains(key))
                return section;
        
        return null;
    }
    
    void setLastLookupTime(Hash key, long lastLookupTime) {
        BucketSection section = getSection(key);
        if (section != null)
            section.lastLookupTime = lastLookupTime;
    }
    
    /**
     * Stores the start and end key, and the time of the last lookup, for one section of the s-bucket.
     * There are ceil(s/k) sections in the bucket.
     * A hash h is considered within a section if <code>section.start <= h < section.end</code>.
     */
    class BucketSection {
        private BigInteger start;
        private BigInteger end;
        private long lastLookupTime;
        
        private BucketSection(BigInteger start, BigInteger end, long lastLookupTime) {
            this.start = start;
            this.end = end;
            this.lastLookupTime = lastLookupTime;
        }
        
        long getLastLookupTime() {
            return lastLookupTime;
        }

        public BigInteger getStart() {
            return start;
        }
        
        public BigInteger getEnd() {
            return end;
        }
        
        private boolean contains(Hash key) {
            BigInteger keyValue = new BigInteger(1, key.getData());
            return (start.compareTo(keyValue)<=0 && keyValue.compareTo(end)<0);
        }
    }
}
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

import java.util.Collections;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * An {@link AbstractBucket} that implements an S/Kademlia sibling list.
 * Peers are kept in an <code>ArrayList</code> sorted by XOR distance
 * from the local destination. The closest peer is at index 0, the
 * most distant peer is at index <code>n-1</code>.
 */
public class SBucket extends AbstractBucket {
    private Log log = new Log(SBucket.class);
    private PeerDistanceComparator distanceComparator;
    
    public SBucket(int capacity, Hash localDestinationHash) {
        super(capacity);
        distanceComparator = new PeerDistanceComparator(localDestinationHash);
    }

    /**
     * Adds/updates a peer if there is room in the bucket, or if the peer is not
     * further away from the local destination than the furthest sibling in the
     * bucket.
     * @param destination
     * @return The peer that was removed to make room for the new peer, or
     * <code>null</code> if no peer was removed from the bucket. If
     * <code>destination</code> didn't exist in the bucket but was not added
     * because it was too far away from the local destination,
     * <code>destination</code> itself is returned.
     */
    Destination addOrUpdate(Destination destination) {
        synchronized(peers) {
            int index = Collections.binarySearch(peers, destination, distanceComparator);
            
            if (index >= 0) {   // destination is already in the bucket, so update it
                peers.get(index).responseReceived();
                return null;
            }
            else {
                int insertionPoint = -(index+1);
                if (isFull()) {
                    // insertionPoint can only be equal to or greater than size(), see Collections.binarySearch javadoc
                    if (insertionPoint > size())
                        log.error("insertionPoint > size(), this shouldn't happen.");
                    if (insertionPoint < size()) {   // if destination is closer than an existing sibling, replace the furthest away sibling and return the removed sibling
                        KademliaPeer removedPeer = peers.remove(size() - 1);
                        peers.add(insertionPoint, new KademliaPeer(destination));
                        return removedPeer;
                    }
                    else   // insertionPoint==size(), this means the new peer is further away than all other siblings
                        return destination;
                }
                else {
                    add(insertionPoint, destination);
                    return null;
                }
            }
        }
    }
}
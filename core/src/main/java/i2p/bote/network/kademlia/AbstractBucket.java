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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.i2p.data.Destination;
import net.i2p.data.Hash;

/**
 * This is the parent class for k-buckets and s-buckets.
 * 
 * <strong>Iterators returned by this class are not thread safe. This
 * includes iterating over bucket entries via a <code>foreach</code> loop.
 * </strong>
 */
abstract class AbstractBucket implements Iterable<KademliaPeer> {
    static final BigInteger MIN_HASH_VALUE = BigInteger.ZERO;   // system-wide minimum hash value
    static final BigInteger MAX_HASH_VALUE = BigInteger.ONE.shiftLeft(Hash.HASH_LENGTH*8).subtract(BigInteger.ONE);   // system-wide maximum hash value

    protected List<KademliaPeer> peers;   // peers are sorted most recently seen to least recently seen
    protected int capacity;
    
    public AbstractBucket(int capacity) {
        peers = new CopyOnWriteArrayList<KademliaPeer>();
        this.capacity = capacity;
    }

    /**
     * Removes a peer from the bucket. If the peer doesn't exist in the bucket, nothing happens.
     * @param node
     */
    void remove(Destination destination) {
        peers.remove(destination);
    }

    Collection<KademliaPeer> getPeers() {
        return peers;
    }
    
    /**
     * Looks up a <code>KademliaPeer</code> by I2P destination. If the bucket
     * doesn't contain the peer, <code>null</code> is returned.
     * @param destination
     */
    protected KademliaPeer getPeer(Destination destination) {
        int index = getPeerIndex(destination);
        if (index >= 0)
            return peers.get(index);
        else
            return null;
    }
    
    /**
     * Looks up the index of a <code>Destination</code> in the bucket.
     * if nothing is found, <code>-1</code> is returned.
     * @param destination
     */
    protected int getPeerIndex(Destination destination) {
        // An alternative to indexOf, which does a linear search, would be to maintain a Map<Destination, KademliaPeer>.
        return peers.indexOf(destination);
    }
    
    /**
     * Returns <code>true</code> if a peer exists in the bucket.
     * @param destination
     * @return
     */
    boolean contains(Destination destination) {
        return getPeer(destination) != null;
    }

    boolean isFull() {
        return size() >= capacity;
    }

    boolean isEmpty() {
        return peers.isEmpty();
    }
    
    int size() {
        return peers.size();
    }
    
    @Override
    public Iterator<KademliaPeer> iterator() {
        return peers.iterator();
    }
}
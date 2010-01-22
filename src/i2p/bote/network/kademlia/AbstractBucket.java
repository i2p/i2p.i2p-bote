package i2p.bote.network.kademlia;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * Parent class for k-buckets and s-buckets.
 * Peers are sorted by the time of the most recent communication: Index 0 = least recent,
 * index n-1 = most recent.
 */
public class AbstractBucket implements Iterable<KademliaPeer> {
    static final BigInteger MIN_HASH_VALUE = BigInteger.ONE.negate().shiftLeft(Hash.HASH_LENGTH*8);   // system-wide minimum hash value
    static final BigInteger MAX_HASH_VALUE = BigInteger.ONE.shiftLeft(Hash.HASH_LENGTH*8).subtract(BigInteger.ONE);   // system-wide maximum hash value

    private Log log = new Log(AbstractBucket.class);
    protected Set<KademliaPeer> peers;   // the list is always kept sorted by "last seen" time
    protected int capacity;
    protected Comparator<KademliaPeer> peerComparator;
    
    public AbstractBucket(int capacity) {
        peerComparator = createLastReceptionComparator();
        peers = new ConcurrentSkipListSet<KademliaPeer>(peerComparator);
        this.capacity = capacity;
    }

    /**
     * Updates a known peer, or adds the peer if it isn't known.
     * TODO If the bucket is full, the peer is added to the bucket's replacement cache.
     * @param destination
     * @return <code>true</code> if the peer was added (or replacement-cached),
     * <code>false</code> if it was updated.
     */
    boolean addOrUpdate(Destination destination) {
        // TODO log an error if peer outside bucket's range
        // TODO handle stale peers
        // TODO manage replacement cache
        KademliaPeer peer = getPeer(destination);
        if (peer == null) {
            peers.add(new KademliaPeer(destination));
            return true;
        }
        else {
            peer.setLastReception(System.currentTimeMillis());
            // TODO move to end of list if lastReception is highest value, which it should be most of the time
            return false;
        }
    }
    
    /**
     * Removes a peer from the bucket. If the peer doesn't exist in the bucket, nothing happens.
     * @param node
     */
    void remove(Destination destination) {
        peers.remove(destination);
    }

    Set<KademliaPeer> getPeers() {
        return peers;
    }
    
    /**
     * Looks up a <code>KademliaPeer</code> by I2P destination. If the bucket contains no
     * Kademlia peer with that destination, <code>null</code> is returned.
     * @param destination
     * @return
     */
    protected KademliaPeer getPeer(Destination destination) {
        // Use linear search for simplicity. An alternative would be to maintain a Map<Destination, KademliaPeer>.
        for (KademliaPeer peer: peers)
            if (peer.equals(destination))
                return peer;
        
        return null;
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

    int size() {
        return peers.size();
    }
    
    private Comparator<KademliaPeer> createLastReceptionComparator() {
        return new Comparator<KademliaPeer>() {
            @Override
            public int compare(KademliaPeer peer1, KademliaPeer peer2) {
                long lastReception1 = peer1.getLastReception();
                long lastReception2 = peer2.getLastReception();
                return Long.valueOf(lastReception1).compareTo(lastReception2);
            }
        };
    }
    
    @Override
    public Iterator<KademliaPeer> iterator() {
        return peers.iterator();
    }
}
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

import i2p.bote.network.DhtPeer;

import java.util.concurrent.atomic.AtomicInteger;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

public class KademliaPeer extends Destination implements DhtPeer {
    private Log log = new Log(KademliaPeer.class);
    private Destination destination;
    private Hash destinationHash;
    private long lastPingSent;
    private long lastReception;
    private long activeSince;
    private AtomicInteger consecutiveTimeouts;
    
    public KademliaPeer(Destination destination, long lastReception) {
        // initialize the Destination part of the KademliaPeer
        setCertificate(destination.getCertificate());
        setSigningPublicKey(destination.getSigningPublicKey());
        setPublicKey(destination.getPublicKey());
        
        // initialize KademliaPeer-specific fields
        this.destination = destination;
        destinationHash = destination.calculateHash();
        if (destinationHash == null)
            log.error("calculateHash() returned null!");
        
        this.lastReception = lastReception;
        lastPingSent = 0;
        activeSince = lastReception;
        consecutiveTimeouts = new AtomicInteger(0);
    }
    
    public KademliaPeer(Destination destination) {
        this(destination, System.currentTimeMillis());
    }
    
    public Destination getDestination() {
    	return destination;
    }
    
    public Hash getDestinationHash() {
    	return destinationHash;
    }
    
    public boolean isDead() {
        return consecutiveTimeouts.get() >= KademliaConstants.STALE_THRESHOLD;
    }
    
    public int getStaleCounter() {
        return consecutiveTimeouts.get();
    }
    
    public void incrementStaleCounter() {
        consecutiveTimeouts.incrementAndGet();
    }
    
    public void resetStaleCounter() {
        consecutiveTimeouts.set(0);
    }
    
    public long getLastPingSent() {
    	return lastPingSent;
    }
    
    public void setLastReception(long time) {
    	lastReception = time;
    }
    
    public long getLastReception() {
    	return lastReception;
    }
    
    public long getActiveSince() {
    	return activeSince;
    }
}
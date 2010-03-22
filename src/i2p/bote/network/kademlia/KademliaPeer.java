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

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.Log;

public class KademliaPeer extends Destination {
    private Log log = new Log(KademliaPeer.class);
    private Destination destination;
    private Hash destinationHash;
    private long activeSince;
    private volatile int consecutiveTimeouts;
    private long lockedUntil;
    
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
        
        activeSince = lastReception;
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

    /**
     * @param activeSince Milliseconds since Jan 1, 1970
     * @return
     */
    void setActiveSince(long activeSince) {
        this.activeSince = activeSince;
    }

    public long getActiveSince() {
    	return activeSince;
    }

    public int getConsecTimeouts() {
        return consecutiveTimeouts;
    }
    
    long getLockedUntil() {
        return lockedUntil;
    }
    
    boolean isLocked() {
        return lockedUntil > System.currentTimeMillis();
    }
    
    /**
     * Locks the peer for 2 minutes after the first timeout, 4 minutes after
     * two consecutive timeouts, 8 minutes after 3 consecutive timeouts, etc.,
     * up to 2^10 minutes (about 17h) for 10 or more consecutive timeouts.
     */
    synchronized void noResponse() {
        consecutiveTimeouts++;
        int lockDuration = 1 << Math.min(consecutiveTimeouts, 10);   // in minutes
        lockedUntil = System.currentTimeMillis() + 60*1000*lockDuration;
    }
    
    void responseReceived() {
        consecutiveTimeouts = 0;
        lockedUntil = 0;
    }
}
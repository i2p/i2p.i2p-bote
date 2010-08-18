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

package i2p.bote.network;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.i2p.data.Destination;

/**
 * A {@link Destination} that contains information about the most
 * recent attempts to contact the peer, and whether or not it responded.
 */
public class RelayPeer extends Destination {
    private static final int MAX_SAMPLES = 20;   // the maximum size of the samples list

    /**
     * Contains one element for each request sent to the peer.<br/>
     * <code>true</code> means the peer responded to a request, <code>false</code> means
     * no response.<br/>
     * The list is ordered oldest to newest.
     */
    private LinkedList<Boolean> samples;

    /**
     * Creates a new <code>RelayPeer</code> with a given I2P destination and
     * an empty list of reachability data.
     * @param destination
     */
    public RelayPeer(Destination destination) {
        // initialize the Destination part of the RelayPeer
        setCertificate(destination.getCertificate());
        setSigningPublicKey(destination.getSigningPublicKey());
        setPublicKey(destination.getPublicKey());
        
        // initialize RelayPeer-specific data
        samples = new LinkedList<Boolean>();
    }
    
    /**
     * Adds information about a new attempt to contact the peer.<br/>
     * @param didRespond <code>true</code> means the peer responded to the request,
     *     <code>false</code> means no response.
     */
    public synchronized void addReachabilitySample(boolean didRespond) {
        samples.add(didRespond);
        while (samples.size() > MAX_SAMPLES)
            samples.removeFirst();
    }

    public List<Boolean> getAllSamples() {
        return Collections.unmodifiableList(samples);
    }
    
    /**
     * Returns the percentage of requests sent to this peer for which 
     * a response was received.<br/>
     * If no request has been sent to the peer yet, <code>0</code> is returned.
     */
    public synchronized int getReachability() {
        int requestsSent = 0;
        int responsesReceived = 0;
        for (boolean didRespond: samples) {
            requestsSent++;
            if (didRespond)
                responsesReceived++;
        }
        
        if (requestsSent == 0)
            return 0;
        else
            return (int)(100L * responsesReceived / requestsSent);
    }
}
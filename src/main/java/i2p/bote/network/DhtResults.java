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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.i2p.data.Destination;

import i2p.bote.packet.dht.DhtStorablePacket;

public class DhtResults implements Iterable<DhtStorablePacket> {
    private Map<Destination, DhtStorablePacket> map;
    private int totalResponses;

    public DhtResults() {
        map = new ConcurrentHashMap<Destination, DhtStorablePacket>();
        totalResponses = -1;
    }
    
    public void put(Destination peer, DhtStorablePacket packet) {
        map.put(peer, packet);
    }
    
    public int getNumResults() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Collection<DhtStorablePacket> getPackets() {
        return map.values();
    }
    
    public Set<Destination> getPeers() {
        return map.keySet();
    }

    public DhtStorablePacket getPacket(Destination peer) {
        return map.get(peer);
    }
    
    /**
     * Sets the number of peers that have responded to the DHT lookup.
     * @param totalResponses
     */
    public void setTotalResponses(int totalResponses) {
        this.totalResponses = totalResponses;
    }

    /**
     * Gets the number of peers that have responded to the DHT lookup,
     * including negative responses (those that did not return a data packet).
     */
    public int getTotalResponses() {
        return totalResponses;
    }

    @Override
    public Iterator<DhtStorablePacket> iterator() {
        return getPackets().iterator();
    }
}
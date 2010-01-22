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

import net.i2p.data.Destination;
import net.i2p.data.Hash;

public class SBucket extends AbstractBucket {
    
    public SBucket(int capacity) {
        super(capacity);
    }

    Destination getMostDistantPeer(Hash key) {
        Destination mostDistantPeer = null;
        BigInteger maxDistance = BigInteger.ZERO;
        for (Destination peer: peers) {
            BigInteger distance = KademliaUtil.getDistance(key, peer.calculateHash());
            if (distance.compareTo(maxDistance) > 0) {
                mostDistantPeer = peer;
                maxDistance = distance;
            }
        }
        return mostDistantPeer;
    }
}
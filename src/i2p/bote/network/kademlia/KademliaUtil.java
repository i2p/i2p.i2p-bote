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

import net.i2p.data.DataHelper;
import net.i2p.data.Hash;

class KademliaUtil {

    /**
     * Calculates the Kademlia distance (XOR distance) between two hashes.
     * If the hashes are equal, the distance is zero; otherwise, it is greater
     * than zero.
     * @param key1
     * @param key2
     */
    static BigInteger getDistance(Hash key1, Hash key2) {
        // This shouldn't be a performance bottleneck, so save some mem by not using Hash.cachedXor
        byte[] xoredData = DataHelper.xor(key1.getData(), key2.getData());
        return new BigInteger(1, xoredData);
    }
}
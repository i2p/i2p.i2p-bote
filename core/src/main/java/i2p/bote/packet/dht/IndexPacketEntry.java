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

package i2p.bote.packet.dht;

import net.i2p.data.Hash;

/**
 * One entry in an {@link IndexPacket}.
 */
public class IndexPacketEntry {
    public Hash emailPacketKey;
    public long storeTime;   // milliseconds since 1-1-1970
    
    Hash delVerificationHash;
    
    /**
     * Constructs an <code>IndexPacketEntry</code> with a time stamp of 0.
     * @param emailPacketKey
     * @param delVerificationHash
     */
    IndexPacketEntry(Hash emailPacketKey, Hash delVerificationHash) {
        this(emailPacketKey, delVerificationHash, 0);
    }
    
    IndexPacketEntry(Hash emailPacketKey, Hash delVerificationHash, long storeTime) {
        this.emailPacketKey = emailPacketKey;
        this.delVerificationHash = delVerificationHash;
        this.storeTime = storeTime;
    }
}
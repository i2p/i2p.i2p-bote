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

package i2p.bote.packet;

import i2p.bote.UniqueId;
import net.i2p.data.Hash;

/**
 * One entry in an {@link IndexPacket}.
 * All fields are non-<code>null</code> after calling any of the constructors.
 */
public class IndexPacketEntry {
    public Hash emailPacketKey;
    public Hash delVerificationHash;
    public UniqueId delAuthorization;
    public long storeTime;   // milliseconds since 1-1-1970
    
    public IndexPacketEntry(Hash emailPacketKey, Hash delVerificationHash) {
        this(emailPacketKey, delVerificationHash, zeroId(), 0);
    }
    
    public IndexPacketEntry(Hash emailPacketKey, UniqueId delAuthorization, long storeTime) {
        this(emailPacketKey, zeroHash(), delAuthorization, storeTime);
    }
    
    public IndexPacketEntry(Hash emailPacketKey, Hash delVerificationHash, UniqueId delAuthorization, long storeTime) {
        this.emailPacketKey = emailPacketKey;
        this.delVerificationHash = delVerificationHash;
        this.delAuthorization = delAuthorization;
        this.storeTime = storeTime;
    }
    
    private static UniqueId zeroId() {
        return new UniqueId(new byte[UniqueId.LENGTH], 0);
    }
    
    private static Hash zeroHash() {
        return new Hash(new byte[Hash.HASH_LENGTH]);
    }
}
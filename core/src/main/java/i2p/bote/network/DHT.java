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

import i2p.bote.UniqueId;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.EncryptedEmailPacket;

import java.util.concurrent.CountDownLatch;

import net.i2p.data.Hash;

public interface DHT {

    void store(DhtStorablePacket packet) throws DhtException, InterruptedException;
    
    DhtResults findOne(Hash key, Class<? extends DhtStorablePacket> dataType) throws InterruptedException;

    DhtResults findAll(Hash key, Class<? extends DhtStorablePacket> dataType) throws InterruptedException;

    /**
     * Returns a Delete Authorization for a DHT key of an {@link EncryptedEmailPacket}, or <code>null</code> if none
     * was found (usually because the Email Packet hasn't been deleted yet).<br/>
     * DHT results are checked against <code>verificationHash</code>, so if a non-null key is returned, it
     * is known to be valid.
     */
    UniqueId findDeleteAuthorizationKey(Hash dhtKey, Hash verificationHash) throws InterruptedException;
    
    /**
     * Registers a <code>DhtStorageHandler</code> that handles incoming storage requests of a certain
     * type (but not its subclasses).
     * @param packetType
     * @param storageHandler
     */
    void setStorageHandler(Class<? extends DhtStorablePacket> packetType, DhtStorageHandler storageHandler);

    /** Returns a <code>CountDownLatch</code> that switches to zero when a connection to the DHT has been established. */
    CountDownLatch readySignal();
    
    /** Returns <code>true</code> if a connection to the DHT has been established. */
    boolean isReady();

    DhtPeerStats getPeerStats(DhtPeerStatsRenderer renderer);
}
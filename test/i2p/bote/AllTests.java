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

package i2p.bote;

import i2p.bote.folder.IncompleteEmailFolderTest;
import i2p.bote.network.kademlia.BucketManagerTest;
import i2p.bote.packet.EmailPacketDeleteRequestTest;
import i2p.bote.packet.EncryptedEmailPacketTest;
import i2p.bote.packet.I2PBotePacketTest;
import i2p.bote.packet.IndexPacketDeleteRequestTest;
import i2p.bote.packet.IndexPacketTest;
import i2p.bote.packet.PeerListTest;
import i2p.bote.packet.ResponsePacketTest;
import i2p.bote.packet.UnencryptedEmailPacketTest;
import i2p.bote.packet.dht.FindClosePeersPacketTest;
import i2p.bote.packet.dht.StoreRequestTest;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { I2PBotePacketTest.class, StoreRequestTest.class, EncryptedEmailPacketTest.class, UnencryptedEmailPacketTest.class,
	FindClosePeersPacketTest.class, PeerListTest.class, ResponsePacketTest.class, IncompleteEmailFolderTest.class,
	EmailPacketDeleteRequestTest.class, IndexPacketDeleteRequestTest.class, IndexPacketTest.class, BucketManagerTest.class })
public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("All I2PBote unit tests");
        //$JUnit-BEGIN$

        //$JUnit-END$
        return suite;
    }
}
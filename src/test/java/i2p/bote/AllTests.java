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

import i2p.bote.crypto.CryptoImplementationTest;
import i2p.bote.email.EmailIdentityTest;
import i2p.bote.email.EmailMetadataTest;
import i2p.bote.email.EmailTest;
import i2p.bote.email.IdentitiesTest;
import i2p.bote.fileencryption.EncryptedStreamTest;
import i2p.bote.fileencryption.FileEncryptionUtilTest;
import i2p.bote.fileencryption.PasswordCacheTest;
import i2p.bote.folder.DirectoryEntryFolderTest;
import i2p.bote.folder.EmailFolderTest;
import i2p.bote.folder.EmailPacketFolderTest;
import i2p.bote.folder.FolderTest;
import i2p.bote.folder.IncompleteEmailFolderTest;
import i2p.bote.folder.IndexPacketFolderTest;
import i2p.bote.folder.RelayPacketFolderTest;
import i2p.bote.network.kademlia.BucketManagerTest;
import i2p.bote.network.kademlia.KBucketTest;
import i2p.bote.packet.I2PBotePacketTest;
import i2p.bote.packet.PeerListTest;
import i2p.bote.packet.ResponsePacketTest;
import i2p.bote.packet.dht.ContactTest;
import i2p.bote.packet.dht.DeletionQueryTest;
import i2p.bote.packet.dht.EmailPacketDeleteRequestTest;
import i2p.bote.packet.dht.EncryptedEmailPacketTest;
import i2p.bote.packet.dht.FindClosePeersPacketTest;
import i2p.bote.packet.dht.IndexPacketDeleteRequestTest;
import i2p.bote.packet.dht.IndexPacketTest;
import i2p.bote.packet.dht.StoreRequestTest;
import i2p.bote.packet.dht.UnencryptedEmailPacketTest;
import i2p.bote.packet.relay.RelayRequestTest;
import i2p.bote.packet.relay.ReturnChainTest;
import i2p.bote.service.OutboxProcessorTest;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
    // Packets
    I2PBotePacketTest.class,
    StoreRequestTest.class,
    ResponsePacketTest.class,
    FindClosePeersPacketTest.class,
    EmailPacketDeleteRequestTest.class,
    EncryptedEmailPacketTest.class,
    UnencryptedEmailPacketTest.class,
    IndexPacketTest.class,
    PeerListTest.class,
    IndexPacketDeleteRequestTest.class,
    RelayRequestTest.class,
    ReturnChainTest.class,
    DeletionQueryTest.class,
    ContactTest.class,
    
    // Folders
    DirectoryEntryFolderTest.class,
    FolderTest.class,
    EmailFolderTest.class,
    EmailPacketFolderTest.class,
    IncompleteEmailFolderTest.class,
    IndexPacketFolderTest.class,
    RelayPacketFolderTest.class,

    // Services
    OutboxProcessorTest.class,

    // Other
    EmailTest.class,
    EmailMetadataTest.class,
    EmailIdentityTest.class,
    IdentitiesTest.class,
    KBucketTest.class,
    BucketManagerTest.class,
    CryptoImplementationTest.class,
    EncryptedStreamTest.class,
    FileEncryptionUtilTest.class,
    PasswordCacheTest.class
})
public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("All I2PBote unit tests");
        //$JUnit-BEGIN$

        //$JUnit-END$
        return suite;
    }
}
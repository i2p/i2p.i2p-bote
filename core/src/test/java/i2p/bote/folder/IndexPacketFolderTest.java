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

package i2p.bote.folder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i2p.bote.UniqueId;
import i2p.bote.email.EmailDestination;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.MalformedPacketException;
import i2p.bote.packet.dht.DeleteRequest;
import i2p.bote.packet.dht.DeletionInfoPacket;
import i2p.bote.packet.dht.DeletionRecord;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.IndexPacket;
import i2p.bote.packet.dht.IndexPacketDeleteRequest;
import i2p.bote.packet.dht.IndexPacketEntry;
import i2p.bote.packet.dht.UnencryptedEmailPacket;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Hash;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexPacketFolderTest {
    private IndexPacketFolder folder;
    private UnencryptedEmailPacket unencryptedPacket1;
    private UnencryptedEmailPacket unencryptedPacket2;
    private EncryptedEmailPacket emailPacket1;
    private EncryptedEmailPacket emailPacket2;
    private EmailDestination destination1;
    private EmailDestination destination2;
    private EmailDestination destination3;
    private File testDir;
    private File folderDir;

    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "IndexPacketFolderTest-" + System.currentTimeMillis());
        folderDir = new File(testDir, "dht_index_pkt");
        folder = new SettableStoreTimeIndexPacketFolder(folderDir);
        
        byte[] content1 = "test TEST test ABCDEFGH asdfsadfsadf 3487562384".getBytes();
        byte[] content2 = "fdlkhgjfljh test 123456".getBytes();
        byte[] messageIdBytes = new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        int fragmentIndex = 0;
        unencryptedPacket1 = new UnencryptedEmailPacket(new ByteArrayInputStream(content1), messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        unencryptedPacket1.setNumFragments(1);
        unencryptedPacket2 = new UnencryptedEmailPacket(new ByteArrayInputStream(content2), messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        unencryptedPacket2.setNumFragments(1);
        
        destination1 = new EmailDestination("2XP9Ep3WWLk3-FTlMgUjgw4h8GYVBCvR6YrPyKdhP4xyQMSh8Da0VjZCmQGbD3PCeaGXAShBKbKjhJjQ7laekI");
        destination2 = new EmailDestination("m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY");
        destination3 = new EmailDestination("0XuJjhgp58aOhvHHgpaxoQYsCUfDS6BECMEoVxFGEFPdk3y8lbzIsq9eUyeizFleMacYwoscCir8nQLlW34lxfRmirkNpD9vU1XnmjnZ5hGdnor1qIDqz3KJ040dVQ617MwyG97xxYLT0FsH907vBXgdc4RCHwKd1~9siagA5CSMaA~wM8ymKXLypiZGYexENLmim7nMzJTQYoOM~fVS99UaGJleDBN3pgZ2EvRYDQV2VqKH7Gee07R3y7b~c0tAKVHS0IbPQfTVJigrIHjTl~ZczxpaeTM04T8IgxKnO~lSmR1w7Ik8TpEkETwT9PDwUqQsjmlSY8E~WwwGMRJVyIRZUkHeRZ0aFq7us8W9EKzYtjjiU1z0QFpZrTfJE8oqCbnH5Lqv5Q86UdTPpriJC1N99E77TpCTnNzcBnpp6ko2JCy2IJUveaigKxS6EmS9KarkkkBRsckOKZZ6UNTOqPZsBCsx0Q9WvDF-Uc3dtouXWyenxRptaQsdkZyYlEQv");
        
        emailPacket1 = new EncryptedEmailPacket(unencryptedPacket1, destination1);
        emailPacket2 = new EncryptedEmailPacket(unencryptedPacket2, destination2);
    }

    @Test
    public void testCheckExpiration() throws GeneralSecurityException, InterruptedException, PasswordException {
        long cutoffTime = System.currentTimeMillis() - ExpirationListener.EXPIRATION_TIME_MILLISECONDS;
        
        // store a packet that expired 3 seconds ago
        IndexPacket indexPacket = new IndexPacket(destination1);
        indexPacket.put(emailPacket1);
        long expirationTime1 = cutoffTime - 3*1000;
        setStoreTime(indexPacket, emailPacket1.getDhtKey(), expirationTime1);
        assertEquals(1, indexPacket.getNumEntries());
        folder.store(indexPacket);
        assertEquals(1, folder.getElements().size());
        assertEquals(1, folder.getElements().get(0).getNumEntries());
        assertEquals(expirationTime1/1000L, folder.getElements().get(0).iterator().next().storeTime/1000L);   // round to seconds
        
        // store a packet that expires in 3 seconds
        indexPacket.put(emailPacket2);
        long expirationTime2 = cutoffTime + 3*1000;
        setStoreTime(indexPacket, emailPacket2.getDhtKey(), expirationTime2);
        assertEquals(2, indexPacket.getNumEntries());
        folder.store(indexPacket);
        assertEquals(1, folder.getElements().size());
        
        // delete expired packets and check that one of the two packets got deleted
        folder.deleteExpired();
        assertEquals(1, folder.iterator().next().getNumEntries());
        
        // 4 seconds later, the remaining packet should have expired
        TimeUnit.SECONDS.sleep(4);
        folder.deleteExpired();
        assertEquals(0, folder.iterator().next().getNumEntries());
    }
    
    private void setStoreTime(IndexPacket indexPacket, Hash emailPacketKey, long storeTime) {
        for (IndexPacketEntry entry: indexPacket)
            if (entry.emailPacketKey.equals(emailPacketKey)) {
                entry.storeTime = storeTime;
                return;
            }
        fail("Email packet key " + emailPacketKey + " not found in index packet.");
    }
    
    @Test
    public void testProcessDeleteRequest() throws GeneralSecurityException, MalformedPacketException, PasswordException {
        IndexPacketFolder folder = new IndexPacketFolder(folderDir);
        
        // create another packet with the same destination as emailPacket1
        EncryptedEmailPacket emailPacket3 = new EncryptedEmailPacket(unencryptedPacket2, destination1);
        
        IndexPacket indexPacket = new IndexPacket(destination1);
        indexPacket.put(emailPacket1);
        indexPacket.put(emailPacket3);
        folder.store(indexPacket);
        assertEquals("Folder should have exactly one element!", 1, folder.getElements().size());
        
        // add two entries and delete them via delete requests
        Hash dest1Hash = destination1.getHash();
        IndexPacketDeleteRequest delRequest = new IndexPacketDeleteRequest(dest1Hash);
        Hash dhtKey1 = emailPacket1.getDhtKey();
        Hash dhtKey3 = emailPacket3.getDhtKey();
        delRequest.put(dhtKey1, unencryptedPacket1.getDeleteAuthorization());
        delRequest.put(dhtKey3, unencryptedPacket2.getDeleteAuthorization());
        folder.process(delRequest);
        DhtStorablePacket storedPacket = folder.retrieve(dest1Hash);
        assertTrue(storedPacket instanceof IndexPacket);
        assertEquals("The index packet should have no entries!", 0, ((IndexPacket)storedPacket).getNumEntries());
        
        // verify that there is one deletion file containing two entries
        File[] files = folder.getStorageDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("DEL_");
            }
        });
        assertEquals(1, files.length);
        DataPacket dataPacket = DataPacket.createPacket(files[0]);
        assertTrue(dataPacket instanceof DeletionInfoPacket);
        DeletionInfoPacket delInfoPacket = (DeletionInfoPacket)dataPacket;
        Iterator<DeletionRecord> delPacketIterator = delInfoPacket.iterator();
        assertTrue("DeletionInfoPacket has no elements!", delPacketIterator.hasNext());
        delPacketIterator.next();
        assertTrue("DeletionInfoPacket has less than one element!", delPacketIterator.hasNext());
        delPacketIterator.next();
        assertFalse("DeletionInfoPacket has more than two elements!", delPacketIterator.hasNext());
        
        // verify that the two deletion records match the DHT keys and auth keys of the deleted packets
        DeleteRequest newDelRequest = folder.storeAndCreateDeleteRequest(indexPacket);
        assertTrue(newDelRequest instanceof IndexPacketDeleteRequest);
        IndexPacketDeleteRequest newIndexPacketDelRequest = (IndexPacketDeleteRequest)newDelRequest;
        assertEquals(newIndexPacketDelRequest.getDeleteAuthorization(dhtKey1), unencryptedPacket1.getDeleteAuthorization());
        assertEquals(newIndexPacketDelRequest.getDeleteAuthorization(dhtKey3), unencryptedPacket2.getDeleteAuthorization());
    }
    
    @After
    public void tearDown() throws Exception {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
        testDir.delete();
    }

    @Test
    public void testIndividualPackets() {
        IndexPacket indexPacket1 = new IndexPacket(destination1);
        indexPacket1.put(emailPacket1);
        IndexPacket indexPacket2 = new IndexPacket(destination2);
        IndexPacket indexPacket3 = new IndexPacket(destination3);
        indexPacket2.put(emailPacket1);
        indexPacket2.put(emailPacket2);
        folder.store(indexPacket1);
        folder.store(indexPacket2);
        folder.store(indexPacket3);
        
        Iterator<IndexPacket> iterator = folder.individualPackets();
        int numIndivEntries = 0;
        while (iterator.hasNext()) {
            iterator.next();
            numIndivEntries++;
        }
        assertEquals(3, numIndivEntries);
    }
    
    /**
     * A modified version of {@link IndexPacketFolder} that does not update the store times
     * of packet entries to the current time when the packet is written to a file.
     * <p/>
     * (this class is only needed in order not to to have to change production code to
     * accommodate unit tests)
     */
    private static class SettableStoreTimeIndexPacketFolder extends IndexPacketFolder {
        Map<Hash, Long> overrideTimes;
        
        SettableStoreTimeIndexPacketFolder(File storagedir) {
            super(storagedir);
            overrideTimes = new HashMap<Hash, Long>();
        }
        
        @Override
        public synchronized void store(DhtStorablePacket packetToStore) {
            assertTrue(packetToStore instanceof IndexPacket);
            IndexPacket indexPacket = (IndexPacket)packetToStore;
            
            for (IndexPacketEntry entry: indexPacket)
                overrideTimes.put(entry.emailPacketKey, entry.storeTime);
            super.store(packetToStore);
        }
        
        /**
         * Called by store(DhtStorablePacket)
         */
        @Override
        protected void add(I2PBotePacket packetToStore, String filename) {
            assertTrue(packetToStore instanceof IndexPacket);
            IndexPacket indexPacket = (IndexPacket)packetToStore;
            
            for (IndexPacketEntry entry: indexPacket)
                entry.storeTime = overrideTimes.get(entry.emailPacketKey);
            super.add(indexPacket, filename);
        }
    }
}
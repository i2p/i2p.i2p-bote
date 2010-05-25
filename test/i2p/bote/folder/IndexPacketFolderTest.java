package i2p.bote.folder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i2p.bote.UniqueId;
import i2p.bote.email.EmailDestination;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.EncryptedEmailPacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.IndexPacketEntry;
import i2p.bote.packet.UnencryptedEmailPacket;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Hash;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexPacketFolderTest {
    private IndexPacketFolder folder;
    private EncryptedEmailPacket emailPacket1;
    private EncryptedEmailPacket emailPacket2;
    private EmailDestination destination;
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
        int numFragments = 1;
        UnencryptedEmailPacket unencryptedPacket1 = new UnencryptedEmailPacket(messageId, fragmentIndex, numFragments, content1);
        UnencryptedEmailPacket unencryptedPacket2 = new UnencryptedEmailPacket(messageId, fragmentIndex, numFragments, content2);
        
        String base64Dest = "m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY";
        destination = new EmailDestination(base64Dest);
        emailPacket1 = new EncryptedEmailPacket(unencryptedPacket1, destination);
        emailPacket2 = new EncryptedEmailPacket(unencryptedPacket2, destination);
    }

    @Test
    public void testCheckExpiration() throws GeneralSecurityException, InterruptedException {
        long cutoffTime = System.currentTimeMillis() - ExpirationListener.EXPIRATION_TIME_MILLISECONDS;
        
        // store a packet that expired 3 seconds ago
        IndexPacket indexPacket = new IndexPacket(destination);
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
    
    @After
    public void tearDown() throws Exception {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
        testDir.delete();
    }

    /**
     * A modified version of {@link IndexPacketFolder} that does not update the store times
     * of packet entries to the current time when the packet is written to a file.
     * 
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
        protected void add(DataPacket packetToStore, String filename) {
            assertTrue(packetToStore instanceof IndexPacket);
            IndexPacket indexPacket = (IndexPacket)packetToStore;
            
            for (IndexPacketEntry entry: indexPacket)
                entry.storeTime = overrideTimes.get(entry.emailPacketKey);
            super.add(indexPacket, filename);
        }
    }
}
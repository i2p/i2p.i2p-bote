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
import i2p.bote.UniqueId;
import i2p.bote.email.EmailDestination;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.MalformedPacketException;
import i2p.bote.packet.TypeCode;
import i2p.bote.packet.dht.DeleteRequest;
import i2p.bote.packet.dht.DeletionInfoPacket;
import i2p.bote.packet.dht.DeletionRecord;
import i2p.bote.packet.dht.EmailPacketDeleteRequest;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.UnencryptedEmailPacket;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Base64;
import net.i2p.data.Destination;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmailPacketFolderTest {
    private File testDir;
    private File folderDir;
    private EmailPacketFolder packetFolder;
    private EncryptedEmailPacket emailPacket;
    private UnencryptedEmailPacket unencryptedPacket;
    private UnencryptedEmailPacket unencryptedPacket2;
    private EmailDestination recipient;
    private Destination sender;

    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "EmailPacketFolderTest-" + System.currentTimeMillis());
        folderDir = new File(testDir, "dht_email_pkt");
        packetFolder = new EmailPacketFolder(folderDir);
        
        // make two UnencryptedEmailPackets with different contents
        byte[] content1 = "test TEST test ABCDEFGH asdfsadfsadf 3487562384".getBytes();
        byte[] content2 = "fdlkhgjfljh test 123456".getBytes();
        byte[] messageIdBytes = new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        int fragmentIndex = 0;
        unencryptedPacket = new UnencryptedEmailPacket(new ByteArrayInputStream(content1), messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        unencryptedPacket.setNumFragments(1);
        unencryptedPacket2 = new UnencryptedEmailPacket(new ByteArrayInputStream(content2), messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        unencryptedPacket.setNumFragments(1);
        
        String base64Dest = "m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY";
        recipient = new EmailDestination(base64Dest);
        emailPacket = new EncryptedEmailPacket(unencryptedPacket, recipient);
        
        sender = new Destination("X3oKYQJ~1EAz7B1ZYGSrOTIMCW5Rnn2Svoc38dx5D9~zvz8vqiWcH-pCqQDwLgPWl9RTBzHtTmZcGRPXIv54i0XWeUfX6rTPDQGuZsnBMM0xrkH2FNLNFaJa0NgW3uKXWpNj9AI1AXUXzK-2MYTYoaZHx5SBoCaKfAGMcFJvTON1~kopxBxdBF9Q7T4~PJ3I2LeU-ycmUlehe9N9bIu7adUGyPGVl8Ka-UxwQromoJ~vSWHHl8HkwcDkW--v9Aj~wvFqxqriFkB1EeBiThi3V4XtVY~GUP4IkRj9YZGTsSBf3eS4xwXgnYWlB7IvxAGBfHY9MCg3lbAa1Dg~1IH6rhtXxsXUtGcXsz9yMZTxXHd~rGo~JrXeM1y~Vcenpr6tJcum6pxevkKzzT0qDegGPH3Zhqz7sSeeIaJEcPBUAkX89csqyFWFIjTMm6yZp2rW-QYUnVNLNTjf7vndYUAEICogAkq~btqpIzrGEpm3Pr9F23br3SpbOmdxQxg51AMmAAAA");
    }

    @After
    public void tearDown() throws Exception {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
        testDir.delete();
    }

    /** Tests processing of one valid and one invalid {@link EmailPacketDeleteRequest}. */
    @Test
    public void testPacketReceived() throws PasswordException {
        packetFolder.store(emailPacket);
        assertEquals(1, packetFolder.getElements().size());
        EmailPacketDeleteRequest delRequest;
        
        // send an invalid delete request
        byte[] delAuthBytes = unencryptedPacket.getDeleteAuthorization().toByteArray().clone();
        delAuthBytes[5] ^= 1;
        UniqueId invalidAuthorization = new UniqueId(delAuthBytes, 0);
        delRequest = new EmailPacketDeleteRequest(emailPacket.getDhtKey(), invalidAuthorization);
        packetFolder.packetReceived(delRequest, sender, System.currentTimeMillis());
        assertEquals(1, packetFolder.getElements().size());
        
        // send a valid delete request
        delRequest = new EmailPacketDeleteRequest(emailPacket.getDhtKey(), unencryptedPacket.getDeleteAuthorization());
        packetFolder.packetReceived(delRequest, sender, System.currentTimeMillis());
        assertEquals(0, packetFolder.getElements().size());
    }
    
    /**
     * Tests creation of delete records when processing {@link EmailPacketDeleteRequest}s,
     * and matching them against email packets.
     */
    @Test
    public void testProcessDeleteRequest() throws GeneralSecurityException, InvalidCipherTextException, MalformedPacketException, PasswordException {
        // create two packets whose base64 DHT keys start with the same two characters
        byte[] emailPkt1Data = Base64.decode("RQTAJHelC-wnxq-OUAnfgqgN84zTCimrfUKw7AgDPHblcwAAAADUk24QY3EPEzk0UmB5PYQxwnEVIWGZkWJ-5ERh30l95QMA0gIs-itszDaiCa4ucekxDL6pZCVMTnI~S0nBx2FQ5Fl4-T50bNojkgJiQZNFB-vwQ4ChB~hxYIomMtbN6tiNDVwYRKjeZHbL3MAffKcwxwF3iPMl8aLLBaU6LxQ~3r7tgTiccHE39Ozm88~Hrf7H-5hWXzTXwm7j1pNJA7hc-wCL3BwngRl8KUsqsII2-PTMI34-a3RfpzMf2cKlY0NVayoW1UxC4dLvtSz-HoLYPMGIMf5zQghPS2PuZaRAKa5oa3PdYJrhvNXCJWpYCL~FynwShQ==");
        byte[] emailPkt2Data = Base64.decode("RQTAJPXENHAvW18uRITLC8n~D0Npd0RwI1e~9IR8QN~BSwAAAAAD9RT-PXacKIest9~E2SzsR5dAlaoh-ZZVcFNcsQbGQwECkgCFnuT5N210f4SbS4pjPeh~hk9hvpEGCEXzOmPfSEeSWhrE5oiQuJI6fet1zrwZfbA6Iqvl-PoLgv5nKU~I7Nlu1f9UXuve2cvQOmTGDlHdzpwd3nTyJCR2bqG4SFPlnaDpa7yDOmH~e8LKBw1YTYSHSyVun7XuijyCcWGKRbSm3tFU382JSSqUQ2APwbHPtPG6akYO5iSq7XMvlBSyLsDHM5wDbaptxEUmnW20x3fVsK-0BNsIXFK-JJVQ12NApVanGaOsyDRh8l-geRWBcYpX7J~RI1A3ZKRBRJj2wgBc6TdAG0-jiI4OfpKZMr2NZ-ugg2-phU9OgY5ZbtBZjlbtADDYlxT~GHvXkCQLwRg4fsxTk9HoshfoWea-4RE1YmwRT96uKRy0SjcUduOPCsI6nEwm1p-5lzGFzcVTTXZyARRzRrhvtAJw616o3RoAj7vGHY~1POJ26m3UBUIXjh2fA7b5HTFARw9VfZYuz9zzT7UjtuPVIgBu-HyX5zBgSrCDhykGqggaVbURCaS2B6BZt8ikU0ponchHkVZzqX2UeT4T1Pd3fyc1fXp1xVInrYWUZvjSoBcJaiqlhvTMMsjzLbiG~a~HoJz67Dvl6GGtcBad1hjhSzwKN9vLpM-sFMqDCBDHDssVZEXk5EXQYltUZVvfaFJYimDfdiM~F23x0Wz2tM5LQXJ86-nn7d5DA-CspMg1xqkAr1iMkgYacOL~2u2PL1ImI1SMZGmY839ekgQlr1gWWU1evQQTUdHH6Bszs0vSW6wymbSTziJopMqv86f2y6SHDEq8qZzYdKhAFQbjFtvNwn7gEJqlhzzI1bO8zjG3xZTiaCpSrmE98L8PZhEIfXvN3PUADSuzsbHmuHQ=");
        EncryptedEmailPacket emailPacket1 = new EncryptedEmailPacket(emailPkt1Data);
        EncryptedEmailPacket emailPacket2 = new EncryptedEmailPacket(emailPkt2Data);
        UniqueId delAuthKey1 = new UniqueId("nJlsaGZb1k6OFze1fUsHEF0osP5Wv9Rgta8EwHl8Te8=");   // matches emailPkt1Data
        UniqueId delAuthKey2 = new UniqueId("ImXwDy7HZq7pyjGwxpYmlwaKAWoQlUl2fdrBK~mTt0g=");   // matches emailPkt2Data
        String prefix1 = emailPacket1.getDhtKey().toBase64().substring(0, 2);
        String prefix2 = emailPacket2.getDhtKey().toBase64().substring(0, 2);
        assertEquals("Error setting up test packets: prefixes of DHT keys differ", prefix1, prefix2);
        assertFalse("Error setting up test packets: DHT keys should be different", emailPacket1.getDhtKey().equals(emailPacket2.getDhtKey()));
        
        // add two packets and delete them via delete requests
        packetFolder.store(emailPacket1);
        assertEquals("Folder should have exactly one element!", 1, packetFolder.getElements().size());
        packetFolder.store(emailPacket2);
        assertEquals("Folder should have two elements!", 2, packetFolder.getElements().size());
        
        EmailPacketDeleteRequest delRequest1 = new EmailPacketDeleteRequest(emailPacket1.getDhtKey(), delAuthKey1);
        packetFolder.process(delRequest1);
        assertEquals("Folder should have exactly one element!", 1, packetFolder.getElements().size());
        EmailPacketDeleteRequest delRequest2 = new EmailPacketDeleteRequest(emailPacket2.getDhtKey(), delAuthKey2);
        packetFolder.process(delRequest2);
        assertEquals("Folder should be empty!", 0, packetFolder.getElements().size());
        
        // verify that there is one deletion file containing two entries
        File[] files = packetFolder.getStorageDirectory().listFiles();
        assertEquals(1, files.length);
        assertTrue(files[0].getName().startsWith("DEL_"));
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
        DeleteRequest newDelRequest1 = packetFolder.storeAndCreateDeleteRequest(emailPacket1);
        assertTrue(newDelRequest1 instanceof EmailPacketDeleteRequest);
        EmailPacketDeleteRequest newEmailPacketDelRequest1 = (EmailPacketDeleteRequest)newDelRequest1;
        assertEquals(newEmailPacketDelRequest1.getDhtKey(), emailPacket1.getDhtKey());
        assertEquals(newEmailPacketDelRequest1.getAuthorization(), delAuthKey1);
        DeleteRequest newDelRequest2 = packetFolder.storeAndCreateDeleteRequest(emailPacket2);
        assertTrue(newDelRequest2 instanceof EmailPacketDeleteRequest);
        EmailPacketDeleteRequest newEmailPacketDelRequest2 = (EmailPacketDeleteRequest)newDelRequest2;
        assertEquals(newEmailPacketDelRequest2.getDhtKey(), emailPacket2.getDhtKey());
        assertEquals(newEmailPacketDelRequest2.getAuthorization(), delAuthKey2);
    }
    
    @Test
    public void testCheckExpiration() throws GeneralSecurityException, InterruptedException, PasswordException {
        final long cutoffTime = System.currentTimeMillis() - ExpirationListener.EXPIRATION_TIME_MILLISECONDS;

        // store a packet that expired 10 seconds ago
        long expirationTime1 = cutoffTime - 10*1000;
        EncryptedEmailPacket emailPacket1 = new SettableStoreTimeEncryptedEmailPacket(unencryptedPacket, recipient, expirationTime1);
        packetFolder.store(emailPacket1);
        assertEquals(expirationTime1/1000L, packetFolder.getElements().get(0).getStoreTime()/1000L);   // round to seconds
        
        // store a packet that expires in 10 seconds
        long expirationTime2 = cutoffTime + 10*1000;
        EncryptedEmailPacket emailPacket2 = new SettableStoreTimeEncryptedEmailPacket(unencryptedPacket2, recipient, expirationTime2);
        packetFolder.store(emailPacket2);
        
        assertEquals(2, packetFolder.getElements().size());
        
        // delete expired packets and check that one of the two packets got deleted
        packetFolder.deleteExpired();
        assertEquals(1, packetFolder.getElements().size());
        
        // 11 seconds later, the remaining packet should have expired
        TimeUnit.SECONDS.sleep(11);
        packetFolder.deleteExpired();
        assertEquals(0, packetFolder.getElements().size());
    }
    
    /**
     * A modified version of {@link EncryptedEmailPacket} that allows for the store time
     * to be set externally (the store time would otherwise be set to the current time).
     * 
     * (this class is only needed in order not to to have to change production code to
     * accommodate unit tests)
     */
    private static class SettableStoreTimeEncryptedEmailPacket extends EncryptedEmailPacket {
        long storeTime;
        
        SettableStoreTimeEncryptedEmailPacket(UnencryptedEmailPacket unencryptedPacket, EmailDestination emailDestination, long storeTime) throws GeneralSecurityException {
            super(unencryptedPacket, emailDestination);
            this.storeTime = storeTime;
        }
        
        @Override
        public void setStoreTime(long time) {
            super.setStoreTime(storeTime);
        }
        
        /**
         * Overridden because the parent method doesn't work due to the {@link TypeCode}
         * annotation not being inherited. This method returns the correct
         * <code>EncryptedEmailPacket</code> type code.
         */
        @Override
        protected char getPacketTypeCode(Class<? extends I2PBotePacket> dataType) {
            return EncryptedEmailPacket.class.getAnnotation(TypeCode.class).value();
        }
    }
}
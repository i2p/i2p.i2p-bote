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
import i2p.bote.UniqueId;
import i2p.bote.email.EmailDestination;
import i2p.bote.packet.EmailPacketDeleteRequest;
import i2p.bote.packet.EncryptedEmailPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.TypeCode;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Destination;

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
        int numFragments = 1;
        unencryptedPacket = new UnencryptedEmailPacket(messageId, fragmentIndex, numFragments, content1);
        unencryptedPacket2 = new UnencryptedEmailPacket(messageId, fragmentIndex, numFragments, content2);
        
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

    /** Tests processing of {@link EmailPacketDeleteRequest}s. */
    @Test
    public void testPacketReceived() {
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
    
    @Test
    public void testCheckExpiration() throws GeneralSecurityException, InterruptedException {
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
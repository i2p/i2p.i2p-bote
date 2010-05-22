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
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.File;

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
    private Destination sender;

    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "IncompleteEmailFolderTest-" + System.currentTimeMillis());
        folderDir = new File(testDir, "dht_email_pkt");
        packetFolder = new EmailPacketFolder(folderDir);
        
        // make an EncryptedEmailPacket
        byte[] content = "test TEST test ABCDEFGH asdfsadfsadf 3487562384".getBytes();
        byte[] messageIdBytes = new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        int fragmentIndex = 0;
        int numFragments = 1;
        unencryptedPacket = new UnencryptedEmailPacket(messageId, fragmentIndex, numFragments, content);
        String base64Dest = "m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY";
        EmailDestination destination = new EmailDestination(base64Dest);
        emailPacket = new EncryptedEmailPacket(unencryptedPacket, destination);
        
        sender = new Destination("X3oKYQJ~1EAz7B1ZYGSrOTIMCW5Rnn2Svoc38dx5D9~zvz8vqiWcH-pCqQDwLgPWl9RTBzHtTmZcGRPXIv54i0XWeUfX6rTPDQGuZsnBMM0xrkH2FNLNFaJa0NgW3uKXWpNj9AI1AXUXzK-2MYTYoaZHx5SBoCaKfAGMcFJvTON1~kopxBxdBF9Q7T4~PJ3I2LeU-ycmUlehe9N9bIu7adUGyPGVl8Ka-UxwQromoJ~vSWHHl8HkwcDkW--v9Aj~wvFqxqriFkB1EeBiThi3V4XtVY~GUP4IkRj9YZGTsSBf3eS4xwXgnYWlB7IvxAGBfHY9MCg3lbAa1Dg~1IH6rhtXxsXUtGcXsz9yMZTxXHd~rGo~JrXeM1y~Vcenpr6tJcum6pxevkKzzT0qDegGPH3Zhqz7sSeeIaJEcPBUAkX89csqyFWFIjTMm6yZp2rW-QYUnVNLNTjf7vndYUAEICogAkq~btqpIzrGEpm3Pr9F23br3SpbOmdxQxg51AMmAAAA");
    }

    @After
    public void tearDown() throws Exception {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
        testDir.delete();
    }

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
}
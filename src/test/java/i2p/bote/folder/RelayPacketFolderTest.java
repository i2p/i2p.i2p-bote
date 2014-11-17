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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import i2p.bote.UniqueId;
import i2p.bote.email.EmailDestination;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.StoreRequest;
import i2p.bote.packet.dht.UnencryptedEmailPacket;
import i2p.bote.packet.relay.RelayRequest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Destination;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RelayPacketFolderTest {
    private File testDir;
    private File folderDir;
    private RelayPacketFolder folder;
    private EncryptedEmailPacket emailPacket;
    private RelayRequest relayRequest;

    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "RelayPacketFolderTest-" + System.currentTimeMillis());
        folderDir = new File(testDir, "relay_pkt");
        folder = new RelayPacketFolder(folderDir);
        
        // make an EncryptedEmailPacket
        String base64Destination = "X3oKYQJ~1EAz7B1ZYGSrOTIMCW5Rnn2Svoc38dx5D9~zvz8vqiWcH-pCqQDwLgPWl9RTBzHtTmZcGRPXIv54i0XWeUfX6rTPDQGuZsnBMM0xrkH2FNLNFaJa0NgW3uKXWpNj9AI1AXUXzK-2MYTYoaZHx5SBoCaKfAGMcFJvTON1~kopxBxdBF9Q7T4~PJ3I2LeU-ycmUlehe9N9bIu7adUGyPGVl8Ka-UxwQromoJ~vSWHHl8HkwcDkW--v9Aj~wvFqxqriFkB1EeBiThi3V4XtVY~GUP4IkRj9YZGTsSBf3eS4xwXgnYWlB7IvxAGBfHY9MCg3lbAa1Dg~1IH6rhtXxsXUtGcXsz9yMZTxXHd~rGo~JrXeM1y~Vcenpr6tJcum6pxevkKzzT0qDegGPH3Zhqz7sSeeIaJEcPBUAkX89csqyFWFIjTMm6yZp2rW-QYUnVNLNTjf7vndYUAEICogAkq~btqpIzrGEpm3Pr9F23br3SpbOmdxQxg51AMmAAAA";
        Destination nextDestination = new Destination(base64Destination.substring(0, 516));
        long delayMilliseconds = TimeUnit.MILLISECONDS.convert(111, TimeUnit.MINUTES);
        String content =
            "Warum, warum, warum\n" +
            "Ist die Banane krumm?\n" +
            "Weil niemand in den Urwald zog\n" +
            "Und die Banane grade bog.\n";
        byte[] messageIdBytes = new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        int fragmentIndex = 0;
        InputStream contentStream = new ByteArrayInputStream(content.getBytes());
        UnencryptedEmailPacket unencryptedPacket = new UnencryptedEmailPacket(contentStream, messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        unencryptedPacket.setNumFragments(1);
        String base64EmailDest = "rIbyUukqtsacD-MDJJ8KbIP9d3WQQo~t~zysc3bNcF1mSwz9PcGJnvWCNhnG2nzbdUAIDouESZjLRnBr7-mxNS";
        EmailDestination recipient = new EmailDestination(base64EmailDest);
        emailPacket = new EncryptedEmailPacket(unencryptedPacket, recipient);
        
        // make a RelayRequest
        StoreRequest storeRequest = new StoreRequest(emailPacket);
        relayRequest = new RelayRequest(storeRequest, nextDestination, delayMilliseconds, 1000);
    }

    @After
    public void tearDown() throws Exception {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
        testDir.delete();
    }
    
    /**
     * Tests {@link RelayPacketFolder#add(RelayRequest)} and removal of the packet from the
     * folder via an <code>Iterator</code>.
     */
    @Test
    public void testAddRemove() {
        folder.add(relayRequest);
        Iterator<RelayRequest> iterator = folder.iterator();
        assertTrue("Folder is empty after a packet was added to it!", iterator.hasNext());
        
        // Read the stored packet, convert to a byte array, and compare.
        // This also verifies the send time which is not stored in the packet itself
        RelayRequest storedPacket = iterator.next();
        byte[] arrayA = relayRequest.toByteArray();
        byte[] arrayB = storedPacket.toByteArray();
        assertArrayEquals("The two arrays differ!", arrayA, arrayB);
        assertFalse("Folder has more than one element!", iterator.hasNext());

        iterator.remove();
        assertFalse("Packet was not deleted!", folder.iterator().hasNext());
    }
}
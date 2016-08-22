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
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.UnencryptedEmailPacket;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FolderTest {
    private File folderDir;
    private EmailPacketFolder folder;
    private EncryptedEmailPacket emailPacket1;
    private EncryptedEmailPacket emailPacket2;

    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        folderDir = new File(tempDir, "FolderTest-" + System.currentTimeMillis());
        folder = new EmailPacketFolder(folderDir);

        // make two email packets
        EmailDestination dest = new EmailDestination("3LbBiN2nxtQVxPXYBQL3~PjBg-xOPalsFKZ0YqobHXP1u3MiBxqthF6TJxqdPS2LWWKb90FVzaPyIIEQOT0qSb");
        emailPacket1 = createEmailPacket(dest, "Gallia est omnis divisa in partes tres...");
        emailPacket2 = createEmailPacket(dest, "...quarum unam incolunt Belgae, aliam Aquitani, tertiam qui ipsorum lingua Celtae, nostra Galli appellantur.");

        // create two valid files and three invalid files in the folder
        folder.store(emailPacket1);
        folder.store(emailPacket2);
        writeToFile("test1.pkt", "asdfasdf");
        writeToFile("test2.pkt", "12345");
        writeToFile("test3.pkt", "xyz");
    }

    EncryptedEmailPacket createEmailPacket(EmailDestination dest, String message) throws Exception {
        byte[] content = message.getBytes();
        byte[] messageIdBytes = new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        int fragmentIndex = 0;
        UnencryptedEmailPacket plaintextPacket = new UnencryptedEmailPacket(new ByteArrayInputStream(content), messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        plaintextPacket.setNumFragments(1);
        
        return new EncryptedEmailPacket(plaintextPacket, dest);
    }

    void writeToFile(String filename, String data) throws IOException {
        File file = new File(folderDir, filename);
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(data);
        }
        finally {
            if (writer != null)
                writer.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
    }
    
    @Test
    public void testIterator() {
        assertEquals(5, folder.getFilenames().length);
        
        Iterator<EncryptedEmailPacket> iterator = folder.iterator();
        assertTrue("Folder is empty!", iterator.hasNext());
        
        EncryptedEmailPacket retrievedPacket1 = iterator.next();
        iterator.remove();
        assertTrue("Folder has less than two elements!", iterator.hasNext());
        
        EncryptedEmailPacket retrievedPacket2 = iterator.next();
        iterator.remove();
        assertFalse("Folder is not empty!", iterator.hasNext());
        
        // the iterator might return the packets in a different order than they were added, so just check that each retrieved packet matches one of the original packets
        assertTrue("Folder packet 1 differs from the original!", equals(emailPacket1, retrievedPacket1) || equals(emailPacket2, retrievedPacket1));
        assertTrue("Folder packet 2 differs from the original!", equals(emailPacket1, retrievedPacket2) || equals(emailPacket2, retrievedPacket2));
        assertFalse("The two retrieved packets are equal!", equals(retrievedPacket1, retrievedPacket2));
        
        assertFalse("Packets were not deleted!", folder.iterator().hasNext());
    }

    boolean equals(EncryptedEmailPacket packet1, EncryptedEmailPacket packet2) {
        byte[] arrayA = packet1.toByteArray();
        byte[] arrayB = packet2.toByteArray();
        return Arrays.equals(arrayA, arrayB);
    }
}
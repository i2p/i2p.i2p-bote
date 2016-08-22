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

import static org.junit.Assert.assertTrue;
import i2p.bote.TestUtil;
import i2p.bote.crypto.KeyUpdateHandler;
import i2p.bote.email.Email;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordCache;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.dht.UnencryptedEmailPacket;

import java.io.File;
import java.util.Collection;
import java.util.Random;

import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IncompleteEmailFolderTest {
    private static final String MSG_ID_CACHE_DIR = "msgidcache.txt";
    
    private File inboxDir;
    private EmailFolder inbox;
    private IncompleteEmailFolder incompleteFolder;
    private File testDir;

    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "IncompleteEmailFolderTest-" + System.currentTimeMillis());

        PasswordCache passwordCache = TestUtil.createPasswordCache(testDir);
        
        inboxDir = new File(testDir, "inbox");
        inbox = new EmailFolder(inboxDir, passwordCache);
        
        File incompleteDir = new File(testDir, "incomplete");
        MessageIdCache messageIdCache = new MessageIdCache(new File(testDir, MSG_ID_CACHE_DIR), 1000);
        incompleteFolder = new IncompleteEmailFolder(incompleteDir, messageIdCache, inbox);
    }
    
    @After
    public void tearDown() throws Exception {
        for (File file: inboxDir.listFiles())
            file.delete();
        inboxDir.delete();
        incompleteFolder.getStorageDirectory().delete();
        new File(testDir, MSG_ID_CACHE_DIR).delete();
        TestUtil.deleteGeneratedFiles(testDir);
        testDir.delete();
        assertTrue(!testDir.exists());
    }
    
    @Test
    public void testAddSinglePacketEmail() throws Exception {
        testAddEmail("Test message", 1);
    }
    
    @Test
    public void testAddThreePacketEmail() throws Exception {
        // Create a 29,000-char string. Use random data (more or less, because it has to be
        // US ASCII chars) so it doesn't get compressed into less than 3 packets.
        Random rng = new Random();
        rng.setSeed(0);
        byte[] message = new byte[29000];
        for (int i=0; i<message.length; i++)
            message[i] = (byte)(32 + rng.nextInt(127-32));
        
        testAddEmail(new String(message), 3);
    }

    private void testAddEmail(String mailContent, int expectedNumPackets) throws Exception {
        Email email = new Email(true);
        String recipient = "test@bote.i2p";
        email.addRecipient(RecipientType.TO, new InternetAddress(recipient));
        email.setText(mailContent);
        
        EmailIdentity identity = new EmailIdentity("DVkhqF6R9SHB5svViGtqRYZO7oI-0-omnIFtae29fNnNtTTH2j37Fr5fWp4t6rseTjiJ8gwg08DnbA4qP72aSQcDQPSErOELOMSU5BUTtsT8hnv1-DKdhIn~1qoIjxzIFHbxT3xnR3nFI7lKd6couscilzPBCjoFDUKb5ds2u23RO29K7~EKxU1O7Ltu6sT5etXkJkhAziOcuyfZyxJXqH1caYX5e2aWIhY3D2ESfy4nMK66r5KcDVQOPTzCkJq6d1FFOmnDGrlJjN~HgHmfUCtLbO~TLugWx9FCiDGfPkBb-3ODYTDaUR1zobOj1tiffV3Nm73PsYddRt84emLKzIRsC77JJpflw~h8UIRYJ29vJDf4VQ54BhZcelmN192sIrWr2nKN8n6PpSP4LI4RAuG2UvLytnDYzFM7O9WcnFP2-Qs3t1lD9aF72JVTYTpH5PZupnB1cglSsdRg8RmtRa41Fseyx8D3EdH~DCdpMGmfupaWp9~dKpFMleqk9scRAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABTjDxn3wEOjCjJ4APg~2IGpqWwy2Hw728aZ3eCC5l0MP913BLdIfSUiXPbs6sN9A2");
        KeyUpdateHandler keyUpdateHandler = TestUtil.createDummyKeyUpdateHandler();
        Collection<UnencryptedEmailPacket> packets = email.createEmailPackets(identity, keyUpdateHandler, recipient, I2PBotePacket.MAX_DATAGRAM_SIZE);
        assertTrue("Expected " + expectedNumPackets + " email packets, got " + packets.size(), packets.size() == expectedNumPackets);
        
        assertTrue("The inbox should be empty at this point!", inbox.getElements().size() == 0);
        for (UnencryptedEmailPacket emailPacket: packets)
            incompleteFolder.addEmailPacket(emailPacket);
        
        assertTrue("The incomplete emails folder is not empty!", incompleteFolder.getElements().size() == 0);
        assertTrue("Expected: one email in the inbox, actual number = " + inbox.getElements().size(), inbox.getElements().size() == 1);
        // Verify that the original email and the email in the folder are the same except for the signature header
        Email storedEmail = inbox.getElements().iterator().next();
        storedEmail.removeHeader("X-I2PBote-Sig-Valid");
        TestUtil.assertEquals("Stored email differs from original email!", email, storedEmail);
    }
}
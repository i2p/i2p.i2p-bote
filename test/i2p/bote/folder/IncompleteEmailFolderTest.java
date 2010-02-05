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
import static org.junit.Assert.assertTrue;
import i2p.bote.email.Email;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.File;
import java.util.Collection;

import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IncompleteEmailFolderTest {
    private File inboxDir;
	private EmailFolder inbox;
	private IncompleteEmailFolder incompleteFolder;
	private File testDir;

	@Before
    public void setUp() throws Exception {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		testDir = new File(tempDir, "IncompleteEmailFolderTest-" + System.currentTimeMillis());
		
		inboxDir = new File(testDir, "inbox");
		inbox = new EmailFolder(inboxDir);
		
		File incompleteDir = new File(testDir, "incomplete");
		incompleteFolder = new IncompleteEmailFolder(incompleteDir, inbox);
	}
	
	@After
    public void tearDown() throws Exception {
	    for (File file: inboxDir.listFiles())
	        file.delete();
	    inboxDir.delete();
		incompleteFolder.getStorageDirectory().delete();
		testDir.delete();
    }
	
	@Test
	public void testAddSinglePacketEmail() throws Exception {
		testAddEmail("Test message", 1);
	}
	
	@Test
	public void testAddThreePacketEmail() throws Exception {
		StringBuilder stringBuilder = new StringBuilder();
		// create a 90,000-char string
		for (int i=0; i<9000; i++)
			stringBuilder.append("0123456789");

        testAddEmail(stringBuilder.toString(), 3);
	}

    private void testAddEmail(String mailContent, int expectedNumPackets) throws Exception {
        Email email = new Email();
        String recipient = "test@bote.i2p";
        email.addRecipient(RecipientType.TO, new InternetAddress(recipient));
        email.setText(mailContent);
        
        Collection<UnencryptedEmailPacket> packets = email.createEmailPackets(recipient);
        assertTrue("Expected " + expectedNumPackets + " email packets, got " + packets.size(), packets.size() == expectedNumPackets);
        
        assertTrue("The inbox should be empty at this point!", inbox.getElements().size() == 0);
        for (UnencryptedEmailPacket emailPacket: packets)
            incompleteFolder.add(emailPacket);
        
        assertTrue("The incomplete emails folder is not empty!", incompleteFolder.getElements().size() == 0);
        assertTrue("Expected: one email in the inbox, actual number = " + inbox.getElements().size(), inbox.getElements().size() == 1);
        assertEquals("Content of stored email differs from content of original email!", email.getContent(), inbox.getElements().iterator().next().getContent());
    }
}
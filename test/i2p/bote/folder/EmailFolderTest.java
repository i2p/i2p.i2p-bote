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
import i2p.bote.TestUtil;
import i2p.bote.email.Email;

import java.io.File;
import java.io.IOException;

import javax.mail.MessagingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmailFolderTest {
    private File testDir;
    private File folderDir1;
    private File folderDir2;
    Email email1;
    Email email2;
    EmailFolder folder1;
    EmailFolder folder2;
    
    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "EmailFolderTest-" + System.currentTimeMillis());
        folderDir1 = new File(testDir, "folder1");
        folderDir2 = new File(testDir, "folder2");
        email1 = new Email(true);
        email1.setText("asdfasdf");
        email2 = new Email(false);
        email2.setText("asdfasdf");
        folder1 = new EmailFolder(folderDir1);
        folder2 = new EmailFolder(folderDir2);
    }
    
    @After
    public void tearDown() throws Exception {
        deleteFolder(folderDir1);
        deleteFolder(folderDir2);
    }

    private void deleteFolder(File folderDir) {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
        testDir.delete();
    }
    
    @Test
    public void testAdd() throws IOException, MessagingException {
        assertEquals(0, folderDir1.list().length);
        folder1.add(email1);
        assertEquals(2, folderDir1.list().length);
        folder1.add(email2);
        assertEquals(4, folderDir1.list().length);
        
        String messageId1 = email1.getMessageID();
        TestUtil.assertEquals("Email in folder differs from the original!", email1, folder1.getEmail(messageId1));
        TestUtil.assertUnequal("Folder returned the wrong email!", email2, folder1.getEmail(messageId1));
        String messageId2 = email2.getMessageID();
        TestUtil.assertEquals("Email in folder differs from the original!", email2, folder1.getEmail(messageId2));
        TestUtil.assertUnequal("Folder returned the wrong email!", email2, folder2.getEmail(messageId2));
    }
    
    @Test
    public void testMove() throws IOException, MessagingException {
        folder1.add(email1);
        folder1.move(email1, folder2);
        assertEquals("Source folder is not empty!", 0, folderDir1.list().length);
        assertEquals("Target folder does not contain two files!", 2, folderDir2.list().length);
        TestUtil.assertEquals("Email differs from the original!", email1, folder2.iterator().next());
    }

    @Test
    public void testDelete() throws IOException, MessagingException {
        folder1.add(email1);
        folder1.delete(email1.getMessageID());
        assertEquals("The email file and/or the metadata were not deleted!", 0, folderDir1.list().length);
    }
    
    @Test
    public void testSetNew() throws IOException, MessagingException {
        folder1.add(email1);
        Email emailFromFolder = folder1.iterator().next();
        assertEquals("\"new\" flag is false after adding email to folder!", emailFromFolder.isNew(), true);
        folder1.setNew(email1.getMessageID(), false);
    }
}
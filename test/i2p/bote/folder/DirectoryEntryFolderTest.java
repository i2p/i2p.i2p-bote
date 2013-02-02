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
import i2p.bote.TestUtil;
import i2p.bote.email.EmailDestination;
import i2p.bote.packet.dht.Contact;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DirectoryEntryFolderTest {
    private File testDir;
    private File folderDir;
    private DirectoryEntryFolder folder;
    private Contact contact1;
    private Contact contact2;
    private Contact contact3;

    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "DirectoryEntryFolderTest-" + System.currentTimeMillis());
        folderDir = new File(testDir, "folder");
        folder = new DirectoryEntryFolder(folderDir);
        
        EmailDestination destination1 = new EmailDestination("xvGHjFocWgadAsrUqFspuWyRrpoa5Hh~SsFBxFjeTZ6tx~IqWbtxt3pV5fzHLkvgFHx0jpTgIStssa98sD5NIU");
        EmailDestination destination2 = new EmailDestination("t6JpsgAD~TrlH~bafT0CvUPk5jfQDcISB2IG6hCE1AS1kh3urYt29bPVvhaRh2gDFk2eyz-MZRoMpZLz7hwpyr");
        EmailDestination destination3 = new EmailDestination("5J75fef~d-1BaJZyrP6Y9uYn3~tki2Vie5Fq7jTT7dbyYGb-PF~7pxIxJwh0lAYALrIgtIt4uaG67xm739FYkB");
        contact1 = new Contact("Tester McTest", destination1);
        contact2 = new Contact("Tester McTest", destination2);
        contact3 = new Contact("Chester McTesticle", destination3);
    }

    /** Tests if a new packet replaces an existing one when the names match (it shouldn't) */
    @Test
    public void testStore() {
        assertEquals(0, folder.getNumElements());
        folder.store(contact1);
        assertEquals(1, folder.getNumElements());
        folder.store(contact2);
        assertEquals(1, folder.getNumElements());
        assertTrue("Contact in the folder was overwritten!", folder.iterator().next().getDestination().toBase64().equals(contact1.getDestination().toBase64()));
        assertTrue("Contact in the folder was overwritten!", !folder.iterator().next().getDestination().toBase64().equals(contact2.getDestination().toBase64()));
        folder.store(contact3);
        assertEquals(2, folder.getNumElements());
    }
    
    @After
    public void tearDown() throws Exception {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
        TestUtil.deleteGeneratedFiles(testDir);
        testDir.delete();
        assertTrue(!testDir.exists());
    }
}
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
import i2p.bote.packet.dht.Contact;

import java.io.File;

import net.i2p.data.Base64;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DirectoryEntryFolderTest {
    private File testDir;
    private File folderDir;
    private DirectoryEntryFolder folder;
    private String contact12Name = "Tester McTest";   // for contact1 and contact2
    private Contact contact1;
    private Contact contact2;
    private Contact contact3;

    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "DirectoryEntryFolderTest-" + System.currentTimeMillis());
        folderDir = new File(testDir, "folder");
        folder = new DirectoryEntryFolder(folderDir);
        
        // contact1 and contact2 contain the same name; name in contact3 is different
        contact1 = new Contact(Base64.decode("QwSlRwEi0IY-mEz6owJfWVG-dZe2qHIcKFKyD8FvdEvdRwBCAxvGHjFocWgadAsrUqFspuWyRrpoa5Hh~SsFBxFjeTZ6Atx~IqWbtxt3pV5fzHLkvgFHx0jpTgIStssa98sD5NIU3unfuQAAAAAAAEgwRgIhAP2-AGnl0Jlpss4rXsAseDCelq~lwbMOtWKimogddYOvAiEAwdMyV12F0N9hQVnns0Zx3MMgoUlDozvGB1CZmx9ZGkA="));
        contact2 = new Contact(Base64.decode("QwSlRwEi0IY-mEz6owJfWVG-dZe2qHIcKFKyD8FvdEvdRwBCAt6JpsgAD~TrlH~bafT0CvUPk5jfQDcISB2IG6hCE1ASA1kh3urYt29bPVvhaRh2gDFk2eyz-MZRoMpZLz7hwpyrl783YwAAAAAAAEYwRAIgRlI6aoQ1tdUp5xAFHdh3Wm9bT6HvyWh8WkMgNwIWMQcCIBBCHzXcOD-xe3-CqRO2cWLvXCDFed5zmE7oEctVpbcQ"));
        contact3 = new Contact(Base64.decode("QwRwRpWqx-xkTC4mYzMJkJtryurqpo0vee2Qnq8uVXD92wBCA5J75fef~d-1BaJZyrP6Y9uYn3~tki2Vie5Fq7jTT7dbAyYGb-PF~7pxIxJwh0lAYALrIgtIt4uaG67xm739FYkBa1b2gwAAAAAAAEcwRQIgGgf3vd28-1YyUN41VLfszN0Nu9xC--LA2is5EJNq8a8CIQDyCK4GSDh1V2hmORmEzmF8xyazB1P~QbtVJHiognSSNA=="));
    }

    /** Tests if a new packet replaces an existing one when the names match (it shouldn't) */
    @Test
    public void testStoreExisting() {
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
    
    /** Tests if a packet is stored if it contains an invalid signature */
    @Test
    public void testStoreInvalid() {
        assertEquals(0, folder.getNumElements());
        
        // try adding an invalid Contact
        char[] chars = contact12Name.toCharArray();
        chars[0]++;
        String alteredName = new String(chars);
        contact1.setName(alteredName);
        assertEquals(0, folder.getNumElements());
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
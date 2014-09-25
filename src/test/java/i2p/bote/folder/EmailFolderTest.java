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
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordCache;
import i2p.bote.fileencryption.PasswordException;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.mail.MessagingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmailFolderTest {
    private File testDir;
    private File folderDir1;
    private File folderDir2;
    private Email email1;
    private Email email2;
    private EmailFolder folder1;
    private EmailFolder folder2;
    
    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "EmailFolderTest-" + System.currentTimeMillis());
        folderDir1 = new File(testDir, "folder1");
        folderDir2 = new File(testDir, "folder2");
        email1 = new Email(true);
        email1.setText("Und nun lag die Entscheidung schon nahe vor ihm, es\n" +
            "war alles klar geworden. Die Kunst war eine schöne Sache,\n" +
            "aber sie war keine Göttin und kein Ziel, für ihn nicht, nicht\n" +
            "der Kunst hatte er zu folgen, nur dem Ruf der Mutter. Was\n" +
            "konnte es nutzen, seine Finger noch immer geschickter zu\n" +
            "machen? Am Meister Niklaus konnte man sehen, wohin\n" +
            "das führte. Es führte zu Ruhm und Namen, zu Geld und\n" +
            "seßhaftem Leben, und zu einer Verdorrung und Verküm-\n" +
            "merung jener inneren Sinne, denen allein das Geheimnis\n" +
            "zugänglich ist. Es führte zum Herstellen hübscher kostba-\n" +
            "rer Spielwaren, zu allerlei reichen Altären und Kanzeln,\n" +
            "heiligen Sebastianen und hübsch gelockten Engels-\n" +
            "köpfchen, das Stück zu vier Talern. Oh, das Gold im Aug’\n" +
            "eines Karpfens und der süße dünne Silberflaum am Rand\n" +
            "eines Schmetterlingsflügels war unendlich viel schöner,\n" +
            "lebendiger, köstlicher als ein ganzer Saal voll von jenen\n" +
            "Kunstwerken.");
        email2 = new Email(false);
        email2.setText("\"And when the trial continued,\" he said in a weeping whisper, \"they asked Prak a most unfortunate thing.\n" +
            "They asked him,\" he paused and shivered, \"to tell the Truth, the Whole Truth and Nothing but the Truth.\n" +
            "Only, don't you see?\"\n" +
            "He suddenly hoisted himself up on to his elbows again and shouted at them.\n" +
            "\"They'd given him much too much of the drug!\"\n" +
            "He collapsed again, moaning quietly. \"Much too much too much too much too ...\"\n" +
            "The group gathered round his bedside glanced at each other. There were goose pimples on backs.\n" +
            "\"What happened?\" said Zaphod at last.\n" +
            "\"Oh, he told it all right,\" said the man savagely, \"for all I know he's still telling it now. Strange, terrible\n" +
            "things ... terrible, terrible!\" he screamed.\n" +
            "They tried to calm him, but he struggled to his elbows again.\n" +
            "\"Terrible things, incomprehensible things,\" he shouted, \"things that would drive a man mad!\"\n" +
            "He stared wildly at them.\n" +
            "\"Or in my case,\" he said, \"half-mad. I'm a journalist.\"\n" +
            "\"You mean,\" said Arthur quietly, \"that you are used to confronting the truth?\"\n" +
            "\"No,\" said the man with a puzzled frown. \"I mean that I made an excuse and left early.\"\n" +
            "He collapsed into a coma from which he recovered only once and briefly.");
        
        PasswordCache passwordCache = TestUtil.createPasswordCache(testDir);
        
        folder1 = new EmailFolder(folderDir1, passwordCache);
        folder2 = new EmailFolder(folderDir2, passwordCache);
    }
    
    @After
    public void tearDown() throws Exception {
        deleteFolder(folderDir1);
        deleteFolder(folderDir2);
        assertTrue(!testDir.exists());
    }

    private void deleteFolder(File folderDir) {
        for (File file: folderDir.listFiles())
            file.delete();
        folderDir.delete();
        TestUtil.deleteGeneratedFiles(testDir);
        testDir.delete();
    }
    
    @Test
    public void testAdd() throws IOException, MessagingException, PasswordException, GeneralSecurityException {
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
    public void testMove() throws IOException, MessagingException, PasswordException, GeneralSecurityException {
        folder1.add(email1);
        folder1.move(email1, folder2);
        assertEquals("Source folder is not empty!", 0, folderDir1.list().length);
        assertEquals("Target folder does not contain two files!", 2, folderDir2.list().length);
        FolderIterator<Email> iterator = folder2.iterate();
        TestUtil.assertEquals("Email differs from the original!", email1, iterator.next());
    }

    @Test
    public void testDelete() throws IOException, MessagingException, PasswordException, GeneralSecurityException {
        folder1.add(email1);
        folder1.delete(email1.getMessageID());
        assertEquals("The email file and/or the metadata were not deleted!", 0, folderDir1.list().length);
    }
    
    @Test
    public void testSetNew() throws IOException, MessagingException, PasswordException, GeneralSecurityException {
        folder1.add(email1);
        FolderIterator<Email> iterator = folder1.iterate();
        Email emailFromFolder = iterator.next();
        assertEquals("\"unread\" flag is false after adding email to folder!", emailFromFolder.isUnread(), true);
        folder1.setNew(email1.getMessageID(), false);
    }
}

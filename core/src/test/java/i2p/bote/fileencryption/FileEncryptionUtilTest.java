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

package i2p.bote.fileencryption;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import i2p.bote.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileEncryptionUtilTest {
    private File testDir;
    private byte[] password = "secret password %&§§%&+ü#".getBytes();
    private byte[] plainText = "this is the unencrypted text".getBytes();
    
    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "FileEncryptionUtilTest-" + System.currentTimeMillis());
        testDir.mkdirs();
    }
    
    @After
    public void tearDown() throws Exception {
        testDir.delete();
    }
    
    /** Tests writePasswordFile() and isPasswordCorrect() */
    @Test
    public void testPasswordFile() throws IOException, GeneralSecurityException {
        File passwordFile = new File(testDir, "password");
        FileEncryptionUtil.writePasswordFile(passwordFile, password, FileEncryptionTestUtil.deriveKey(password));
        assertTrue(passwordFile.exists());
        
        assertTrue(FileEncryptionUtil.isPasswordCorrect(password, passwordFile));
        assertFalse(FileEncryptionUtil.isPasswordCorrect("this is the wrong password".getBytes(), passwordFile));
        
        // setting an empty password should delete the password file
        byte[] emptyPassword = new byte[0];
        FileEncryptionUtil.writePasswordFile(passwordFile, emptyPassword, FileEncryptionTestUtil.deriveKey(emptyPassword));
        assertTrue(FileEncryptionUtil.isPasswordCorrect("random string adfsasdfafsd".getBytes(), passwordFile));
        assertFalse(passwordFile.exists());
        
        // same for a null password
        FileEncryptionUtil.writePasswordFile(passwordFile, password, FileEncryptionTestUtil.deriveKey(password));
        assertTrue(passwordFile.exists());
        FileEncryptionUtil.writePasswordFile(passwordFile, null, FileEncryptionTestUtil.deriveKey(null));
        assertFalse(passwordFile.exists());
    }
    
    @Test
    public void testChangePassword() throws IOException, GeneralSecurityException, PasswordException {
        File encryptedFile = new File(testDir, "encrypted");
        OutputStream outputStream = new EncryptedOutputStream(new FileOutputStream(encryptedFile), FileEncryptionTestUtil.deriveKey(password));
        outputStream.write(plainText);
        outputStream.close();
        
        byte[] newPassword = "new password".getBytes();
        FileEncryptionUtil.changePassword(encryptedFile, password, FileEncryptionTestUtil.deriveKey(newPassword));
        InputStream inputStream = new EncryptedInputStream(new FileInputStream(encryptedFile), newPassword);
        byte[] decryptedText = Util.readBytes(inputStream);
        assertArrayEquals(plainText, decryptedText);
        
        encryptedFile.delete();
   }
}
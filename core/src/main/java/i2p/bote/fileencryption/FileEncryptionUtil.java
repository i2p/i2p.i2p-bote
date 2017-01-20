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

import static i2p.bote.fileencryption.FileEncryptionConstants.DEFAULT_PASSWORD;
import static i2p.bote.fileencryption.FileEncryptionConstants.KEY_LENGTH;
import static i2p.bote.fileencryption.FileEncryptionConstants.PASSWORD_FILE_PLAIN_TEXT;
import i2p.bote.Util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import net.i2p.util.Log;

import com.lambdaworks.crypto.SCrypt;

public class FileEncryptionUtil {

    /**
     * Generates a symmetric encryption key from a password and salt.
     * A given set of input parameters will always produce the same key.
     * @param password
     * @param salt
     * @param sCryptParams Parameters for scrypt: CPU cost, memory cost, and parallelization factor
     * @throws GeneralSecurityException 
     */
    static byte[] getEncryptionKey(byte[] password, byte[] salt, SCryptParameters sCryptParams) throws GeneralSecurityException {
        if (password==null || password.length<=0)
            password = DEFAULT_PASSWORD;
       
        byte[] key = SCrypt.scrypt(password, salt, sCryptParams.N, sCryptParams.r, sCryptParams.p, KEY_LENGTH);
        return key;
    }
    
    static DerivedKey getEncryptionKey(byte[] password, File derivParamFile) throws GeneralSecurityException, IOException {
        DataInputStream inputStream = null;
        try {
            inputStream = new DataInputStream(new FileInputStream(derivParamFile));
            SCryptParameters scryptParams = new SCryptParameters(inputStream);
            byte[] salt = new byte[FileEncryptionConstants.SALT_LENGTH];
            inputStream.read(salt);
            byte[] key = FileEncryptionUtil.getEncryptionKey(password, salt, scryptParams);
            DerivedKey derivedKey = new DerivedKey(salt, scryptParams, key);
            return derivedKey;
        }
        finally {
            if (inputStream != null)
                inputStream.close();
        }
    }
    
    /**
     * Decrypts a file with a given password and returns <code>true</code> if the decrypted
     * text is {@link FileEncryptionConstants#PASSWORD_FILE_PLAIN_TEXT}; <code>false</code>
     * otherwise.
     * @param password
     * @param passwordFile
     * @throws IOException
     * @throws GeneralSecurityException 
     */
    public static boolean isPasswordCorrect(byte[] password, File passwordFile) throws IOException, GeneralSecurityException {
        if (!passwordFile.exists())
            return true;
        
        EncryptedInputStream inputStream = null;
        try {
            inputStream = new EncryptedInputStream(new FileInputStream(passwordFile), password);
            byte[] decryptedText = Util.readBytes(inputStream);
            return Arrays.equals(PASSWORD_FILE_PLAIN_TEXT, decryptedText);
        }
        catch (PasswordException e) {
            return false;
        }
        finally {
            if (inputStream != null)
                inputStream.close();
        }
    }
    
    /**
     * Encrypts the array {@link FileEncryptionConstants#PASSWORD_FILE_PLAIN_TEXT} with a
     * password and writes the encrypted data to a file.
     * @param passwordFile
     * @param password
     * @param newKey
     * @throws IOException
     */
    public static void writePasswordFile(File passwordFile, byte[] password, DerivedKey newKey) throws IOException {
        if (password==null || password.length==0) {
            if (!passwordFile.delete())
                new Log(FileEncryptionUtil.class).error("Can't delete file: " + passwordFile.getAbsolutePath());
            return;
        }
        
        EncryptedOutputStream outputStream = null;
        try {
            outputStream = new EncryptedOutputStream(new FileOutputStream(passwordFile), newKey);
            outputStream.write(PASSWORD_FILE_PLAIN_TEXT);
        } catch (IOException e) {
            new Log(FileEncryptionUtil.class).error("Can't write password file <" + passwordFile.getAbsolutePath() + ">", e);
            throw e;
        }
        finally {
            if (outputStream != null)
                outputStream.close();
        }
    }
    
    /**
     * Encrypts a file with a new password. No verification of the old password is done.<br/>
     * The new password is implicitly given by <code>newKey</code> which also specifies the
     * salt. This is done so the salt vector and hence the encryption key can be reused,
     * avoiding expensive recomputation.
     * @param file
     * @param oldPassword
     * @param newKey
     * @throws IOException
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     */
    public static void changePassword(File file, byte[] oldPassword, DerivedKey newKey) throws IOException, GeneralSecurityException, PasswordException {
        InputStream inputStream = null;
        byte[] decryptedData = null;
        try {
            inputStream = new EncryptedInputStream(new FileInputStream(file), oldPassword);
            decryptedData = Util.readBytes(inputStream);
        }
        finally {
            if (inputStream != null)
                inputStream.close();
        }
        OutputStream outputStream = null;
        try {
            outputStream = new EncryptedOutputStream(new FileOutputStream(file), newKey);
            outputStream.write(decryptedData);
        }
        finally {
            if (outputStream != null)
                outputStream.close();
        }
    }
}
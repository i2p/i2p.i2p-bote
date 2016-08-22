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

package i2p.bote.crypto;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.i2p.I2PAppContext;

/**
 * Implements {@link #toByteArray(PublicKeyPair)} and {@link #toByteArray(PrivateKeyPair)},
 * and provides methods for AES encryption and decryption.
 */
public abstract class AbstractCryptoImplementation implements CryptoImplementation {
    protected I2PAppContext appContext;
    private Cipher aesCipher;

    protected AbstractCryptoImplementation() throws GeneralSecurityException {
        appContext = I2PAppContext.getGlobalContext();
        try {
            aesCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        } catch (NoSuchPaddingException e) {
            // SUN provider incorrectly calls it PKCS5Padding
            aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }
    }
    
    /** This implementation returns the whole set of Base64 characters. */
    @Override
    public String getBase64InitialCharacters() {
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    }
    
    @Override
    public byte[] toByteArray(PublicKeyPair keyPair) {
        byte[] encKey = keyPair.encryptionKey.getEncoded();
        byte[] sigKey = keyPair.signingKey.getEncoded();
        byte[] encodedKeys = new byte[encKey.length + sigKey.length];
        System.arraycopy(encKey, 0, encodedKeys, 0, encKey.length);
        System.arraycopy(sigKey, 0, encodedKeys, encKey.length, sigKey.length);
        return encodedKeys;
    }
    
    @Override
    public byte[] toByteArray(PrivateKeyPair keyPair) {
        byte[] encKey = keyPair.encryptionKey.getEncoded();
        
        byte[] sigKey = keyPair.signingKey.getEncoded();
        
        byte[] encodedKeys = new byte[encKey.length + sigKey.length];
        System.arraycopy(encKey, 0, encodedKeys, 0, encKey.length);
        System.arraycopy(sigKey, 0, encodedKeys, encKey.length, sigKey.length);
        return encodedKeys;
    }

    protected byte[] encryptAes(byte[] data, byte[] key, byte[] iv) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivps = new IvParameterSpec(iv, 0, 16);
        aesCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivps, appContext.random());

        byte[] encryptedData = new byte[aesCipher.getOutputSize(data.length)];
        int encLen = aesCipher.doFinal(data, 0, data.length, encryptedData, 0);
        byte[] ret = new byte[encLen];
        System.arraycopy(encryptedData, 0, ret, 0, encLen);
        return ret;
    }
    
    protected byte[] decryptAes(byte[] data, byte[] key, byte[] iv) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivps = new IvParameterSpec(iv, 0, 16);
        aesCipher.init(Cipher.DECRYPT_MODE, keySpec, ivps, appContext.random());

        byte[] decryptedData = new byte[aesCipher.getOutputSize(data.length)];
        int decLen = aesCipher.doFinal(data, 0, data.length, decryptedData, 0);
        byte[] ret = new byte[decLen];
        System.arraycopy(decryptedData, 0, ret, 0, decLen);
        return ret;
    }
}
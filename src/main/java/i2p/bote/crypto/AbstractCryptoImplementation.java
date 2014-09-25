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

import java.util.Arrays;

import net.i2p.I2PAppContext;
import net.i2p.data.SessionKey;
import net.i2p.util.Log;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.paddings.PKCS7Padding;

/**
 * Implements {@link #toByteArray(PublicKeyPair)} and {@link #toByteArray(PrivateKeyPair)},
 * and provides methods for AES encryption and decryption.
 */
public abstract class AbstractCryptoImplementation implements CryptoImplementation {
    private static final int BLOCK_SIZE = 16;   // the AES block size for padding. Not to be confused with the AES key size.
    
    protected I2PAppContext appContext;
    private Log log = new Log(AbstractCryptoImplementation.class);
    
    protected AbstractCryptoImplementation() {
        appContext = I2PAppContext.getGlobalContext();
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

    protected byte[] encryptAes(byte[] data, byte[] key, byte[] iv) {
        // pad the data
        int unpaddedLength = data.length;
        data = Arrays.copyOf(data, unpaddedLength + BLOCK_SIZE - unpaddedLength%BLOCK_SIZE);  // make data.length a multiple of BLOCK_SIZE; if the length is a multiple of BLOCK_LENGTH, add a block of zeros
        PKCS7Padding padding = new PKCS7Padding();
        int numAdded = padding.addPadding(data, unpaddedLength);
        if (log.shouldLog(Log.DEBUG) && numAdded != BLOCK_SIZE-unpaddedLength%BLOCK_SIZE)
            log.error("Error: " + numAdded + " pad bytes added, expected: " + (BLOCK_SIZE-unpaddedLength%BLOCK_SIZE));

        byte[] encryptedData = new byte[data.length];
        SessionKey sessionKey = new SessionKey(key);
        appContext.aes().encrypt(data, 0, encryptedData, 0, sessionKey, iv, data.length);   // this method also checks that data.length is divisible by 16
        return encryptedData;
    }
    
    protected byte[] decryptAes(byte[] data, byte[] key, byte[] iv) throws InvalidCipherTextException {
        SessionKey sessionKey = new SessionKey(key);
        byte[] decryptedData = new byte[data.length];
        if (data.length%BLOCK_SIZE != 0)
            log.error("Length of encrypted data is not divisible by " + BLOCK_SIZE + ". Length=" + decryptedData.length);
        appContext.aes().decrypt(data, 0, decryptedData, 0, sessionKey, iv, data.length);
        
        // unpad the decrypted data
        byte[] lastBlock = Arrays.copyOfRange(decryptedData, decryptedData.length-iv.length, decryptedData.length);
        PKCS7Padding padding = new PKCS7Padding();
        int padCount = padding.padCount(lastBlock);
        decryptedData = Arrays.copyOf(decryptedData, decryptedData.length-padCount);
        
        return decryptedData;
    }
}
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

import i2p.bote.Util;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import net.i2p.I2PAppContext;
import net.i2p.data.Base64;
import net.i2p.data.SessionKey;
import net.sf.ntru.EncryptionKeyPair;
import net.sf.ntru.EncryptionParameters;
import net.sf.ntru.EncryptionPrivateKey;
import net.sf.ntru.EncryptionPublicKey;
import net.sf.ntru.NtruEncrypt;
import net.sf.ntru.NtruSign;
import net.sf.ntru.SignatureKeyPair;
import net.sf.ntru.SignatureParameters;
import net.sf.ntru.SignaturePrivateKey;
import net.sf.ntru.SignaturePublicKey;

/**
 * NTRUEncrypt with N=1087 and NTRUSign with N=349.
 * <p/>
 * Both algorithms provide 256 bits (???) of security and are considered safe from
 * quantum computer attacks.<br/>
 * NTRU uses public keys that are shorter than ElGamal keys (at the same security
 * level) but longer than ECC keys.
 */
public class NTRUEncrypt1087_NTRUSign349 implements CryptoImplementation {
    private static final EncryptionParameters NTRUENCRYPT_PARAMETERS = EncryptionParameters.EES1087EP2;
    private static final SignatureParameters NTRUSIGN_PARAMETERS = SignatureParameters.T349;
    private static final int PUBLIC_ENCRYPTION_KEY_BYTES = 1495;
    private static final int PUBLIC_SIGNING_KEY_BYTES = 393;
    private static final int PRIVATE_ENCRYPTION_KEY_BYTES = 216;
    private static final int PRIVATE_SIGNING_KEY_BYTES = 673;
    private static final int ENCRYPTED_LENGTH_BYTES = PUBLIC_ENCRYPTION_KEY_BYTES;   // length of an NTRU-encrypted message (no AES)
    private static final int BLOCK_SIZE = 16;   // length of the AES initialization vector; also the AES block size for padding. Not to be confused with the AES key size.
    
    private I2PAppContext appContext;

    NTRUEncrypt1087_NTRUSign349() {
        appContext = new I2PAppContext();
    }
    
    @Override
    public String getName() {
        return Util._("NTRU-1087 Encryption");
    }
    
    @Override
    public byte getId() {
        return 4;
    }

    @Override
    public int getBase64PublicKeyPairLength() {
        // #base64 chars = ceil(#bytes*8/6)
        return (getByteArrayPublicKeyPairLength()*8+5) / 6;
    }
    
    @Override
    public int getBase64CompleteKeySetLength() {
        int privateKeyPairLength = ((PRIVATE_ENCRYPTION_KEY_BYTES+PRIVATE_SIGNING_KEY_BYTES)*8+5) / 6;
        return getBase64PublicKeyPairLength() + privateKeyPairLength;
    }
    
    @Override
    public int getByteArrayPublicKeyPairLength() {
        return PUBLIC_ENCRYPTION_KEY_BYTES + PUBLIC_SIGNING_KEY_BYTES;
    }
    
    @Override
    public PublicKeyPair createPublicKeyPair(byte[] bytes) {
        PublicKeyPair keyPair = new PublicKeyPair();
        
        byte[] encryptionKeyBytes = Arrays.copyOf(bytes, PUBLIC_ENCRYPTION_KEY_BYTES);
        keyPair.encryptionKey = new NtruEncrypt1087PublicKey(encryptionKeyBytes);
        
        byte[] signingKeyBytes = Arrays.copyOfRange(bytes, PUBLIC_ENCRYPTION_KEY_BYTES, PUBLIC_ENCRYPTION_KEY_BYTES+PUBLIC_ENCRYPTION_KEY_BYTES);
        keyPair.signingKey = new NtruSign349PublicKey(signingKeyBytes);
        
        return keyPair;
    }
    
    @Override
    public PrivateKeyPair createPrivateKeyPair(byte[] bytes) {
        PrivateKeyPair keyPair = new PrivateKeyPair();
        
        byte[] encryptionKeyBytes = Arrays.copyOf(bytes, PRIVATE_ENCRYPTION_KEY_BYTES);
        keyPair.encryptionKey = new NtruEncrypt1087PrivateKey(encryptionKeyBytes);
        
        byte[] signingKeyBytes = Arrays.copyOfRange(bytes, PRIVATE_ENCRYPTION_KEY_BYTES, bytes.length);
        keyPair.signingKey = new NtruSign349PrivateKey(signingKeyBytes);
        
        return keyPair;
    }
    
    @Override
    public String toBase64(PublicKeyPair keyPair) {
        String base64 = Base64.encode(toByteArray(keyPair));
        // the last two chars are always '==', so drop them
        return base64.substring(0, base64.length()-2);
    }

    @Override
    public String toBase64(PrivateKeyPair keyPair) {
        String base64 = Base64.encode(toByteArray(keyPair));
        // the last two chars are always '==', so drop them
        return base64.substring(0, base64.length()-2);
    }

    @Override
    public PublicKeyPair createPublicKeyPair(String base64) throws GeneralSecurityException {
        // append the "==" that is omitted in the encoding
        base64 += "==";
        byte[] keyBytes = Base64.decode(base64);
        return createPublicKeyPair(keyBytes);
    }

    @Override
    public PrivateKeyPair createPrivateKeyPair(String base64) throws GeneralSecurityException {
        // append the "==" that is omitted in the encoding
        base64 += "==";
        byte[] keyBytes = Base64.decode(base64);
        return createPrivateKeyPair(keyBytes);
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

    @Override
    public KeyPair generateEncryptionKeyPair() throws KeyException {
        EncryptionKeyPair encKeyPair = NtruEncrypt.generateKeyPair(NTRUENCRYPT_PARAMETERS);
        PublicKey publicKey = new NtruEncrypt1087PublicKey(encKeyPair.pub);
        PrivateKey privateKey = new NtruEncrypt1087PrivateKey(encKeyPair.priv);
        
        return new KeyPair(publicKey, privateKey);
    }
    
    @Override
    public KeyPair generateSigningKeyPair() throws KeyException {
        SignatureKeyPair sigKeyPair = NtruSign.generateKeyPair(NTRUSIGN_PARAMETERS);
        PublicKey publicKey = new NtruSign349PublicKey(sigKeyPair.pub);
        PrivateKey privateKey = new NtruSign349PrivateKey(sigKeyPair.priv);
        
        return new KeyPair(publicKey, privateKey);
    }
    
    /**
     * Only accepts <code>Ntru1087PublicKey</code>s. 
     * @throws NoSuchAlgorithmException
     */
    @Override
    public byte[] encrypt(byte[] data, PublicKey key) throws NoSuchAlgorithmException {
        byte[] symmKey = new byte[32];
        appContext.random().nextBytes(symmKey);
        SessionKey sessionKey = new SessionKey(symmKey);
        
        byte iv[] = new byte[BLOCK_SIZE];
        appContext.random().nextBytes(iv);
        byte[] encryptedData = appContext.aes().safeEncrypt(data, sessionKey, iv, 0);
        
        NtruEncrypt1087PublicKey ntruKey = castToNtruEncryptKey(key);
        byte[] encryptedSymmKey = NtruEncrypt.encrypt(symmKey, ntruKey.key, NTRUENCRYPT_PARAMETERS);
        
        ByteBuffer output = ByteBuffer.allocate(encryptedSymmKey.length + iv.length + encryptedData.length);
        output.put(encryptedSymmKey);
        output.put(iv);
        output.put(encryptedData);
        return output.array();
    }

    private NtruEncrypt1087PublicKey castToNtruEncryptKey(PublicKey key) {
        if (key instanceof NtruEncrypt1087PublicKey)
            return (NtruEncrypt1087PublicKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + NtruEncrypt1087PublicKey.class.getName());
    }

    /**
     * Only accepts <code>Ntru1087PublicKey</code>s and <code>Ntru1087PrivateKey</code>s.
     * @throws NoSuchAlgorithmException 
     */
    @Override
    public byte[] decrypt(byte[] data, PublicKey publicKey, PrivateKey privateKey) throws NoSuchAlgorithmException {
        if (data == null)
            return null;
        
        ByteBuffer inputBuffer = ByteBuffer.wrap(data);
        byte[] encryptedSymmKey = new byte[ENCRYPTED_LENGTH_BYTES];
        inputBuffer.get(encryptedSymmKey);
        NtruEncrypt1087PublicKey publicNtruKey = castToNtruEncryptKey(publicKey);
        NtruEncrypt1087PrivateKey privateNtruKey = castToNtruEncryptKey(privateKey);
        EncryptionKeyPair keyPair = new EncryptionKeyPair(privateNtruKey.key, publicNtruKey.key);
        byte[] symmKey = NtruEncrypt.decrypt(encryptedSymmKey, keyPair, NTRUENCRYPT_PARAMETERS);
        
        SessionKey sessionKey = new SessionKey(symmKey);
        byte[] iv = new byte[BLOCK_SIZE];
        inputBuffer.get(iv);
        byte[] encryptedData = new byte[inputBuffer.remaining()];
        inputBuffer.get(encryptedData);
        byte[] decryptedData = appContext.aes().safeDecrypt(encryptedData, sessionKey, iv);
        return decryptedData;
    }

    private NtruEncrypt1087PrivateKey castToNtruEncryptKey(PrivateKey key) {
        if (key instanceof NtruEncrypt1087PrivateKey)
            return (NtruEncrypt1087PrivateKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + NtruEncrypt1087PrivateKey.class.getName());
    }

    /** Only accepts <code>NtruSign349PublicKey</code>s and <code>NtruSign349PrivateKey</code>s. */
    @Override
    public byte[] sign(byte[] data, PublicKey publicKey, PrivateKey privateKey) {
        NtruSign349PublicKey publicNtruKey = castToNtruSignKey(publicKey);
        NtruSign349PrivateKey privateNtruKey = castToNtruSignKey(privateKey);
        return NtruSign.sign(data, privateNtruKey.key, publicNtruKey.key, NTRUSIGN_PARAMETERS);
    }

    private NtruSign349PublicKey castToNtruSignKey(PublicKey key) {
        if (key instanceof NtruSign349PublicKey)
            return (NtruSign349PublicKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + NtruSign349PublicKey.class.getName());
    }

    private NtruSign349PrivateKey castToNtruSignKey(PrivateKey key) {
        if (key instanceof NtruSign349PrivateKey)
            return (NtruSign349PrivateKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + NtruSign349PrivateKey.class.getName());
    }

    /** Only accepts <code>NtruSign349PublicKey</code>s. */
    @Override
    public boolean verify(byte[] data, byte[] signature, PublicKey key) {
        NtruSign349PublicKey publicNtruKey = castToNtruSignKey(key);
        return NtruSign.verify(data, signature, publicNtruKey.key, NTRUSIGN_PARAMETERS);
    }
    
    private class NtruEncrypt1087PublicKey implements PublicKey {
        private static final long serialVersionUID = 8103999492335873827L;
        
        private EncryptionPublicKey key;
        
        public NtruEncrypt1087PublicKey(EncryptionPublicKey key) {
            this.key = key;
        }

        public NtruEncrypt1087PublicKey(byte[] keyBytes) {
            key = new EncryptionPublicKey(keyBytes, NTRUENCRYPT_PARAMETERS);
        }

        @Override
        public String getAlgorithm() {
            return "NTRUEncrypt-1087";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }
        
        @Override
        public byte[] getEncoded() {
            return key.getEncoded();
        }
    }
    
    private class NtruEncrypt1087PrivateKey implements PrivateKey {
        private static final long serialVersionUID = 8103999492335873827L;
        
        private EncryptionPrivateKey key;
        
        public NtruEncrypt1087PrivateKey(EncryptionPrivateKey key) {
            this.key = key;
        }

        public NtruEncrypt1087PrivateKey(byte[] keyBytes) {
            key = new EncryptionPrivateKey(keyBytes, NTRUENCRYPT_PARAMETERS);
        }

        @Override
        public String getAlgorithm() {
            return "NTRUEncrypt-1087";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }
        
        @Override
        public byte[] getEncoded() {
            return key.getEncoded();
        }
    }

    private class NtruSign349PublicKey implements PublicKey {
        private static final long serialVersionUID = 8103999492335873827L;
        
        private SignaturePublicKey key;
        
        public NtruSign349PublicKey(SignaturePublicKey key) {
            this.key = key;
        }

        public NtruSign349PublicKey(byte[] keyBytes) {
            key = new SignaturePublicKey(keyBytes, NTRUSIGN_PARAMETERS);
        }

        @Override
        public String getAlgorithm() {
            return "NTRUSign-349";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }
        
        @Override
        public byte[] getEncoded() {
            return key.getEncoded();
        }
    }

    private class NtruSign349PrivateKey implements PrivateKey {
        private static final long serialVersionUID = 8103999492335873827L;
        
        private SignaturePrivateKey key;
        
        public NtruSign349PrivateKey(SignaturePrivateKey key) {
            this.key = key;
        }

        public NtruSign349PrivateKey(byte[] keyBytes) {
            key = new SignaturePrivateKey(keyBytes, NTRUSIGN_PARAMETERS);
        }

        @Override
        public String getAlgorithm() {
            return "NTRUSign-349";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }
        
        @Override
        public byte[] getEncoded() {
            return key.getEncoded();
        }
    }
}
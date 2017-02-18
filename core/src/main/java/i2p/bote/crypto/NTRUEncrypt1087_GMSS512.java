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

import net.i2p.data.Base64;
import net.sf.ntru.encrypt.EncryptionKeyPair;
import net.sf.ntru.encrypt.EncryptionParameters;
import net.sf.ntru.encrypt.EncryptionPrivateKey;
import net.sf.ntru.encrypt.EncryptionPublicKey;
import net.sf.ntru.encrypt.NtruEncrypt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import de.flexiprovider.api.exceptions.InvalidKeySpecException;
import de.flexiprovider.api.keys.KeySpec;
import de.flexiprovider.pki.PKCS8EncodedKeySpec;
import de.flexiprovider.pqc.hbc.gmss.GMSSKeyFactory;
import de.flexiprovider.pqc.hbc.gmss.GMSSKeyPairGenerator;
import de.flexiprovider.pqc.hbc.gmss.GMSSParameterSpec;
import de.flexiprovider.pqc.hbc.gmss.GMSSParameterset;
import de.flexiprovider.pqc.hbc.gmss.GMSSPublicKeySpec;
import de.flexiprovider.pqc.hbc.gmss.GMSSSignature;
import i2p.bote.fileencryption.PasswordException;

/**
 * NTRUEncrypt with N=1087 and GMSS with SHA512.
 * <p/>
 * Both algorithms provide 256 bits of security and are considered safe from
 * quantum computer attacks.<br/>
 * NTRUEncrypt uses public keys that are shorter than ElGamal keys (at the same
 * security level) but longer than ECC keys. GMSS public keys are roughly the
 * same length as ECC keys.
 * <p/>
 * Key generation with this <code>CryptoImplementation</code> takes some time,
 * almost all of which is spent on GMSS key generation. On fast computers, it
 * takes a minute or less; on slower machines, it takes several minutes.
 * <p/>
 * One GMSS key can only create <code>2^(Σ HEIGHTS)</code> signatures.
 * Since <code>HEIGHTS</code> is <code>[6, 6, 5, 5]</code>, a single email
 * identity can be used to send 100 emails a day to 10 recipients each for 167
 * years before the email identity is used up.
 * <p/>
 * GMSS is a key-evolving signature scheme, meaning the private key changes
 * after every signature. This affects the length of the private key. The
 * maximum amount by which the key can grow can be calculated using Lemma 7 in
 * <a href=http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.95.1374&rep=rep1&type=pdf>
 * <i>Merkle Signatures with Virtually Unlimited Signature Capacity</i></a>.
 * Because the private key is variable in length, it is padded to the maximum
 * length so the <code>CryptoImplementation</code> can be assigned a key length
 * that identifies it.
 * <p/>
 * GMSS signatures are much longer than ElGamal or ECC signatures. GMSS allows
 * a trade-off between signature size, private key size, and key generation
 * time. Parameters for this <code>CryptoImplementation</code> were chosen to
 * keep signature size and key generation time low at the expense of private
 * key size. Signatures are 13712 bytes and private keys are 71584 bytes
 * (including padding).
 */
public class NTRUEncrypt1087_GMSS512 extends AbstractCryptoImplementation {
    private static final EncryptionParameters NTRUENCRYPT_PARAMETERS = EncryptionParameters.EES1087EP2;
    private static final int[] HEIGHTS = new int[] {6, 6, 5, 5};   // GMSS tree heights
    private static final int[] WINTERNITZ = new int[] {12, 11, 11, 11};   // Winternitz parameters for GMSS
    private static final int[] AUTH_PATH_PARAMETERS = new int[] {2, 2, 3, 3};   // the parameter K in GMSS
    private static final GMSSParameterset GMSS_PARAMETERS = new GMSSParameterset(HEIGHTS.length, HEIGHTS, WINTERNITZ, AUTH_PATH_PARAMETERS);
    private static final int BASE64_PRIVATE_KEY_PAIR_LENGTH = 95734;
    private static final int PUBLIC_ENCRYPTION_KEY_BYTES = 1495;
    private static final int PUBLIC_SIGNING_KEY_BYTES = 64;
    private static final int PRIVATE_ENCRYPTION_KEY_BYTES = 216;
    private static final int PRIVATE_SIGNING_KEY_BYTES = 57180 + 4 + 14400;   // ASN1-encoded private key + 4 length bytes + padding
    private static final int ENCRYPTED_LENGTH_BYTES = PUBLIC_ENCRYPTION_KEY_BYTES;   // length of an NTRU-encrypted message (no AES)
    private static final int BLOCK_SIZE = 16;   // length of the AES initialization vector; also the AES block size for padding. Not to be confused with the AES key size.

    private GMSSKeyFactory gmssKeyFactory;
    private NtruEncrypt ntruEngine;

    public NTRUEncrypt1087_GMSS512() throws GeneralSecurityException {
        super();

        gmssKeyFactory = new GMSSKeyFactory();
        ntruEngine = new NtruEncrypt(NTRUENCRYPT_PARAMETERS);
    }
    
    @Override
    public String getName() {
        return "NTRUEncrypt-1087 / GMSS-512";
    }
    
    @Override
    public byte getId() {
        return 4;
    }

    @Override
    public int getBase64PublicKeyPairLength() {
        return 2079;
    }
    
    @Override
    public int getBase64CompleteKeySetLength() {
        return getBase64PublicKeyPairLength() + BASE64_PRIVATE_KEY_PAIR_LENGTH;
    }
    
    @Override
    public int getByteArrayPublicKeyPairLength() {
        return PUBLIC_ENCRYPTION_KEY_BYTES + PUBLIC_SIGNING_KEY_BYTES;
    }
    
    @Override
    public PublicKeyPair createPublicKeyPair(byte[] bytes) throws InvalidKeySpecException {
        PublicKeyPair keyPair = new PublicKeyPair();
        
        byte[] encryptionKeyBytes = Arrays.copyOf(bytes, PUBLIC_ENCRYPTION_KEY_BYTES);
        keyPair.encryptionKey = new NtruEncrypt1087PublicKey(encryptionKeyBytes);
        
        byte[] signingKeyBytes = Arrays.copyOfRange(bytes, PUBLIC_ENCRYPTION_KEY_BYTES, PUBLIC_ENCRYPTION_KEY_BYTES+PUBLIC_SIGNING_KEY_BYTES);
        keyPair.signingKey = new Gmss512PublicKey(signingKeyBytes);
        
        return keyPair;
    }
    
    @Override
    public PrivateKeyPair createPrivateKeyPair(byte[] bytes) throws InvalidKeySpecException {
        PrivateKeyPair keyPair = new PrivateKeyPair();
        
        byte[] encryptionKeyBytes = Arrays.copyOf(bytes, PRIVATE_ENCRYPTION_KEY_BYTES);
        keyPair.encryptionKey = new NtruEncrypt1087PrivateKey(encryptionKeyBytes);
        
        byte[] signingKeyBytes = Arrays.copyOfRange(bytes, PRIVATE_ENCRYPTION_KEY_BYTES, bytes.length);
        keyPair.signingKey = new Gmss512PrivateKey(signingKeyBytes);
        
        return keyPair;
    }
    
    @Override
    public String toBase64(PublicKeyPair keyPair) {
        String base64 = Base64.encode(toByteArray(keyPair));
        // the last char is always '=', so drop it
        return base64.substring(0, base64.length()-1);
    }

    @Override
    public String encryptionKeyToBase64(PublicKey key) throws GeneralSecurityException {
        return Base64.encode(key.getEncoded());
    }

    @Override
    public String toBase64(PrivateKeyPair keyPair) {
        String base64 = Base64.encode(toByteArray(keyPair));
        // the last two chars are always "==", so drop them
        return base64.substring(0, base64.length()-2);
    }

    @Override
    public PublicKeyPair createPublicKeyPair(String base64) throws GeneralSecurityException {
        // append the '=' that is omitted in the encoding
        base64 += '=';
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
    public KeyPair generateEncryptionKeyPair() throws KeyException {
        EncryptionKeyPair encKeyPair = ntruEngine.generateKeyPair();
        PublicKey publicKey = new NtruEncrypt1087PublicKey(encKeyPair.getPublic());
        PrivateKey privateKey = new NtruEncrypt1087PrivateKey(encKeyPair.getPrivate());
        
        return new KeyPair(publicKey, privateKey);
    }
    
    @Override
    public KeyPair generateSigningKeyPair() throws KeyException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        GMSSKeyPairGenerator gmssKeyPairGenerator = new GMSSKeyPairGenerator.GMSSwithSHA512();
        gmssKeyPairGenerator.initialize(new GMSSParameterSpec(GMSS_PARAMETERS), appContext.random());
        
        de.flexiprovider.api.keys.KeyPair flexiKeyPair = gmssKeyPairGenerator.genKeyPair();
        Gmss512PublicKey publicKey = new Gmss512PublicKey(flexiKeyPair.getPublic());
        Gmss512PrivateKey privateKey = new Gmss512PrivateKey(flexiKeyPair.getPrivate());
        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Only accepts <code>NtruEncrypt1087PublicKey</code>s. 
     * @throws GeneralSecurityException
     */
    @Override
    public byte[] encrypt(byte[] data, PublicKey key) throws GeneralSecurityException {
        byte[] symmKey = new byte[32];
        appContext.random().nextBytes(symmKey);
        
        byte iv[] = new byte[BLOCK_SIZE];
        appContext.random().nextBytes(iv);
        byte[] encryptedData = encryptAes(data, symmKey, iv);
        
        NtruEncrypt1087PublicKey ntruKey = castToNtruEncryptKey(key);
        byte[] encryptedSymmKey = ntruEngine.encrypt(symmKey, ntruKey.key);
        
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
     * Only accepts <code>NtruEncrypt1087PublicKey</code>s and <code>Ntru1087PrivateKey</code>s.
     * @throws GeneralSecurityException
     */
    @Override
    public byte[] decrypt(byte[] data, PublicKey publicKey, PrivateKey privateKey) throws GeneralSecurityException {
        if (data == null)
            return null;
        
        ByteBuffer inputBuffer = ByteBuffer.wrap(data);
        byte[] encryptedSymmKey = new byte[ENCRYPTED_LENGTH_BYTES];
        inputBuffer.get(encryptedSymmKey);
        NtruEncrypt1087PublicKey publicNtruKey = castToNtruEncryptKey(publicKey);
        NtruEncrypt1087PrivateKey privateNtruKey = castToNtruEncryptKey(privateKey);
        EncryptionKeyPair keyPair = new EncryptionKeyPair(privateNtruKey.key, publicNtruKey.key);
        byte[] symmKey = ntruEngine.decrypt(encryptedSymmKey, keyPair);
        
        byte[] iv = new byte[BLOCK_SIZE];
        inputBuffer.get(iv);
        byte[] encryptedData = new byte[inputBuffer.remaining()];
        inputBuffer.get(encryptedData);
        byte[] decryptedData = decryptAes(encryptedData, symmKey, iv);
        return decryptedData;
    }

    private NtruEncrypt1087PrivateKey castToNtruEncryptKey(PrivateKey key) {
        if (key instanceof NtruEncrypt1087PrivateKey)
            return (NtruEncrypt1087PrivateKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + NtruEncrypt1087PrivateKey.class.getName());
    }

    /** Only accepts <code>Gmss512PrivateKey</code>s. */
    @Override
    public byte[] sign(byte[] data, PrivateKey key, KeyUpdateHandler keyUpdateHandler) throws GeneralSecurityException, PasswordException {
        Gmss512PrivateKey gmssKey = castToGMSS(key);
        GMSSSignature signer = new GMSSSignature.GMSSwithSHA512();
        signer.initSign(gmssKey.key);
        signer.update(data);
        byte[] signature = signer.sign();
        try {
            keyUpdateHandler.updateKey();
        } catch (IOException e) {
            throw new KeyStoreException("Error updating GMSS key after signing.", e);
        }
        return signature;
    }

    private Gmss512PrivateKey castToGMSS(PrivateKey key) {
        if (key instanceof Gmss512PrivateKey)
            return (Gmss512PrivateKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + Gmss512PrivateKey.class.getName());
    }

    /** Only accepts <code>Gmss512PublicKey</code>s. */
    @Override
    public boolean verify(byte[] data, byte[] signature, PublicKey key) throws GeneralSecurityException {
        Gmss512PublicKey gmssKey = castToGMSS(key);
        GMSSSignature signer = new GMSSSignature.GMSSwithSHA512();
        signer.initVerify(gmssKey.key);
        signer.update(data);
        return signer.verify(signature);
    }
    
    private Gmss512PublicKey castToGMSS(PublicKey key) {
        if (key instanceof Gmss512PublicKey)
            return (Gmss512PublicKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + Gmss512PublicKey.class.getName());
    }

    private class NtruEncrypt1087PublicKey implements PublicKey {
        private static final long serialVersionUID = 8103999492335873827L;
        
        private EncryptionPublicKey key;
        
        public NtruEncrypt1087PublicKey(EncryptionPublicKey key) {
            this.key = key;
        }

        public NtruEncrypt1087PublicKey(byte[] keyBytes) {
            // NTRU expects the values N and q before the actual key
            ByteBuffer buffer = ByteBuffer.allocate(keyBytes.length + 4);
            buffer.putShort((short)NTRUENCRYPT_PARAMETERS.N);
            buffer.putShort((short)NTRUENCRYPT_PARAMETERS.q);
            buffer.put(keyBytes);
            key = new EncryptionPublicKey(buffer.array());
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
            byte[] keyBytes = key.getEncoded();
            // strip the N and q parameters which are not part of the raw key
            keyBytes = Arrays.copyOfRange(keyBytes, 4, keyBytes.length);
            return keyBytes;
        }
        
        @Override
        public boolean equals(Object anotherObj) {
            if (anotherObj==null || !NtruEncrypt1087PublicKey.class.equals(anotherObj.getClass()))
                return false;
            
            NtruEncrypt1087PublicKey otherKey = (NtruEncrypt1087PublicKey)anotherObj;
            return Arrays.equals(getEncoded(), otherKey.getEncoded());
        }
    }
    
    private class NtruEncrypt1087PrivateKey implements PrivateKey {
        private static final long serialVersionUID = 8103999492335873827L;
        
        private EncryptionPrivateKey key;
        
        public NtruEncrypt1087PrivateKey(EncryptionPrivateKey key) {
            this.key = key;
        }

        public NtruEncrypt1087PrivateKey(byte[] keyBytes) {
            // NTRU expects the values N, q, and flags before the actual key
            ByteBuffer buffer = ByteBuffer.allocate(keyBytes.length + 5);
            buffer.putShort((short)NTRUENCRYPT_PARAMETERS.N);
            buffer.putShort((short)NTRUENCRYPT_PARAMETERS.q);
            buffer.put((byte)1);   // the flags byte as calculated in EncryptionPrivateKey.getEncoded()
            buffer.put(keyBytes);
            key = new EncryptionPrivateKey(buffer.array());
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
            byte[] keyBytes = key.getEncoded();
            // strip the N, q, and flags parameters which are not part of the raw key
            keyBytes = Arrays.copyOfRange(keyBytes, 5, keyBytes.length);
            return keyBytes;
        }
        
        @Override
        public boolean equals(Object anotherObj) {
            if (anotherObj==null || !NtruEncrypt1087PrivateKey.class.equals(anotherObj.getClass()))
                return false;
            
            NtruEncrypt1087PrivateKey otherKey = (NtruEncrypt1087PrivateKey)anotherObj;
            return Arrays.equals(getEncoded(), otherKey.getEncoded());
        }
    }
    
    private class Gmss512PublicKey implements PublicKey {
        private static final long serialVersionUID = 6542076074673466836L;
        
        private de.flexiprovider.api.keys.PublicKey key;
        
        public Gmss512PublicKey(de.flexiprovider.api.keys.PublicKey key) throws InvalidKeySpecException {
            this.key = key;
        }

        public Gmss512PublicKey(byte[] keyBytes) throws InvalidKeySpecException {
            GMSSPublicKeySpec keySpec = new GMSSPublicKeySpec(keyBytes, GMSS_PARAMETERS);
            key = gmssKeyFactory.generatePublic(keySpec);
        }

        @Override
        public String getAlgorithm() {
            return "GMSS-512";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }
        
        @Override
        public byte[] getEncoded() {
            byte[] encodedKey = key.getEncoded();
            return Arrays.copyOfRange(encodedKey, 27, 91);   // strip everything but the key itself
        }
        
        @Override
        public boolean equals(Object anotherObj) {
            if (anotherObj==null || !Gmss512PublicKey.class.equals(anotherObj.getClass()))
                return false;
            
            Gmss512PublicKey otherKey = (Gmss512PublicKey)anotherObj;
            return Arrays.equals(getEncoded(), otherKey.getEncoded());
        }
    }
    
    private class Gmss512PrivateKey implements PrivateKey {
        private static final long serialVersionUID = -8488638051563793833L;
        
        private de.flexiprovider.api.keys.PrivateKey key;
        
        public Gmss512PrivateKey(de.flexiprovider.api.keys.PrivateKey key) {
            this.key = key;
        }
        
        public Gmss512PrivateKey(byte[] keyBytes) throws InvalidKeySpecException {
            ByteBuffer paddedSigningKey = ByteBuffer.wrap(keyBytes);
            int sigKeySize = paddedSigningKey.getInt();
            byte[] signingKeyBytes = new byte[sigKeySize];
            paddedSigningKey.get(signingKeyBytes);
            KeySpec signingKeySpec = new PKCS8EncodedKeySpec(signingKeyBytes);
            key = gmssKeyFactory.generatePrivate(signingKeySpec);
        }

        @Override
        public String getAlgorithm() {
            return "GMSS-512";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }
        
        @Override
        public byte[] getEncoded() {
            byte[] encodedKey = key.getEncoded();
            ByteBuffer paddedSigKey = ByteBuffer.allocate(4 + encodedKey.length);
            paddedSigKey.putInt(encodedKey.length);
            paddedSigKey.put(encodedKey);
            encodedKey = paddedSigKey.array();
            encodedKey = Arrays.copyOf(encodedKey, PRIVATE_SIGNING_KEY_BYTES);
            return encodedKey;
        }
        
        @Override
        public boolean equals(Object anotherObj) {
            if (anotherObj==null || !Gmss512PrivateKey.class.equals(anotherObj.getClass()))
                return false;
            
            Gmss512PrivateKey otherKey = (Gmss512PrivateKey)anotherObj;
            return Arrays.equals(getEncoded(), otherKey.getEncoded());
        }
    }
}

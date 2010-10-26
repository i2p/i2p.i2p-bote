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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import net.i2p.I2PAppContext;
import net.i2p.data.Base64;
import net.i2p.data.SessionKey;
import net.i2p.util.Log;

import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.jce.provider.asymmetric.ec.KeyAgreement;
import org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory;
import org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator;
import org.bouncycastle.jce.provider.asymmetric.ec.Signature;
import org.bouncycastle.jce.provider.asymmetric.ec.Signature.ecDSA256;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;

/**
 * Abstract class for ECDH and ECDSA.
 * <p/>
 * The key length used for ECDH and ECDSA depends on the concrete subclass.
 * Symmetric encryption is always AES-256, which for shorter ECDH keys wastes a few
 * bytes and a few CPU cycles (because AES-128 would be sufficient).
 * The same is true for hashing which uses SHA-256 regardless of the ECDH/ECDSA key length.
 * <p/>
 * This class uses BouncyCastle for everything related to ECC (key encoding and decoding,
 * asymmetric encryption, signing, asymmetric key generation), and the AES implementation
 * in I2P for symmetric encryption.
 * <p/>
 * Because the first 6 bits are always zero for all currently existing subclasses, public
 * and private keys produced by this class always start with an upper case A when
 * base64-encoded. The leading A is omitted, which saves two bytes in email destinations
 * (see the {@link #toBase64(PublicKey)} and {@link #toBase64(PrivateKey)} methods).
 */
public abstract class ECDH_ECDSA implements CryptoImplementation {
    private static final int BLOCK_SIZE = 16;   // length of the AES initialization vector; also the AES block size for padding. Not to be confused with the AES key size.
    
    protected int keyLengthBytes;
    protected ECNamedCurveSpec ecParameterSpec;
    private KeyPairGenerator.ECDH encryptionKeyPairGenerator;
    private KeyPairGenerator.ECDSA signingKeyPairGenerator;
    private BouncyECDHKeyFactory ecdhKeyFactory;
    private BouncyECDSAKeyFactory ecdsaKeyFactory;
    private I2PAppContext appContext;
    private Log log = new Log(ECDH_ECDSA.class);
    
    /**
     * 
     * @param curveName
     * @param keyLengthBytes Length of a byte array encoding of one (public or private) key
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    ECDH_ECDSA(String curveName, int keyLengthBytes) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        X9ECParameters params = NISTNamedCurves.getByName(curveName);
        ecParameterSpec = new ECNamedCurveSpec(curveName, params.getCurve(), params.getG(), params.getN(), params.getH(), null);
        this.keyLengthBytes = keyLengthBytes;
        
        encryptionKeyPairGenerator = new KeyPairGenerator.ECDH();
        encryptionKeyPairGenerator.initialize(ecParameterSpec);
        
        signingKeyPairGenerator = new KeyPairGenerator.ECDSA();
        signingKeyPairGenerator.initialize(ecParameterSpec);
        
        ecdhKeyFactory = new BouncyECDHKeyFactory();
        ecdsaKeyFactory = new BouncyECDSAKeyFactory();
        
        appContext = new I2PAppContext();
    }
    
    @Override
    public int getByteArrayPublicKeyPairLength() {
        return 2 * keyLengthBytes;
    }
    
    @Override
    public KeyPair generateEncryptionKeyPair() {
        return encryptionKeyPairGenerator.generateKeyPair();
    }
    
    @Override
    public KeyPair generateSigningKeyPair() {
        return signingKeyPairGenerator.generateKeyPair();
    }
    
    @Override
    public byte[] toByteArray(PublicKeyPair keyPair) {
        byte[] encrKeyArray = toByteArray(keyPair.encryptionKey);
        byte[] signKeyArray = toByteArray(keyPair.signingKey);
        
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            byteStream.write(encrKeyArray);
            byteStream.write(signKeyArray);
        } catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteStream.toByteArray();
    }

    /**
     * Encodes an EC public key into a byte array of length <code>keyLengthBytes</code>.
     * Only accepts <code>ECPublicKey</code>s.
     */
    protected abstract byte[] toByteArray(PublicKey key);
    
    protected ECPublicKey castToEcKey(PublicKey key) {
        if (key instanceof ECPublicKey)
            return (ECPublicKey)key;
        else
            throw new IllegalArgumentException("<key> must be a ECPublicKey.");
    }

    @Override
    public byte[] toByteArray(PrivateKeyPair keyPair) {
        byte[] encrKeyArray = toByteArray(keyPair.encryptionKey);
        byte[] signKeyArray = toByteArray(keyPair.signingKey);
        
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            byteStream.write(encrKeyArray);
            byteStream.write(signKeyArray);
        } catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
    
    /**
     * Encodes an EC private key into a byte array of length <code>keyLengthBytes</code>.
     * Only accepts <code>ECPrivateKey</code>s.
     */
    protected byte[] toByteArray(PrivateKey key) {
        ECPrivateKey ecKey = castToEcKey(key);
        BigInteger s = ecKey.getS();
        byte[] bytes = s.toByteArray();
        // The size of the array depends on the number returned by getS. If the
        // array is less than keyLengthBytes bytes big, add zeros at the beginning.
        if (bytes.length < keyLengthBytes) {
            byte[] paddedBytes = new byte[keyLengthBytes];
            System.arraycopy(bytes, 0, paddedBytes, keyLengthBytes-bytes.length, bytes.length);
            bytes = paddedBytes;
        }
        return bytes;
    }
    
    private ECPrivateKey castToEcKey(PrivateKey key) {
        if (key instanceof ECPrivateKey)
            return (ECPrivateKey)key;
        else
            throw new IllegalArgumentException("<key> must be a ECPrivateKey.");
    }

    @Override
    public PublicKeyPair createPublicKeyPair(byte[] bytes) throws GeneralSecurityException {
        PublicKeyPair keyPair = new PublicKeyPair();
        
        ECPublicKeySpec encryptionKeySpec = createPublicKeySpec(Arrays.copyOf(bytes, keyLengthBytes));
        keyPair.encryptionKey = ecdhKeyFactory.generatePublic(encryptionKeySpec);
        
        ECPublicKeySpec signingKeySpec = createPublicKeySpec(Arrays.copyOfRange(bytes, keyLengthBytes, 2*keyLengthBytes));
        keyPair.signingKey = ecdsaKeyFactory.generatePublic(signingKeySpec);
        
        return keyPair;
    }
    
    @Override
    public PrivateKeyPair createPrivateKeyPair(byte[] bytes) throws GeneralSecurityException {
        PrivateKeyPair keyPair = new PrivateKeyPair();
        
        ECPrivateKeySpec encryptionKeySpec = createPrivateKeySpec(Arrays.copyOf(bytes, keyLengthBytes));
        keyPair.encryptionKey = ecdhKeyFactory.generatePrivate(encryptionKeySpec);
        
        ECPrivateKeySpec signingKeySpec = createPrivateKeySpec(Arrays.copyOfRange(bytes, keyLengthBytes, 2*keyLengthBytes));
        keyPair.signingKey = ecdsaKeyFactory.generatePrivate(signingKeySpec);
        
        return keyPair;
    }
    
    @Override
    public String toBase64(PublicKeyPair keyPair) throws GeneralSecurityException {
        return toBase64(keyPair.encryptionKey) + toBase64(keyPair.signingKey);
    }

    /**
     * This method assumes a base64 encoding of a byte array encoded key always starts
     * with an 'A', which is currently the case for all subclasses.
     * @param publicKey
     * @throws GeneralSecurityException
     */
    protected String toBase64(PublicKey publicKey) throws GeneralSecurityException {
        String base64 = Base64.encode(toByteArray(publicKey));
        if (!base64.startsWith("A"))
            log.error("Error: key does not start with 6 zero bits. Key = " + publicKey);
        return base64.substring(1);
    }

    /**
     * This method assumes a base64 encoding of a byte array encoded key always starts
     * with an 'A', which is currently the case for all subclasses.
     * @param privateKey
     * @throws GeneralSecurityException
     */
    protected String toBase64(PrivateKey privateKey) throws GeneralSecurityException {
        String base64 = Base64.encode(toByteArray(privateKey));
        if (!base64.startsWith("A"))
            log.error("Error: key does not start with 6 zero bits. Key = " + privateKey);
        return base64.substring(1);
    }

    @Override
    public String toBase64(PrivateKeyPair keyPair) throws GeneralSecurityException {
        return toBase64(keyPair.encryptionKey) + toBase64(keyPair.signingKey);
    }

    @Override
    public PublicKeyPair createPublicKeyPair(String base64) throws GeneralSecurityException {
        int base64PublicKeyLength = getBase64PublicKeyPairLength() / 2;
        base64 = "A" + base64.substring(0, base64PublicKeyLength) + "A" + base64.substring(base64PublicKeyLength);
        byte[] bytes = Base64.decode(base64);
        return createPublicKeyPair(bytes);
    }

    public abstract PrivateKeyPair createPrivateKeyPair(String base64) throws GeneralSecurityException;
    
    protected int getBase64PrivateKeyPairLength() {
        return getBase64PublicKeyPairLength();
    }
    
    protected ECPrivateKeySpec createPrivateKeySpec(byte[] encodedKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        // make a private key from the private point s
        BigInteger s = new BigInteger(encodedKey);
        return new ECPrivateKeySpec(s, ecParameterSpec);
    }
    
    /**
     * Encrypts a block of data using the following steps:
     * <p/>
     * <ol>
     *   <li/>Generate an ephemeral EC key.<br/>
     *   <li/>Use that key and the public key of the recipient, generate a secret using ECDH.<br/>
     *   <li/>Use that secret as a key to encrypt the message with AES.<br/>
     *   <li/>Return the encrypted message and the ephemeral public key generated in step 1.<br/>
     * </ol>
     * TODO Replace AESEngine.encrypt() with AESEngine.safeEncrypt(), remove padding code
     * @throws NoSuchProviderException 
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     * @throws NoSuchPaddingException 
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     */
    @Override
    public byte[] encrypt(byte[] data, PublicKey encryptionKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        
        // pad the data
        int unpaddedLength = data.length;
        data = Arrays.copyOf(data, unpaddedLength + BLOCK_SIZE - unpaddedLength%BLOCK_SIZE);  // make data.length a multiple of BLOCK_SIZE; if the length is a multiple of BLOCK_LENGTH, add a block of zeros
        PKCS7Padding padding = new PKCS7Padding();
        int numAdded = padding.addPadding(data, unpaddedLength);
        if (log.shouldLog(Log.DEBUG) && numAdded != BLOCK_SIZE-unpaddedLength%BLOCK_SIZE)
            log.error("Error: " + numAdded + " pad bytes added, expected: " + (BLOCK_SIZE-unpaddedLength%BLOCK_SIZE));

        // generate an ephemeral EC key and a shared secret
        KeyPair ephKeyPair = encryptionKeyPairGenerator.generateKeyPair();
        ECDHKeyAgreement keyAgreement = new ECDHKeyAgreement();
        keyAgreement.init(ephKeyPair.getPrivate());
        keyAgreement.doPhase(encryptionKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();
        MessageDigest hashAlg = MessageDigest.getInstance("SHA-256");
        byte[] secretHash = hashAlg.digest(sharedSecret);
        if (sharedSecret.length < secretHash.length)
            log.warn("Not enough data in shared secret!");
        
        // encrypt the data using the hash of the shared secret as an AES key
        SessionKey aesKey = new SessionKey(secretHash);
        byte iv[] = new byte[BLOCK_SIZE];
        appContext.random().nextBytes(iv);
        byte[] encryptedData = new byte[data.length];
        appContext.aes().encrypt(data, 0, encryptedData, 0, aesKey, iv, data.length);   // this method also checks that data.length is divisible by 16
        
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            byteStream.write(toByteArray(ephKeyPair.getPublic()));
            byteStream.write(iv);
            byteStream.write(encryptedData);
        } catch (IOException e) {
            log.debug("Can't write to ByteArrayOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
    
    /**
     * Decrypts a block of data using the following steps:
     * <ol>
     *   <li/>Read the ephemeral public key from the message.<br/>
     *   <li/>Use that public key together with your recipient key to generate a secret using ECDH.<br/>
     *   <li/>Use that secret as a key to decrypt the message with AES.<br/>
     * </ol>
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     * @throws InvalidKeySpecException 
     * @throws InvalidCipherTextException 
     */
    @Override
    public byte[] decrypt(byte[] data, PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, InvalidCipherTextException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        try {
            // read the ephemeral public key
            byte[] encodedKey = new byte[keyLengthBytes];
            byteStream.read(encodedKey);
            ECPublicKeySpec ephPublicKeySpec = createPublicKeySpec(encodedKey);
            PublicKey ephPublicKey = ecdhKeyFactory.generatePublic(ephPublicKeySpec);
        
            // reconstruct the shared secret
            ECDHKeyAgreement keyAgreement = new ECDHKeyAgreement();
            keyAgreement.init(key);
            keyAgreement.doPhase(ephPublicKey, true);
            byte[] sharedSecret = keyAgreement.generateSecret();
            MessageDigest hashAlg = MessageDigest.getInstance("SHA-256");
            byte[] secretHash = hashAlg.digest(sharedSecret);
        
            // decrypt using the shared secret as an AES key
            byte[] iv = new byte[BLOCK_SIZE];
            byteStream.read(iv);
            byte[] encryptedData = Util.readBytes(byteStream);
            SessionKey aesKey = new SessionKey(secretHash);
            byte[] decryptedData = new byte[encryptedData.length];
            if (encryptedData.length%16 != 0)
                log.error("Length of encrypted data is not divisible by " + BLOCK_SIZE + ". Length=" + decryptedData.length);
            appContext.aes().decrypt(encryptedData, 0, decryptedData, 0, aesKey, iv, decryptedData.length);
            
            // unpad the decrypted data
            byte[] lastBlock = Arrays.copyOfRange(decryptedData, decryptedData.length-BLOCK_SIZE, decryptedData.length);
            PKCS7Padding padding = new PKCS7Padding();
            int padCount = padding.padCount(lastBlock);
            decryptedData = Arrays.copyOf(decryptedData, decryptedData.length-padCount);
            
            return decryptedData;
        }
        catch (IOException e) {
            log.debug("Can't read from ByteArrayOutputStream.", e);
            return null;
        }
    }

    protected abstract ECPublicKeySpec createPublicKeySpec(byte[] encodedKey) throws InvalidKeySpecException, NoSuchAlgorithmException;
    
    @Override
    public byte[] sign(byte[] data, PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        BouncyECDSASigner signatureAlg = new BouncyECDSASigner();
        signatureAlg.initSign(key);
        signatureAlg.update(data);
        byte[] signature = signatureAlg.sign();
        
        return signature;
    }

    @Override
    public boolean verify(byte[] data, byte[] signature, PublicKey key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        BouncyECDSASigner signatureAlg = new BouncyECDSASigner();
        signatureAlg.initVerify(key);
        signatureAlg.update(data);
        boolean valid = signatureAlg.verify(signature);
        
        return valid;
    }
    
    /** This class exposes the protected <code>engine*</code> methods in {@link KeyAgreement.DH} */
    private class ECDHKeyAgreement extends KeyAgreement.DH {
        
        public void init(Key key) throws InvalidKeyException {
            engineInit(key, appContext.random());
        }
        
        public void doPhase(Key key, boolean lastPhase) throws InvalidKeyException, IllegalStateException {
            engineDoPhase(key, lastPhase);
        }
        
        public byte[] generateSecret() {
            return engineGenerateSecret();
        }
    }
    
    /** This class exposes the protected <code>engine*</code> methods in {@link KeyFactory.ECDH} */
    @SuppressWarnings("unchecked")   // this eliminates a warning in the ant build caused by org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory.engineGetKeySpec(java.security.Key,java.lang.Class)
    private class BouncyECDHKeyFactory extends KeyFactory.ECDH {
        
        public PublicKey generatePublic(KeySpec keySpec) throws InvalidKeySpecException {
            return engineGeneratePublic(keySpec);
        }
        
        public PrivateKey generatePrivate(KeySpec keySpec) throws InvalidKeySpecException {
            return engineGeneratePrivate(keySpec);
        }
    }
    
    /** This class exposes the protected <code>engine*</code> methods in {@link KeyFactory.ECDSA} */
    @SuppressWarnings("unchecked")   // this eliminates a warning in the ant build caused by org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory.engineGetKeySpec(java.security.Key,java.lang.Class)
    private class BouncyECDSAKeyFactory extends KeyFactory.ECDSA {
        
        public PublicKey generatePublic(KeySpec keySpec) throws InvalidKeySpecException {
            return engineGeneratePublic(keySpec);
        }
        
        public PrivateKey generatePrivate(KeySpec keySpec) throws InvalidKeySpecException {
            return engineGeneratePrivate(keySpec);
        }
    }
    
    /** This class exposes the protected <code>engine*</code> methods in {@link Signature.ecDSA256} */
    private class BouncyECDSASigner extends ecDSA256 {
        
        public final void initSign(PrivateKey privateKey) throws InvalidKeyException {
            engineInitSign(privateKey);
        }
        
        public final void initVerify(PublicKey publicKey) throws InvalidKeyException {
            engineInitVerify(publicKey);
        }
        
        public final void update(byte[] data) throws SignatureException {
            engineUpdate(data, 0, data.length);
        }
        
        public final byte[] sign() throws SignatureException {
            return engineSign();
        }
        
        public final boolean verify(byte[] signature) throws SignatureException {
            return engineVerify(signature);
        }
    }
}
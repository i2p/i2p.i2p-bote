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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.i2p.data.Base64;
import net.i2p.util.Log;

import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.asymmetric.ec.EC5Util;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;

/**
 * 521-bit ECDH with AES-256 and 521-bit ECDSA with SHA-256.
 * 
 * This class uses BouncyCastle for key encoding and decoding, but it can use any ECC provider for
 * encryption, signing, and key generation.
 * 
 * Because the first 6 bits are always zero, public and private keys produced by this class always
 * start with an upper case A when base64-encoded. The leading A is omitted, which saves two
 * bytes in email destinations.
 * 
 * TODO document the 66-byte format
 */
public class ECDH521_ECDSA521 implements CryptoImplementation {
    private static final String CURVE_NAME = "P-521";   // The NIST P-521 curve, also known as secp521r1
	private static final int IV_LENGTH = 16;   // length of the AES initialization vector
	private static final int ENCODED_KEY_LENGTH = 66;   // length of a byte-encoded key
	
	private ECNamedCurveSpec ecParameterSpec;
	private KeyPairGenerator encryptionKeyPairGenerator;
	private KeyPairGenerator signingKeyPairGenerator;
    private Log log = new Log(ECDH521_ECDSA521.class);

	ECDH521_ECDSA521() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
	    // Attempt to install the BouncyCastle JCE provider; if there is an error, continue anyway because a
	    // provider might have been installed in lib/security/java.security.
        try {
            Security.addProvider(new BouncyCastleProvider());
        } catch (Exception e) {
            Log log = new Log(CryptoFactory.class);
            log.debug("Can't instantiate crypto provider.", e);
        }
        
        X9ECParameters params = NISTNamedCurves.getByName(CURVE_NAME);
        ecParameterSpec = new ECNamedCurveSpec(CURVE_NAME, params.getCurve(), params.getG(), params.getN(), params.getH(), null);
        
		encryptionKeyPairGenerator = KeyPairGenerator.getInstance("ECDH");
        encryptionKeyPairGenerator.initialize(ecParameterSpec);
        
        signingKeyPairGenerator = KeyPairGenerator.getInstance("ECDSA");
        signingKeyPairGenerator.initialize(ecParameterSpec);
	}
	
    @Override
    public String getName() {
        return Util._("521-bit Elliptic Curve Encryption");
    }
    
    @Override
    public byte getId() {
        return 2;
    }

    @Override
    public int getBase64PublicKeyPairLength() {
        return 174;
    }
    
    @Override
    public int getBase64CompleteKeySetLength() {
        return 348;
    }
    
    @Override
    public int getByteArrayPublicKeyPairLength() {
        return 132;
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
     * Encodes an EC public key into a byte array of length <code>ENCODED_KEY_LENGTH</code>.
     * Only accepts <code>ECPublicKey</code>s.
     */
    private byte[] toByteArray(PublicKey key) {
        ECPublicKey ecKey = castToEcKey(key);
        
        byte[] bouncyCompressedKey = EC5Util.convertPoint(ecKey.getParams(), ecKey.getW(), true).getEncoded();
        if (bouncyCompressedKey.length != ENCODED_KEY_LENGTH+1)
            log.error("Wrong key length: " + bouncyCompressedKey.length + ", should be " + (ENCODED_KEY_LENGTH+1));
        
        // shorten by one byte (bouncyCompressedKey[0] is either 2 or 3, bouncyCompressedKey[1] is either 0 or 1, so they can fit in two bits)
        if (bouncyCompressedKey[0]!=2 && bouncyCompressedKey[0]!=3)
            log.error("Illegal value in encoded EC key at byte 0: " + bouncyCompressedKey[0] + ", can only be 2 or 3.");
        if (bouncyCompressedKey[1]!=0 && bouncyCompressedKey[1]!=1)
            log.error("Illegal value in encoded EC key at byte 1: " + bouncyCompressedKey[1] + ", can only be 0 or 1.");
        byte[] compressedKey = Arrays.copyOfRange(bouncyCompressedKey, 1, ENCODED_KEY_LENGTH+1);
        compressedKey[0] |= (bouncyCompressedKey[0]-2) << 1;
        
        return compressedKey;
    }
    
    private ECPublicKey castToEcKey(PublicKey key) {
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
     * Encodes an EC private key into a byte array of length <code>ENCODED_KEY_LENGTH</code>.
     * Only accepts <code>ECPrivateKey</code>s.
     */
    private byte[] toByteArray(PrivateKey key) {
        ECPrivateKey ecKey = castToEcKey(key);
        byte[] bytes = ecKey.getS().toByteArray();
        // make sure the array is ENCODED_KEY_LENGTH bytes big (it will sometimes be less, depending on the number returned by getS)
        if (bytes.length < ENCODED_KEY_LENGTH)
            bytes = Arrays.copyOf(bytes, ENCODED_KEY_LENGTH);
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
        keyPair.encryptionKey = createPublicKey(Arrays.copyOf(bytes, ENCODED_KEY_LENGTH), "ECDH");
        keyPair.signingKey = createPublicKey(Arrays.copyOfRange(bytes, ENCODED_KEY_LENGTH, 2*ENCODED_KEY_LENGTH), "ECDSA");
        return keyPair;
    }
    
    @Override
    public PrivateKeyPair createPrivateKeyPair(byte[] bytes) throws GeneralSecurityException {
        PrivateKeyPair keyPair = new PrivateKeyPair();
        keyPair.encryptionKey = createPrivateKey(Arrays.copyOf(bytes, ENCODED_KEY_LENGTH), "ECDH");
        keyPair.signingKey = createPrivateKey(Arrays.copyOfRange(bytes, ENCODED_KEY_LENGTH, 2*ENCODED_KEY_LENGTH), "ECDSA");
        return keyPair;
    }
    
    @Override
    public String toBase64(PublicKeyPair keyPair) throws GeneralSecurityException {
        return toBase64(keyPair.encryptionKey) + toBase64(keyPair.signingKey);
    }

    private String toBase64(PublicKey publicKey) throws GeneralSecurityException {
        String base64 = Base64.encode(toByteArray(publicKey));
        if (!base64.startsWith("A"))
            log.error("Error: key does not start with 6 zero bits. Key = " + publicKey);
        return base64.substring(1);
    }

    @Override
    public String toBase64(PrivateKeyPair keyPair) throws GeneralSecurityException {
        return toBase64(keyPair.encryptionKey) + toBase64(keyPair.signingKey);
    }

    private String toBase64(PrivateKey privateKey) throws GeneralSecurityException {
        String base64 = Base64.encode(toByteArray(privateKey));
        if (!base64.startsWith("A"))
            log.error("Error: key does not start with 6 zero bits. Key = " + privateKey);
        return base64.substring(1);
    }

    @Override
    public PublicKeyPair createPublicKeyPair(String base64) throws GeneralSecurityException {
        base64 = "A" + base64.substring(0, 87) + "A" + base64.substring(87);
        byte[] bytes = Base64.decode(base64);
        return createPublicKeyPair(bytes);
    }

    @Override
    public PrivateKeyPair createPrivateKeyPair(String base64) throws GeneralSecurityException {
        base64 = "A" + base64.substring(0, 87) + "A" + base64.substring(87);
        byte[] bytes = Base64.decode(base64);
        return createPrivateKeyPair(bytes);
    }

    private PrivateKey createPrivateKey(byte[] encodedKey, String keyType) throws InvalidKeySpecException, NoSuchAlgorithmException {
        // make a private key from the private point s
        BigInteger s = new BigInteger(encodedKey);
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(s, ecParameterSpec);
        PrivateKey key = KeyFactory.getInstance(keyType).generatePrivate(privateKeySpec);
        
        return key;
    }
    
	/**
	 * Encrypts a block of data using the following steps:
	 * 
     * 1. Generate an ephemeral EC key.
     * 2. Use that key and the public key of the recipient, generate a secret using ECDH.
     * 3. Use that secret as a key to encrypt the message with AES.
     * 4. Return the encrypted message and the ephemeral public key generated in step 1.
     * 
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	@Override
	public byte[] encrypt(byte[] data, PublicKey encryptionKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        // generate an ephemeral EC key and a shared secret
        KeyPair ephKeyPair = encryptionKeyPairGenerator.generateKeyPair();
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(ephKeyPair.getPrivate());
        keyAgreement.doPhase(encryptionKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();
        MessageDigest hashAlg = MessageDigest.getInstance("SHA-256");
        byte[] secretHash = hashAlg.digest(sharedSecret);
        
        // encrypt the data using the hash of the shared secret as an AES key
        SecretKeySpec symmKeySpec = new SecretKeySpec(secretHash, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, symmKeySpec);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            byteStream.write(toByteArray(ephKeyPair.getPublic()));
        	byte[] iv = cipher.getIV();
        	if (iv.length != IV_LENGTH)
        		log.error("Length of the initialization vector is " + iv.length + ", should be " + IV_LENGTH);
			byteStream.write(iv);
			byteStream.write(cipher.doFinal(data));
		} catch (IOException e) {
			log.debug("Can't write to ByteArrayOutputStream.", e);
		}
        byte[] encrypted = byteStream.toByteArray();
        
        return encrypted;
	}

	/**
     * Signs a block of data using the following steps:
     * 
     * 1. Read the ephemeral public key from the message.
     * 2. Use that public key together with your recipient key to generate a secret using ECDH.
     * 3. Use that secret as a key to decrypt the message with AES.
     * 
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeySpecException 
	 */
	@Override
	public byte[] decrypt(byte[] data, PrivateKey key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeySpecException {
		KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(key);
        
        byte[] encodedKey = Arrays.copyOf(data, ENCODED_KEY_LENGTH);
        PublicKey ephPublicKey = createPublicKey(encodedKey, "ECDH");
        
        keyAgreement.doPhase(ephPublicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();
        MessageDigest hashAlg = MessageDigest.getInstance("SHA-256");
        byte[] secretHash = hashAlg.digest(sharedSecret);
        
        SecretKeySpec symmKeySpec = new SecretKeySpec(secretHash, "AES");
        if (data.length < ENCODED_KEY_LENGTH+IV_LENGTH)
            log.error("Can't read all " + IV_LENGTH + " bytes of the initialization vector from the encrypted data.");
        byte[] iv = Arrays.copyOfRange(data, ENCODED_KEY_LENGTH, ENCODED_KEY_LENGTH+IV_LENGTH);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, symmKeySpec, ivSpec);
        byte[] encryptedData = Arrays.copyOfRange(data, ENCODED_KEY_LENGTH+IV_LENGTH, data.length);
        byte[] decryptedData = cipher.doFinal(encryptedData);
        
        return decryptedData;
	}

	private PublicKey createPublicKey(byte[] encodedKey, String keyType) throws InvalidKeySpecException, NoSuchAlgorithmException {
		// convert the key to the format used by BouncyCastle, which adds one byte
        byte[] bouncyCompressedKey = new byte[ENCODED_KEY_LENGTH+1];
        System.arraycopy(encodedKey, 0, bouncyCompressedKey, 1, ENCODED_KEY_LENGTH);
        bouncyCompressedKey[0] = (byte)((bouncyCompressedKey[1] >> 1) + 2);
        bouncyCompressedKey[1] &= 1;
        // decompress into an EC point
        ECPoint w = ECPointUtil.decodePoint(ecParameterSpec.getCurve(), bouncyCompressedKey);
        
        // make a public key from the public point w
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(w, ecParameterSpec);
        PublicKey key = KeyFactory.getInstance(keyType).generatePublic(publicKeySpec);
        
        return key;
	}
	
	@Override
	public byte[] sign(byte[] data, PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signatureAlg = Signature.getInstance("SHA256WITHECDSA");
        signatureAlg.initSign(key);
        signatureAlg.update(data);
        byte[] signature = signatureAlg.sign();
        
        return signature;
	}

	@Override
	public boolean verify(byte[] data, byte[] signature, PublicKey key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
	    Signature signatureAlg = Signature.getInstance("SHA256WITHECDSA");
        signatureAlg.initVerify(key);
        signatureAlg.update(data);
        boolean valid = signatureAlg.verify(signature);
        
        return valid;
	}
}
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
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.crypto.DSAEngine;
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.DataStructure;
import net.i2p.data.Destination;
import net.i2p.data.Signature;

/**
 * 2048-bit ElGamal / 1024-bit DSA.
 * 
 * Uses the I2P crypto routines.
 */
public class ElGamal2048_DSA1024 extends AbstractCryptoImplementation {
    public ElGamal2048_DSA1024() throws GeneralSecurityException {
        super();
    }

    @Override
    public String getName() {
        return "ElGamal-2048 / DSA-1024";
    }
    
    @Override
    public byte getId() {
        return 1;
    }

    @Override
    public int getBase64PublicKeyPairLength() {
        return 512;
    }
    
    @Override
    public int getBase64CompleteKeySetLength() {
        return 880;
    }
    
    @Override
    public int getByteArrayPublicKeyPairLength() {
        return 384;
    }
    
    @Override
    public PublicKeyPair createPublicKeyPair(byte[] bytes) throws GeneralSecurityException {
        return createPublicKeyPair(Base64.encode(bytes));
    }
    
    @Override
    public PrivateKeyPair createPrivateKeyPair(byte[] bytes) throws GeneralSecurityException {
        return createPrivateKeyPair(Base64.encode(bytes));
    }
    
    @Override
    public String toBase64(PublicKeyPair keyPair) throws GeneralSecurityException {
        return Base64.encode(toByteArray(keyPair));
    }
    
    @Override
    public String encryptionKeyToBase64(PublicKey key) throws GeneralSecurityException {
        return Base64.encode(key.getEncoded());
    }

    @Override
    public String toBase64(PrivateKeyPair keyPair) throws GeneralSecurityException {
        return Base64.encode(toByteArray(keyPair));
    }

    @Override
    public PublicKeyPair createPublicKeyPair(String base64) throws GeneralSecurityException {
        Destination i2pDestination;
        base64 += "AAAA";   // add a null certificate
        try {
            i2pDestination = new Destination(base64);
        } catch (DataFormatException e) {
            throw new KeyException("Can't create I2P destination from Base64: <" + base64 + ">", e);
        }
        PublicKeyPair keyPair = new PublicKeyPair();
        keyPair.encryptionKey = new ElGamalPublicKey(i2pDestination.getPublicKey());
        keyPair.signingKey = new DSAPublicKey(i2pDestination.getSigningPublicKey());
        return keyPair;
    }

    @Override
    public PrivateKeyPair createPrivateKeyPair(String base64) throws GeneralSecurityException {
        // convert to byte[] first because the two keys end at byte boundaries, but not at base64 char boundaries
        byte[] bytes = Base64.decode(base64);
        
        byte[] encryptionKeyBytes = Arrays.copyOfRange(bytes, 0, net.i2p.data.PrivateKey.KEYSIZE_BYTES);
        net.i2p.data.PrivateKey i2pEncryptionKey = new net.i2p.data.PrivateKey(encryptionKeyBytes);
        
        int signingKeyStart = net.i2p.data.PrivateKey.KEYSIZE_BYTES;
        int signingKeyEnd = signingKeyStart + net.i2p.data.SigningPrivateKey.KEYSIZE_BYTES;
        byte[] signingKeyBytes = Arrays.copyOfRange(bytes, signingKeyStart, signingKeyEnd);
        net.i2p.data.SigningPrivateKey i2pSigningKey = new net.i2p.data.SigningPrivateKey(signingKeyBytes);
        
        PrivateKeyPair keyPair = new PrivateKeyPair();
        keyPair.encryptionKey = new ElGamalPrivateKey(i2pEncryptionKey);
        keyPair.signingKey = new DSAPrivateKey(i2pSigningKey);
        return keyPair;
    }

    @Override
    public KeyPair generateEncryptionKeyPair() throws KeyException {
        I2PSession i2pSession = createI2PSession();
        
        net.i2p.data.PublicKey i2pPublicKey = i2pSession.getMyDestination().getPublicKey();
        net.i2p.data.PrivateKey i2pPrivateKey = i2pSession.getDecryptionKey();
        PublicKey publicKey = new ElGamalPublicKey(i2pPublicKey);
        PrivateKey privateKey = new ElGamalPrivateKey(i2pPrivateKey);
        
        return new KeyPair(publicKey, privateKey);
    }
    
    @Override
    public KeyPair generateSigningKeyPair() throws KeyException {
        I2PSession i2pSession = createI2PSession();
        
        net.i2p.data.SigningPublicKey i2pPublicKey = i2pSession.getMyDestination().getSigningPublicKey();
        net.i2p.data.SigningPrivateKey i2pPrivateKey = i2pSession.getPrivateKey();
        PublicKey publicKey = new DSAPublicKey(i2pPublicKey);
        PrivateKey privateKey = new DSAPrivateKey(i2pPrivateKey);
        
        return new KeyPair(publicKey, privateKey);
    }
    
    private I2PSession createI2PSession() throws KeyException {
        try {
            I2PClient i2pClient = I2PClientFactory.createClient();
            ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
            i2pClient.createDestination(arrayStream);
            byte[] destinationArray = arrayStream.toByteArray();
            I2PSession i2pSession = i2pClient.createSession(new ByteArrayInputStream(destinationArray), null);
            return i2pSession;
        }
        catch (Exception e) {
            throw new KeyException("Can't generate I2P destination.", e);
        }
    }
    
    /** Only accepts <code>ElGamalPublicKey</code>s. */
    @Override
    public byte[] encrypt(byte[] data, PublicKey key) throws GeneralSecurityException {
        ElGamalPublicKey elGamalKey = castToElGamal(key);
        net.i2p.data.PublicKey i2pPublicKey = elGamalKey.getI2PKey();
        return Util.encrypt(data, i2pPublicKey);
    }

    private ElGamalPublicKey castToElGamal(PublicKey key) {
        if (key instanceof ElGamalPublicKey)
            return (ElGamalPublicKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + ElGamalPublicKey.class.getName());
    }

    /** Only accepts <code>ElGamalPrivateKey</code>s. The public key is not used. */
    @Override
    public byte[] decrypt(byte[] data, PublicKey publicKey, PrivateKey privateKey) throws GeneralSecurityException {
        if (data == null)
            return null;
        
        ElGamalPrivateKey elGamalKey = castToElGamal(privateKey);
        try {
            net.i2p.data.PrivateKey i2pPrivateKey = elGamalKey.getI2PKey();
            return Util.decrypt(data, i2pPrivateKey);
        } catch (DataFormatException e) {
            byte[] shortenedData = data.length>10?Arrays.copyOf(data, 10):data;
            throw new KeyException("Can't decrypt data: " + Arrays.toString(shortenedData) + " (only the first 10 elements are shown).", e);
        }
    }

    private ElGamalPrivateKey castToElGamal(PrivateKey key) {
        if (key instanceof ElGamalPrivateKey)
            return (ElGamalPrivateKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + ElGamalPrivateKey.class.getName());
    }

    /** Only accepts <code>DSAPrivateKey</code>s. */
    @Override
    public byte[] sign(byte[] data, PrivateKey privateKey, KeyUpdateHandler keyupdateHandler) throws GeneralSecurityException {
        DSAPrivateKey dsaKey = castToDSA(privateKey);
        Signature signature = DSAEngine.getInstance().sign(data, dsaKey.getI2PKey());
        return signature.toByteArray();
    }

    private DSAPrivateKey castToDSA(PrivateKey key) {
        if (key instanceof DSAPrivateKey)
            return (DSAPrivateKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + DSAPrivateKey.class.getName());
    }

    /** Only accepts <code>DSAPublicKey</code>s. */
    @Override
    public boolean verify(byte[] data, byte[] signature, PublicKey key) throws GeneralSecurityException {
        DSAPublicKey dsaKey = castToDSA(key);
        Signature signatureObj = new Signature(signature);
        boolean valid = DSAEngine.getInstance().verifySignature(signatureObj, data, dsaKey.getI2PKey());
        return valid;
    }
    
    private DSAPublicKey castToDSA(PublicKey key) {
        if (key instanceof DSAPublicKey)
            return (DSAPublicKey)key;
        else
            throw new IllegalArgumentException("<key> must be a " + DSAPublicKey.class.getName());
    }

    /**
     * This class and its subclasses wrap I2P key objects in JCE interfaces.
     * @param <T>
     */
    private abstract class KeyImpl<T extends DataStructure> implements Key {
        private static final long serialVersionUID = -8188867382999056897L;
        
        private T i2pKey;
        private String algorithm;
        
        /**
         * @param i2pKey One of the four I2P asymmetric key classes
         * @param algorithm Can be anything
         */
        public KeyImpl(T i2pKey, String algorithm) {
            this.i2pKey = i2pKey;
            this.algorithm = algorithm;
        }

        public T getI2PKey() {
            return i2pKey;
        }
        
        @Override
        public String getAlgorithm() {
            return algorithm;
        }

        @Override
        public String getFormat() {
            return "RAW";
        }
        
        @Override
        public byte[] getEncoded() {
            return i2pKey.toByteArray();
        }
    }
    
    private class ElGamalPublicKey extends KeyImpl<net.i2p.data.PublicKey> implements PublicKey {
        private static final long serialVersionUID = -4454000993523471441L;

        public ElGamalPublicKey(net.i2p.data.PublicKey i2pPublicKey) {
            super(i2pPublicKey, "ElGamal-2048");
        }
    }
    
    private class ElGamalPrivateKey extends KeyImpl<net.i2p.data.PrivateKey> implements PrivateKey {
        private static final long serialVersionUID = -9067327625123945685L;

        public ElGamalPrivateKey(net.i2p.data.PrivateKey i2pPrivateKey) {
            super(i2pPrivateKey, "ElGamal-2048");
        }
    }
    
    private class DSAPublicKey extends KeyImpl<net.i2p.data.SigningPublicKey> implements PublicKey {
        private static final long serialVersionUID = -6326463273460925920L;

        public DSAPublicKey(net.i2p.data.SigningPublicKey i2pPublicKey) {
            super(i2pPublicKey, "DSA-1024");
        }
    }
    
    private class DSAPrivateKey extends KeyImpl<net.i2p.data.SigningPrivateKey> implements PrivateKey {
        private static final long serialVersionUID = 9200457056905555105L;

        public DSAPrivateKey(net.i2p.data.SigningPrivateKey i2pPrivateKey) {
            super(i2pPrivateKey, "DSA-1024");
        }
    }
}

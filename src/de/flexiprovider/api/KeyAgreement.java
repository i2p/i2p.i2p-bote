package de.flexiprovider.api;

import javax.crypto.KeyAgreementSpi;

import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.api.exceptions.ShortBufferException;
import de.flexiprovider.api.keys.Key;
import de.flexiprovider.api.keys.PrivateKey;
import de.flexiprovider.api.keys.PublicKey;
import de.flexiprovider.api.keys.SecretKey;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.common.util.JavaSecureRandomWrapper;

public abstract class KeyAgreement extends KeyAgreementSpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * JCA adapter for FlexiAPI method init(): initialize this
     * <tt>KeyAgreementSpi</tt> with a key and a source of randomness.
     * 
     * @param key
     *                the secret key of the party initializing the key agreement
     * @param javaRand
     *                the source of randomness
     * @throws java.security.InvalidKeyException
     *                 if the key is invalid.
     * @throws RuntimeException
     *                 if parameters are required for initialization.
     */
    protected final void engineInit(java.security.Key key,
	    java.security.SecureRandom javaRand)
	    throws java.security.InvalidKeyException {

	if (!(key instanceof PrivateKey)) {
	    throw new java.security.InvalidKeyException();
	}

	try {
	    SecureRandom flexiRand = new JavaSecureRandomWrapper(javaRand);
	    init((PrivateKey) key, (AlgorithmParameterSpec) null, flexiRand);
	} catch (InvalidAlgorithmParameterException e) {
	    throw new RuntimeException("algorithm parameters required");
	}
    }

    /**
     * JCA adapter for FlexiAPI method init(): initialize this
     * <tt>KeyAgreementSpi</tt> with a key, algorithm parameters, and a source
     * of randomness.
     * 
     * @param key
     *                the secret key of the party initializing the key agreement
     * @param params
     *                the algorithm parameters
     * @param javaRand
     *                the source of randomness
     * @throws java.security.InvalidKeyException
     *                 if the key is invalid.
     * @throws java.security.InvalidAlgorithmParameterException
     *                 if the parameters are invalid or null and parameters are
     *                 needed for initialization.
     */
    protected final void engineInit(java.security.Key key,
	    java.security.spec.AlgorithmParameterSpec params,
	    java.security.SecureRandom javaRand)
	    throws java.security.InvalidKeyException,
	    java.security.InvalidAlgorithmParameterException {

	if (!(key instanceof PrivateKey)) {
	    throw new java.security.InvalidKeyException();
	}

	SecureRandom flexiRand = new JavaSecureRandomWrapper(javaRand);
	init((PrivateKey) key, (AlgorithmParameterSpec) params, flexiRand);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #generateSecret()} : generate a
     * shared secret and return it as a byte array.
     * 
     * @return the shared secret as byte array.
     * @throws IllegalStateException
     *                 if the object is not in doPhase.
     */
    protected final byte[] engineGenerateSecret() throws IllegalStateException {
	return generateSecret();
    }

    /**
     * JCA adapter for FlexiAPI method {@link #generateSecret(byte[], int)}:
     * generate a shared secret and place it into the buffer
     * <tt>sharedSecret</tt>, beginning at <tt>offset</tt>.
     * 
     * @param sharedSecret
     *                the buffer to hold the shared secret
     * @param offset
     *                the offset in <tt>sharedSecret</tt> where the shared
     *                secret will be stored
     * @return the number of bytes written in <tt>sharedSecret</tt>
     * @throws IllegalStateException
     *                 if the key agreement scheme has not been initialized
     *                 properly.
     * @throws javax.crypto.ShortBufferException
     *                 if <tt>sharedSecret</tt> is too small to to hold the
     *                 shared secret
     */
    protected final int engineGenerateSecret(byte[] sharedSecret, int offset)
	    throws IllegalStateException, javax.crypto.ShortBufferException {

	return generateSecret(sharedSecret, offset);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #generateSecret(String)}:
     * generate a shared secret via the algorithm specified in
     * <tt>algorithm</tt>.
     * 
     * @param algorithm
     *                the desired algorithm for the generation of the secret
     * @return the shared secret
     * @throws IllegalStateException
     *                 if the key agreement scheme has not been initialized
     *                 properly.
     * @throws java.security.NoSuchAlgorithmException
     *                 if <tt>algorithm</tt> is invalid.
     */
    protected final javax.crypto.SecretKey engineGenerateSecret(String algorithm)
	    throws IllegalStateException,
	    java.security.NoSuchAlgorithmException {

	return generateSecret(algorithm);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #doPhase(PublicKey, boolean)}:
     * execute the next phase of this key agreement with the given key that was
     * received from one of the other parties involved in this key agreement.
     * 
     * @param key
     *                the public key of the other party
     * @param lastPhase
     *                <tt>true</tt> if this is the last phase of the key
     *                agreement. After the last phase only
     *                <tt>generateSecret</tt> may be called.
     * @return the shared secret
     * @throws IllegalStateException
     *                 if the key agreement scheme has not been initialized
     *                 properly.
     * @throws java.security.InvalidKeyException
     *                 if the key is invalid.
     */
    protected final java.security.Key engineDoPhase(java.security.Key key,
	    boolean lastPhase) throws java.security.InvalidKeyException,
	    IllegalStateException {

	if (!(key instanceof PublicKey)) {
	    throw new java.security.InvalidKeyException();
	}

	return doPhase((PublicKey) key, lastPhase);
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Initialize this key agreement with a private key, algorithm parameters,
     * and a source of randomness.
     * 
     * @param privKey
     *                the private key of the party initializing the key
     *                agreement
     * @param params
     *                the algorithm parameters
     * @param random
     *                the source of randomness
     * @throws InvalidKeyException
     *                 if the key is invalid.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are invalid.
     */
    public abstract void init(PrivateKey privKey,
	    AlgorithmParameterSpec params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Generate a shared secret and return it as a byte array.
     * 
     * @return the shared secret as byte array.
     * @throws IllegalStateException
     *                 if the key agreement scheme has not been initialized
     *                 properly.
     */
    public abstract byte[] generateSecret() throws IllegalStateException;

    /**
     * Generate a shared secret and place it into the buffer
     * <tt>sharedSecret</tt>, beginning at <tt>offset</tt>.
     * 
     * @param sharedSecret
     *                the buffer to hold the shared secret
     * @param offset
     *                the offset in <tt>sharedSecret</tt> where the shared
     *                secret will be stored
     * @return the number of bytes written in <tt>sharedSecret</tt>
     * @throws IllegalStateException
     *                 if the key agreement scheme has not been initialized
     *                 properly.
     * @throws ShortBufferException
     *                 if <tt>sharedSecret</tt> is too small to to hold the
     *                 shared secret
     */
    public abstract int generateSecret(byte[] sharedSecret, int offset)
	    throws IllegalStateException, ShortBufferException;

    /**
     * Generate a shared secret via the algorithm specified in
     * <tt>algorithm</tt>.
     * 
     * @param algorithm
     *                the desired algorithm for the generation of the secret
     * @return the shared secret
     * @throws IllegalStateException
     *                 if the key agreement scheme has not been initialized
     *                 properly.
     * @throws NoSuchAlgorithmException
     *                 if <tt>algorithm</tt> is invalid.
     */
    public abstract SecretKey generateSecret(String algorithm)
	    throws IllegalStateException, NoSuchAlgorithmException;

    /**
     * Execute the next phase of this key agreement with the given public key
     * that was received from one of the other parties involved in this key
     * agreement.
     * 
     * @param pubKey
     *                the public key of the other party
     * @param lastPhase
     *                <tt>true</tt> if this is the last phase of the key
     *                agreement. After the last phase only
     *                <tt>generateSecret</tt> may be called.
     * @return the shared secret
     * @throws IllegalStateException
     *                 if the key agreement scheme has not been initialized
     *                 properly.
     * @throws InvalidKeyException
     *                 if the key is invalid.
     */
    public abstract Key doPhase(PublicKey pubKey, boolean lastPhase)
	    throws InvalidKeyException, IllegalStateException;

}

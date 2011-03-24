/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */
package de.flexiprovider.api;

import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.exceptions.SignatureException;
import de.flexiprovider.api.keys.PrivateKey;
import de.flexiprovider.api.keys.PublicKey;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.common.util.JavaSecureRandomWrapper;

/**
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>) for
 * the <tt>Signature</tt> class, which is used to provide the functionality of
 * a digital signature algorithm. Digital signatures are used for authentication
 * and integrity assurance of digital data. .
 * <p>
 * All the abstract methods in this class must be implemented by each
 * cryptographic service provider who wishes to supply the implementation of a
 * particular signature algorithm.
 * 
 * @author Martin Döring, Johannes Müller
 */
public abstract class Signature extends java.security.SignatureSpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * @deprecated
     */
    protected final Object engineGetParameter(String param)
	    throws java.security.InvalidParameterException {
	// method is deprecated
	return null;
    }

    /**
     * @deprecated
     */
    protected final void engineSetParameter(String param, Object value)
	    throws java.security.InvalidParameterException {
	// method is deprecated
    }

    protected final void engineInitSign(java.security.PrivateKey privateKey)
	    throws java.security.InvalidKeyException {
	if ((privateKey == null) || !(privateKey instanceof PrivateKey)) {
	    throw new java.security.InvalidKeyException();
	}
	initSign((PrivateKey) privateKey);
    }

    protected final void engineInitSign(java.security.PrivateKey privateKey,
	    java.security.SecureRandom javaRand)
	    throws java.security.InvalidKeyException {
	if ((privateKey == null) || !(privateKey instanceof PrivateKey)) {
	    throw new java.security.InvalidKeyException();
	}
	SecureRandom flexiRand = new JavaSecureRandomWrapper(javaRand);
	initSign((PrivateKey) privateKey, flexiRand);
    }

    protected final void engineInitVerify(java.security.PublicKey publicKey)
	    throws java.security.InvalidKeyException {
	if ((publicKey == null) || !(publicKey instanceof PublicKey)) {
	    throw new java.security.InvalidKeyException();
	}
	initVerify((PublicKey) publicKey);
    }

    protected void engineSetParameter(
	    java.security.spec.AlgorithmParameterSpec params)
	    throws java.security.InvalidAlgorithmParameterException {
	if (params != null && !(params instanceof AlgorithmParameterSpec)) {
	    throw new java.security.InvalidAlgorithmParameterException();
	}
	setParameters((AlgorithmParameterSpec) params);
    }

    protected final void engineUpdate(byte b)
	    throws java.security.SignatureException {
	update(b);
    }

    protected final void engineUpdate(byte[] b, int off, int len)
	    throws java.security.SignatureException {
	update(b, off, len);
    }

    protected final byte[] engineSign() throws java.security.SignatureException {
	return sign();
    }

    protected final boolean engineVerify(byte[] sigBytes)
	    throws java.security.SignatureException {
	return verify(sigBytes);
    }

    protected final boolean engineVerify(byte[] sigBytes, int offset, int length)
	    throws java.security.SignatureException {
	return verify(sigBytes, offset, length);
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Initialize the signature with the specified private key for signing
     * operations.
     * 
     * @param privKey
     *                the private key of the identity whose signature will be
     *                generated.
     * @throws InvalidKeyException
     *                 if the key is invalid for initializing the signature.
     */
    public final void initSign(PrivateKey privKey) throws InvalidKeyException {
	initSign(privKey, Registry.getSecureRandom());
    }

    /**
     * Initialize the signature with the specified private key and source of
     * randomness for signing operations.
     * 
     * @param privKey
     *                the private key of the identity whose signature will be
     *                generated.
     * @param random
     *                the source of randomness
     * @throws InvalidKeyException
     *                 if the key is invalid for initializing the signature.
     */
    public abstract void initSign(PrivateKey privKey, SecureRandom random)
	    throws InvalidKeyException;

    /**
     * Initialize the signature with the specified public key for verification
     * operations.
     * 
     * @param pubKey
     *                the public key of the identity whose signature is going to
     *                be verified
     * @throws InvalidKeyException
     *                 if the key is invalid for initializing the signature.
     */
    public abstract void initVerify(PublicKey pubKey)
	    throws InvalidKeyException;

    /**
     * Initialize the signature with the specified parameters.
     * 
     * @param params
     *                the parameters
     * @throws InvalidAlgorithmParameterException
     *                 if the given parameters are inappropriate for this
     *                 signature.
     */
    public abstract void setParameters(AlgorithmParameterSpec params)
	    throws InvalidAlgorithmParameterException;

    /**
     * Update the data to be signed or verified using the specified byte.
     * 
     * @param input
     *                the data byte
     * @throws SignatureException
     *                 if the engine is not initialized properly.
     */
    public abstract void update(byte input) throws SignatureException;

    /**
     * Update the data to be signed or verified using the specified byte array.
     * 
     * @param input
     *                the data byte array
     * @throws SignatureException
     *                 if the engine is not initialized properly.
     */
    public final void update(byte[] input) throws SignatureException {
	update(input, 0, input.length);
    }

    /**
     * Update the data to be signed or verified, using the specified byte array
     * of the specified length, starting at the specified offset.
     * 
     * @param input
     *                the data byte array
     * @param inOff
     *                the offset to start from in the array of bytes
     * @param inLen
     *                the number of bytes to use, starting at <tt>inOff</tt>
     * @throws SignatureException
     *                 if the engine is not initialized properly
     */
    public abstract void update(byte[] input, int inOff, int inLen)
	    throws SignatureException;

    /**
     * Return the signature of all the data updated so far.
     * 
     * @return the signature
     * @throws SignatureException
     *                 if the engine is not initialized properly.
     */
    public abstract byte[] sign() throws SignatureException;

    /**
     * Update the data to be signed and return the signature of all the data
     * updated so far.
     * 
     * @param input
     *                the data byte array
     * @return the signature
     * @throws SignatureException
     *                 if the engine is not initialized properly.
     */
    public final byte[] sign(byte[] input) throws SignatureException {
	update(input);
	return sign();
    }

    /**
     * Verify the passed-in signature of the specified message.
     * 
     * @param signature
     *                the signature
     * @return <tt>true</tt> if the signature is valid, <tt>false</tt>
     *         otherwise.
     * @throws SignatureException
     *                 if the engine is not initialized properly or the
     *                 passed-in signature is improperly encoded or of the wrong
     *                 type.
     */
    public abstract boolean verify(byte[] signature) throws SignatureException;

    /**
     * Update the data to be verified and verify the passed-in signature.
     * 
     * @param input
     *                the data byte array
     * @param signature
     *                the signature
     * @return <tt>true</tt> if the signature is valid, <tt>false</tt>
     *         otherwise.
     * @throws SignatureException
     *                 if the engine is not initialized properly or the
     *                 passed-in signature is improperly encoded or of the wrong
     *                 type.
     */
    public final boolean verify(byte[] input, byte[] signature)
	    throws SignatureException {
	update(input);
	return verify(signature);
    }

    /**
     * Verify the passed-in signature.
     * 
     * @param signature
     *                the signature
     * @param sigOff
     *                the offset where the signature starts
     * @param sigLen
     *                the length of the signature
     * @return <tt>true</tt> if the signature is valid, <tt>false</tt>
     *         otherwise.
     * @throws SignatureException
     *                 if the engine is not initialized properly or the
     *                 passed-in signature is improperly encoded or of the wrong
     *                 type.
     */
    public final boolean verify(byte[] signature, int sigOff, int sigLen)
	    throws SignatureException {
	byte[] sig = new byte[sigLen];
	System.arraycopy(signature, sigOff, sig, 0, sigLen);
	return verify(sig);
    }

    /**
     * Update the data to be verified and verify the passed-in signature.
     * 
     * @param input
     *                the data byte array
     * @param signature
     *                the signature
     * @param sigOff
     *                the offset where the signature starts
     * @param sigLen
     *                the length of the signature
     * @return <tt>true</tt> if the signature is valid, <tt>false</tt>
     *         otherwise.
     * @throws SignatureException
     *                 if the engine is not initialized properly or the
     *                 passed-in signature is improperly encoded or of the wrong
     *                 type.
     */
    public final boolean verify(byte[] input, byte[] signature, int sigOff,
	    int sigLen) throws SignatureException {
	update(input);
	return verify(signature, sigOff, sigLen);
    }

}

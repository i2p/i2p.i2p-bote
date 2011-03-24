package de.flexiprovider.api;

import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.exceptions.ShortBufferException;
import de.flexiprovider.api.keys.SecretKey;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;

/**
 * This class implements a "message authentication code" (MAC), a method to
 * ensure the integrity of data transmitted between two parties who share a
 * common secret key.
 * 
 * <p>
 * The best way to describe a MAC is as a <i>keyed one-way hash function</i>,
 * which looks like:
 * 
 * <blockquote>
 * <p>
 * <tt>D = MAC(K, M)</tt></blockquote>
 * 
 * <p>
 * where <tt>K</tt> is the key, <tt>M</tt> is the message, and <tt>D</tt>
 * is the resulting digest. One party will usually send the concatenation
 * <tt>M || D</tt> to the other party, who will then verify <tt>D</tt> by
 * computing <tt>D'</tt> in a similar fashion. If <tt>D == D'</tt>, then
 * the message is assumed to be authentic.
 * 
 * @author Martin Döring, Johannes Müller
 */
public abstract class Mac extends javax.crypto.MacSpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    protected final int engineGetMacLength() {
	return getMacLength();
    }

    protected void engineInit(java.security.Key key,
	    java.security.spec.AlgorithmParameterSpec params)
	    throws java.security.InvalidKeyException,
	    java.security.InvalidAlgorithmParameterException {

	if (!(key instanceof SecretKey)) {
	    throw new java.security.InvalidKeyException();
	}
	if ((params != null) && !(params instanceof AlgorithmParameterSpec)) {
	    throw new java.security.InvalidAlgorithmParameterException();
	}
	init((SecretKey) key, (AlgorithmParameterSpec) params);
    }

    protected final void engineUpdate(byte input) {
	update(input);
    }

    protected final void engineUpdate(byte[] input, int offset, int len) {
	update(input, offset, len);
    }

    protected final byte[] engineDoFinal() {
	return doFinal();
    }

    protected final void engineReset() {
	reset();
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Return the MAC length. This method is guaranteed to return a sane value
     * only after the MAC has been initialized.
     * 
     * @return the MAC length
     */
    public abstract int getMacLength();

    /**
     * Initialize this MAC with a key and no parameters.
     * 
     * @param key
     *                The key to initialize this instance with.
     * @throws InvalidKeyException
     *                 If the key is unacceptable.
     */
    public final void init(SecretKey key) throws InvalidKeyException {
	try {
	    init(key, null);
	} catch (InvalidAlgorithmParameterException iape) {
	    throw new IllegalArgumentException("This MAC needs parameters");
	}
    }

    /**
     * Initialize this MAC with a key and parameters.
     * 
     * @param key
     *                The key to initialize this instance with.
     * @param params
     *                The algorithm-specific parameters.
     * @throws InvalidAlgorithmParameterException
     *                 If the algorithm parameters are unacceptable.
     * @throws InvalidKeyException
     *                 If the key is unacceptable.
     */
    public abstract void init(SecretKey key, AlgorithmParameterSpec params)
	    throws InvalidAlgorithmParameterException, InvalidKeyException;

    /**
     * Update the computation with a single byte.
     * 
     * @param input
     *                The next byte.
     */
    public abstract void update(byte input);

    /**
     * Update the computation with a byte array.
     * 
     * @param input
     *                The next bytes.
     */
    public final void update(byte[] input) {
	update(input, 0, input.length);
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Update the computation with a portion of a byte array.
     * 
     * @param input
     *                The next bytes.
     * @param offset
     *                The index in <tt>input</tt> to start.
     * @param length
     *                The number of bytes to update.
     */
    public abstract void update(byte[] input, int offset, int length);

    /**
     * Finishes the computation of a MAC and returns the digest.
     * 
     * <p>
     * After this method succeeds, it may be used again as just after a call to
     * <tt>init</tt>, and can compute another MAC using the same key and
     * parameters.
     * 
     * @return The message authentication code.
     */
    public abstract byte[] doFinal();

    /**
     * Finishes the computation of a MAC with a final byte array (or computes a
     * MAC over those bytes only) and returns the digest.
     * 
     * <p>
     * After this method succeeds, it may be used again as just after a call to
     * <tt>init</tt>, and can compute another MAC using the same key and
     * parameters.
     * 
     * @param input
     *                The bytes to add.
     * @return The message authentication code.
     */
    public final byte[] doFinal(byte[] input) {
	update(input);
	return doFinal();
    }

    /**
     * Finishes the computation of a MAC and places the result into the given
     * array.
     * <p>
     * After this method succeeds, it may be used again as just after a call to
     * <tt>init</tt>, and can compute another MAC using the same key and
     * parameters.
     * 
     * @param output
     *                The destination for the result.
     * @param outOffset
     *                The index in the output array to start.
     * @return the number of bytes stored in output.
     * @throws ShortBufferException
     *                 If <tt>output</tt> is not large enough to hold the
     *                 result.
     */
    public final int doFinal(byte[] output, int outOffset)
	    throws ShortBufferException {
	if (output.length - outOffset < getMacLength()) {
	    throw new ShortBufferException();
	}
	byte[] mac = doFinal();
	System.arraycopy(mac, 0, output, outOffset, mac.length);
	return mac.length;
    }

    /**
     * Reset this instance. A call to this method returns this instance back to
     * the state it was in just after it was initialized.
     */
    public abstract void reset();

}

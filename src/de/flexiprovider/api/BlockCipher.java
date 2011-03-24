/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */
package de.flexiprovider.api;

import java.lang.reflect.Method;

import de.flexiprovider.api.exceptions.BadPaddingException;
import de.flexiprovider.api.exceptions.IllegalBlockSizeException;
import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.exceptions.InvalidParameterException;
import de.flexiprovider.api.exceptions.NoSuchModeException;
import de.flexiprovider.api.exceptions.NoSuchPaddingException;
import de.flexiprovider.api.exceptions.ShortBufferException;
import de.flexiprovider.api.keys.Key;
import de.flexiprovider.api.keys.SecretKey;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.common.mode.ModeParameterSpec;
import de.flexiprovider.common.util.JavaSecureRandomWrapper;

/**
 * The BlockCipher class extends CipherSpi and handles the creation of
 * PaddingScheme, BlockCipher and Mode. An instance of this class will be
 * created by the Cipher.getInstance(String)-mechanism of the Cipher class.
 * <p>
 * BlockCipher ensures that a Mode and a PaddingsScheme are created, even if the
 * user has set no preferences. For this to work, BlockCipher requires version
 * 1.3 of the mode class that can handle a getInstance()-call without arguments
 * to return a default Mode or Padding.
 * <p>
 * BlockCipher will acknowledge only <b>one</b> call of engineSetMode() and
 * engineSetPadding() to ensure that these settings aren't changed while
 * encrypting.
 * 
 * @author Christoph Sesterhenn, Christoph Ender
 * @author Ralf-Philipp Weinmann
 * @author Martin Döring, Johannes Müller
 */
public abstract class BlockCipher extends Cipher {

    /**
     * The reference to the mode class.
     */
    private Mode mode;

    /**
     * The reference to the padding scheme.
     */
    private PaddingScheme paddingScheme;

    /**
     * AlgorithmParameterSpec
     */
    private AlgorithmParameterSpec paramSpec;

    /**
     * This buffer holds the outsize left by an update operation
     */
    private byte[] buffer = null;

    /**
     * the block size of the mode
     */
    private int modeBlockSize;

    /**
     * Used to check if an initialization method has been called.
     */
    private boolean initialized = false;

    /**
     * the source of randomness, if necessary
     */
    protected SecureRandom random;

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * Initialize this cipher object with proper key and algorithm parameters,
     * and some random seed. Before a cipher object is ready for data
     * processing, it has to be initialized according to the desired
     * cryptographic operation, which is specified by the opmode parameter
     * (either ENCRYPT_MODE or DECCRYPT_MODE). e.g.
     * cipher_obj.init(Cipher.ENCRYPT_MODE, key, alg_params, random_seed); The
     * Cipher init will call the proper CipherSpi engineInit method. Note: If
     * the Mode needs an initialization vector, a try to retrieve it from the
     * AlgorithmParametersSpec is made.
     * 
     * @param opmode
     *                the operation mode for which this cipher is used
     *                (ENCRYPT_MODE or DECRYPT_MODE)
     * @param key
     *                the key
     * @param paramSpec
     *                the algorithm parameters
     * @param javaRand
     *                the random seed
     * @throws java.security.InvalidKeyException
     *                 if the key is inappropriate for initializing this block
     *                 cipher.
     * @throws java.security.InvalidAlgorithmParameterException
     *                 if the parameters are inappropriate for initializing this
     *                 block cipher.
     */
    protected final void engineInit(int opmode, java.security.Key key,
	    java.security.spec.AlgorithmParameterSpec paramSpec,
	    java.security.SecureRandom javaRand)
	    throws java.security.InvalidKeyException,
	    java.security.InvalidAlgorithmParameterException {

	random = new JavaSecureRandomWrapper(javaRand);
	initModeAndPadding();
	opMode = opmode;

	buffer = new byte[0];
	ModeParameterSpec modeParams;
	AlgorithmParameterSpec cipherParams;

	if (paramSpec == null) {
	    modeParams = null;
	    cipherParams = null;
	} else if (paramSpec instanceof javax.crypto.spec.IvParameterSpec) {
	    modeParams = new ModeParameterSpec(
		    (javax.crypto.spec.IvParameterSpec) paramSpec);
	    cipherParams = null;

	} else if (paramSpec instanceof ModeParameterSpec) {
	    modeParams = (ModeParameterSpec) paramSpec;
	    cipherParams = null;

	} else {
	    if (!(paramSpec instanceof AlgorithmParameterSpec)) {
		throw new java.security.InvalidAlgorithmParameterException(
			"unsupported type");
	    }
	    cipherParams = (AlgorithmParameterSpec) paramSpec;

	    byte[] iv;
	    Method getIV;
	    try {
		getIV = cipherParams.getClass().getMethod("getIV", null);
		iv = (byte[]) getIV.invoke(cipherParams, null);
	    } catch (Exception ex) {
		// if no getIV() method is found, iv remains null
		iv = null;
	    }

	    if (iv == null) {
		modeParams = null;
	    } else {
		modeParams = new ModeParameterSpec(iv);
	    }
	}

	if (!(key instanceof SecretKey)) {
	    throw new java.security.InvalidKeyException("unsupported type");
	}

	if (opmode == ENCRYPT_MODE) {
	    mode.initEncrypt((SecretKey) key, modeParams, cipherParams);
	} else if (opmode == DECRYPT_MODE) {
	    mode.initDecrypt((SecretKey) key, modeParams, cipherParams);
	}
	modeBlockSize = mode.blockSize;
	paddingScheme.setBlockSize(modeBlockSize);

	initialized = true;
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Set the mode of this cipher. The mode can only be set once.
     * 
     * @param modeName
     *                the cipher mode
     * @throws NoSuchModeException
     *                 if neither the mode with the given name nor the default
     *                 mode can be found
     */
    public final void setMode(String modeName) throws NoSuchModeException {
	if (mode != null) {
	    return;
	}

	if ((modeName == null) || (modeName == "")) {
	    mode = Registry.getMode();
	} else {
	    mode = Registry.getMode(modeName);
	}
	mode.setBlockCipher(this);
    }

    /**
     * Set the padding scheme of this cipher. The padding scheme can only be set
     * once.
     * 
     * @param paddingName
     *                the padding scheme
     * @throws NoSuchPaddingException
     *                 if the requested padding scheme cannot be found.
     */
    public final void setPadding(String paddingName)
	    throws NoSuchPaddingException {
	if (paddingScheme != null) {
	    return;
	}

	if (paddingName == null || paddingName.equals("")) {
	    paddingScheme = Registry.getPaddingScheme();
	} else {
	    paddingScheme = Registry.getPaddingScheme(paddingName);
	}
    }

    /**
     * Return the initialization vector. This is useful in the context of
     * password-based encryption or decryption, where the IV is derived from a
     * user-provided passphrase.
     * 
     * @return the initialization vector in a new buffer, or <tt>null</tt> if
     *         the underlying algorithm does not use an IV, or if the IV has not
     *         yet been set.
     */
    public final byte[] getIV() {
	return initialized ? mode.iv : null;
    }

    /**
     * Return the blocksize the algorithm uses. This method will usually be
     * called by the mode.
     * 
     * @return the blocksize of the cipher
     */
    protected abstract int getCipherBlockSize();

    /**
     * @return the block size of the used mode, or -1 if the cipher has not been
     *         initialized.
     */
    public final int getBlockSize() {
	return initialized ? modeBlockSize : -1;
    }

    /**
     * Return the length in bytes that an output buffer would need to be in
     * order to hold the result of the next update or doFinal operation, given
     * the input length inputLen (in bytes).
     * <p>
     * This call takes into account any unprocessed (buffered) data from a
     * previous update call, and padding.
     * <p>
     * The actual output length of the next update or doFinal call may be
     * smaller than the length returned by this method.
     * 
     * @param inLen
     *                the input length (in bytes)
     * @return the required output buffer size (in bytes)
     */
    public final int getOutputSize(int inLen) {
	if (!initialized) {
	    return -1;
	}
	final int newInLen = inLen + (buffer == null ? 0 : buffer.length);
	return newInLen + paddingScheme.padLength(newInLen);
    }

    /**
     * Return the parameters used with this cipher.
     * <p>
     * The returned parameters may be the same that were used to initialize this
     * cipher, or may contain the default set of parameters or a set of randomly
     * generated parameters used by the underlying cipher implementation
     * (provided that the underlying cipher implementation uses a default set of
     * parameters or creates new parameters if it needs parameters but was not
     * initialized with any).
     * 
     * @return the parameters used with this cipher, or null if this cipher does
     *         not use any parameters.
     */
    public final AlgorithmParameterSpec getParameters() {
	return initialized ? paramSpec : null;
    }

    /**
     * Initialize this cipher with a key and a source of randomness for
     * encryption.
     * <p>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using engineGetParameters or
     * engineGetIV (if the parameter is an IV).
     * <p>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it
     * 
     * @param key
     *                the encryption key
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for initializing this
     *                 cipher
     * @throws InvalidParameterException
     *                 if this block cipher requires parameters for
     *                 initialization and cannot generate parameters itself.
     */
    public final void initEncrypt(Key key) throws InvalidKeyException,
	    InvalidParameterException {
	initEncrypt(key, Registry.getSecureRandom());
    }

    /**
     * Initialize this cipher with a key and a source of randomness for
     * encryption.
     * <p>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using engineGetParameters or
     * engineGetIV (if the parameter is an IV).
     * <p>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random and they will be stored in the class variable rndBytes.
     * <p>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it
     * 
     * @param key
     *                the encryption key
     * @param random
     *                the source of randomness
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for initializing this
     *                 cipher
     * @throws InvalidParameterException
     *                 if this block cipher requires parameters for
     *                 initialization and cannot generate parameters itself.
     */
    public final void initEncrypt(Key key, SecureRandom random)
	    throws InvalidKeyException, InvalidParameterException {

	try {
	    initEncrypt(key, (ModeParameterSpec) null,
		    (AlgorithmParameterSpec) null, random);
	} catch (InvalidAlgorithmParameterException e) {
	    throw new InvalidParameterException(
		    "This cipher needs algorithm parameters for initialization (cannot be null).");
	}
    }

    /**
     * Initialize this cipher with a key, a set of algorithm parameters, and a
     * source of randomness for encryption.
     * <p>
     * If this cipher requires any algorithm parameters and paramSpec is null,
     * the underlying cipher implementation is supposed to generate the required
     * parameters itself (using provider-specific default or random values) if
     * it is being initialized for encryption, and raise an
     * InvalidAlgorithmParameterException if it is being initialized for
     * decryption. The generated parameters can be retrieved using
     * engineGetParameters or engineGetIV (if the parameter is an IV).
     * <p>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it.
     * 
     * @param key
     *                the encryption key
     * @param cipherParams
     *                the cipher parameters
     * @param random
     *                the source of randomness
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for initializing this
     *                 block cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are inappropriate for initializing this
     *                 block cipher.
     */
    public final void initEncrypt(Key key, AlgorithmParameterSpec cipherParams,
	    SecureRandom random) throws InvalidKeyException,
	    InvalidAlgorithmParameterException {
	initEncrypt(key, null, cipherParams, random);
    }

    /**
     * Initialize this cipher with a key, a set of algorithm parameters, and a
     * source of randomness for encryption.
     * <p>
     * If this cipher requires any algorithm parameters and paramSpec is null,
     * the underlying cipher implementation is supposed to generate the required
     * parameters itself (using provider-specific default or random values) if
     * it is being initialized for encryption, and raise an
     * InvalidAlgorithmParameterException if it is being initialized for
     * decryption. The generated parameters can be retrieved using
     * engineGetParameters or engineGetIV (if the parameter is an IV).
     * <p>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it.
     * 
     * @param key
     *                the encryption key
     * @param modeParams
     *                the mode parameters
     * @param cipherParams
     *                the cipher parameters
     * @param random
     *                the source of randomness
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for initializing this
     *                 block cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are inappropriate for initializing this
     *                 block cipher.
     */
    public final void initEncrypt(Key key, ModeParameterSpec modeParams,
	    AlgorithmParameterSpec cipherParams, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {

	this.random = random != null ? random : Registry.getSecureRandom();
	initModeAndPadding();
	opMode = ENCRYPT_MODE;

	buffer = new byte[0];
	paramSpec = cipherParams;

	if (!(key instanceof SecretKey)) {
	    throw new InvalidKeyException("unsupported type");
	}

	mode.initEncrypt((SecretKey) key, modeParams, cipherParams);
	modeBlockSize = mode.blockSize;
	paddingScheme.setBlockSize(modeBlockSize);

	initialized = true;
    }

    /**
     * Initialize this cipher with a key and a source of randomness for
     * decryption.
     * <p>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using engineGetParameters or
     * engineGetIV (if the parameter is an IV).
     * <p>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it
     * 
     * @param key
     *                the encryption key
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for initializing this
     *                 cipher
     * @throws InvalidParameterException
     *                 if the parameters are
     */
    public final void initDecrypt(Key key) throws InvalidKeyException,
	    InvalidParameterException {
	try {
	    initDecrypt(key, (ModeParameterSpec) null,
		    (AlgorithmParameterSpec) null);
	} catch (InvalidAlgorithmParameterException e) {
	    throw new InvalidParameterException(
		    "This cipher needs algorithm parameters for initialization (cannot be null).");
	}
    }

    /**
     * Initialize this cipher with a key, a set of algorithm parameters, and a
     * source of randomness for decryption.
     * <p>
     * If this cipher requires any algorithm parameters and paramSpec is null,
     * the underlying cipher implementation is supposed to generate the required
     * parameters itself (using provider-specific default or random values) if
     * it is being initialized for encryption, and throw an
     * {@link InvalidAlgorithmParameterException} if it is being initialized for
     * decryption. The generated parameters can be retrieved using
     * engineGetParameters or engineGetIV (if the parameter is an IV).
     * <p>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it.
     * 
     * @param key
     *                the encryption key
     * @param cipherParams
     *                the cipher parameters
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for initializing this
     *                 block cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are inappropriate for initializing this
     *                 block cipher.
     */
    public final void initDecrypt(Key key, AlgorithmParameterSpec cipherParams)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
	initDecrypt(key, null, cipherParams);
    }

    /**
     * Initialize this cipher with a key, a set of algorithm parameters, and a
     * source of randomness for decryption.
     * <p>
     * If this cipher requires any algorithm parameters and paramSpec is null,
     * the underlying cipher implementation is supposed to generate the required
     * parameters itself (using provider-specific default or random values) if
     * it is being initialized for encryption, and throw an
     * {@link InvalidAlgorithmParameterException} if it is being initialized for
     * decryption. The generated parameters can be retrieved using
     * engineGetParameters or engineGetIV (if the parameter is an IV).
     * <p>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it.
     * 
     * @param key
     *                the encryption key
     * @param modeParams
     *                the mode parameters
     * @param cipherParams
     *                the cipher parameters
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for initializing this
     *                 block cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are inappropriate for initializing this
     *                 block cipher.
     */
    public final void initDecrypt(Key key, ModeParameterSpec modeParams,
	    AlgorithmParameterSpec cipherParams) throws InvalidKeyException,
	    InvalidAlgorithmParameterException {

	initModeAndPadding();
	opMode = DECRYPT_MODE;

	buffer = new byte[0];
	paramSpec = cipherParams;

	if (!(key instanceof SecretKey)) {
	    throw new InvalidKeyException("unsupported type");
	}

	mode.initDecrypt((SecretKey) key, modeParams, cipherParams);
	modeBlockSize = mode.blockSize;
	paddingScheme.setBlockSize(modeBlockSize);

	initialized = true;
    }

    /**
     * Continue a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized), processing another data part.
     * 
     * @param input
     *                the input buffer
     * @param inOff
     *                the offset where the input starts
     * @param inLen
     *                the input length
     * @return a new buffer with the result (maybe an empty byte array)
     */
    public final byte[] update(byte[] input, int inOff, int inLen) {

	// if the cipher is not initialized or input is empty, return an empty
	// byte array
	if (!initialized || input == null || inLen <= 0) {
	    return new byte[0];
	}

	// concatenate buffer and input (only if buffer is not empty)
	int bufLen = buffer.length;
	int newInLen, newInOff;
	byte[] newInput;
	if (bufLen == 0) {
	    newInOff = inOff;
	    newInLen = inLen;
	    newInput = input;
	} else {
	    newInLen = bufLen + inLen;
	    newInOff = 0;
	    newInput = new byte[newInLen];
	    System.arraycopy(buffer, 0, newInput, 0, bufLen);
	    System.arraycopy(input, inOff, newInput, bufLen, inLen);
	}

	// compute number of blocks to process and remaining bytes
	int numBlocks = newInLen / modeBlockSize;
	int numBytes = numBlocks * modeBlockSize;
	int remaining = newInLen - numBytes;
	if (opMode == DECRYPT_MODE && remaining == 0) {
	    remaining = modeBlockSize;
	    numBlocks--;
	    numBytes -= modeBlockSize;
	}

	byte[] output = new byte[numBytes];
	int outOff = 0;

	// process whole blocks
	for (int block = 0; block < numBlocks; block++) {
	    if (opMode == ENCRYPT_MODE) {
		mode.nextChunkEncrypt(newInput, newInOff, output, outOff);
	    } else if (opMode == DECRYPT_MODE) {
		mode.nextChunkDecrypt(newInput, newInOff, output, outOff);
	    }
	    newInOff += modeBlockSize;
	    outOff += modeBlockSize;
	}

	// copy unprocessed bytes to buffer
	buffer = new byte[remaining];
	System.arraycopy(newInput, newInOff, buffer, 0, remaining);

	return output;
    }

    /**
     * Continue a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized), processing another data part.
     * 
     * @param input
     *                the input buffer
     * @param inOff
     *                the offset where the input starts
     * @param inLen
     *                the input length
     * @param output
     *                the output buffer
     * @param outOff
     *                the offset where the result is stored
     * @return the length of the output
     * @throws ShortBufferException
     *                 if the output buffer is too small to hold the result.
     */
    public final int update(byte[] input, int inOff, int inLen, byte[] output,
	    int outOff) throws ShortBufferException {

	// if the cipher is not initialized or input is empty return 0
	if (!initialized || input == null || inLen <= 0) {
	    return 0;
	}

	// compute number of bytes to process
	int newInLen = buffer.length + inLen;
	int remaining = newInLen % modeBlockSize;
	if (opMode == DECRYPT_MODE && remaining == 0) {
	    remaining = modeBlockSize;
	}
	int numBytes = newInLen - remaining;

	if (output.length - outOff < numBytes) {
	    throw new ShortBufferException("output");
	}

	byte[] update = update(input, inOff, inLen);
	System.arraycopy(update, 0, output, outOff, update.length);

	return update.length;
    }

    /**
     * Finish a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized).
     * 
     * @param input
     *                the input buffer
     * @param inOff
     *                the offset where the input starts
     * @param inLen
     *                the input length
     * @return a new buffer with the result
     * @throws IllegalBlockSizeException
     *                 if the total input length is not a multiple of the block
     *                 size (for encryption when no padding is used or for
     *                 decryption).
     * @throws BadPaddingException
     *                 if unpadding fails.
     */
    public final byte[] doFinal(byte[] input, int inOff, int inLen)
	    throws IllegalBlockSizeException, BadPaddingException {

	byte[] output = new byte[0];

	if (input == null && buffer == null) {
	    return output;
	}

	byte[] update = update(input, inOff, inLen);
	int updLen = update.length;
	int bufLen = buffer.length;

	if (opMode == ENCRYPT_MODE) {
	    int padLen = paddingScheme.padLength(bufLen);
	    if (padLen == 0) {
		return update;
	    }
	    output = new byte[updLen + bufLen + padLen];
	    System.arraycopy(update, 0, output, 0, updLen);
	    System.arraycopy(buffer, 0, output, updLen, bufLen);
	    paddingScheme.pad(output, updLen, bufLen);
	    mode.nextChunkEncrypt(output, updLen, output, updLen);
	} else if (opMode == DECRYPT_MODE) {
	    if (bufLen != modeBlockSize) {
		throw new IllegalBlockSizeException(
			"ciphertext length is not a multiple of block size");
	    }
	    mode.nextChunkDecrypt(buffer, 0, buffer, 0);
	    int padOffset = paddingScheme.unpad(buffer, 0, modeBlockSize);
	    output = new byte[updLen + padOffset];
	    System.arraycopy(update, 0, output, 0, updLen);
	    System.arraycopy(buffer, 0, output, updLen, padOffset);
	}

	buffer = null;
	mode.reset();

	return output;
    }

    /**
     * Finish a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized).
     * 
     * @param input
     *                the input buffer
     * @param inOff
     *                the offset where the input starts
     * @param inLen
     *                the input length
     * @param output
     *                the buffer for the result
     * @param outOff
     *                the offset where the result is stored
     * @return the output length
     * @throws ShortBufferException
     *                 if the output buffer is too small to hold the result.
     * @throws IllegalBlockSizeException
     *                 if the total input length is not a multiple of the block
     *                 size (for encryption when no padding is used or for
     *                 decryption).
     * @throws BadPaddingException
     *                 if unpadding fails.
     */
    public final int doFinal(byte[] input, int inOff, int inLen, byte[] output,
	    int outOff) throws ShortBufferException, IllegalBlockSizeException,
	    BadPaddingException {

	byte[] doFinal = doFinal(input, inOff, inLen);
	int outLen = doFinal.length;
	if (outLen == 0) {
	    return 0;
	}
	if (output.length - outOff < outLen) {
	    throw new ShortBufferException("output");
	}
	System.arraycopy(doFinal, 0, output, outOff, outLen);
	return outLen;
    }

    /**
     * Initialize the block cipher with a secret key and parameters for data
     * encryption.
     * 
     * @param key
     *                the secret key
     * @param params
     *                the parameters
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for this cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the given parameters are inappropriate for this
     *                 cipher.
     */
    protected abstract void initCipherEncrypt(SecretKey key,
	    AlgorithmParameterSpec params) throws InvalidKeyException,
	    InvalidAlgorithmParameterException;

    /**
     * Initialize the block cipher with a secret key and parameters for data
     * decryption.
     * 
     * @param key
     *                the secret key
     * @param params
     *                the parameters
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for this cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the given parameters are inappropriate for this
     *                 cipher.
     */
    protected abstract void initCipherDecrypt(SecretKey key,
	    AlgorithmParameterSpec params) throws InvalidKeyException,
	    InvalidAlgorithmParameterException;

    /**
     * Encrypt a single block of data. It has to be assured that the array
     * <tt>input</tt> contains a whole block starting at <tt>inOff</tt> and
     * that <tt>out</tt> is large enough to hold an encrypted block starting
     * at <tt>outOff</tt>.
     * 
     * @param input
     *                array of bytes which contains the plaintext to be
     *                encrypted
     * @param inOff
     *                index in array in, where the plaintext block starts
     * @param output
     *                array of bytes which will contain the ciphertext startig
     *                at outOffset
     * @param outOff
     *                index in array out, where the ciphertext block will start
     */
    protected abstract void singleBlockEncrypt(byte[] input, int inOff,
	    byte[] output, int outOff);

    /**
     * Decrypt a single block of data. It has to be assured that the array
     * <tt>input</tt> contains a whole block starting at <tt>inOff</tt> and
     * that <tt>output</tt> is large enough to hold an decrypted block
     * starting at <tt>outOff</tt>.
     * 
     * @param input
     *                array of bytes which contains the ciphertext to be
     *                decrypted
     * @param inOff
     *                index in array in, where the ciphertext block starts
     * @param output
     *                array of bytes which will contain the plaintext starting
     *                at outOffset
     * @param outOff
     *                index in array out, where the plaintext block will start
     */
    protected abstract void singleBlockDecrypt(byte[] input, int inOff,
	    byte[] output, int outOff);

    /**
     * Check if mode and padding are set. If not, instantiate the default ones.
     */
    private void initModeAndPadding() {
	if (mode == null) {
	    mode = Registry.getMode();
	    mode.setBlockCipher(this);
	}

	if (paddingScheme == null) {
	    paddingScheme = Registry.getPaddingScheme();
	}
    }

}

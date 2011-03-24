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
import de.flexiprovider.api.keys.SecretKey;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.common.mode.ModeParameterSpec;

/**
 * The abstract class Mode is the base for all modes implemented in the package
 * cdc. It defines the set of methods which have to be implemented to cooperate
 * with the BasicCipher class. Each class that implements such a mode has to
 * exist in the package "de.flexiprovider.common.mode" and the classname must be
 * the same as the name used in the call of <tt>Cipher.getInstance()</tt>.
 * e.g. if you call <tt>Cipher.getInstance("SAFER+/CBC/NoPadding")</tt>, the
 * code will look for the class de.flexiprovider.common.mode.CBC
 * 
 * @author Marcus Lippert
 * @author Ralf-Philipp Weinmann
 */
public abstract class Mode {

    /**
     * Reference to the underlying block cipher
     */
    private BlockCipher blockCipher;

    /**
     * The initialization vector
     */
    protected byte[] iv;

    /**
     * The block size of the mode
     */
    protected int blockSize;

    /**
     * Set the {@link BlockCipher} to use with this mode.
     * 
     * @param blockCipher
     *                the block cipher
     */
    final void setBlockCipher(BlockCipher blockCipher) {
	this.blockCipher = blockCipher;
    }

    /*---------------------------------------------------
     * Mode specific abstract methods
     ---------------------------------------------------*/

    /**
     * Initialize the Mode object for encryption and compute the mode block
     * size. This value usually depends on the block size of the used block
     * cipher. It is supposed that all block ciphers return a sane value via
     * {@link BlockCipher#getCipherBlockSize()} after initialization.
     * 
     * @param key
     *                the key used for encryption
     * @param modeParams
     *                additional mode parameters
     * @param cipherParams
     *                additional algorithm parameters
     * @throws InvalidKeyException
     *                 if the key is inappropriate for initializing the
     *                 underlying block cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are inappropriate for initializing the
     *                 underlying block cipher.
     */
    protected abstract void initEncrypt(SecretKey key,
	    ModeParameterSpec modeParams, AlgorithmParameterSpec cipherParams)
	    throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Initialize the Mode object for decryption and compute the mode block
     * size. This value usually depends on the block size of the used block
     * cipher. It is supposed that all block ciphers return a sane value via
     * {@link BlockCipher#getCipherBlockSize()} after initialization.
     * 
     * @param key
     *                the key used for decryption
     * @param modeParams
     *                additional mode parameters
     * @param cipherParams
     *                additional algorithm parameters
     * @throws InvalidKeyException
     *                 if the key is inappropriate for initializing the
     *                 underlying block cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are inappropriate for initializing the
     *                 underlying block cipher.
     */
    protected abstract void initDecrypt(SecretKey key,
	    ModeParameterSpec modeParams, AlgorithmParameterSpec cipherParams)
	    throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Encrypt the next data block. Any special features of the Mode should be
     * implemented here.
     * 
     * @param input
     *                input data buffer
     * @param inOff
     *                input data offset
     * @param output
     *                output data buffer
     * @param outOff
     *                output data offset
     */
    protected abstract void nextChunkEncrypt(final byte[] input,
	    final int inOff, byte[] output, final int outOff);

    /**
     * Decrypt the next data block. Any special features of the Mode should be
     * implemented here.
     * 
     * @param input
     *                input data buffer
     * @param inOff
     *                input data offset
     * @param output
     *                output data buffer
     * @param outOff
     *                output data offset
     */
    protected abstract void nextChunkDecrypt(final byte[] input,
	    final int inOff, byte[] output, final int outOff);

    /**
     * reset() is called after doFinal() in order to prepare the mode for the
     * next operation.
     */
    protected abstract void reset();

    /*---------------------------------------------------
     * Adapter classes to BlockCipher
     ---------------------------------------------------*/

    /**
     * Initialize the block cipher for encryption.
     * 
     * @param key
     *                the secret key to use for encryption
     * @param cipherParams
     *                the parameters
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for this cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the given parameters are inappropriate for this
     *                 cipher.
     */
    protected final void initCipherEncrypt(SecretKey key,
	    AlgorithmParameterSpec cipherParams) throws InvalidKeyException,
	    InvalidAlgorithmParameterException {
	blockCipher.initCipherEncrypt(key, cipherParams);
    }

    /**
     * Initialize the block cipher for decryption.
     * 
     * @param key
     *                the secret key to use for decryption
     * @param cipherParams
     *                the parameters
     * @throws InvalidKeyException
     *                 if the given key is inappropriate for this cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the given parameters are inappropriate for this
     *                 cipher.
     */
    protected final void initCipherDecrypt(SecretKey key,
	    AlgorithmParameterSpec cipherParams) throws InvalidKeyException,
	    InvalidAlgorithmParameterException {
	blockCipher.initCipherDecrypt(key, cipherParams);
    }

    /**
     * Return the block size of the underlying cipher. It is supposed that all
     * block ciphers return a sane value via
     * {@link BlockCipher#getCipherBlockSize()} after initialization.
     * 
     * @return the block size of the underlying cipher
     */
    protected final int getCipherBlockSize() {
	return blockCipher.getCipherBlockSize();
    }

    /**
     * Encrypt a single block with the cipher engine.
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
    protected final void singleBlockEncrypt(byte[] input, int inOff,
	    byte[] output, int outOff) {
	blockCipher.singleBlockEncrypt(input, inOff, output, outOff);
    }

    /**
     * Decrypt a single block with the cipher engine.
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
    protected final void singleBlockDecrypt(byte[] input, int inOff,
	    byte[] output, int outOff) {
	blockCipher.singleBlockDecrypt(input, inOff, output, outOff);
    }

}

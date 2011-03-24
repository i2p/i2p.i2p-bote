/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt 
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */

package de.flexiprovider.common.mode;

import de.flexiprovider.api.Mode;
import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.keys.SecretKey;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;

/**
 * Cipher feedback mode for symmetric block ciphers.
 * <p>
 * This class currently only supports 1-byte feedback which should be sufficient
 * for most applications.
 * 
 * For further information, see "Handbook of Applied Cryptography", Note 7.17.
 * 
 * <b>WARNING: CFB</b> feedback should be in <b>BYTES</b>. The default value
 * is 1 Byte(8 Bits)
 * 
 * @author Ralf-P. Weinmann
 */
public class CFB extends Mode {

    /**
     * a help buffer
     */
    private byte[] buf;

    /**
     * The shift register needed by the mode.
     */
    private byte[] feedbackBlock;

    /**
     * Initialize the Mode object for encryption.
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
    protected final void initEncrypt(SecretKey key,
	    ModeParameterSpec modeParams, AlgorithmParameterSpec cipherParams)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {

	initCipherEncrypt(key, cipherParams);
	int cipherBlockSize = getCipherBlockSize();

	iv = new byte[cipherBlockSize];
	if (modeParams != null) {
	    // obtain IV from mode parameters
	    byte[] iv = modeParams.getIV();

	    if (iv.length < cipherBlockSize) {
		// if IV is too short, fill with zeroes
		System.arraycopy(iv, 0, this.iv, 0, iv.length);
	    } else if (iv.length > cipherBlockSize) {
		// if IV is too long, use only first bytes
		System.arraycopy(iv, 0, this.iv, 0, cipherBlockSize);
	    } else {
		// else, use the IV
		this.iv = iv;
	    }
	}

	if (modeParams instanceof CFBParameterSpec) {
	    // get block size
	    blockSize = ((OFBParameterSpec) modeParams).getBlockSize();
	    // check block size
	    if (blockSize > cipherBlockSize) {
		blockSize = cipherBlockSize;
	    }
	} else {
	    // default: set block size to cipher block size
	    blockSize = cipherBlockSize;
	}

	buf = new byte[cipherBlockSize];
	feedbackBlock = new byte[cipherBlockSize];
	reset();
    }

    /**
     * Initialize the Mode object for decryption.
     * 
     * @param key
     *                the key used for decryption
     * @param modeParams
     *                additional mode parameters
     * @param paramSpec
     *                additional algorithm parameters
     * @throws InvalidKeyException
     *                 if the key is inappropriate for initializing the
     *                 underlying block cipher.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are inappropriate for initializing the
     *                 underlying block cipher.
     */
    protected final void initDecrypt(SecretKey key,
	    ModeParameterSpec modeParams, AlgorithmParameterSpec paramSpec)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
	initEncrypt(key, modeParams, paramSpec);
    }

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
    protected final void nextChunkEncrypt(final byte[] input, final int inOff,
	    byte[] output, final int outOff) {
	nextChunk(input, inOff, output, outOff);
	// fill feedback block with ciphertext
	System.arraycopy(output, outOff, feedbackBlock, 0, blockSize);
    }

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
    protected final void nextChunkDecrypt(byte[] input, int inOff,
	    byte[] output, int outOff) {
	nextChunk(input, inOff, output, outOff);
	// fill feedback block with ciphertext
	System.arraycopy(input, inOff, feedbackBlock, 0, blockSize);
    }

    private void nextChunk(byte[] input, int inOff, byte[] output, int outOff) {

	// encrypt feedback block
	singleBlockEncrypt(feedbackBlock, 0, buf, 0);

	// compute ciphertext block
	for (int i = 0; i < blockSize; i++) {
	    output[outOff + i] = (byte) (buf[i] ^ input[inOff + i]);
	}

	// shift feedback block
	System.arraycopy(feedbackBlock, 0, feedbackBlock, blockSize,
		feedbackBlock.length - blockSize);
    }

    /**
     * Reset shift block to initialization vector.
     */
    protected final void reset() {
	System.arraycopy(iv, 0, feedbackBlock, 0, iv.length);
    }

}

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
 * Output feedback mode for symmetric block ciphers as per ISO/IEC 10116 (full
 * feedback)
 * <p>
 * We do not support the FIPS PUB 81 version of OFB, since the expected cycle
 * length dramatically decreases if the feedback is smaller than the block size
 * of the cipher.
 * <p>
 * TODO WARNING! OFB currently supports just 1-byte feedback
 * <p>
 * For further information, see "Handbook of Applied Cryptography", Note 7.24.
 * 
 * @author Ralf-P. Weinmann
 */
public class OFB extends Mode {

    // the output buffer
    private byte[] buf;

    // the feedback block
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

	if (modeParams instanceof OFBParameterSpec) {
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

	feedbackBlock = new byte[cipherBlockSize];
	buf = new byte[cipherBlockSize];
	reset();
    }

    /**
     * Initialize the Mode object for decryption.
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
    protected final void initDecrypt(SecretKey key,
	    ModeParameterSpec modeParams, AlgorithmParameterSpec cipherParams)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
	initEncrypt(key, modeParams, cipherParams);
    }

    /**
     * Encrypt the next data block.
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

	singleBlockEncrypt(buf, 0, feedbackBlock, 0);

	byte[] swap = buf;
	buf = feedbackBlock;
	feedbackBlock = swap;

	for (int i = 0; i < blockSize; i++) {
	    output[outOff + i] = (byte) (feedbackBlock[i] ^ input[inOff + i]);
	}
    }

    /**
     * Decrypt the next data block.
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
    protected final void nextChunkDecrypt(final byte[] input, final int inOff,
	    byte[] output, final int outOff) {
	nextChunkEncrypt(input, inOff, output, outOff);
    }

    /**
     * Reset feedback block to encrypted initialization vector.
     */
    protected final void reset() {
	singleBlockEncrypt(iv, 0, feedbackBlock, 0);
    }

}

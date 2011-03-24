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
 * Cipher Block Chaining (CBC) mode for symmetric block ciphers. For further
 * information, see "Handbook of Applied Cryptography", Note 7.13.
 * 
 * @author Ralf-P. Weinmann
 */
public class CBC extends Mode {

    /**
     * just a help buffer
     */
    private byte[] buf;

    /**
     * Temporary buffer used for chaining two blocks (by an xor operation).
     */
    private byte[] chainingBlock;

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
	initCommon(modeParams);
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

	initCipherDecrypt(key, cipherParams);
	initCommon(modeParams);
    }

    /**
     * CBC common initialization.
     */
    private void initCommon(ModeParameterSpec modeParams) {
	blockSize = getCipherBlockSize();

	iv = new byte[blockSize];
	if (modeParams != null) {
	    // obtain IV from mode parameters
	    byte[] iv = modeParams.getIV();

	    if (iv.length < blockSize) {
		// if IV is too short, fill with zeroes
		System.arraycopy(iv, 0, this.iv, 0, iv.length);
	    } else if (iv.length > blockSize) {
		// if IV is too long, use only first bytes
		System.arraycopy(iv, 0, this.iv, 0, blockSize);
	    } else {
		// else, use the IV
		this.iv = iv;
	    }
	}

	buf = new byte[blockSize];
	chainingBlock = new byte[blockSize];
	reset();
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
    protected final void nextChunkEncrypt(byte[] input, int inOff,
	    byte[] output, int outOff) {

	for (int i = blockSize - 1; i >= 0; i--) {
	    chainingBlock[i] ^= input[inOff + i];
	}

	singleBlockEncrypt(chainingBlock, 0, output, outOff);
	System.arraycopy(output, outOff, chainingBlock, 0, blockSize);
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

	singleBlockDecrypt(input, inOff, buf, 0);
	for (int i = blockSize - 1; i >= 0; i--) {
	    output[outOff + i] = (byte) (chainingBlock[i] ^ buf[i]);
	}

	System.arraycopy(input, inOff, chainingBlock, 0, blockSize);
    }

    /**
     * Reset chaining block to initialization vector.
     */
    protected final void reset() {
	System.arraycopy(iv, 0, chainingBlock, 0, iv.length);
    }

}

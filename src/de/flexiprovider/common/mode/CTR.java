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
 * Counter (CTR) mode for symmetric block ciphers.
 * 
 * Please see the following document for more information: H. Lipmaa, P.
 * Rogaway, D. Wagner: Comments to NIST concerning AES Modes of Operations:
 * CTR-Mode Encryption
 * http://csrc.nist.gov/CryptoToolkit/modes/proposedmodes/ctr/ctr-spec.pdf
 * 
 * @author Ralf-P. Weinmann
 * @author Martin Döring
 */
public class CTR extends Mode {

	// the counter value
	private byte[] counter;

	// the feedback block
	private byte[] feedbackBlock;

	/**
	 * Initialize the Mode object for encryption.
	 * 
	 * @param key
	 *            the key used for encryption
	 * @param modeParams
	 *            additional mode parameters
	 * @param cipherParams
	 *            additional algorithm parameters
	 * @throws InvalidKeyException
	 *             if the key is inappropriate for initializing the underlying
	 *             block cipher.
	 * @throws InvalidAlgorithmParameterException
	 *             if the parameters are inappropriate for initializing the
	 *             underlying block cipher.
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
	 *            the key used for decryption
	 * @param modeParams
	 *            additional mode parameters
	 * @param cipherParams
	 *            additional algorithm parameters
	 * @throws InvalidKeyException
	 *             if the key is inappropriate for initializing the underlying
	 *             block cipher.
	 * @throws InvalidAlgorithmParameterException
	 *             if the parameters are inappropriate for initializing the
	 *             underlying block cipher.
	 */
	protected final void initDecrypt(SecretKey key,
			ModeParameterSpec modeParams, AlgorithmParameterSpec cipherParams)
			throws InvalidKeyException, InvalidAlgorithmParameterException {

		initCipherDecrypt(key, cipherParams);
		initCommon(modeParams);
	}

	/**
	 * CTR common initialization.
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

		feedbackBlock = new byte[blockSize];
		counter = new byte[blockSize];
		reset();
	}

	/**
	 * Encrypt the next data block. Any special features of the Mode should be
	 * implemented here.
	 * 
	 * @param input
	 *            input data buffer
	 * @param inOff
	 *            input data offset
	 * @param output
	 *            output data buffer
	 * @param outOff
	 *            output data offset
	 */
	protected final void nextChunkEncrypt(final byte[] input, final int inOff,
			byte[] output, final int outOff) {

		singleBlockEncrypt(counter, 0, feedbackBlock, 0);

		int inCarry = 1;
		for (int i = blockSize - 1; i >= 0; i--) {
			output[outOff + i] = (byte) (feedbackBlock[i] ^ input[inOff + i]);
			// increase counter value
			int x = (counter[i] & 0xff) + inCarry;
			counter[i] = (byte) x;
			inCarry = (x > 255) ? 1 : 0;
		}
	}

	/**
	 * Decrypt the next data block. Any special features of the Mode should be
	 * implemented here.
	 * 
	 * @param input
	 *            input data buffer
	 * @param inOff
	 *            input data offset
	 * @param output
	 *            output data buffer
	 * @param outOff
	 *            output data offset
	 */
	protected final void nextChunkDecrypt(final byte[] input, final int inOff,
			byte[] output, final int outOff) {
		nextChunkEncrypt(input, inOff, output, outOff);
	}

	/**
	 * Reset counter value to initialization vector.
	 */
	protected final void reset() {
		System.arraycopy(iv, 0, counter, 0, blockSize);
	}

}

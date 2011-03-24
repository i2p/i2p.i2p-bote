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
 * This class implements the Electronic Codebook Mode (ECB) for symmetric block
 * ciphers.
 * <p>
 * The ECB mode directly applies the forward cipher function onto each block of
 * plaintext. Under a given key, blocks of plaintext that are equal are mapped
 * onto the same block of ciphertext. Decryption is accomplished by applying the
 * inverse cipher function onto blocks of ciphertext.
 * 
 * @author Ralf-P. Weinmann
 */
public class ECB extends Mode {

    /**
     * Initialize the Mode object for encryption.
     * 
     * @param key
     *                the key used for encryption
     * @param modeParams
     *                additional mode parameters (not used)
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
	blockSize = getCipherBlockSize();
    }

    /**
     * Initialize the Mode object for decryption.
     * 
     * @param key
     *                the key used for decryption
     * @param modeParams
     *                additional mode parameters (not used)
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
	blockSize = getCipherBlockSize();
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
	singleBlockEncrypt(input, inOff, output, outOff);
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
    protected final void nextChunkDecrypt(final byte[] input, final int inOff,
	    byte[] output, final int outOff) {
	singleBlockDecrypt(input, inOff, output, outOff);
    }

    /**
     * ECB does not perform any operations on reset.
     */
    protected final void reset() {
	// empty
    }

}

/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt 
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */

package de.flexiprovider.api;

import de.flexiprovider.api.exceptions.BadPaddingException;

/**
 * To encrypt a plaintext with a block cipher the input is divided into blocks
 * whose length depends on the encryption algorithm used. To ensure that the
 * block size will divide the length of the input, the last block is padded to
 * the needed length. Vice versa, when decrypting a ciphertext, the padding has
 * to be removed.
 * <p>
 * To make a new padding scheme available, one has to write a subclass of
 * <tt>de.flexiprovider.common.padding.PaddingScheme</tt> in order to allow a
 * blockcipher to use it. This subclass has to exist in the package
 * <tt>de.flexiprovider.common.padding</tt> and the class name must be the
 * same as the name used in the call of Cipher.getInstance(). E.g., if you call
 * Cipher.getInstance("SAFER+/CBC/NoPadding"), then there must exist a class
 * <tt>de.flexiprovider.common.padding.NoPadding</tt>.
 * 
 * @author Christoph Ender
 * @author Christoph Sesterhenn
 * @author Marcus Lippert
 * @author Martin Strese
 */
public abstract class PaddingScheme {

    /**
     * Block size used for padding
     */
    protected int blockSize = -1;

    /**
     * Tell the padding scheme the block size to which input will be padded.
     * 
     * @param blockSize
     *                length of one block
     */
    final void setBlockSize(int blockSize) {
	if (blockSize > 0) {
	    this.blockSize = blockSize;
	}
    }

    /**
     * Return the number of bytes which will be appended to the the plaintext
     * during padding.
     * 
     * @param inLen
     *                the length of the plaintext to be padded
     * @return the number of padding bytes (may be 0)
     */
    protected abstract int padLength(int inLen);

    /**
     * Pad the input to make its length divisible by the the block length. The
     * padding is written to the same buffer which is used for input. The caller
     * has to ensure that the input array is large enough to hold the padding
     * bytes.
     * 
     * @param input
     *                byte array containing the plaintext to be padded
     * @param inOff
     *                index where the plaintext starts
     * @param inLen
     *                length of the plaintext
     * @throws BadPaddingException
     *                 if the input buffer is too small to hold the padding
     *                 bytes.
     */
    protected abstract void pad(byte[] input, int inOff, int inLen)
	    throws BadPaddingException;

    /**
     * Given the plaintext that includes the padding bytes, unpad the plaintext
     * and return the index indicating where the padding bytes start.
     * 
     * @param input
     *                byte array containing the padded plaintext
     * @param inOff
     *                index where the plaintext starts
     * @param inLen
     *                size of the plaintext
     * @return index in the array where the padding bytes start
     * @throws BadPaddingException
     *                 if unpadding fails.
     */
    protected abstract int unpad(byte[] input, int inOff, int inLen)
	    throws BadPaddingException;

}

/*
 * Copyright (c) 1998-2008 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt 
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */
package de.flexiprovider.common.padding;

import de.flexiprovider.api.PaddingScheme;
import de.flexiprovider.api.exceptions.BadPaddingException;

/**
 * OneAndZeroesPadding pads the plaintext by appending a single byte 1 and
 * several 0 bytes.
 * 
 * @author Christoph Sesterhenn
 * @author Christoph Ender
 * @author Martin Döring
 */
public class OneAndZeroesPadding extends PaddingScheme {

    protected int padLength(int inLen) {
	return blockSize - (inLen % blockSize);
    }

    protected void pad(byte[] input, int inOff, int inLen) {
	int padLength = padLength(inLen);

	input[inOff + inLen] = (byte) 0x80;
	for (int i = 1; i < padLength; i++) {
	    input[inOff + inLen + i] = 0;
	}
    }

    protected int unpad(byte[] input, int inOff, int inLen)
	    throws BadPaddingException {
	while (inLen >= 0 && input[inOff + inLen - 1] != (byte) 0x80) {
	    if (input[inOff + inLen - 1] != 0) {
		return -1;
	    }
	    inLen--;
	}

	// compute start index of padding bytes
	int padOffset = inOff + inLen - 1;

	// check correctness
	if (padOffset == -1) {
	    throw new BadPaddingException("unpadding failed");
	}

	// return start index of padding bytes
	return padOffset;
    }

}

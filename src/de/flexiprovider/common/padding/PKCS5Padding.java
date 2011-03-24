/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
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
 * PKCS5Padding pads the plaintext by the method described in the PKCS #5 and
 * PKCS #7 standards.
 * 
 * @author Ulrich Dudszus
 * @author Tom Kollmar
 * @author Martin Döring
 */
public class PKCS5Padding extends PaddingScheme {

    protected int padLength(int inLen) {
	return blockSize - (inLen % blockSize);
    }

    protected void pad(byte[] input, int inOff, int inLen) {
	// compute the pad length
	int padLength = padLength(inLen);

	// pad the input
	int index = inOff + inLen;
	for (int i = 0; i < padLength; i++) {
	    input[index++] = (byte) padLength;
	}
    }

    protected int unpad(byte[] input, int inOff, int inLen)
	    throws BadPaddingException {
	// the pad length is stored in last byte of the input
	int last = inOff + inLen - 1;
	byte padLength = input[last--];

	// check correctness
	if (padLength < 0 || padLength > inLen) {
	    throw new BadPaddingException("unpadding failed");
	}
	for (int i = 1; i < padLength; i++) {
	    if (input[last--] != padLength) {
		throw new BadPaddingException("unpadding failed");
	    }
	}

	// return start index of padding bytes
	return ++last;
    }

}

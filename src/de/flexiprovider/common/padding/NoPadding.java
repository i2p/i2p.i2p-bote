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
 * NoPadding leaves the input unchanged in case of a suitable blocksize.
 * Otherwise, it will throw a BadPaddingException.
 * 
 * @author Andre Maric
 * @author Witold Wegner
 */
public class NoPadding extends PaddingScheme {

    protected int padLength(int inLen) {
	return 0;
    }

    protected void pad(byte[] input, int inOff, int inLen)
	    throws BadPaddingException {
	if (blockSize < 1 || (inLen % blockSize != 0)) {
	    throw new BadPaddingException("invalid input length");
	}
    }

    protected int unpad(byte[] input, int inOff, int inLen) {
	return inOff + inLen;
    }

}

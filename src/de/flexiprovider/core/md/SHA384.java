/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt 
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */

package de.flexiprovider.core.md;

/**
 * SHA-384 is a 384-bit hash and is meant to provide 192 bits of security
 * against collision attacks.
 * 
 * To obtain a 384-bit hash value will require truncating the SHA-512 output.
 * 
 * @author Ralf-P. Weinmann
 */
public final class SHA384 extends SHA384_512 {

	/**
	 * The algorithm name.
	 */
	public static final String ALG_NAME = "SHA384";

	/**
	 * The OID of SHA384 (defined by NIST).
	 */
	public static final String OID = "2.16.840.1.101.3.4.2.2";

	// Initial hash value H<sup>(0)</sup>. These were obtained by taking the
	// fractional parts of the square roots of the ninth to sixteenth prime.
	private static final long[] H0 = { 0xcbbb9d5dc1059ed8L,
			0x629a292a367cd507L, 0x9159015a3070dd17L, 0x152fecd8f70e5939L,
			0x67332667ffc00b31L, 0x8eb44a8768581511L, 0xdb0c2e0d64f98fa7L,
			0x47b5481dbefa4fa4L };

	// length of the SHA384 message digest in bytes
	private static final int SHA384_DIGEST_LENGTH = 48;

	/**
	 * Constructor.
	 */
	public SHA384() {
		super(SHA384_DIGEST_LENGTH);
	}

	/**
	 * Reset the digest objects to its initial state.
	 */
	public void reset() {
		initMessageDigest(H0);
	}

}

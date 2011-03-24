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
 * SHA224 is a 224-bit one-way hash function based on SHA-256, but the initial
 * value is different and the result is truncated to 224 bits.
 * 
 * For detailed information please refer to RFC 3874.
 * 
 */
public final class SHA224 extends SHA224_256 {

	/**
	 * The algorithm name.
	 */
	public static final String ALG_NAME = "SHA224";

	/**
	 * The OID of SHA224 (defined by NIST).
	 */
	public static final String OID = "2.16.840.1.101.3.4.2.4";

	// Initial hash value H<sup>(0)</sup>. These were obtained by taking the
	// fractional parts of the square roots of the first eight primes.
	private static final int[] H0 = { 0xc1059ed8, 0x367cd507, 0x3070dd17,
			0xf70e5939, 0xffc00b31, 0x68581511, 0x64f98fa7, 0xbefa4fa4 };

	// length of the SHA224 message digest in bytes
	private static final int SHA224_DIGEST_LENGTH = 28;

	/**
	 * Default constructor.
	 */
	public SHA224() {
		super(SHA224_DIGEST_LENGTH);
	}

	/**
	 * Reset the digest objects to its initial state.
	 */
	public void reset() {
		initMessageDigest(H0);
	}

}

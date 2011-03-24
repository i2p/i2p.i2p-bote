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
 * SHA256 is a 256-bit hash and is meant to provide 128 bits of security against
 * collision attacks.
 * 
 * @author Ralf-P. Weinmann
 */
public final class SHA256 extends SHA224_256 {

	/**
	 * The algorithm name.
	 */
	public static final String ALG_NAME = "SHA256";

	/**
	 * The OID of SHA256 (defined by NIST).
	 */
	public static final String OID = "2.16.840.1.101.3.4.2.1";

	// Initial hash value H<sup>(0)</sup>. These were obtained by taking the
	// fractional parts of the square roots of the first eight primes.
	private static final int[] H0 = { 0x6a09e667, 0xbb67ae85, 0x3c6ef372,
			0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19 };

	// length of the SHA256 message digest in bytes
	private static final int SHA256_DIGEST_LENGTH = 32;

	/**
	 * Default constructor.
	 */
	public SHA256() {
		super(SHA256_DIGEST_LENGTH);
	}

	/**
	 * Reset the digest objects to its initial state.
	 */
	public void reset() {
		initMessageDigest(H0);
	}

}

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
 * SHA-512 is a 512-bit hash and is meant to provide 256 bits of security
 * against collision attacks.
 * 
 * @author Ralf-P. Weinmann
 */
public final class SHA512 extends SHA384_512 {

	/**
	 * The algorithm name.
	 */
	public static final String ALG_NAME = "SHA512";

	/**
	 * The OID of SHA512 (defined by NIST).
	 */
	public static final String OID = "2.16.840.1.101.3.4.2.3";

	// Initial hash value H<sup>(0)</sup>. These were obtained by taking the
	// fractional parts of the square roots of the first eight primes.
	private static final long[] H0 = { 0x6a09e667f3bcc908L,
			0xbb67ae8584caa73bL, 0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L,
			0x510e527fade682d1L, 0x9b05688c2b3e6c1fL, 0x1f83d9abfb41bd6bL,
			0x5be0cd19137e2179L };

	// length of the SHA512 message digest in bytes
	private static final int SHA512_DIGEST_LENGTH = 64;

	/**
	 * Constructor.
	 */
	public SHA512() {
		super(SHA512_DIGEST_LENGTH);
	}

	/**
	 * Reset the digest objects to its initial state.
	 */
	public void reset() {
		initMessageDigest(H0);
	}

}

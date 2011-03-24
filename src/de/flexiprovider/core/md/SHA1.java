/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt 
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */

package de.flexiprovider.core.md;

import de.flexiprovider.api.MessageDigest;
import de.flexiprovider.common.util.BigEndianConversions;

/**
 * SHA1 extends java.security.MessageDigestSpi A class that implements the NIST
 * Secure Hash Algorithm - version 1. The Algorithm was implemented according to
 * Standard FIPS PUB 180-1.
 * 
 * @author Torsten Ehli
 * @author Sylvain Franke
 * @author Ralf-Philipp Weinmann
 */
public final class SHA1 extends MessageDigest {

	/**
	 * The algorithm name.
	 */
	public static final String ALG_NAME = "SHA1";

	/**
	 * An alternative algorithm name.
	 */
	public static final String ALG_NAME2 = "SHA";

	/**
	 * The OID of SHA1 (defined by IEEE P1363).
	 */
	public static final String OID = "1.3.14.3.2.26";

	// array to buffer the input before attaining the blocksize that can be
	// hashed
	private byte[] buffer;

	// counter for the bytes processed thus far
	private long count;

	// h0-h5 contain the digest after the original message has been processed
	private int h0, h1, h2, h3, h4;

	// array w is a temporary buffer used while computing any block
	private int[] w;

	// some constants are used while processing the digest. You might refer to
	// them as "The magic".

	private static final int const1 = 0x5a827999;

	private static final int const2 = 0x6ed9eba1;

	private static final int const3 = 0x8f1bbcdc;

	private static final int const4 = 0xca62c1d6;

	// length of the resulting message digest in bytes
	private static final int SHA1_DIGEST_LENGTH = 20;

	/**
	 * Constructor. Create and initialize the arrays needed for computing the
	 * digest.
	 */
	public SHA1() {
		w = new int[80];
		buffer = new byte[64];
		reset();
	}

	/**
	 * This function processBlock contains the actual SHA1 Algorithm, which is
	 * coded in accordance to the definition of the FIPS PUB 180-1 Standard.
	 */
	private synchronized void processBlock() {
		int a, b, c, d, e;
		int i;
		int register;

		/* step a */
		w[0] = BigEndianConversions.OS2IP(buffer, 0);
		w[1] = BigEndianConversions.OS2IP(buffer, 4);
		w[2] = BigEndianConversions.OS2IP(buffer, 8);
		w[3] = BigEndianConversions.OS2IP(buffer, 12);
		w[4] = BigEndianConversions.OS2IP(buffer, 16);
		w[5] = BigEndianConversions.OS2IP(buffer, 20);
		w[6] = BigEndianConversions.OS2IP(buffer, 24);
		w[7] = BigEndianConversions.OS2IP(buffer, 28);
		w[8] = BigEndianConversions.OS2IP(buffer, 32);
		w[9] = BigEndianConversions.OS2IP(buffer, 36);
		w[10] = BigEndianConversions.OS2IP(buffer, 40);
		w[11] = BigEndianConversions.OS2IP(buffer, 44);
		w[12] = BigEndianConversions.OS2IP(buffer, 48);
		w[13] = BigEndianConversions.OS2IP(buffer, 52);
		w[14] = BigEndianConversions.OS2IP(buffer, 56);
		w[15] = BigEndianConversions.OS2IP(buffer, 60);

		/* step b */
		for (i = 16; i < 80; i++) {
			register = w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16];
			// circular left shift by one bit
			w[i] = (register << 1) | (register >>> 31);
		}

		/* step c */
		a = h0;
		b = h1;
		c = h2;
		d = h3;
		e = h4;

		/* step d */

		e += const1 + (a << 5 | a >>> 27) + (b & c | ~b & d) + w[0];
		b = b << 30 | b >>> 2;
		d += const1 + (e << 5 | e >>> 27) + (a & b | ~a & c) + w[1];
		a = a << 30 | a >>> 2;
		c += const1 + (d << 5 | d >>> 27) + (e & a | ~e & b) + w[2];
		e = e << 30 | e >>> 2;
		b += const1 + (c << 5 | c >>> 27) + (d & e | ~d & a) + w[3];
		d = d << 30 | d >>> 2;
		a += const1 + (b << 5 | b >>> 27) + (c & d | ~c & e) + w[4];
		c = c << 30 | c >>> 2;
		e += const1 + (a << 5 | a >>> 27) + (b & c | ~b & d) + w[5];
		b = b << 30 | b >>> 2;
		d += const1 + (e << 5 | e >>> 27) + (a & b | ~a & c) + w[6];
		a = a << 30 | a >>> 2;
		c += const1 + (d << 5 | d >>> 27) + (e & a | ~e & b) + w[7];
		e = e << 30 | e >>> 2;
		b += const1 + (c << 5 | c >>> 27) + (d & e | ~d & a) + w[8];
		d = d << 30 | d >>> 2;
		a += const1 + (b << 5 | b >>> 27) + (c & d | ~c & e) + w[9];
		c = c << 30 | c >>> 2;
		e += const1 + (a << 5 | a >>> 27) + (b & c | ~b & d) + w[10];
		b = b << 30 | b >>> 2;
		d += const1 + (e << 5 | e >>> 27) + (a & b | ~a & c) + w[11];
		a = a << 30 | a >>> 2;
		c += const1 + (d << 5 | d >>> 27) + (e & a | ~e & b) + w[12];
		e = e << 30 | e >>> 2;
		b += const1 + (c << 5 | c >>> 27) + (d & e | ~d & a) + w[13];
		d = d << 30 | d >>> 2;
		a += const1 + (b << 5 | b >>> 27) + (c & d | ~c & e) + w[14];
		c = c << 30 | c >>> 2;
		e += const1 + (a << 5 | a >>> 27) + (b & c | ~b & d) + w[15];
		b = b << 30 | b >>> 2;
		d += const1 + (e << 5 | e >>> 27) + (a & b | ~a & c) + w[16];
		a = a << 30 | a >>> 2;
		c += const1 + (d << 5 | d >>> 27) + (e & a | ~e & b) + w[17];
		e = e << 30 | e >>> 2;
		b += const1 + (c << 5 | c >>> 27) + (d & e | ~d & a) + w[18];
		d = d << 30 | d >>> 2;
		a += const1 + (b << 5 | b >>> 27) + (c & d | ~c & e) + w[19];
		c = c << 30 | c >>> 2;
		e += const2 + (a << 5 | a >>> 27) + (b ^ c ^ d) + w[20];
		b = b << 30 | b >>> 2;
		d += const2 + (e << 5 | e >>> 27) + (a ^ b ^ c) + w[21];
		a = a << 30 | a >>> 2;
		c += const2 + (d << 5 | d >>> 27) + (e ^ a ^ b) + w[22];
		e = e << 30 | e >>> 2;
		b += const2 + (c << 5 | c >>> 27) + (d ^ e ^ a) + w[23];
		d = d << 30 | d >>> 2;
		a += const2 + (b << 5 | b >>> 27) + (c ^ d ^ e) + w[24];
		c = c << 30 | c >>> 2;
		e += const2 + (a << 5 | a >>> 27) + (b ^ c ^ d) + w[25];
		b = b << 30 | b >>> 2;
		d += const2 + (e << 5 | e >>> 27) + (a ^ b ^ c) + w[26];
		a = a << 30 | a >>> 2;
		c += const2 + (d << 5 | d >>> 27) + (e ^ a ^ b) + w[27];
		e = e << 30 | e >>> 2;
		b += const2 + (c << 5 | c >>> 27) + (d ^ e ^ a) + w[28];
		d = d << 30 | d >>> 2;
		a += const2 + (b << 5 | b >>> 27) + (c ^ d ^ e) + w[29];
		c = c << 30 | c >>> 2;
		e += const2 + (a << 5 | a >>> 27) + (b ^ c ^ d) + w[30];
		b = b << 30 | b >>> 2;
		d += const2 + (e << 5 | e >>> 27) + (a ^ b ^ c) + w[31];
		a = a << 30 | a >>> 2;
		c += const2 + (d << 5 | d >>> 27) + (e ^ a ^ b) + w[32];
		e = e << 30 | e >>> 2;
		b += const2 + (c << 5 | c >>> 27) + (d ^ e ^ a) + w[33];
		d = d << 30 | d >>> 2;
		a += const2 + (b << 5 | b >>> 27) + (c ^ d ^ e) + w[34];
		c = c << 30 | c >>> 2;
		e += const2 + (a << 5 | a >>> 27) + (b ^ c ^ d) + w[35];
		b = b << 30 | b >>> 2;
		d += const2 + (e << 5 | e >>> 27) + (a ^ b ^ c) + w[36];
		a = a << 30 | a >>> 2;
		c += const2 + (d << 5 | d >>> 27) + (e ^ a ^ b) + w[37];
		e = e << 30 | e >>> 2;
		b += const2 + (c << 5 | c >>> 27) + (d ^ e ^ a) + w[38];
		d = d << 30 | d >>> 2;
		a += const2 + (b << 5 | b >>> 27) + (c ^ d ^ e) + w[39];
		c = c << 30 | c >>> 2;
		e += const3 + (a << 5 | a >>> 27) + (b & c | b & d | c & d) + w[40];
		b = b << 30 | b >>> 2;
		d += const3 + (e << 5 | e >>> 27) + (a & b | a & c | b & c) + w[41];
		a = a << 30 | a >>> 2;
		c += const3 + (d << 5 | d >>> 27) + (e & a | e & b | a & b) + w[42];
		e = e << 30 | e >>> 2;
		b += const3 + (c << 5 | c >>> 27) + (d & e | d & a | e & a) + w[43];
		d = d << 30 | d >>> 2;
		a += const3 + (b << 5 | b >>> 27) + (c & d | c & e | d & e) + w[44];
		c = c << 30 | c >>> 2;
		e += const3 + (a << 5 | a >>> 27) + (b & c | b & d | c & d) + w[45];
		b = b << 30 | b >>> 2;
		d += const3 + (e << 5 | e >>> 27) + (a & b | a & c | b & c) + w[46];
		a = a << 30 | a >>> 2;
		c += const3 + (d << 5 | d >>> 27) + (e & a | e & b | a & b) + w[47];
		e = e << 30 | e >>> 2;
		b += const3 + (c << 5 | c >>> 27) + (d & e | d & a | e & a) + w[48];
		d = d << 30 | d >>> 2;
		a += const3 + (b << 5 | b >>> 27) + (c & d | c & e | d & e) + w[49];
		c = c << 30 | c >>> 2;
		e += const3 + (a << 5 | a >>> 27) + (b & c | b & d | c & d) + w[50];
		b = b << 30 | b >>> 2;
		d += const3 + (e << 5 | e >>> 27) + (a & b | a & c | b & c) + w[51];
		a = a << 30 | a >>> 2;
		c += const3 + (d << 5 | d >>> 27) + (e & a | e & b | a & b) + w[52];
		e = e << 30 | e >>> 2;
		b += const3 + (c << 5 | c >>> 27) + (d & e | d & a | e & a) + w[53];
		d = d << 30 | d >>> 2;
		a += const3 + (b << 5 | b >>> 27) + (c & d | c & e | d & e) + w[54];
		c = c << 30 | c >>> 2;
		e += const3 + (a << 5 | a >>> 27) + (b & c | b & d | c & d) + w[55];
		b = b << 30 | b >>> 2;
		d += const3 + (e << 5 | e >>> 27) + (a & b | a & c | b & c) + w[56];
		a = a << 30 | a >>> 2;
		c += const3 + (d << 5 | d >>> 27) + (e & a | e & b | a & b) + w[57];
		e = e << 30 | e >>> 2;
		b += const3 + (c << 5 | c >>> 27) + (d & e | d & a | e & a) + w[58];
		d = d << 30 | d >>> 2;
		a += const3 + (b << 5 | b >>> 27) + (c & d | c & e | d & e) + w[59];
		c = c << 30 | c >>> 2;
		e += const4 + (a << 5 | a >>> 27) + (b ^ c ^ d) + w[60];
		b = b << 30 | b >>> 2;
		d += const4 + (e << 5 | e >>> 27) + (a ^ b ^ c) + w[61];
		a = a << 30 | a >>> 2;
		c += const4 + (d << 5 | d >>> 27) + (e ^ a ^ b) + w[62];
		e = e << 30 | e >>> 2;
		b += const4 + (c << 5 | c >>> 27) + (d ^ e ^ a) + w[63];
		d = d << 30 | d >>> 2;
		a += const4 + (b << 5 | b >>> 27) + (c ^ d ^ e) + w[64];
		c = c << 30 | c >>> 2;
		e += const4 + (a << 5 | a >>> 27) + (b ^ c ^ d) + w[65];
		b = b << 30 | b >>> 2;
		d += const4 + (e << 5 | e >>> 27) + (a ^ b ^ c) + w[66];
		a = a << 30 | a >>> 2;
		c += const4 + (d << 5 | d >>> 27) + (e ^ a ^ b) + w[67];
		e = e << 30 | e >>> 2;
		b += const4 + (c << 5 | c >>> 27) + (d ^ e ^ a) + w[68];
		d = d << 30 | d >>> 2;
		a += const4 + (b << 5 | b >>> 27) + (c ^ d ^ e) + w[69];
		c = c << 30 | c >>> 2;
		e += const4 + (a << 5 | a >>> 27) + (b ^ c ^ d) + w[70];
		b = b << 30 | b >>> 2;
		d += const4 + (e << 5 | e >>> 27) + (a ^ b ^ c) + w[71];
		a = a << 30 | a >>> 2;
		c += const4 + (d << 5 | d >>> 27) + (e ^ a ^ b) + w[72];
		e = e << 30 | e >>> 2;
		b += const4 + (c << 5 | c >>> 27) + (d ^ e ^ a) + w[73];
		d = d << 30 | d >>> 2;
		a += const4 + (b << 5 | b >>> 27) + (c ^ d ^ e) + w[74];
		c = c << 30 | c >>> 2;
		e += const4 + (a << 5 | a >>> 27) + (b ^ c ^ d) + w[75];
		b = b << 30 | b >>> 2;
		d += const4 + (e << 5 | e >>> 27) + (a ^ b ^ c) + w[76];
		a = a << 30 | a >>> 2;
		c += const4 + (d << 5 | d >>> 27) + (e ^ a ^ b) + w[77];
		e = e << 30 | e >>> 2;
		b += const4 + (c << 5 | c >>> 27) + (d ^ e ^ a) + w[78];
		d = d << 30 | d >>> 2;
		a += const4 + (b << 5 | b >>> 27) + (c ^ d ^ e) + w[79];
		c = c << 30 | c >>> 2;

		/* step e */
		h0 += a;
		h1 += b;
		h2 += c;
		h3 += d;
		h4 += e;
	}

	/**
	 * Completes the hash computation by performing final operations such as
	 * padding.
	 * 
	 * @return the digest value
	 */
	public synchronized byte[] digest() {

		pad();

		byte[] digestValue = new byte[SHA1_DIGEST_LENGTH];
		BigEndianConversions.I2OSP(h0, digestValue, 0);
		BigEndianConversions.I2OSP(h1, digestValue, 4);
		BigEndianConversions.I2OSP(h2, digestValue, 8);
		BigEndianConversions.I2OSP(h3, digestValue, 12);
		BigEndianConversions.I2OSP(h4, digestValue, 16);

		reset();

		return digestValue;
	}

	/**
	 * Returns the digest length in bytes.
	 * 
	 * @return the digest length in bytes.
	 */
	public int getDigestLength() {
		return SHA1_DIGEST_LENGTH;
	}

	/**
	 * Resets the digest for further use.
	 */
	public void reset() {
		h0 = 0x67452301;
		h1 = 0xefcdab89;
		h2 = 0x98badcfe;
		h3 = 0x10325476;
		h4 = 0xc3d2e1f0;
		count = 0;
	}

	/**
	 * Updates the digest using the specified array of bytes, starting at the
	 * specified offset.
	 * 
	 * @param input
	 *            - the byte[] to use for the update.
	 * @param offset
	 *            - the offset to start from in the array of bytes.
	 * @param len
	 *            - the number of bytes to use, starting at offset.
	 */
	public synchronized void update(byte[] input, int offset, int len) {
		int bufOffset = ((int) count) & 0x3f;
		int copyLen;

		while (len > 0) {
			copyLen = 64 - bufOffset;
			copyLen = (len > copyLen) ? copyLen : len;

			System.arraycopy(input, offset, buffer, bufOffset, copyLen);

			len -= copyLen;
			offset += copyLen;
			count += copyLen;
			bufOffset = (bufOffset + copyLen) & 0x3f;

			if (bufOffset == 0) {
				processBlock();
			}
		}
	}

	/**
	 * Updates the digest using the specified byte.
	 * 
	 * @param input
	 *            - the byte to use for the update.
	 */
	public synchronized void update(byte input) {
		buffer[(int) count & 0x3f] = input;

		if ((int) (count & 0x3f) == 63) {
			processBlock();
		}

		count++;
	}

	/**
	 * This Method performs the padding for the SHA1 algorithm. A single 1-bit
	 * is appended and then 0-bits, until only 64 bits are left free in the
	 * final block to enter the total length of the entered message.
	 */
	private void pad() {
		long bitLength = count << 3;
		buffer[(int) count & 0x3f] = (byte) 0x80;
		count++;

		if ((int) (count & 0x3f) > 56) {
			for (int i = (int) count & 0x3f; i < 64; i++) {
				buffer[i] = 0;
				count++;
			}
			processBlock();
		} else if ((int) (count & 0x3f) == 0) {
			processBlock();
		}

		for (int i = (int) count & 0x3f; i < 56; i++) {
			buffer[i] = 0;
		}

		BigEndianConversions.I2OSP(bitLength, buffer, 56);

		processBlock();
	}

}

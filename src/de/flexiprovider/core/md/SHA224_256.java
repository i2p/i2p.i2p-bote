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
 * Super class for SHA-224 and SHA-256
 * 
 * @author Ralf-P. Weinmann
 */
public abstract class SHA224_256 extends MessageDigest {

    // Constant words K<sub>0...63</sub>. These are the first thirty-two bits of
    // the fractional parts of the cube roots of the first sixty-four primes.
    private static final int[] K = { 0x428a2f98, 0x71374491, 0xb5c0fbcf,
	    0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
	    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74,
	    0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786,
	    0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc,
	    0x76f988da, 0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
	    0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967, 0x27b70a85,
	    0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb,
	    0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b, 0xc24b8b70,
	    0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
	    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3,
	    0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f,
	    0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7,
	    0xc67178f2, };

    private int[] W;

    // contains the digest value after complete message has been processed
    private int[] H;

    // input buffer
    private byte[] buffer;

    // number of bytes we already digested
    private long count;

    // the digest length
    private int digestLength;

    /**
     * Constructor.
     * 
     * @param digestLength
     *                the digest length
     */
    protected SHA224_256(int digestLength) {
	H = new int[8];
	W = new int[64];
	buffer = new byte[64];
	this.digestLength = digestLength;
	reset();
    }

    /**
     * Initialize the function with an initial state.
     * 
     * @param initialState
     *                the initial state
     */
    protected void initMessageDigest(int[] initialState) {
	count = 0;
	System.arraycopy(initialState, 0, H, 0, initialState.length);
    }

    /**
     * Sigma 0 function.
     * 
     * @param x
     *                the input
     * @return the rotated value
     */
    private static int s0(int x) {
	return ((x >>> 7) | (x << 25)) ^ ((x >>> 18) | (x << 14)) ^ (x >>> 3);
    }

    /**
     * Sigma 1 function.
     * 
     * @param x
     *                the input
     * @return the rotated value
     */
    private static int s1(int x) {
	return ((x >>> 17) | (x << 15)) ^ ((x >>> 19) | (x << 13)) ^ (x >>> 10);
    }

    /**
     * Compute the hash value of the current block and store it in H
     */
    private void processBlock() {
	int i, i2;
	int T1, T2;
	int a, b, c, d, e, f, g, h;

	a = H[0];
	b = H[1];
	c = H[2];
	d = H[3];
	e = H[4];
	f = H[5];
	g = H[6];
	h = H[7];

	for (i = 0; i < 64; i++) {

	    i2 = i << 2;
	    W[i] = i < 16 ? (((buffer[i2] & 0xff) << 24)
		    | (buffer[++i2] & 0xff) << 16 | (buffer[++i2] & 0xff) << 8 | (buffer[++i2] & 0xff))
		    : s1(W[i - 2]) + W[i - 7] + s0(W[i - 15]) + W[i - 16];

	    T1 = ((e >>> 6 | e << 26) ^ (e >>> 11 | e << 21) ^ (e >>> 25 | e << 7))
		    + (e & f ^ ~e & g) + h + K[i] + W[i];
	    T2 = ((a >>> 2 | a << 30) ^ (a >>> 13 | a << 19) ^ (a >>> 22 | a << 10))
		    + (a & b ^ a & c ^ b & c);

	    h = g;
	    g = f;
	    f = e;
	    e = d + T1;
	    d = c;
	    c = b;
	    b = a;
	    a = T1 + T2;
	}

	H[0] += a;
	H[1] += b;
	H[2] += c;
	H[3] += d;
	H[4] += e;
	H[5] += f;
	H[6] += g;
	H[7] += h;

    }

    /**
     * Pad and hash the value
     */
    private void pad() {
	// compute length of message in bits
	long bitLength = count << 3;
	// append single 1-bit trailed by 0-bits to message

	buffer[(int) count & 63] = (byte) 0x80;
	count++;

	if ((int) (count & 63) > 56) {
	    for (int i = (int) count & 63; i < 64; i++) {
		buffer[i] = 0;
		count++;
	    }
	    processBlock();
	} else if ((int) (count & 63) == 0) {
	    processBlock();
	}

	for (int i = (int) count & 63; i < 56; i++) {
	    buffer[i] = 0;
	}

	// append length of message
	BigEndianConversions.I2OSP(bitLength, buffer, 56);

	// chomp last block
	processBlock();
    }

    /**
     * @return the digest length in bytes
     */
    public int getDigestLength() {
	return digestLength;
    }

    /**
     * Update the digest using the specified input byte.
     * 
     * @param input
     *                the input byte
     */
    public synchronized void update(byte input) {
	buffer[(int) count & 63] = input;

	if ((int) (count & 63) == 63) {
	    processBlock();
	}

	count++;
    }

    /**
     * Update the digest using the specified input byte array.
     * 
     * @param input
     *                the input
     * @param inOff
     *                the offset where the input starts
     * @param inLen
     *                the input length
     */
    public synchronized void update(byte[] input, int inOff, int inLen) {
	int bufOff = ((int) count) & 63;

	while (inLen > 0) {
	    int copyLen = 64 - bufOff;
	    copyLen = (inLen > copyLen) ? copyLen : inLen;

	    System.arraycopy(input, inOff, buffer, bufOff, copyLen);

	    inLen -= copyLen;
	    inOff += copyLen;
	    count += copyLen;
	    bufOff = (bufOff + copyLen) & 63;

	    if (bufOff == 0) {
		processBlock();
	    }
	}
    }

    /**
     * Complete the hash computation by performing final operations such as
     * padding.
     * 
     * @return the digest value
     */
    public synchronized byte[] digest() {
	pad();

	byte[] digestValue = new byte[digestLength];
	for (int i = digestLength >> 2; --i >= 0;) {
	    BigEndianConversions.I2OSP(H[i], digestValue, i << 2);
	}

	reset();

	return digestValue;
    }

}

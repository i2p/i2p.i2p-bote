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
 * Abstract class for SHA message digests operating on 64 bit words (SHA-384 and
 * SHA-512).
 * 
 * @author Ralf-P. Weinmann
 */
public abstract class SHA384_512 extends MessageDigest {

    // Constant words K<sub>0...79</sub>. These are the first sixty-four bits of
    // the fractional parts of the cube roots of the first eighty primes.
    private static final long[] K = { 0x428a2f98d728ae22L, 0x7137449123ef65cdL,
	    0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL, 0x3956c25bf348b538L,
	    0x59f111f1b605d019L, 0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L,
	    0xd807aa98a3030242L, 0x12835b0145706fbeL, 0x243185be4ee4b28cL,
	    0x550c7dc3d5ffb4e2L, 0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L,
	    0x9bdc06a725c71235L, 0xc19bf174cf692694L, 0xe49b69c19ef14ad2L,
	    0xefbe4786384f25e3L, 0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
	    0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L,
	    0x76f988da831153b5L, 0x983e5152ee66dfabL, 0xa831c66d2db43210L,
	    0xb00327c898fb213fL, 0xbf597fc7beef0ee4L, 0xc6e00bf33da88fc2L,
	    0xd5a79147930aa725L, 0x06ca6351e003826fL, 0x142929670a0e6e70L,
	    0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL,
	    0x53380d139d95b3dfL, 0x650a73548baf63deL, 0x766a0abb3c77b2a8L,
	    0x81c2c92e47edaee6L, 0x92722c851482353bL, 0xa2bfe8a14cf10364L,
	    0xa81a664bbc423001L, 0xc24b8b70d0f89791L, 0xc76c51a30654be30L,
	    0xd192e819d6ef5218L, 0xd69906245565a910L, 0xf40e35855771202aL,
	    0x106aa07032bbd1b8L, 0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L,
	    0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L, 0x391c0cb3c5c95a63L,
	    0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L,
	    0x748f82ee5defb2fcL, 0x78a5636f43172f60L, 0x84c87814a1f0ab72L,
	    0x8cc702081a6439ecL, 0x90befffa23631e28L, 0xa4506cebde82bde9L,
	    0xbef9a3f7b2c67915L, 0xc67178f2e372532bL, 0xca273eceea26619cL,
	    0xd186b8c721c0c207L, 0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L,
	    0x06f067aa72176fbaL, 0x0a637dc5a2c898a6L, 0x113f9804bef90daeL,
	    0x1b710b35131c471bL, 0x28db77f523047d84L, 0x32caab7b40c72493L,
	    0x3c9ebe0a15c9bebcL, 0x431d67c49c100d4cL, 0x4cc5d4becb3e42b6L,
	    0x597f299cfc657e2aL, 0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L };

    // input buffer
    private byte[] buffer;

    // the number of bytes already digested
    private long count;

    // the digest length
    private int digestLength;

    // contains the digest value after complete message has been processed
    private long[] H;

    private long[] W;

    /**
     * Constructor.
     * 
     * @param digestLength
     *                the digest length
     * 
     */
    protected SHA384_512(int digestLength) {
	buffer = new byte[128];
	W = new long[80];
	H = new long[8];
	this.digestLength = digestLength;
	reset();
    }

    /**
     * Initialize the message digest with an initial state.
     * 
     * @param initialState
     *                the initial state
     */
    protected void initMessageDigest(long[] initialState) {
	System.arraycopy(initialState, 0, H, 0, initialState.length);
	count = 0;
    }

    /**
     * Sigma 0 function.
     * 
     * @param x
     *                the input
     * @return the rotated value
     */
    private static long sigma0(long x) {
	return (x >>> 1 | x << 63) ^ (x >>> 8 | x << 56) ^ (x >>> 7);
    }

    /**
     * Sigma 1 function.
     * 
     * @param x
     *                the input
     * @return the rotated value
     */
    private static long sigma1(long x) {
	return (x >>> 19 | x << 45) ^ (x >>> 61 | x << 3) ^ (x >>> 6);
    }

    /**
     * Compute the hash value of the current block and store it in H.
     */
    private void processBlock() {
	long a = H[0];
	long b = H[1];
	long c = H[2];
	long d = H[3];
	long e = H[4];
	long f = H[5];
	long g = H[6];
	long h = H[7];

	for (int i = 0; i < 80; i++) {

	    W[i] = i < 16 ? BigEndianConversions.OS2LIP(buffer, i << 3)
		    : sigma1(W[i - 2]) + W[i - 7] + sigma0(W[i - 15])
			    + W[i - 16];

	    long T1 = ((e >>> 14 | e << 50) ^ (e >>> 18 | e << 46) ^ (e >>> 41 | e << 23))
		    + (e & f ^ ~e & g) + h + K[i] + W[i];

	    long T2 = ((a >>> 28 | a << 36) ^ (a >>> 34 | a << 30) ^ (a >>> 39 | a << 25))
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
	buffer[(int) count & 127] = (byte) 0x80;
	count++;

	if ((count & 127) > 112) {
	    for (int i = (int) count & 127; i < 128; i++) {
		buffer[i] = 0;
		count++;
	    }
	    processBlock();
	} else if ((count & 127) == 0) {
	    processBlock();
	}

	for (int i = (int) count & 127; i < 112; i++) {
	    buffer[i] = 0;
	}

	// append length of message
	buffer[112] = 0;
	buffer[113] = 0;
	buffer[114] = 0;
	buffer[115] = 0;
	buffer[116] = 0;
	buffer[117] = 0;
	buffer[118] = 0;
	buffer[119] = 0;
	BigEndianConversions.I2OSP(bitLength, buffer, 120);

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
	buffer[(int) count & 127] = input;

	if ((int) (count & 127) == 127) {
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
	int bufOffset = ((int) count) & 127;

	while (inLen > 0) {
	    int copyLen = 128 - bufOffset;
	    copyLen = (inLen > copyLen) ? copyLen : inLen;

	    System.arraycopy(input, inOff, buffer, bufOffset, copyLen);

	    inLen -= copyLen;
	    inOff += copyLen;
	    count += copyLen;
	    bufOffset = (bufOffset + copyLen) & 127;

	    if (bufOffset == 0) {
		processBlock();
	    }
	}
    }

    /**
     * Completes the hash computation by performing final operations such as
     * padding.
     * 
     * @return the digest value
     */
    public synchronized byte[] digest() {
	pad();

	byte[] digestValue = new byte[digestLength];
	for (int i = digestLength >> 3; --i >= 0;) {
	    BigEndianConversions.I2OSP(H[i], digestValue, i << 3);
	}

	reset();

	return digestValue;
    }

}

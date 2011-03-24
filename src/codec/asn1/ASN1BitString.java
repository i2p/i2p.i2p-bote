/* ========================================================================
 *
 *  This file is part of CODEC, which is a Java package for encoding
 *  and decoding ASN.1 data structures.
 *
 *  Author: Fraunhofer Institute for Computer Graphics Research IGD
 *          Department A8: Security Technology
 *          Fraunhoferstr. 5, 64283 Darmstadt, Germany
 *
 *  Rights: Copyright (c) 2004 by Fraunhofer-Gesellschaft 
 *          zur Foerderung der angewandten Forschung e.V.
 *          Hansastr. 27c, 80686 Munich, Germany.
 *
 * ------------------------------------------------------------------------
 *
 *  The software package is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 2.1 of the 
 *  License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public 
 *  License along with this software package; if not, write to the Free 
 *  Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 *  MA 02110-1301, USA or obtain a copy of the license at 
 *  http://www.fsf.org/licensing/licenses/lgpl.txt.
 *
 * ------------------------------------------------------------------------
 *
 *  The CODEC library can solely be used and distributed according to 
 *  the terms and conditions of the GNU Lesser General Public License for 
 *  non-commercial research purposes and shall not be embedded in any 
 *  products or services of any user or of any third party and shall not 
 *  be linked with any products or services of any user or of any third 
 *  party that will be commercially exploited.
 *
 *  The CODEC library has not been tested for the use or application 
 *  for a determined purpose. It is a developing version that can 
 *  possibly contain errors. Therefore, Fraunhofer-Gesellschaft zur 
 *  Foerderung der angewandten Forschung e.V. does not warrant that the 
 *  operation of the CODEC library will be uninterrupted or error-free. 
 *  Neither does Fraunhofer-Gesellschaft zur Foerderung der angewandten 
 *  Forschung e.V. warrant that the CODEC library will operate and 
 *  interact in an uninterrupted or error-free way together with the 
 *  computer program libraries of third parties which the CODEC library 
 *  accesses and which are distributed together with the CODEC library.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not warrant that the operation of the third parties's computer 
 *  program libraries themselves which the CODEC library accesses will 
 *  be uninterrupted or error-free.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  shall not be liable for any errors or direct, indirect, special, 
 *  incidental or consequential damages, including lost profits resulting 
 *  from the combination of the CODEC library with software of any user 
 *  or of any third party or resulting from the implementation of the 
 *  CODEC library in any products, systems or services of any user or 
 *  of any third party.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not provide any warranty nor any liability that utilization of 
 *  the CODEC library will not interfere with third party intellectual 
 *  property rights or with any other protected third party rights or will 
 *  cause damage to third parties. Fraunhofer Gesellschaft zur Foerderung 
 *  der angewandten Forschung e.V. is currently not aware of any such 
 *  rights.
 *
 *  The CODEC library is supplied without any accompanying services.
 *
 * ========================================================================
 */
package codec.asn1;

import java.io.IOException;

/**
 * Represents an ASN.1 BIT STRING type. The corresponding Java type is
 * <code>boolean[]</code>.
 * <p>
 * 
 * This class has to modes of initialization which play a crucial role in
 * standards compliant DER encoding. One mode initializes instances of this
 * class as a representation of &quot;named bits&quot;. The second mode
 * initializes it as a plain bitstring.
 * <p>
 * 
 * An ASN.1 structure with named bits looks e.g., as follows:
 * 
 * <pre>
 * Rights ::= BIT STRING { read(0), write(1), execute(2)}
 * </pre>
 * 
 * Such bitstrings have a special canonical encoding. The mode (and defaults)
 * are specified in the documentation of the constructors of this class.
 * Basically, in named bits mode, trailing zeroes are truncated from the
 * internal representation of the bitstring.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1BitString.java,v 1.6 2005/03/23 13:01:20 flautens Exp $"
 */
public class ASN1BitString extends ASN1AbstractType {

    private static final byte[] DEFAULT_VALUE = new byte[0];

    private static final byte[] MASK = { (byte) 0x80, (byte) 0x40, (byte) 0x20,
	    (byte) 0x10, (byte) 0x08, (byte) 0x04, (byte) 0x02, (byte) 0x01 };

    private static final byte[] TRAIL_MASK = { (byte) 0xff, (byte) 0xfe,
	    (byte) 0xfc, (byte) 0xf8, (byte) 0xf0, (byte) 0xe0, (byte) 0xc0,
	    (byte) 0x80 };

    private int pad_ = 0;

    private byte[] value_ = DEFAULT_VALUE;

    /**
     * Says whether this instance is a &quot;named bits&quot; bitstring. The
     * default is <code>false</code>.
     */
    private boolean namedBits_ = false;

    /**
     * Initializes an instance for decoding. This initializes this instance to
     * be decoded as a plain bitstring (no named bits). Use this for bitstrings
     * with named bits as well. It does not make a difference for the
     * application, but it ensures that bitstrings that have been decoded are
     * encoded in the same way again.
     */
    public ASN1BitString() {
    }

    /**
     * Initializes the instance for encoding with the given bits. The index of
     * each bit corresponds to its number in the ASN.1 definition. This
     * constructor initializes the instance as a bitstring with named bits. The
     * array is not copied into this instance and can be modified subsequently
     * without causing side effects.
     * 
     * @param b
     *                The string of bits to be encoded.
     */
    public ASN1BitString(boolean[] b) {
	setBits0(b);
    }

    /**
     * Creates an instance with the given contents. Use of this constructor
     * copies the given byte array by reference and may cause side effects. The
     * mode of this bitstring is plain (no named bits are assumed).
     * 
     * @param b
     *                The left aligned contents bits.
     * @param pad
     *                The number of pad bits.
     */
    public ASN1BitString(byte[] b, int pad) {
	setBits0(b, pad);
    }

    /**
     * This method calls {@link #getBits getBits()}.
     * 
     * @return The contents bits as a boolean array.
     */
    public Object getValue() {
	return getBits();
    }

    /**
     * Returns the contents bits of this instance. No side effects occur when
     * the returned array is modified.
     * 
     * @return The contents bits.
     */
    public boolean[] getBits() {
	int n, i;
	boolean[] b;

	if (value_.length == 0) {
	    return new boolean[0];
	}
	b = new boolean[(value_.length * 8) - pad_];

	for (n = 0, i = 0; i < b.length; i++) {
	    if ((value_[n] & MASK[i & 0x07]) != 0) {
		b[i] = true;
	    } else {
		b[i] = false;
	    }
	    if ((i & 0x07) == 0x07) {
		n++;
	    }
	}
	return b;
    }

    /**
     * Sets the contents bits of this instance. This method does not cause side
     * effects. It switches to the named bits mode.
     * 
     * @param bits
     *                The contents bits that are set.
     * @throws ConstraintException
     *                 if the given bits violates the specified constraint.
     */
    public void setBits(boolean[] bits) throws ConstraintException {
	setBits0(bits);
	checkConstraints();
    }

    /**
     * Sets the contents bits of this instance. This method does not cause side
     * effects. It switches to named bits mode.
     * 
     * @param bits
     *                The contents bits that are set.
     */
    protected void setBits0(boolean[] bits) {
	namedBits_ = true;

	if ((bits == null) || (bits.length == 0)) {
	    value_ = DEFAULT_VALUE;
	    pad_ = 0;

	    return;
	}
	int i;
	int j;
	int n;
	int k;
	byte m;
	byte[] b;

	/*
	 * We skip trailing zero bits. Trailing bits are to the "right" in ASN.1
	 * terms (bits with high numbers).
	 */
	for (k = bits.length - 1; k >= 0; k--) {
	    if (bits[k]) {
		break;
	    }
	}
	/*
	 * If all bits are zero then we set the default zero and we are done.
	 */
	if (k < 0) {
	    value_ = DEFAULT_VALUE;
	    pad_ = 0;

	    return;
	}
	k++;

	/*
	 * If required we truncate trailing bits. This is compliant to the
	 * X.690/DER encoding rules and saves us trouble in the encoder.
	 */
	b = new byte[(k + 7) / 8];
	m = 0;

	for (n = 0, i = 0; i < k; i++) {
	    j = i & 0x07;

	    if (bits[i]) {
		m = (byte) (m | MASK[j]);
	    }
	    if (j == 7) {
		b[n++] = m;
		m = 0;
	    }
	}
	j = i & 0x07;

	if (j != 0) {
	    b[n] = m;
	}
	value_ = b;
	pad_ = (8 - j) & 0x07;
    }

    /**
     * Sets the bit string from the given byte aray and pad count. Bit 0 is the
     * most significant bit in the first byte of the array and bit <i>n</i> is
     * bit 7-(<i>n</i><code>&amp;0x07</code>) in byte floor(<i>n</i>/8).
     * The length of the bit string is <code>b.length</code>*8-pad. The pad
     * value be in the range of [0..7]. In other words the bits in the byte
     * array are left aligned.
     * <p>
     * 
     * The given byte array may be copied by reference. Subsequent modification
     * of it can cause side effects. The mode is set not to represent named
     * bits.
     * 
     * @param b
     *                The bits encoded into a byte array.
     * @param pad
     *                The number of pad bits after the actual bits in the array.
     * @throws IllegalArgumentException
     *                 if the pad value is out of range.
     * @throws ConstraintException
     *                 if the given bits violates the
     */
    public void setBits(byte[] b, int pad) throws ConstraintException {
	setBits0(b, pad);
	checkConstraints();
    }

    /**
     * Sets the bits and number of trailing pad bits from the given byte array.
     * The given instance is copied by reference. Therefor side effects can
     * occur if the given byte array is modified subsequently. The
     * initialization mode is plain (no named bits).
     * 
     * @param b
     *                The minimum number of bytes to hold the left aligned
     *                contents bits. Any unused trailing bits (as defined by the
     *                pad count) MUST be zero!
     * @param p
     *                The number of trailing padding bits.
     */
    protected void setBits0(byte[] b, int p) {
	int n;

	if ((p < 0) || (p > 7)) {
	    throw new IllegalArgumentException("Illegal pad value (" + p + ")");
	}
	namedBits_ = false;

	/*
	 * We first skip all leading zero bytes.
	 */
	for (n = b.length - 1; n >= 0; n--) {
	    if (b[n] != 0) {
		break;
	    }
	}
	/*
	 * If all bytes are zero then we encode a zero bit string and are done.
	 */
	if (n < 0) {
	    if (p != 0) {
		throw new IllegalArgumentException(
			"Zero length bit strings can't have pad bits!");
	    }
	    value_ = DEFAULT_VALUE;
	    pad_ = 0;

	    return;
	}
	/*
	 * We test whether the trailing pad bits are really zeroes.
	 */
	if ((b[b.length - 1] & ~TRAIL_MASK[p]) != 0) {
	    throw new IllegalArgumentException(
		    "trailing pad bits are not zero!");
	}
	value_ = b;
	pad_ = p;
    }

    /**
     * Returns the contents octets of this instance. The bits are left aligned
     * and the most significant bit is a one (or else the bitstring is zero and
     * an empty array is returned). The returned byte array is the one used
     * internally. Modifying it causes side effects which may result in
     * erroneous encoding. So, don't modify it!
     * <p>
     * 
     * Please also note that the bits in the bytes are left aligned. In other
     * words, the bits are shifted to the left by the amount of pad bits. Bit X
     * in the byte array corresponds to the logical bit with the number X minus
     * pad count.
     * 
     * @return The bits left aligned in a byte array with no trailing zeroes.
     */
    public byte[] getBytes() {
	return value_;
    }

    /**
     * @return The number of unused bits in the last byte of the bitstring's
     *         byte array representation. This number is always in the range
     *         zero to seven.
     */
    public int getPadCount() {
	return pad_;
    }

    /**
     * @return The number of bytes of the bitstring's byte array representation.
     */
    public int byteCount() {
	return value_.length;
    }

    /**
     * Returns the number of bits of the bitstring representation. Bits are
     * counted only to the most significant bit that is a one. Trailing zeroes
     * are neither present in the internal representation nor are they counted.
     * 
     * @return The number of bits of the bitstring representation.
     */
    public int bitCount() {
	return (value_.length * 8) - pad_;
    }

    /**
     * @return <code>true</code> iff this instance has named bits.
     */
    public boolean isNamedBits() {
	return namedBits_;
    }

    /**
     * Returns <code>true</code> if the bit string contains no bits that are
     * 1. Otherwise, <code>false</code> is returned. This method is used by
     * the {@link DEREncoder DEREncoder} in order to determine cases in which
     * special encoding is to be used. If no bits of a BIT STRING are 1 then it
     * is encoded as <tt>0x03 0x01 0x00
     * </tt> even if the BIT STRING has
     * hundreds of bits in length.
     * 
     * @return <code>true</code> if all bits are zero.
     */
    public boolean isZero() {
	return (value_.length == 0);
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public int getTag() {
	return ASN1.TAG_BITSTRING;
    }

    /**
     * DOCUMENT ME!
     * 
     * @param enc
     *                The encoder which is used to encode
     * 
     * @throws ASN1Exception
     *                 DOCUMENT ME!
     * @throws IOException
     *                 if an I/O error occures
     */
    public void encode(Encoder enc) throws ASN1Exception, IOException {
	enc.writeBitString(this);
    }

    /**
     * Decodes this instance. After decoding, this method restores the mode
     * (named bits vs no named bits) of this instance to the one it had before
     * decoding. This is to maintain the original mode while assuring that
     * encoded values are identical to decoded ones even if the encoding.
     * 
     * @param dec
     *                The decoder which is used to decode
     * @throws IOException
     *                 if an I/O error occures
     */
    public void decode(Decoder dec) throws ASN1Exception, IOException {
	boolean tmp;

	tmp = namedBits_;

	try {
	    dec.readBitString(this);
	} finally {
	    namedBits_ = tmp;
	}

	// Redudant, also called by setBits(byte[], int)
	// checkConstraints();
    }

    /**
     * Returns the string representation of this ASN1BitString.
     * 
     * @return the string representation of this ASN1BitString
     */
    public String toString() {
	StringBuffer buf;
	boolean[] bits;
	int i;

	bits = getBits();
	buf = new StringBuffer(12 + bits.length);

	buf.append("BitString '");

	for (i = 0; i < bits.length; i++) {
	    if (bits[i]) {
		buf.append("1");
	    } else {
		buf.append("0");
	    }
	}
	buf.append("'");

	return buf.toString();
    }
}

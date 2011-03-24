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
import java.math.BigInteger;

/**
 * Represents an ASN.1 INTEGER type. The corresponding Java type is
 * java.math.BigInteger.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1Integer.java,v 1.2 2000/12/06 17:47:24 vroth Exp $"
 */
public class ASN1Integer extends ASN1AbstractType {
    /**
     * Holds the octets per integer count. This value is computed by means of
     * static initializer code below.
     */
    private static int opi_;

    /**
     * The value of this ASN.1 INTEGER.
     */
    private BigInteger value_;

    /*
     * Initializes the opi_ field above.
     */
    static {
	int n;
	int i;

	for (n = 1, i = 0; n != 0; n = n << 8) {
	    i++;
	}
	opi_ = i;
    }

    /**
     * Creates a new instance ready for parsing. The value of this instance is
     * set to 0.
     */
    public ASN1Integer() {
	value_ = BigInteger.ZERO;
    }

    /**
     * Creates a new instance with the given BigInteger as its initial value.
     * 
     * @param val
     *                The value.
     */
    public ASN1Integer(BigInteger val) {
	if (val == null)
	    throw new NullPointerException("Need a number!");

	value_ = val;
    }

    /**
     * Creates an ASN.1 INTEGER from the given string representation.
     * 
     * This method calls the equivalent constructor of class
     * {@link java.math.BigInteger java.math.BigInteger}.
     * 
     * @param val
     *                The string representation of the multiple precision
     *                integer.
     * @throws NumberFormatException
     *                 if the string could not be parsed successfully.
     */
    public ASN1Integer(String val) throws NumberFormatException {
	value_ = new BigInteger(val);
    }

    /**
     * Creates a new instance from the given byte array. The byte array contains
     * the two's-complement binary representation of a BigInteger. The input
     * array is assumed to be in <i>big endian</i> byte-order. The most
     * significant byte is in the zeroth element.
     * 
     * This method calls the equivalent constructor of class
     * {@link java.math.BigInteger java.math.BigInteger}.
     * 
     * @param val
     *                The two's-complement input number in big endian
     *                byte-order.
     * @throws NumberFormatException
     *                 if val is zero bytes long.
     */
    public ASN1Integer(byte[] val) throws NumberFormatException {
	value_ = new BigInteger(val);
    }

    /**
     * Translates the sign-magnitude representation of a BigInteger into an
     * ASN.1 INTEGER. The sign is represented as an integer signum value: -1 for
     * negative, 0 for zero, or 1 for positive. The magnitude is a byte array in
     * big-endian byte-order: the most significant byte is in the zeroth
     * element. A zero-length magnitude array is permissible, and will result in
     * in a BigInteger value of 0, whether signum is -1, 0 or 1.
     * <p>
     * 
     * This method calls the equivalent constructor of class
     * {@link java.math.BigInteger java.math.BigInteger}.
     * 
     * @param signum
     *                signum of the number (-1 for negative, 0 for zero, 1 for
     *                positive).
     * @param magnitude
     *                The big endian binary representation of the magnitude of
     *                the number.
     * @throws NumberFormatException
     *                 signum is not one of the three legal values (-1, 0, and
     *                 1), or signum is 0 and magnitude contains one or more
     *                 non-zero bytes.
     */
    public ASN1Integer(int signum, byte[] magnitude)
	    throws NumberFormatException {
	value_ = new BigInteger(signum, magnitude);
    }

    /**
     * Creates an instance with the given int value.
     * 
     * @param n
     *                The integer to initialize with.
     */
    public ASN1Integer(int n) {
	byte[] b;
	int i;
	int m;

	b = new byte[opi_];
	m = n;

	for (i = opi_ - 1; i >= 0; i--) {
	    b[i] = (byte) (m & 0xff);
	    m = m >>> 8;
	}
	value_ = new BigInteger(b);
    }

    public Object getValue() {
	return value_;
    }

    public BigInteger getBigInteger() {
	return value_;
    }

    public void setBigInteger(BigInteger n) throws ConstraintException {
	value_ = n;
	checkConstraints();
    }

    public int getTag() {
	return ASN1.TAG_INTEGER;
    }

    public void encode(Encoder enc) throws ASN1Exception, IOException {
	enc.writeInteger(this);
    }

    public void decode(Decoder dec) throws ASN1Exception, IOException {
	dec.readInteger(this);
	checkConstraints();
    }

    public String toString() {
	return "Integer " + value_.toString();
    }
}

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
 * The root class of all ASN.1 string types including but not limited to
 * IA5String, VisibleString, PrintableString, UTCTime, and GeneralizedTime.
 * <p>
 * 
 * Each string type is encoded as if it is declared as <tt>[UNIVERSAL </tt><i>x</i><tt>] IMPLICIT OCTET STRING</tt>
 * where <i>x</i> is the tag number of the respective string type (see ITU-T
 * Rec. X.690, paragraph 8.20.3).
 * <p>
 * 
 * There are 8 restructed string types of which 4 do not allow escape sequences,
 * namely NumericString, PrintableString, VisibleString (ISO646String) and
 * IA5String. TeletexString (T61String), VideotextString, GraphicString, and
 * GeneralString allow the use of escape sequences. However, the srings must be
 * encoded such as to use the minimum number of octets possible. All these
 * strings use 1-octet representations; IA5String uses 2-octet representations
 * for special characters.
 * <p>
 * 
 * Two unrestricted string types are defined in X.680, namely BMPString and
 * UniversalString. BMPString uses a 2-octet representation per character and
 * UniversalString uses a 4-octet representation.
 * <p>
 * 
 * Each string type represented in this package handles octets to character and
 * character to octets conversion according to the general coding scheme of the
 * particular string, but not neccessarily restriction to a particular character
 * set. This is to be implemented through {@link Constraint constraints} that
 * are added to the respective types on creation (in the constructors).
 * Restriction of character sets is thus done on the Unicode character set used
 * by Java.
 * <p>
 * 
 * This class implements plain 1-octet to character conversion by default. Class
 * {@link ASN1BMPString ASN1BMPString} handles 2-octet conversion and class
 * {@link ASN1UniversalString ASN1UniversalString} handles 4-octets conversion.
 * Without reference to ISO defined character encodings these implementations
 * assume that the <i>n</i>-octet tuples represent the least significant bits
 * of the Unicode characters with the corresponding bits set to zero.
 * 
 * 
 * @author Volker Roth
 * @version "$Id: ASN1AbstractString.java,v 1.4 2004/08/26 15:08:21 pebinger Exp $"
 */
public abstract class ASN1AbstractString extends ASN1AbstractType implements
	ASN1String {

    private static final String DEFAULT_VALUE = "";

    private String value_ = DEFAULT_VALUE;

    public ASN1AbstractString() {
	super();
    }

    /**
     * Creates an instance with the given string value.
     * 
     * This constructor calls {@link #setString setString} to set the string
     * value.
     * 
     * @param s
     *                The string value.
     */
    public ASN1AbstractString(String s) {
	setString0(s);
    }

    /**
     * Returns the represented string value.
     * 
     * @return The string value of this type.
     */
    public Object getValue() {
	return value_;
    }

    /**
     * Returns the represented string value.
     * 
     * @return The string value of this type.
     */
    public String getString() {
	return value_;
    }

    /**
     * Sets the string value.
     * 
     * @param s
     *                The string value.
     * @throws ConstraintException
     *                 if the given string does not match the constraint set for
     *                 this instance.
     */
    public void setString(String s) throws ConstraintException {
	setString0(s);
	checkConstraints();
    }

    protected void setString0(String s) {
	if (s == null)
	    throw new NullPointerException("Need a string!");

	value_ = s;
    }

    public void encode(Encoder enc) throws ASN1Exception, IOException {
	enc.writeString(this);
    }

    public void decode(Decoder enc) throws ASN1Exception, IOException {
	enc.readString(this);
	checkConstraints();
    }

    /**
     * Converts the given byte array to a string by filling up each consecutive
     * byte with 0's to the size of the Unicode characters.
     * 
     * @param b
     *                The byte array to convert.
     * @throws ASN1Exception
     *                 never, only for compliance with the {@link ASN1String}
     *                 interface.
     */
    public String convert(byte[] b) throws ASN1Exception {
	if (b == null)
	    throw new NullPointerException("Cannot convert null array!");

	char[] c = new char[b.length];
	for (int i = 0; i < b.length; i++)
	    c[i] = (char) (b[i] & 0xff);

	return String.valueOf(c);
    }

    /**
     * Converts the given string to a byte array by chopping away all but the
     * least significant byte of each character.
     * 
     * @param s
     *                The string to convert.
     * @throws ASN1Exception
     *                 never, only for compliance with the {@link ASN1String}
     *                 interface.
     */
    public byte[] convert(String s) throws ASN1Exception {
	if (s == null)
	    throw new NullPointerException("Cannot convert null string!");

	char[] c = s.toCharArray();
	byte[] b = new byte[c.length];

	for (int i = 0; i < c.length; i++)
	    b[i] = (byte) (c[i] & 0xff);

	return b;
    }

    /**
     * Returns the number of bytes required to store the converted string.
     * 
     * @param s
     *                The string.
     * @throws ASN1Exception
     *                 never, only for compliance with the {@link ASN1String}
     *                 interface.
     */
    public int convertedLength(String s) throws ASN1Exception {
	return s.length();
    }

    public String toString() {
	String s;
	int n;

	s = getClass().getName();
	n = s.lastIndexOf('.');

	if (n < 0)
	    n = -1;

	s = s.substring(n + 1);
	if (s.startsWith("ASN1"))
	    s = s.substring(4);

	return s + " \"" + value_ + "\"";
    }

    /**
     * Indicates whether some other ASN.1 string is "equal to" this one.
     * 
     * @param s
     *                the reference string with which to compare.
     * @return true if this string is the same as the s argument; false
     *         otherwise.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object s) {
	if (this.getClass().equals(s.getClass())) {
	    return value_.equals(((ASN1AbstractString) s).getString());
	}
	return false;
    }

    /**
     * Returns a hash code value for the object calculated from the contained
     * <code>String</code>.
     * 
     * @return a hash code value for this object.
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
	return (this.getClass().hashCode() + value_.hashCode()) / 2;
    }

}

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
 * Represents an ASN.1 OCTET STRING type. The corresponding Java type is
 * <code>byte[]</code>.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1OctetString.java,v 1.3 2001/01/21 13:46:55 vroth Exp $"
 */
public class ASN1OctetString extends ASN1AbstractType {
    private static final byte[] DEFAULT_VALUE = new byte[0];

    private byte[] value_ = DEFAULT_VALUE;

    public ASN1OctetString() {
    }

    /**
     * Creates an instance with side effects. The given array is copied by
     * reference.
     * 
     * @param b
     *                The byte array that is set as contents.
     */
    public ASN1OctetString(byte[] b) {
	setByteArray0(b);
    }

    public Object getValue() {
	return value_;
    }

    /**
     * Returns the contents octets as a byte array. The returned byte array is
     * is the instance used internally. Do not modify it, otherwise side effects
     * occur.
     * 
     * @return The contents octets as a byte array.
     */
    public byte[] getByteArray() {
	return (byte[]) value_.clone();
    }

    /**
     * Sets the given bytes. The given byte array is copied by reference. Be
     * careful, side effects can occur if the array is modified subsequent to
     * calling this method. Constraints are checked after setting the bytes.
     * 
     * @param b
     *                The byte array that is set.
     * @throws ConstraintException
     *                 if the constraint is not met by the given byte array.
     */
    public void setByteArray(byte[] b) throws ConstraintException {
	setByteArray0(b);
	checkConstraints();
    }

    /**
     * Sets the given bytes. The given byte array is copied by reference. Be
     * careful, side effects can occur if the array is modified subsequent to
     * calling this method.
     * 
     * @param b
     *                The byte array that is set.
     */
    private void setByteArray0(byte[] b) {
	if (b == null)
	    value_ = DEFAULT_VALUE;
	else
	    value_ = b;
    }

    public int byteCount() {
	return value_.length;
    }

    public int getTag() {
	return ASN1.TAG_OCTETSTRING;
    }

    public void encode(Encoder enc) throws ASN1Exception, IOException {
	enc.writeOctetString(this);
    }

    public void decode(Decoder dec) throws ASN1Exception, IOException {
	dec.readOctetString(this);
	checkConstraints();
    }

    public String toString() {
	StringBuffer buf;
	String octet;
	int i;

	buf = new StringBuffer("Octet String");

	for (i = 0; i < value_.length; i++) {
	    octet = Integer.toHexString(value_[i] & 0xff);

	    buf.append(' ');

	    if (octet.length() == 1) {
		buf.append('0');
	    }
	    buf.append(octet);
	}
	return buf.toString();
    }

    /**
     * Returns a clone. The clone is a deep copy of this instance with the
     * exception of constraints. Constraints are copied by reference.
     * 
     * @return The clone.
     */
    public Object clone() {
	ASN1OctetString o;

	try {
	    o = (ASN1OctetString) super.clone();
	} catch (CloneNotSupportedException e) {
	    /*
	     * This cannot, cannot, cannot, CANNOT happen ! If it does, put back
	     * the import statement for 'Cloneable' into ASN1AbstractType !
	     */
	    throw new Error("Internal, clone support mismatch!");
	}
	o.value_ = (byte[]) value_.clone();

	return o;
    }
}

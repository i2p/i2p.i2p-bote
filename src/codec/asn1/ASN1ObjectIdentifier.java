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
import java.util.StringTokenizer;

/**
 * Represents an ASN.1 OBJECT IDENTIFIER type. The corresponding Java type is
 * <code>int[]</code>. Constraints are checked for this type only at the end
 * of method {@link #decode decode}.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1ObjectIdentifier.java,v 1.4 2005/03/22 14:57:58 flautens
 *          Exp $"
 */
public class ASN1ObjectIdentifier extends ASN1AbstractType implements
	Cloneable, Comparable {
    /**
     * holds the int[] representation of the OID
     */
    private int[] value_ = new int[2];

    /**
     * Creates a new ASN1ObjectIdentifier object.
     */
    public ASN1ObjectIdentifier() {
	super();
    }

    /**
     * Creates an instance with the given array of integers as elements. No
     * constraints are checked by this constructor.
     * 
     * @param oid
     *                The array of consecutive integers of the OID.
     * @throws NullPointerException
     *                 if the given <code>oid
     *   </code> is <code>null</code>.
     * @throws IllegalArgumentException
     *                 if the given <code>
     *   oid</code> is not well-formed. For
     *                 instance, a bad <code>oid</code> might have a value
     *                 greater than 2 as its first element.
     */
    public ASN1ObjectIdentifier(int[] oid) {
	set0(oid);
    }

    /**
     * Creates an ASN.1 OBJECT IDENTIFIER instance initialized from the given
     * OID string representation. The format must be either OID.1.2.3.4,
     * oid.1.2.3.4, or 1.2.3.4 for the initiliser to work properly. Trailing
     * dots are ignored.
     * 
     * @param s
     *                string representation of oid
     * @throws NumberFormatException
     *                 if some element of the OID string is not an integer
     *                 number.
     * @throws IllegalArgumentException
     *                 if the string is not a well-formed OID.
     */
    public ASN1ObjectIdentifier(String s) throws NumberFormatException {
	int n;
	int[] oid;
	String t;
	StringTokenizer tok;

	oid = new int[16];

	if (s.startsWith("OID.") || s.startsWith("oid.")) {
	    s = s.substring(4);
	}

	tok = new StringTokenizer(s, ".");

	if (tok.countTokens() >= oid.length) {
	    throw new IllegalArgumentException("OID has too many elements!");
	}

	n = 0;

	while (tok.hasMoreTokens()) {
	    t = tok.nextToken();
	    oid[n++] = Integer.parseInt(t);
	}

	int[] buf = new int[n];
	System.arraycopy(oid, 0, buf, 0, n);
	set0(buf);
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public Object getValue() {
	return value_.clone();
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public int[] getOID() {
	return (int[]) value_.clone();
    }

    /**
     * DOCUMENT ME!
     * 
     * @param oid
     *                DOCUMENT ME!
     * 
     * @throws ConstraintException
     *                 DOCUMENT ME!
     */
    public void setOID(int[] oid) throws ConstraintException {
	set0(oid);
	checkConstraints();
    }

    /**
     * Checks if the given oid is valid and stores the oid. If the oid is not
     * valid the method throws an IllegalArgumentException.
     * 
     * @param oid
     *                The oid to be checked
     */
    private void set0(int[] oid) {
	int n;

	if (oid == null) {
	    throw new NullPointerException("Need an OID!");
	}

	n = oid.length;

	if (n < 2) {
	    throw new IllegalArgumentException(
		    "OID must have at least 2 elements!");
	}

	if ((oid[0] < 0) || (oid[0] > 2)) {
	    throw new IllegalArgumentException("OID[0] must be 0, 1, or 2!");
	}

	if ((oid[1] < 0) || (oid[1] > 39)) {
	    throw new IllegalArgumentException(
		    "OID[1] must be in the range 0,..,39!");
	}

	value_ = new int[n];
	System.arraycopy(oid, 0, value_, 0, n);
    }

    /**
     * Returns the number of elements of the oid.
     * 
     * @return the number of elements
     */
    public int elementCount() {
	return value_.length;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public int getTag() {
	return ASN1.TAG_OID;
    }

    /**
     * Encodes this ASN1ObjectIdentifier.
     * 
     * @param enc
     *                The encoder to encode to.
     * 
     * @throws ASN1Exception
     * @throws IOException
     */
    public void encode(Encoder enc) throws ASN1Exception, IOException {
	enc.writeObjectIdentifier(this);
    }

    /**
     * Decodes to this ASN1ObjectIdentifier.
     * 
     * @param dec
     *                DOCUMENT ME!
     * 
     * @throws ASN1Exception
     * @throws IOException
     */
    public void decode(Decoder dec) throws ASN1Exception, IOException {
	dec.readObjectIdentifier(this);
	checkConstraints();
    }

    /**
     * Returns the string representation of this OID. The string consists of the
     * numerical elements of the OID separated by periods.
     * 
     * @return The string representation of the OID.
     */
    public String toString() {
	StringBuffer buf;
	int i;

	buf = new StringBuffer();

	for (i = 0; i < value_.length; i++) {
	    buf.append(value_[i] + ".");
	}

	if (value_.length > 0) {
	    buf.setLength(buf.length() - 1);
	}

	return buf.toString();
    }

    /**
     * Compares two OIDs for equality. Two OIDs are equal if the have the same
     * number of elements and all corresponding elements are equal.
     * 
     * @param o
     *                The object to compare to.
     * @return <code>true</code> iff the given object is an
     *         ASN1ObjectIdentifier and iff it equals this one.
     */
    public boolean equals(Object o) {
	int i;
	ASN1ObjectIdentifier oid;

	if (!(o instanceof ASN1ObjectIdentifier)) {
	    return false;
	}

	oid = (ASN1ObjectIdentifier) o;
	if (oid.value_.length != value_.length) {
	    return false;
	}

	for (i = 0; i < value_.length; i++) {
	    if (value_[i] != oid.value_[i]) {
		return false;
	    }
	}

	return true;
    }

    /**
     * This method computes the hash code of this instance. The hash code of
     * this instance is defined as a hash function of the underlying integer
     * array.
     * 
     * @return the hash code of this instance.
     */
    public int hashCode() {
	int i;
	int h;

	h = 23;
	for (i = 0; i < value_.length; i++) {
	    h = (h * 7) + value_[i];
	}

	return h;
    }

    /**
     * This method compares two OID and returns -1, 0, 1 if this OID is less
     * than, equal or greater than the given one. OID are interpreted as strings
     * of numbers. An OID that is a prefix of another is always smaller than the
     * other.
     * 
     * @param o
     *                The OID to compare to.
     * @return -1, 0, 1 if this OID is smaller than, equal to, or greater than
     *         the given OID.
     * @throws ClassCastException
     *                 iff <code>o</code> is not an ASN1ObjectIdentifier.
     */
    public int compareTo(Object o) {
	int n;
	int i;
	int[] oid;

	oid = ((ASN1ObjectIdentifier) o).value_;

	n = Math.min(value_.length, oid.length);
	for (i = 0; i < n; i++) {
	    if (value_[i] < oid[i]) {
		return -1;
	    } else if (value_[i] > oid[i]) {
		return 1;
	    }
	}
	if (value_.length > n) {
	    return 1;
	}

	if (oid.length > n) {
	    return -1;
	}

	return 0;
    }

    /**
     * This method determines whether the given OID is part of the OID family
     * defined by this OID prefix. In other words, this method returns
     * <code>true</code> if this OID is a prefix of the given one.
     * 
     * @param o
     *                the oid to check
     * @return true if this OID is a prefix of the given one.
     */
    public boolean isPrefixOf(ASN1ObjectIdentifier o) {
	int i;

	i = value_.length;
	if (o.value_.length < i) {
	    return false;
	}

	while (i > 0) {
	    i--;
	    if (value_[i] != o.value_[i]) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Returns a clone of this instance. This method is not thread safe. The
     * constraints are copied by reference.
     * 
     * @return The clone.
     */
    public Object clone() {
	int[] m;
	ASN1ObjectIdentifier oid;

	oid = new ASN1ObjectIdentifier();
	m = new int[value_.length];
	System.arraycopy(value_, 0, m, 0, m.length);
	oid.value_ = m;

	oid.setConstraint(getConstraint());

	return oid;
    }
}

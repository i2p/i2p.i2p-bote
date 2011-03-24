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
import java.util.ArrayList;
import java.util.Collection;

/**
 * This encoder makes one pass through the given ASN.1 type and computes the
 * length of the type encoding according to the DER (ITU-T Recommendation
 * X.690). The result is an array of integers with the length of the individual
 * non-optional and non-implicit type encodings in the reverse order of the
 * order in which the given type is traversed during actual encoding. This array
 * is used by the {@link DEREncoder DEREncoder} when encoding a type.
 * 
 * @author Volker Roth
 * @version "$Id: RunLengthEncoder.java,v 1.2 2000/12/06 17:47:29 vroth Exp $"
 */
public class RunLengthEncoder extends Object implements Encoder {
    /**
     * The number of slots by which the internal buffer is incremented if its
     * capacity is eexceeded.
     */
    public static final int INCREMENT = 256;

    private int[] stack_;
    private int tops_;

    private int[] acc_;
    private int topa_;

    /**
     * Creates an encoder.
     */
    public RunLengthEncoder() {
    }

    /**
     * This method brings in the harvest of the encoding procedure. It returns
     * the individual lengths of the DER encodings of the types written to to
     * this encoder. The order of length fields is the reverse order of the pre
     * order parsing of the written types.
     * <p>
     * 
     * If this method is called before a type has been encoded then an array of
     * zero length is returned. Only non-optional types are counted thus the
     * zero length array might be returned also when all encoded types were
     * declared optional.
     * 
     * @return The lengths fields.
     */
    public int[] getLengthFields() {
	if (tops_ == 0)
	    return new int[0];

	int[] res;

	res = new int[tops_];
	System.arraycopy(stack_, 0, res, 0, tops_);
	return res;
    }

    /**
     * Encodes the length array of the given type.
     */
    public void writeType(ASN1Type o) throws ASN1Exception {
	try {
	    o.encode(this);
	} catch (IOException e) {
	    throw new ASN1Exception("Caught IOException without I/O!");
	}
    }

    /**
     * This method computes the number of octets needed to encode the identifier
     * and length octets of the {@link ASN1Type ASN.1 type} with the given tag
     * and contents length. The length must not be negative else an exception is
     * thrown. Since this encoder is meant to work in conjunction with a
     * {@link DEREncoder DEREncoder} no indefinite length is supported.
     * 
     * @return The number of octets required for encoding the identifier and
     *         length octets.
     * @param tag
     *                The ASN.1 tag.
     * @param len
     *                The number of contents octets of the ASN.1 type with the
     *                given tag and length.
     * @throws ASN1Exception
     *                 if the given length is negative.
     */
    public int getHeaderLength(int tag, int len) throws ASN1Exception {
	int n;

	if (len < 0)
	    throw new ASN1Exception("Length is negative!");

	n = 2;
	if (tag > 30)
	    n = n + (significantBits(tag) + 6) / 7;

	if (len > 127)
	    n = n + (significantBits(len) + 7) / 8;

	return n;
    }

    /**
     * Counts the number of significant bits in the given integer. There is
     * always at least one significant bit.
     * 
     * @param n
     *                The integer.
     * @return The number of significant bits in the given integer.
     */
    protected int significantBits(int n) {
	int i;

	if (n == 0)
	    return 1;

	i = 0;
	while (n > 255) {
	    n = n >>> 8;
	    i += 8;
	}
	while (n > 0) {
	    n = n >>> 1;
	    i++;
	}
	return i;
    }

    public void writeBoolean(ASN1Boolean t) throws ASN1Exception {
	if (t.isOptional())
	    return;

	push(t, 1);
    }

    public void writeInteger(ASN1Integer t) throws ASN1Exception {
	if (t.isOptional())
	    return;

	int n;

	n = t.getBigInteger().bitLength() / 8 + 1;
	push(t, n);
    }

    public void writeBitString(ASN1BitString t) throws ASN1Exception {
	if (t.isOptional())
	    return;

	int n;

	if (t.isZero())
	    n = 1;
	else
	    n = (t.bitCount() + 7) / 8 + 1;

	push(t, n);
    }

    public void writeOctetString(ASN1OctetString t) throws ASN1Exception {
	if (t.isOptional())
	    return;

	push(t, t.byteCount());
    }

    public void writeNull(ASN1Null t) throws ASN1Exception {
	if (t.isOptional())
	    return;

	push(t, 0);
    }

    public void writeObjectIdentifier(ASN1ObjectIdentifier t)
	    throws ASN1Exception {
	if (t.isOptional())
	    return;

	int n;
	int i;
	int[] e;

	e = t.getOID();
	if (e.length < 2)
	    throw new ASN1Exception("OID must have at least 2 elements!");

	for (n = 1, i = 2; i < e.length; i++)
	    n = n + (significantBits(e[i]) + 6) / 7;

	push(t, n);
    }

    public void writeReal(ASN1Type t) {
	throw new UnsupportedOperationException("Real is not yet supported!");
    }

    public void writeString(ASN1String t) throws ASN1Exception {
	if (t.isOptional())
	    return;

	push(t, t.convertedLength(t.getString()));
    }

    public void writeCollection(ASN1Collection t) throws ASN1Exception {
	if (t.isOptional())
	    return;

	int n;
	int p;
	int i;
	ArrayList l;
	Collection c;

	c = t.getCollection();
	if (c instanceof ArrayList)
	    l = (ArrayList) c;
	else {
	    l = new ArrayList(c.size());
	    l.addAll(c);
	}
	try {
	    for (p = sp(), i = l.size() - 1; i >= 0; i--)
		writeType((ASN1Type) l.get(i));

	    n = accumulate(p);
	    push(t, n);
	} catch (ClassCastException e) {
	    throw new ASN1Exception("Non-ASN.1 type in collection!");
	}
    }

    public void writeCollectionOf(ASN1Collection t) throws ASN1Exception {
	writeCollection(t);
    }

    public void writeTime(ASN1Time t) throws ASN1Exception {
	writeString(t);
    }

    public void writeTaggedType(ASN1TaggedType t) throws ASN1Exception {
	if (t.isOptional())
	    return;

	int n;
	int p;

	p = sp();
	writeType(t.getInnerType());
	n = accumulate(p);
	push(t, n);
    }

    public void writeTypeIdentifier(ASN1Type t) {
	throw new UnsupportedOperationException(
		"TypeIdentifier is not yet supported!");
    }

    /**
     * Clears the length array and prepares this encoder for a new run.
     */
    protected void reset() {
	tops_ = 0;
	topa_ = 0;
    }

    /**
     * Pushes another length integer onto the internal stacks. The value is
     * pushed both on the running stack as well as on the accumulator stack. The
     * stacks increase dynamically in size in chunks of
     * {@link #INCREMENT INCREMENT} integers and never shrink in capacity.
     * 
     * @param t
     *                The ASN.1 type.
     * @param n
     *                The integer.
     */
    protected void push(ASN1Type t, int n) throws ASN1Exception {
	if (stack_ == null) {
	    stack_ = new int[INCREMENT];
	    tops_ = 0;
	}
	if (tops_ == stack_.length) {
	    int[] stack;

	    stack = new int[stack_.length + INCREMENT];
	    System.arraycopy(stack_, 0, stack, 0, stack_.length);
	    stack_ = stack;
	}
	if (acc_ == null) {
	    acc_ = new int[INCREMENT];
	    topa_ = 0;
	}
	if (topa_ == acc_.length) {
	    int[] stack;

	    stack = new int[acc_.length + INCREMENT];
	    System.arraycopy(acc_, 0, stack, 0, acc_.length);
	    acc_ = stack;
	}
	if (t.isExplicit()) {
	    stack_[tops_++] = n;
	    acc_[topa_++] = n + getHeaderLength(t.getTag(), n);
	} else
	    acc_[topa_++] = n;
    }

    /**
     * Returns the accumulator stack pointer.
     * 
     * @return The accumulator stack pointer.
     */
    protected int sp() {
	return topa_;
    }

    /**
     * Accumulates all values on the accumulator stack from the given position
     * to the top of the stack and returns the result. All accumulated values
     * are popped off the stack.
     * 
     * @param pos
     *                The position to start from.
     * @throws IllegalStateException
     *                 if the given position is atop the top of the stack.
     */
    protected int accumulate(int pos) {
	int n;
	int i;

	if (pos > topa_)
	    throw new IllegalStateException(
		    "Internal error, bad stack pointer!");

	for (n = 0, i = pos; i < topa_; i++)
	    n = n + acc_[i];

	topa_ = pos;
	return n;
    }
}

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * If ,right after instantiating the encoder, the strict parameter is set to
 * true, the encoder uses the strict encoding rules from X680/X690 for Sets and
 * SetOfs. As long as the flag is not set, the encoder behaves as usual.
 * 
 * @author Volker Roth
 * @version "$Id: DEREncoder.java,v 1.7 2004/08/09 11:10:08 flautens Exp $"
 */
public class DEREncoder extends AbstractEncoder {
    private int[] stack_;
    private int sp_;

    /**
     * if true, the strict encoding rules for sets and set of are used.
     */
    private boolean strict = false;

    /**
     * This variable has a bit set for each tag that denotes a CONSTRUCTED type.
     */
    private int constructed_ = ((1 << ASN1.TAG_SEQUENCE) | (1 << ASN1.TAG_SET) | (1 << ASN1.TAG_REAL));

    /**
     * Creates an encoder that writes its output to the given output stream.
     * 
     * @param out
     *                The output stream to which the encoded ASN.1 objects are
     *                written.
     */
    public DEREncoder(OutputStream out) {
	super(out);
    }

    /**
     * returns the value of the strict parameter.
     */
    public boolean isStrict() {
	return this.strict;
    }

    /**
     * Sets the value of the strict parameter.
     */
    public void setStrict(boolean _strictness) {
	this.strict = _strictness;
    }

    /**
     * Encodes the identifier and length octets. If there are no known lengths
     * then this method creates and runs a
     * {@link RunLengthEncoder RunLengthEncoder} on the given type in order to
     * establish the length of it and of any contained types. This method must
     * not be called with OPTIONAL types else errors may occur. It is the
     * responsibility of the caller to ascertain this precondition. Only the
     * headers of types that are tagged {@link ASN1Type#isExplicit EXPLICIT} are
     * encoded. If the given type is tagged IMPLICIT then this method simply
     * returns.
     * 
     * @param t
     *                The type of which the header is encoded.
     * @param primitive
     *                <code>true</code> if the encoding is PRIMITIVE and
     *                <code>false</code> if it is CONSTRUCTED.
     */
    protected void writeHeader(ASN1Type t, boolean primitive)
	    throws ASN1Exception, IOException {
	int length;

	if (!t.isExplicit()) {
	    return;
	}
	if (stack_ == null || sp_ == 0) {
	    RunLengthEncoder enc;

	    enc = new RunLengthEncoder();
	    enc.writeType(t);
	    stack_ = enc.getLengthFields();
	    sp_ = stack_.length;

	    if (sp_ < 1) {
		throw new ASN1Exception("Cannot determine length!");
	    }
	}
	length = stack_[--sp_];

	writeHeader(t.getTag(), t.getTagClass(), primitive, length);
    }

    public void writeBoolean(ASN1Boolean t) throws ASN1Exception, IOException {
	if (t.isOptional())
	    return;

	writeHeader(t, true);
	write(t.isTrue() ? 0xff : 0x00);
    }

    public void writeInteger(ASN1Integer t) throws ASN1Exception, IOException {
	if (t.isOptional())
	    return;

	writeHeader(t, true);
	write(t.getBigInteger().toByteArray());
    }

    public void help(byte[] b) {
	int i;

	for (i = 0; i < b.length; i++) {
	    System.err.println("  " + (b[i] & 0xff));
	}
    }

    /**
     * Encodes a bitstring. This method expects that the bitstring instance is
     * already compact. In other words, it does not contain trailing zeroes and
     * the pad count is in the range of zero to seven.
     */
    public void writeBitString(ASN1BitString t) throws ASN1Exception,
	    IOException {
	if (t.isOptional()) {
	    return;
	}
	writeHeader(t, true);
	write(t.getPadCount());

	if (!t.isZero()) {
	    write(t.getBytes());
	}
    }

    public void writeOctetString(ASN1OctetString t) throws ASN1Exception,
	    IOException {
	if (t.isOptional())
	    return;

	writeHeader(t, true);
	write(t.getByteArray());
    }

    public void writeNull(ASN1Null t) throws ASN1Exception, IOException {
	if (t.isOptional())
	    return;

	writeHeader(t, true);
    }

    public void writeObjectIdentifier(ASN1ObjectIdentifier t)
	    throws ASN1Exception, IOException {
	if (t.isOptional())
	    return;

	writeHeader(t, true);

	int i;
	int[] e;

	e = t.getOID();
	if (e.length < 2)
	    throw new ASN1Exception("OID must have at least 2 elements!");

	write(e[0] * 40 + e[1]);
	for (i = 2; i < e.length; i++)
	    writeBase128(e[i]);
    }

    public void writeReal(ASN1Type t) {
	throw new UnsupportedOperationException("Real is not yet supported!");
    }

    public void writeString(ASN1String t) throws ASN1Exception, IOException {
	if (t.isOptional())
	    return;

	writeHeader(t, true);
	write(t.convert(t.getString()));
    }

    public void writeCollection(ASN1Collection t) throws ASN1Exception,
	    IOException {
	if (t.isOptional())
	    return;

	Iterator i;
	Collection c;
	// Collection h;
	ArrayList h;
	writeHeader(t, false);

	c = t.getCollection();

	if (isStrict() && t instanceof ASN1SetOf) {
	    // von VR war writeStrictSetOf(t);
	    writeStrictSetOf((ASN1SetOf) t);
	    return;
	}
	if (isStrict() && t instanceof ASN1Set) {
	    h = new ArrayList(c.size());

	    h.addAll(c);
	    Collections.sort(h, new ASN1TagComparator());

	    c = h;
	}
	try {
	    for (i = c.iterator(); i.hasNext();)
		writeType((ASN1Type) i.next());
	} catch (ClassCastException e) {
	    throw new ASN1Exception("Non-ASN.1 type in collection!");
	}
    }

    /**
     * This method fixes the DER SET OF strict encoding issue brought up by
     * pea-counter implementations of decoders that insist on sorting the SET OF
     * components by encoding -- something no sane implementation requires.
     * 
     * The method of implementation is basically a hack. Don't take this as a
     * good programming pattern. Moreover, this implementation becomes
     * completely and utterly THREAD UNSAFE. Not that this would mater too much.
     */
    protected void writeStrictSetOf(ASN1SetOf t) throws ASN1Exception,
	    IOException {
	ByteArrayOutputStream bos;
	OutputStream old;
	Collection c;
	ArrayList res;
	Iterator i;
	byte[] buf;

	/*
	 * Prepare working variables
	 */
	c = t.getCollection();
	res = new ArrayList(c.size());
	bos = new ByteArrayOutputStream();

	/*
	 * Safe the original output stream and replace it with our byte array
	 * output stream.
	 */
	old = super.out;
	super.out = bos;

	try {
	    /*
	     * Go ahead and encode everything into byte arrays.
	     */
	    for (i = c.iterator(); i.hasNext();) {
		writeType((ASN1Type) i.next());

		/*
		 * If something has been encoded then we safe it for later
		 * sorting.
		 */
		if (bos.size() > 0) {
		    res.add(bos.toByteArray());
		    bos.reset();
		}
	    }
	} finally {
	    /*
	     * Restore the original output stream.
	     */
	    super.out = old;
	}
	/*
	 * Now, we sort the different codes and write them
	 */
	Collections.sort(res, new DERCodeComparator());

	for (i = res.iterator(); i.hasNext();) {
	    buf = (byte[]) i.next();

	    write(buf, 0, buf.length);
	}
    }

    public void writeTime(ASN1Time t) throws ASN1Exception, IOException {
	writeString(t);
    }

    public void writeTaggedType(ASN1TaggedType t) throws ASN1Exception,
	    IOException {
	if (t.isOptional())
	    return;

	boolean primitive;
	ASN1Type o;
	int tag;

	o = t.getInnerType();

	if (!o.isExplicit()) {
	    if (t instanceof ASN1Opaque)
		tag = t.getTag();
	    else
		tag = o.getTag();

	    primitive = ((constructed_ & (1 << tag)) == 0);
	} else
	    primitive = false;

	writeHeader(t, primitive);
	writeType(t.getInnerType());
    }

    public void writeTypeIdentifier(ASN1Type t) {
	throw new UnsupportedOperationException(
		"TypeIdentifier is not yet supported!");
    }

    public void write(byte[] b) throws IOException {
	out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
	out.write(b, off, len);
    }

    /**
     * Writes an arbitrary {@link ASN1Type ASN1Type}. The given type is written
     * only if it is not declared OPTIONAL. The type is written by calling its
     * {@link ASN1Type#encode encode} method with <code>this</code> as the
     * argument. The called emthod then should invoke the appropriate encoder
     * method of the primitive type to which the given type corresponds.
     * 
     * @param t
     *                The type to write.
     * @throws ASN1Exception
     *                 if the given type cannot be encoded.
     * @throws IOException
     *                 if an I/O error occurs.
     */
    public void writeType(ASN1Type t) throws ASN1Exception, IOException {
	if (!t.isOptional())
	    t.encode(this);
    }

    /**
     * This method encodes identifier and length octets. The given length can be
     * negative in which case 0x80 is written to indicate INDEFINITE LENGTH
     * encoding. Please note that this is appropriate only for a BER encoding or
     * CER encoding (ITU-T Recommenation X.690). Encoders are responsible for
     * writing the end of code octets <code>0x00 0x00</code> after encoding
     * the content octets.
     * 
     * @param tag
     *                The ASN.1 tag
     * @param cls
     *                The ASN.1 tag class.
     * @param prim
     *                <code>true</code> if the encoding is PRIMITIVE and
     *                <code>false</code> if it is CONSTRUCTED.
     * @param len
     *                The number of content octets or -1 to indicate INDEFINITE
     *                LENGTH encoding.
     */
    protected void writeHeader(int tag, int cls, boolean prim, int len)
	    throws IOException {
	int b, i;

	b = cls & ASN1.CLASS_MASK;

	if (!prim)
	    b = b | ASN1.CONSTRUCTED;

	if (tag > 30) {
	    b = b | ASN1.TAG_MASK;
	    out.write(b);
	    writeBase128(tag);
	} else {
	    b = b | tag;
	    out.write(b);
	}
	if (len == -1)
	    out.write(0x80);
	else {
	    if (len > 127) {
		i = (significantBits(len) + 7) / 8;
		out.write(i | 0x80);
		writeBase256(len);
	    } else
		out.write(len);
	}
    }

    /**
     * This method computes the number of octets needed to encode the identifier
     * and length octets of the {@link ASN1Type ASN.1 type} with the given tag
     * and contents length. The given length can be negative in which case
     * INDEFINITE LENGTH encoding is assumed.
     * 
     * @return The number of octets required for encoding the identifier and
     *         length octets.
     * @param tag
     *                The ASN.1 tag.
     * @param len
     *                The number of contents octets of the ASN.1 type with the
     *                given tag and length.
     */
    protected int getHeaderLength(int tag, int len) {
	int n;

	n = 2;
	if (tag > 30)
	    n = n + (significantBits(tag) + 6) / 7;

	if (len > 127)
	    n = n + (significantBits(len) + 7) / 8;

	return n;
    }

    /**
     * Writes the given integer to the output in base 128 representation with
     * bit 7 of all octets except the last one being set to &quot;1&quot;. The
     * minimum number of octets necessary is used.
     * 
     * @param n
     *                The integer to be written to the output.
     * @throws IOException
     *                 Thrown by the underlying output stream.
     */
    protected void writeBase128(int n) throws IOException {
	int i, j;

	i = (significantBits(n) + 6) / 7;
	j = (i - 1) * 7;

	while (i > 1) {
	    out.write(((n >>> j) & 0x7f) | 0x80);
	    j = j - 7;
	    i--;
	}
	out.write(n & 0x7f);
    }

    /**
     * Writes the given integer to the output in base 256 with the minimal
     * number of octets.
     * 
     * @param n
     *                The integer to be written to the output.
     * @throws IOException
     *                 Thrown by the underlying output stream.
     */
    protected void writeBase256(int n) throws IOException {
	int i, j;

	i = (significantBits(n) + 7) / 8;
	j = (i - 1) * 8;

	while (i > 0) {
	    out.write((n >>> j) & 0xff);
	    j = j - 8;
	    i--;
	}
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
}

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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Iterator;

/**
 * Decodes ASN.1/DER encoded types according to the rules set forth in ITU-T
 * Recommendation X.690.
 * <p>
 * 
 * Decoders can be operated in two modi. The first mode just reads any ASN.1
 * type encountered in a stream and returns the instantiated objects. This mode
 * is used if for instance method {@link #readType() readType()} is called.
 * <p>
 * 
 * The second mode matches the decoded data against an application-specified
 * ASN.1 structure. Violations of the structure definition causes an exception
 * being thrown.
 * 
 * @author Volker Roth
 * @version "$Id: DERDecoder.java,v 1.7 2004/08/09 08:02:10 flautens Exp $"
 */
public class DERDecoder extends FilterInputStream implements Decoder {
    protected int tag_;
    protected int tagclass_;
    protected int length_;
    protected boolean indefinite_;
    protected boolean primitive_;
    protected boolean skip_ = false;

    /**
     * The number of octets read from the stream.
     */
    protected int pos_ = 0;

    /**
     * The marked position in the stream. Marking is not required by this
     * decoder anymore.
     */
    protected int markpos_ = 0;

    /**
     * The maximum allowed length of the input. This is to prevent DoS attacks
     * by forging ASN.1/DER structures with insane length octets.
     */
    protected int limit_;

    /**
     * A buffer for decoding OIDs. At most 32 OID elements are supported.
     */
    protected int[] oidbuf_ = new int[32];

    /**
     * A debug switch.
     */
    protected boolean debug_ = false;

    /**
     * The array holding the suffixes of the classes that implement ASN.1
     * encoding and decoding, indexed by tag number. The array
     * {@link #typeclass_ typeclass_} is computed from these names.
     */
    static private String[] typename_ = { null, // [0]
	    "Boolean", // [1]
	    "Integer", // [2]
	    "BitString", // [3]
	    "OctetString", // [4]
	    "Null", // [5]
	    "ObjectIdentifier", // [6]
	    null, // [7]
	    null, // [8]
	    null, // [9]
	    "Enumerated", // [10]
	    null, // [11]
	    "UTF8String", // [12]
	    null, // [13]
	    null, // [14]
	    null, // [15]
	    "Sequence", // [16]
	    "Set", // [17]
	    null, // [18]
	    "PrintableString", // [19]
	    "T61String", // [20]
	    null, // [21]
	    "IA5String", // [22]
	    "UTCTime", // [23]
	    "GeneralizedTime", // [24]
	    null, // [25]
	    "VisibleString", // [26]
	    null, // [27]
	    "UniversalString", // [28]
	    null, // [29]
	    "BMPString" // [30]
    };

    /**
     * The array of ASN.1 classes implementing the primitive ASN.1 types indexed
     * by tag number. A <code>null</code> in the array denotes that no
     * implementing class is available for the requested tag number.
     */
    static private Class[] typeclass_ = getClasses();

    /**
     * Initializes the static lookup table for mapping ASN1 tags onto
     * implementing classes.
     * 
     * @return The class array of implementing classes.
     */
    static private Class[] getClasses() {
	int n, i;
	String s;
	Class[] c;

	s = DERDecoder.class.getName();
	i = s.lastIndexOf('.');
	if (i < 0)
	    s = "";
	else
	    s = s.substring(0, i) + ".ASN1";

	n = typename_.length;
	c = new Class[n];

	for (i = 0; i < n; i++) {
	    if (typename_[i] != null) {
		try {
		    c[i] = Class.forName(s + typename_[i]);
		} catch (ClassNotFoundException e) {
		    c[i] = null;
		}
	    } else
		c[i] = null;
	}
	return c;
    }

    /**
     * Returns the class that is representing the ASN.1 type with the given tag.
     * If no class is known for this tag then an exception is thrown.
     * 
     * @param tag
     *                The ASN.1 tag of the class to be returned.
     * @return The class implementing the ASN.1 type with the given tag.
     * @throws ASN1Exception
     *                 if no implementing class is known for the given tag.
     * @throws IllegalArgumentException
     *                 if the given tag is negative.
     */
    static public Class getClass(int tag) throws ASN1Exception {
	Class cls;

	if (tag < 0)
	    throw new IllegalArgumentException("Tag number is negative!");

	if (tag < typeclass_.length) {
	    cls = typeclass_[tag];
	    if (cls != null)
		return cls;
	}
	throw new ASN1Exception("Unknown tag! (" + tag + ")");
    }

    /**
     * Creates an instance that reads from the given input stream.
     * 
     * @param in
     *                The input stream to read from.
     */
    public DERDecoder(InputStream in) {
	super(in);
    }

    /**
     * Creates an instance that reads from the given input stream, and enforces
     * the given maximum number of input octets to the decoder.
     * 
     * @param in
     *                The input stream to read from.
     * @param limit
     *                The maximum number of octets allowed in the input stream.
     *                A value of 0 disables checking of upper limits.
     */
    public DERDecoder(InputStream in, int limit) {
	super(in);

	setInputLimit(limit);
    }

    /**
     * Sets the maximum number of input octets read from the underlying stream.
     * A value of 0 disables limits checking. If a limit is set and the decoder
     * detects that the length of a read type wil exceed the limit then an
     * exception will be thrown.
     * <p>
     * 
     * Setting limits prevents DoS attacks on cryptographic services that handle
     * ASN.1/DER/BER input. Such code might be tricked into allocating insane
     * amounts of buffer memory to decode a structure with forged length octets,
     * which might result in out of memory errors. Such code should enforce a
     * reasonable upper bound on the size of processed input structures.
     * 
     * @param limit
     *                The maximum number of octets allowed when readinginput.
     */
    public void setInputLimit(int limit) {
	if (limit < 0) {
	    throw new IllegalArgumentException("No negative limits allowed!");
	}
	limit_ = limit;
    }

    /**
     * @return The maximum number of octets the decoded structures are allowed
     *         to have.
     */
    public int getInputLimit() {
	return limit_;
    }

    public void setDebug(boolean debug) {
	debug_ = debug;
    }

    public boolean isDebug() {
	return debug_;
    }

    /**
     * Reads the next identifier and length octets from the stream and decodes
     * them if {@link #skip_ skip_} is <code>
     * false</code>.
     * <p>
     * 
     * The resulting values are written to {@link #tag_ tag_},
     * {@link #tagclass_ tagclass_}, {@link #primitive_ primitive_},
     * {@link #length_ length_}, and {@link #indefinite_ indefinite_}.
     * <p>
     * 
     * If the stream is at its end then <code>false</code> is returned and
     * <code>true</code> else. If {@link #skip_ skip_} is <code>true</code>
     * then nothing is read and <code>true</code> is returned.
     * <p>
     * 
     * This method does not enforce DER, it can be used to parse BER as well. In
     * other words, calling methods have to check for indefinite length
     * encodings.
     * 
     * @return <code>true</code> if the next header was read.
     */
    protected boolean readNext() throws ASN1Exception, IOException {
	int n;
	int m;
	int j;

	/*
	 * For debugging purposes.
	 */
	j = pos_;

	if (skip_) {
	    if (debug_) {
		System.out.println("(" + j + ")\tSkipping.");
	    }
	    skip_ = false;
	    return true;
	}
	n = read();

	if (n < 0) {
	    /*
	     * Invalidate the current tag. This is required e.g. for detecting
	     * missing EOC in indefinite length encodings.
	     */
	    tag_ = -1;
	    return false;
	}
	if ((n & ASN1.CONSTRUCTED) == 0) {
	    primitive_ = true;
	} else {
	    primitive_ = false;
	}
	tagclass_ = n & ASN1.CLASS_MASK;

	if ((n & ASN1.TAG_LONGFORM) == ASN1.TAG_LONGFORM) {
	    tag_ = readBase128();
	} else {
	    tag_ = n & ASN1.TAG_MASK;
	}
	n = read();

	if (n < 0) {
	    throw new ASN1Exception("Unexpected EOF, length missing!");
	}
	indefinite_ = false;
	m = n & ASN1.LENGTH_MASK;

	if ((n & ASN1.LENGTH_LONGFORM) == ASN1.LENGTH_LONGFORM) {
	    if (m == 0) {
		indefinite_ = true;
	    } else {
		m = readBase256(m);
	    }
	}
	length_ = m;

	if (length_ < 0) {
	    throw new ASN1Exception("Negative length: " + length_);
	}
	/*
	 * Here, we check if the current position plus the given length exceeds
	 * the allowed maximum of input. This is to prevent DoS attacks by means
	 * of forged DER code. Such code could trick the ASN.1 classes into
	 * allocating insane amounts of memory for ASN.1 structures, hence
	 * causing out of memory errors. Limits are enforced only if a limit
	 * greater 0 is set.
	 */
	if (limit_ > 0) {
	    m = pos_ + length_ - limit_;

	    if (m > 0) {
		throw new ASN1Exception("Maximum input limit violated by " + m
			+ " octets!");
	    }
	}
	if (primitive_ && indefinite_) {
	    throw new ASN1Exception(
		    "Encoding can't be PRIMITIVE and INDEFINITE LENGTH!");
	}
	// DEBUG
	{
	    if (debug_)
		debugHeader(j);
	}
	return true;
    }

    private void debugHeader(int offset) {
	StringBuffer sb;
	String s;
	String t;

	sb = new StringBuffer();
	sb.append("(" + offset + ")\t");

	switch (tagclass_) {
	case ASN1.CLASS_UNIVERSAL:
	    t = "UNIVERSAL";
	    break;
	case ASN1.CLASS_PRIVATE:
	    t = "PRIVATE";
	    break;
	case ASN1.CLASS_CONTEXT:
	    t = "CONTEXT SPECIFIC";
	    break;
	case ASN1.CLASS_APPLICATION:
	    t = "APPLICATION";
	    break;
	default:
	    t = "*INTERNAL ERROR*";
	}
	if (tagclass_ == ASN1.CLASS_UNIVERSAL && tag_ < typename_.length) {
	    s = typename_[tag_];
	    if (s == null) {
		sb.append("[UNIVERSAL " + tag_ + "] ");
	    } else {
		sb.append(s + " ");
	    }
	} else {
	    sb.append("[" + t + " " + tag_ + "] ");
	}
	sb.append((primitive_) ? "PRIMITIVE " : "CONSTRUCTED ");
	sb.append("length: ");
	if (indefinite_) {
	    sb.append("indefinite");
	} else {
	    sb.append(length_);
	}
	System.out.println(sb.toString());
    }

    /**
     * Reads the next header from the stream and matches the given encoding type
     * flag and the tag and tag class of the given type against the the current
     * header.
     * <p>
     * 
     * If the given type is tagged IMPLICIT then only then encoding type
     * (PRIMITIVE vs. CONSTRUCTED) is verified to the current header. In any
     * case will the skipping flag be cleared by this method. This flag can be
     * manipulated with method {@link #skipNext skipNext(boolean)}.
     * 
     * Exceptions are thrown in cases of mismatches and if the end of the stream
     * is reached.
     * 
     * @param t
     *                The type to match.
     * @param primitive
     *                A flag saying whether the expected encoding is PRIMITIVE (<code>true</code>)
     *                or CONSTRUCTED (<code>false</code>).
     * @throws EOFException
     *                 if no more types are available because the end of the
     *                 encoding is reached.
     * @throws ASN1Exception
     *                 if a type mismatch is detected.
     */
    protected void match0(ASN1Type t, boolean primitive) throws ASN1Exception,
	    IOException {

	if (!t.isExplicit()) {
	    if (primitive != primitive_) {
		throw new ASN1Exception("PRIMTIVE vs. CONSTRUCTED mismatch!");
	    }
	    /*
	     * Now this is nasty. If the tagging is IMPLICIT and the skipping
	     * flag is set then this flag has to be cleared since the identifier
	     * octets must be read implicitely with the data.
	     */
	    skipNext(false);

	    return;
	}
	if (!readNext()) {
	    throw new EOFException("End of stream reached!");
	}
	if (t.isType(tag_, tagclass_)) {
	    if (primitive != primitive_) {
		throw new ASN1Exception("CONSTRUCTED vs. PRIMITIVE mismatch!");
	    }
	    return;
	}
	throw new ASN1Exception("Type mismatch!");
    }

    /**
     * Reads the next header from the stream and matches the tag and tag class
     * of the given type against the current header. Exceptions are thrown in
     * the case of a mismatch. If the tagging type is not EXPLICIT then this
     * method returns silently. In any case will the skipping flag be cleared by
     * this method. This flag is manipulated by method
     * {@link #skipNext skipNext(boolean)}.
     * 
     * @param t
     *                The type to match.
     * @throws EOFException
     *                 if no more types are available because the end of the
     *                 encoding is reached.
     * @throws ASN1Exception
     *                 if a type mismatch is detected.
     */
    protected void match1(ASN1Type t) throws ASN1Exception, IOException {
	if (!t.isExplicit()) {
	    /*
	     * Now this is nasty. If the tagging is IMPLICIT and the skipping
	     * flag is set then this flag has to be cleared since the identifier
	     * octets must be read implicitely with the data.
	     */
	    skipNext(false);

	    return;
	}
	if (!readNext()) {
	    throw new EOFException("End of stream reached!");
	}
	if (t.isType(tag_, tagclass_)) {
	    return;
	}
	throw new ASN1Exception("Type mismatch!");
    }

    /**
     * Reads the next header from the stream and matches it against the given
     * tag and tag class.
     * 
     * @param tag
     *                The tag to match.
     * @param tagclass
     *                The tag class to match.
     * @throws ASN1Exception
     *                 if a mismatch is detected.
     * @throws EOFException
     *                 if the end of the stream is reached.
     */
    protected void match2(int tag, int tagclass) throws IOException,
	    ASN1Exception {
	if (!readNext()) {
	    throw new EOFException("End of stream reached!");
	}
	if (tag != tag_ || tagclass != tagclass_) {
	    throw new ASN1Exception("Type mismatch!");
	}
	return;
    }

    /**
     * Sets a flag indicating whether reading of the next header should be
     * skipped. This is required for decoding types that are tagged IMPLICIT.
     * 
     * @param skip
     *                <code>true</code> iff the next call to
     *                {@link #readNext readNext} shall be skipped.
     */
    protected void skipNext(boolean skip) {
	skip_ = skip;
    }

    /**
     * Reads the next ASN.1 type in the stream. If the next type is end-of-code
     * (EOC) then <code>null</code> is returned. EOC marks the end of
     * indefinite length encodings.
     * 
     * @return The ASN.1 type read from the stream or <code>null</code> if the
     *         type is EOC.
     */
    public ASN1Type readType() throws ASN1Exception, IOException {
	if (!readNext()) {
	    throw new EOFException("End of encoding reached!");
	}
	if (tag_ == 0 && tagclass_ == 0) {
	    if (length_ != 0) {
		throw new ASN1Exception("EOC with non-zero length!");
	    }
	    return null;
	}
	if (tagclass_ != ASN1.CLASS_UNIVERSAL) {
	    ASN1OctetString o;
	    ASN1TaggedType t;

	    /*
	     * If we encountered a non-UNIVERSAL type with INDEFINITE LENGTH
	     * encoding then we are in deep trouble. There is no way to
	     * determine the length of such types in a deterministic way without
	     * knowledge of the underlying type. We cannot even search for an
	     * EOC because two consecutive zeroes can appear also in the
	     * encoding of an underlying INTEGER, OCTET STRING or BIT STRING.
	     * 
	     * The only thing we can do is to abort with an error message that
	     * asks the user to try again by decoding with a well-known
	     * structure instead of using the "fetch what comes next mode"
	     * operation mode of the decoder.
	     */
	    if (indefinite_) {
		throw new ASN1Exception(
			"The decoder encountered a non-UNIVERSAL "
				+ "type with INDEFINITE LENGTH encoding. "
				+ "There is not sufficient information to "
				+ "determine the actual length of this "
				+ "type. Please try again by providing the "
				+ "appropriate template structure to the "
				+ "decoder.");
	    }
	    /*
	     * Here I use a nasty trick. The decoded type might be CONSTRUCTED.
	     * This will trigger an exception if the contents octets are read
	     * using an OCTET STRING. For this reason I force the encoding type
	     * to PRIMITIVE.
	     */
	    primitive_ = true;

	    /*
	     * The result of this construction is isomorphic to the use of an
	     * ASN1Opaque type but processing is slightly faster this way.
	     */
	    o = new ASN1OctetString();
	    t = new ASN1TaggedType(tag_, tagclass_, o, false);

	    readOctetString(o);

	    return t;
	}
	ASN1Type t;

	try {
	    t = (ASN1Type) getClass(tag_).newInstance();
	} catch (InstantiationException e) {
	    throw new ASN1Exception("Internal error, can't instantiate type!");
	} catch (IllegalAccessException e) {
	    throw new ASN1Exception("Internal error, can't access type!");
	}
	/*
	 * We now decode the UNIVERSAL type we encountered in the stream. If the
	 * type is an ASN.1 collection then we delegate decoding of the elements
	 * to method readTypes(ASN1Collection c). Subclasses can override that
	 * method in order to support indefinite length encodings (BER), and can
	 * fall back to the implementation in this class in the case of definite
	 * length encodings (DER).
	 */
	if (t instanceof ASN1Collection) {
	    if (primitive_) {
		throw new ASN1Exception("Collections cannot be PRIMITIVE!");
	    }
	    readTypes((ASN1Collection) t);
	} else {
	    skipNext(true);
	    t.decode(this);
	}
	return t;
    }

    /**
     * Reads in a sequence of ASN.1 types and stores them in the given
     * collection. This method is for use by subclasses only. The
     * {@link #length_ length field} of this instance must already be
     * initialized with the overall length of the sequence elements to read in.
     * <p>
     * 
     * Subclasses can override this method in order to handle indefinite length
     * encodings as used in BER. In the case of definite length
     * encodings,subclasses can fall back to this implementation.
     * 
     * @param c
     *                The ASN.1 collection in which decoded types are stored.
     * @throws ASN1Exception
     *                 if a decoding error occurs.
     * @throws IOException
     *                 if guess what...
     */
    protected void readTypes(ASN1Collection c) throws ASN1Exception,
	    IOException {
	ASN1Type o;
	int end;

	end = pos_ + length_;

	while (end > pos_) {
	    o = readType();

	    if (o == null) {
		throw new ASN1Exception(
			"EOC cannot be component of a collection!");
	    }
	    c.add(o);
	}
	if (end < pos_) {
	    throw new ASN1Exception("Length short by " + (pos_ - end)
		    + " octets!");
	}
    }

    public void readType(ASN1Type t) throws ASN1Exception, IOException {
	t.decode(this);
    }

    public void readBoolean(ASN1Boolean t) throws ASN1Exception, IOException {
	int b;

	match0(t, true);
	b = read();

	if (b < 0) {
	    throw new ASN1Exception("Unexpected EOF!");
	}
	if (b == 0) {
	    t.setTrue(false);
	} else if (b == 0xff) {
	    t.setTrue(true);
	} else {
	    throw new ASN1Exception("Bad ASN.1 Boolean encoding!");
	}
    }

    public void readInteger(ASN1Integer t) throws ASN1Exception, IOException {
	byte[] buf;

	match0(t, true);

	buf = new byte[length_];

	if (read(buf) < buf.length) {
	    throw new ASN1Exception("Unexpected EOF!");
	}
	t.setBigInteger(new BigInteger(buf));
    }

    public void readBitString(ASN1BitString t) throws ASN1Exception,
	    IOException {
	byte[] buf;
	int pad;

	match0(t, true);

	if (length_ < 1) {
	    throw new ASN1Exception("Length is zero, no initial octet!");
	}
	pad = read();

	if (pad < 0) {
	    throw new ASN1Exception("Unexpected EOF!");
	}
	buf = new byte[length_ - 1];

	if (buf.length > 0 && read(buf) < buf.length) {
	    throw new ASN1Exception("Unexpected EOF!");
	}
	t.setBits(buf, pad);
    }

    public void readOctetString(ASN1OctetString t) throws ASN1Exception,
	    IOException {
	byte[] buf;

	match0(t, true);

	buf = new byte[length_];

	if (length_ > 0) {
	    if (read(buf) < buf.length) {
		throw new ASN1Exception("Unexpected EOF!");
	    }
	}
	t.setByteArray(buf);
    }

    public void readNull(ASN1Null t) throws ASN1Exception, IOException {
	match0(t, true);

	if (length_ != 0 || indefinite_) {
	    throw new ASN1Exception("ASN.1 Null has bad length!");
	}
    }

    public void readObjectIdentifier(ASN1ObjectIdentifier t)
	    throws ASN1Exception, IOException {
	int[] oid;
	int end;
	int n;
	int i;

	match0(t, true);

	if (length_ < 1) {
	    throw new ASN1Exception("OID with not contents octets!");
	}
	end = pos_ + length_;
	n = read();

	if (n < 0 || n > 119) {
	    throw new ASN1Exception("OID contents octet[0] must be [0,119]!");
	}
	oidbuf_[0] = n / 40;
	oidbuf_[1] = n % 40;
	i = 2;

	try {
	    while (pos_ < end) {
		oidbuf_[i++] = readBase128();
	    }
	    if (pos_ != end) {
		throw new ASN1Exception("Bad length!");
	    }
	    oid = new int[i];

	    System.arraycopy(oidbuf_, 0, oid, 0, i);
	    t.setOID(oid);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new ASN1Exception("Can't handle more than " + oidbuf_.length
		    + " OID elements!");
	}
    }

    public void readReal(ASN1Type t) throws ASN1Exception {
	throw new ASN1Exception("Reals are not yet supported!");
    }

    public void readString(ASN1String t) throws ASN1Exception, IOException {
	byte[] buf;

	match0(t, true);

	buf = new byte[length_];

	if (read(buf) < buf.length) {
	    throw new ASN1Exception("Unexpected EOF!");
	}
	t.setString(t.convert(buf));
    }

    public void readCollection(ASN1Collection t) throws ASN1Exception,
	    IOException {
	Iterator i;
	ASN1Type o;
	int end;
	int n;

	match0(t, false);

	end = pos_ + length_;
	i = t.iterator();
	n = 0;

	/*
	 * This if statement fixes a problem with decoding empty sequences when
	 * the sequence contains only optional elements.
	 */
	if (pos_ < end) {
	    while (i.hasNext()) {
		if (!readNext()) {
		    break;
		}
		skipNext(true);
		o = (ASN1Type) i.next();
		n++;

		if (o.isType(tag_, tagclass_)) {
		    o.decode(this);
		    o.setOptional(false);

		    if (pos_ == end) {
			break;
		    }
		    if (pos_ > end) {
			throw new ASN1Exception("Length short by "
				+ (pos_ - end) + " octets!");
		    }
		} else {
		    if (!o.isOptional()) {
			throw new ASN1Exception("ASN.1 type mismatch!"
				+ "\nExpected: " + o.getClass().getName()
				+ "\nIn      : " + t.getClass().getName()
				+ "\nAt index: " + (n - 1) + "\nGot tag : "
				+ tag_ + " and class: " + tagclass_);
		    }
		}
	    }
	}
	while (i.hasNext()) {
	    o = (ASN1Type) i.next();
	    n++;

	    if (!o.isOptional()) {
		throw new ASN1Exception("ASN.1 type missing!" + "\nExpected: "
			+ o.getClass().getName() + "\nIn      : "
			+ t.getClass().getName() + "\nAt index: " + (n - 1));
	    }
	}
	if (pos_ < end) {
	    throw new ASN1Exception("Bad length, " + (end - pos_)
		    + " contents octets left!");
	}
    }

    public void readCollectionOf(ASN1CollectionOf t) throws ASN1Exception,
	    IOException {
	int end;
	ASN1Type o;

	match0(t, false);

	t.clear();
	end = pos_ + length_;

	while (pos_ < end) {
	    try {
		o = t.newElement();
	    } catch (IllegalStateException e) {
		throw new ASN1Exception("Cannot create new element! ");
	    }
	    o.decode(this);
	}
	if (pos_ != end) {
	    throw new ASN1Exception("Bad length!");
	}
    }

    public void readTime(ASN1Time t) throws ASN1Exception, IOException {
	readString(t);
    }

    /**
     * This method also reads in {@link ASN1Opaque ASN1Opaque} types. Opaque
     * types take on the tag and tag class of the decoded type and read in the
     * contents octets into an OCTET STRING. The opaque type can seamlessly be
     * encoded back into the original encoding. No traversal of the inner
     * structure of the encoded type is required.
     * 
     * @param t
     *                The {@link ASN1TaggedType ASN1TaggedType} or
     *                {@link ASN1Opaque ASN1Opaque} to decode.
     */
    public void readTaggedType(ASN1TaggedType t) throws ASN1Exception,
	    IOException {
	ASN1Type o;

	match1(t);

	o = t.getInnerType();
	if (o.isExplicit() && primitive_) {
	    throw new ASN1Exception("PRIMITIVE vs. CONSTRUCTED mismatch!");
	}
	/*
	 * A nasty trick to make the construction [CLASS TAG] IMPLICIT OCTET
	 * STRING work for types that are CONSTRUCTED.
	 */
	if (t instanceof ASN1Opaque) {
	    if (indefinite_) {
		throw new ASN1Exception(
			"Cannot decode indefinite length encodings "
				+ "with ASN1Opaque type!");
	    }
	    primitive_ = true;
	}
	o.decode(this);
    }

    /**
     * Reads an ASN.1 CHOICE type. This type is required only on decoding since
     * on encoding the type to encode should be clear. The selection of
     * alternative types is provided by the given {@link ASN1Choice ASN1Choice}
     * instance. The choice must be unambiguous.
     * <p>
     * The CHOICE elements <b>must</b> be tagged EXPLICIT. Otherwise, the
     * decoding will abort with an exception. The {@link ASN1Choice ASN1Choice}
     * class assures this condition upon adding alternative types to the CHOICE
     * type.
     * 
     * @param t
     *                The collection of choices from which the correct needs to
     *                be selected.
     */
    public void readChoice(ASN1Choice t) throws ASN1Exception, IOException {
	ASN1Type o;

	if (!readNext())
	    throw new IOException("Unexpected EOF!");

	skipNext(true);

	o = t.getType(tag_, tagclass_);
	if (o == null)
	    throw new ASN1Exception("Type mismatch!");

	o.decode(this);
	t.setInnerType(o);
    }

    /**
     * Reads a base 128 number from this input stream. Each consecutive octet
     * gives 7 bit of the number with all octets but the last one having the
     * most significant bit set to '1'.
     * <p>
     * 
     * This method is used for reading e.g. the long form of ASN.1 tags.
     * However, only tags up to the <code>int</code> range are supported.
     * <b>Note:</b> tags longer than that are not detected, but a wrong number
     * is being returned.
     * 
     * @return The decoded number.
     * @throws ASN1Exception
     *                 iff the end of the data is reached before the number is
     *                 decoded.
     * @throws IOException
     *                 iff guess what...
     */
    public int readBase128() throws ASN1Exception, IOException {
	int n;
	int b;

	n = 0;

	while ((b = read()) >= 0) {
	    n = (n << 7) | (b & 0x7f);

	    if ((b & 0x80) == 0) {
		break;
	    }
	}
	if (b < 0) {
	    throw new ASN1Exception("Unexpected EOF, base 128 octet missing!");
	}
	return n;
    }

    /**
     * This method reads in an decodes a number in base 256 format. This method
     * is used primarily for decoding the ASN.1 length octets. Only lengths up
     * to the <code>
     * long</code> range are supported. Even that is probably too
     * much since we'll hardly have to account for computers with more than 2<sup>64</sup>
     * bytes of memory, right?
     * <p>
     * 
     * The number is expected to be <code>num</code> bytes long. Certain bad
     * encodings are tolerated by this implementation and may lead to errors in
     * rare cases. For instance this implementation does not throw an error if
     * the number of octets of the encoded number is not minimal (e.g. has
     * leading zeroes). Hence, the re-encoding of such a decoded number will
     * result in an encoding that is not identical to the original one.
     * 
     * @param num
     *                The number of octets of the base 256 number to read from
     *                the input stream.
     * @return The decoded number.
     * @throws ASN1Exception
     *                 iff the end of the stream is reached before the number
     *                 could be decoded completely.
     */
    public int readBase256(int num) throws ASN1Exception, IOException {
	int n, b;

	n = 0;

	while (num > 0) {
	    b = read();

	    if (b < 0) {
		throw new ASN1Exception(
			"Unexpected EOF, base 256 octet missing!");
	    }
	    n = (n << 8) + b;
	    num--;
	}
	return n;
    }

    /**
     * This method calls the corresponding method of the underlying stream and
     * increases the position counter by the number of octets actually read.
     */
    public int read() throws IOException {
	int b;

	b = in.read();

	if (b >= 0) {
	    pos_++;
	}
	return b;
    }

    /**
     * This method calls the corresponding method of the underlying stream and
     * increases the position counter by the number of octets actually read.
     */
    public int read(byte[] b, int off, int len) throws IOException {
	int l;
	int ls;

	/*
	 * Since it is not garanteed that the method read(byte[] b, int off, int
	 * len) from InputStream actually reads as much bytes as specified by
	 * len-off, it's better to use the following while-loop instead of the
	 * previous construct:
	 * 
	 * int l; l = in.read(b,off,len); pos_ += l; return l;
	 * 
	 * Bugfix from Saied Tazari: on WindowsNT platforms it happened that
	 * agent communication ended up in unexplainable exceptions, which
	 * haven't occured since this method has been adjusted.
	 */

	ls = 0;

	while (ls < len) {
	    l = in.read(b, off + ls, len - ls);

	    if (l < 0) {
		break;
	    }
	    ls += l;
	}

	pos_ += ls;
	return ls;
    }

    /**
     * This method calls the corresponding method of the underlying stream and
     * increases the position counter by the number of octets actually read.
     */
    public int read(byte[] b) throws IOException {
	int l;
	int ls;

	/*
	 * Since it is not garanteed that the method read(byte[] b) from
	 * InputStream actually reads as much bytes as specified by b.length,
	 * it's better to use the following while-loop instead of the previous
	 * construct:
	 * 
	 * int l; l = in.read(b); pos_ += l; return l;
	 * 
	 * Bugfix from Saied Tazari: on WindowsNT platforms it happened that
	 * agent communication ended up in unexplainable exceptions, which
	 * haven't occured since this method has been adjusted.
	 */

	ls = 0;

	while (ls < b.length) {
	    l = in.read(b, ls, b.length - ls);

	    if (l < 0) {
		break;
	    }
	    ls += l;
	}

	pos_ += ls;
	return ls;
    }

    /**
     * This method calls the corresponding method of the underlying stream and
     * increases the position counter by the number of octets actually skipped.
     */
    public long skip(long n) throws IOException {
	long l;

	l = in.skip(n);
	pos_ += (int) l;
	return l;
    }

    /**
     * This method calls the corresponding method of the underlying stream and
     * also marks this stream's position, which denotes the overall number of
     * octets already read from this stream.
     * 
     * @param readAheadLimit
     *                The maximum number of bytes that can be read and still
     *                reset to the marked position.
     */
    public void mark(int readAheadLimit) {
	in.mark(readAheadLimit);
	markpos_ = pos_;
    }

    /**
     * Resets the unerlying input stream to the last marked position and also
     * resets this stream's position to the last marked one.
     */
    public void reset() throws IOException {
	in.reset();
	pos_ = markpos_;
    }

    /**
     * Returns <code>true</code> iff the underlying stream supports marking of
     * stream positions. This should better be the case, otherwise an exception
     * is raised when objects or headers are read from this stream.
     * 
     * @return <code>true</code> if marking is supported by the underlying
     *         stream.
     */
    public boolean markSupported() {
	return in.markSupported();
    }

    /**
     * Returns the number of bytes that can be read from the underlying stream
     * without blocking.
     * 
     * @return The number of bytes available.
     */
    public int available() throws IOException {
	return in.available();
    }

    /**
     * This method closes the underlying stream.
     */
    public void close() throws IOException {
	in.close();
	in = null;
    }

}

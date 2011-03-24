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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Represents an opaque type. An opaque type merely decodes the tag and tag
 * class and stores the contents octets in an OCTET STRING. The opaque type is
 * represented in ASN.1 as <code><blockquote>
 * [UNIVERSAL x] IMPLICIT OCTET STRING
 * </blockquote></code>
 * where <code>x</code> is the tag.
 * <p>
 * 
 * The opaque type is comparable to an {@link ASN1OpenType open type} in that it
 * matches any type (just like the deprecated ANY type) on decoding. The
 * encoding can be reconstructed easily. This type is used whenever decoding of
 * a structure should be deferred to a later point in time. For instance an
 * AlgorithmIdentifier implementation can use an opaque type in order to decode
 * algorithm parameters. The encoding of the algorithm parameters is then done
 * by JCA/JCE classes later on.
 * <p>
 * 
 * One drawback of the opaque type is that special handling by the encoders and
 * decoders is rquired to make it work properly. The main problem is that the
 * opaque type does not store whether the underlying type is constructed or
 * primitive. This decision must be made by the encoder.
 * <p>
 * 
 * Due to this limitation the opaque type can be used only for decoding types of
 * class UNIVERSAL.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1Opaque.java,v 1.2 2000/12/06 17:47:25 vroth Exp $"
 */
public class ASN1Opaque extends ASN1TaggedType {
    /**
     * Creates an instance. On decoding, opaque types pretend to be of a
     * particular type and read the actual type's encoding into an OCTET STRING
     * from which it can be retrieved later.
     * 
     */
    public ASN1Opaque() {
	super(-1, ASN1.CLASS_UNIVERSAL, new ASN1OctetString(), false);
    }

    /**
     * Creates an instance that stores the given encoding. The encoding must be
     * a valid DER encoding as specified in X.690. This constructor uses a
     * {@link DERDecoder DERDecoder} in order to decode the identifier octets in
     * the given encoding.
     * <p>
     * 
     * <b>Note:</b> If the given encoding contains the concatenation of
     * multiple encodings then only the first one will be stored. All others
     * will be lost.
     * 
     * @throws ASN1Exception
     *                 if the given code cannot be decoded.
     */
    public ASN1Opaque(byte[] code) throws ASN1Exception {
	super(-1, ASN1.CLASS_UNIVERSAL, new ASN1OctetString(), false);

	ByteArrayInputStream bis;
	DERDecoder dec;

	try {
	    bis = new ByteArrayInputStream(code);
	    dec = new DERDecoder(bis);
	    decode(dec);
	    dec.close();
	} catch (IOException e) {
	    throw new ASN1Exception("Internal, caught IOException!");
	}
    }

    /**
     * Creates an instance with the given type, class, and inner type. <b>Be
     * careful</b>, the given octet string must contain the valid DER encoding
     * of the contents octets of a type that matches the tag and tag class.
     * Otherwise coding exceptions are most probably thrown subsequently.
     * 
     * @param tag
     *                The ASN.1 tag of the opaque type.
     * @param tagclass
     *                The tag class of the opaque type.
     * @param b
     *                The DER compliant encoding of the contents octets of the
     *                opaque type.
     * @throws NullPointerException
     *                 if the given byte array is <code>null</code>.
     */
    public ASN1Opaque(int tag, int tagclass, byte[] b) {
	super(tag, tagclass, new ASN1OctetString((byte[]) b.clone()), false);
    }

    /**
     * This method adopts the given tag and tag class if this instance is not
     * yet initialized with a tag or tag class. In that case <code>true</code>
     * is returned.
     * <p>
     * 
     * If a tag or tag class is already set then this method calls its super
     * method.
     * 
     * @param tag
     *                The tag to compare with.
     * @param tagclass
     *                The tag class to compare with.
     */
    public boolean isType(int tag, int tagclass) {
	if (tagclass != ASN1.CLASS_UNIVERSAL)
	    return false;

	if (getTag() == -1) {
	    setTag(tag);
	    return true;
	}
	return super.isType(tag, tagclass);
    }

    /**
     * This method is a convenience method in order to encode this type with
     * DER. It uses a {@link DEREncoder DEREncoder} in order to encode this type
     * to a byte array which is returned.
     * <p>
     * 
     * @return The DER encoding of this type.
     */
    public byte[] getEncoded() throws ASN1Exception {
	ByteArrayOutputStream bos;
	DEREncoder enc;
	byte[] code;

	try {
	    bos = new ByteArrayOutputStream();
	    enc = new DEREncoder(bos);
	    encode(enc);
	    code = bos.toByteArray();
	    enc.close();

	    return code;
	} catch (IOException e) {
	    throw new ASN1Exception("Internal, caught IOException!");
	}
    }

    /**
     * Sets the inner type of this opaque type. The given type must be
     * {@link ASN1OctetString ASN1OctetString} or a ClassCastException is
     * thrown.
     * 
     * @param t
     *                The type to set as the inner type.
     * @throws NullPointerException
     *                 if the given type is <code>null</code>.
     * @throws ClassCastException
     *                 if the given type is not an ASN1OctetString.
     */
    public void setInnerType(ASN1Type t) {
	super.setInnerType(t);
    }

    /**
     * Returns a clone. The clone is a deep copy of this instance except the
     * constraints. Constraints are copied by reference.
     * 
     * @return The clone.
     */
    public Object clone() {
	ASN1OctetString b;
	ASN1Opaque o;

	try {
	    o = (ASN1Opaque) super.clone();
	    b = (ASN1OctetString) o.getInnerType();

	    o.setInnerType((ASN1OctetString) b.clone());
	} catch (CloneNotSupportedException e) {
	    /*
	     * This cannot, cannot, cannot, CANNOT happen ! If it does, put back
	     * the import statement for 'Cloneable' into ASN1AbstractType !
	     */
	    throw new Error("Internal, clone support mismatch!");
	}
	return o;
    }

}

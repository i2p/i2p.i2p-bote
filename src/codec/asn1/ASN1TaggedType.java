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
 * Represents an ASN.1 tagged type. Tagged types are types that modify the tag
 * of an underlying type. The ASN.1 type classes
 * {@link ASN1#CLASS_CONTEXT CONTEXT}, {@link ASN1#CLASS_PRIVATE PRIVATE}, and
 * {@link ASN1#CLASS_APPLICATION APPLICATION} specify tagged types.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1TaggedType.java,v 1.4 2004/08/09 07:45:30 flautens Exp $"
 */
public class ASN1TaggedType extends ASN1AbstractType {
    private int tag_;
    private int cls_ = ASN1.CLASS_CONTEXT;

    private ASN1Type inner_;

    /**
     * Creates an instance with the given tag, tag class, and inner type. The
     * tagging method is EXPLICIT if <code>
     * explicit</code> is
     * <code>true</code> and IMPLICIT otherwise.
     * 
     * @param tag
     *                The tag of this type.
     * @param cls
     *                The tag class of this type, for instance
     *                {@link ASN1#CLASS_CONTEXT CONTEXT SPECIFIC}.
     * @param inner
     *                The inner type of this tagged type.
     * @param explicit
     *                <code>true</code> if EXPLICIT tagging shall be used and
     *                <code>false</code> if the tagging method shall be
     *                IMPLICIT.
     * @throws NullPointerException
     *                 if the given inner type is <code>null</code>.
     */
    public ASN1TaggedType(int tag, int cls, ASN1Type inner, boolean explicit) {
	setTag(tag);
	setTagClass(cls);
	setInnerType(inner);
	inner_.setExplicit(explicit);
    }

    /**
     * Creates an instance with the given tag and inner type. The tagging method
     * is EXPLICIT if <code>
     * explicit</code> is <code>true</code> and
     * IMPLICIT otherwise. The tag class is set to {@link ASN1#CLASS_CONTEXT
     * CONTEXT SPECIFIC}.
     * 
     * @param tag
     *                The tag of this type.
     * @param inner
     *                The inner type of this tagged type.
     * @param explicit
     *                <code>true</code> if EXPLICIT tagging shall be used and
     *                <code>false</code> if the tagging method shall be
     *                IMPLICIT.
     * @throws NullPointerException
     *                 if the given inner type is <code>null</code>.
     */
    public ASN1TaggedType(int tag, ASN1Type inner, boolean explicit) {
	setTag(tag);
	setTagClass(ASN1.CLASS_CONTEXT);
	setInnerType(inner);
	inner_.setExplicit(explicit);
    }

    /**
     * Creates an instance with the given tag, tag class, and inner type. The
     * tagging method is EXPLICIT if <code>
     * explicit</code> is
     * <code>true</code> and IMPLICIT otherwise. The tag class is set to
     * {@link ASN1#CLASS_CONTEXT CONTEXT SPECIFIC}. If <code>
     * optional</code>
     * is <code>true</code> then this type is declared OPTIONAL.
     * 
     * @param tag
     *                The tag of this type.
     * @param inner
     *                The inner type of this tagged type.
     * @param explicit
     *                <code>true</code> if EXPLICIT tagging shall be used and
     *                <code>false</code> if the tagging method shall be
     *                IMPLICIT.
     * @param optional
     *                <code>true</code> declares this type as OPTIONAL.
     * @throws NullPointerException
     *                 if the given inner type is <code>null</code>.
     */
    public ASN1TaggedType(int tag, ASN1Type inner, boolean explicit,
	    boolean optional) {
	setTag(tag);
	setTagClass(ASN1.CLASS_CONTEXT);
	setInnerType(inner);
	inner_.setExplicit(explicit);
	setOptional(optional);
    }

    /**
     * Returns the underlying ASN.1 type. Please note that OPTIONAL modifiers of
     * (for instance) context-specific types in compound ASN.1 types refer to
     * the outer type and not to the inner type. Types are declared OPTIONAL by
     * calling their {@link ASN1Type#setOptional setOptional} method.
     * 
     * @return The underlying ASN.1 type.
     */
    public ASN1Type getInnerType() {
	return inner_;
    }

    /**
     * Returns the value of the inner type. The default inner type is
     * {@link ASN1Null ASN1Null}. This method calls
     * {@link ASN1Type#getValue getValue} on the inner type and returns the
     * result.
     * 
     * @return The value of the inner type.
     */
    public Object getValue() {
	return inner_.getValue();
    }

    /**
     * Sets the inner type of this CONTEXT SPECIFIC type.
     * 
     * @param t
     *                The type to set as the inner type.
     * @throws NullPointerException
     *                 if the given type is <code>null</code>.
     */
    public void setInnerType(ASN1Type t) {
	if (t == null) {
	    throw new NullPointerException("Type is NULL!");
	}
	inner_ = t;
    }

    /**
     * Sets the tag of this type.
     * 
     * @param tag
     *                The tag.
     */
    public void setTag(int tag) {
	tag_ = tag;
    }

    /**
     * Returns the tag of this type.
     * 
     * @return The tag of this type.
     */
    public int getTag() {
	return tag_;
    }

    /**
     * Sets the tag class of this type. This tag class may be one of
     * {@link ASN1#CLASS_UNIVERSAL UNIVERSAL}, {@link ASN1#CLASS_CONTEXT
     * CONTEXT SPECIFIC}, {@link ASN1#CLASS_PRIVATE PRIVATE}, or {@link
     * ASN1#CLASS_APPLICATION APPLICATION}.
     * 
     * @param cls
     *                The tag class.
     */
    public void setTagClass(int cls) {
	cls_ = cls;
    }

    /**
     * Returns the tag class of this type. The default class of this instance is
     * {@link ASN1#CLASS_CONTEXT CONTEXT SPECIFIC}.
     * 
     * @return The class of this ASN.1 tag.
     */
    public int getTagClass() {
	return cls_;
    }

    /**
     * Tagged types themselves are always tagged EXPLICIT. The inner type can be
     * tagged either EXPLICIT or IMPLICIT. IMPLICIT types are isomorphic to the
     * underlying type except that the tag and tag class is distinct (with
     * regard to encoding).
     * 
     * @return <code>true</code>, tagged types themselves are always tagged
     *         EXPLICIT.
     */
    public boolean isExplicit() {
	return true;
    }

    /**
     * Throws an exception if the give tagging type is not EXPLICIT (<code>true</code>).
     * Tagged types themselves are always EXPLICIT; re-tagging tagged types is
     * <b> very</b> bad style!
     * 
     * @param explicit
     *                The tagging method of the tagged (outer) type. This should
     *                not be mixed with the tagging method of the inner type
     *                which can be tagged either EXPLICIT or IMPLICIT.
     */
    public void setExplicit(boolean explicit) {
	if (!explicit)
	    throw new IllegalArgumentException(
		    "Tagget types are never IMPLICIT!");
    }

    public void encode(Encoder enc) throws ASN1Exception, IOException {
	enc.writeTaggedType(this);
    }

    public void decode(Decoder dec) throws ASN1Exception, IOException {
	dec.readTaggedType(this);
    }

    public String toString() {
	StringBuffer buf;

	buf = new StringBuffer();
	buf.append("[");

	switch (cls_) {
	case ASN1.CLASS_CONTEXT:
	    buf.append("CONTEXT SPECIFIC ");
	    break;
	case ASN1.CLASS_UNIVERSAL:
	    buf.append("UNIVERSAL ");
	    break;
	case ASN1.CLASS_APPLICATION:
	    buf.append("APPLICATION ");
	    break;
	case ASN1.CLASS_PRIVATE:
	    buf.append("PRIVATE ");
	    break;
	}
	buf.append(tag_ + "] ");

	if (inner_.isExplicit())
	    buf.append("EXPLICIT ");
	else
	    buf.append("IMPLICIT ");

	buf.append(inner_.toString());
	return buf.toString();
    }

}

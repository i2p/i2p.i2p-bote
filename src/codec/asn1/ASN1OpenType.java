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
 * This type represents what was formerly called the ASN.1 ANY type. The ANY and
 * ANY DEFINED BY types are superseded as of ITU-T Recommendation X.680 version
 * current December 1997 by the ability to define type classes. Modelling type
 * classes is beyond the scope of this ASN.1 package although the package can be
 * enhanced accordingly. ASN.1 type classes can contain components whose type is
 * unspecified. Such components are called &quot;open types&quot;. This class
 * mimics an open type insofar as it decodes any type encountered in an encoded
 * stream of ASN.1 types. On encoding the proper type is encoded in place of the
 * open type. Decoding an open type that was not properly initialized either by
 * a call to a creator with an argument or by decoding it from a valid ASN.1
 * encoding results in an {@link ASN1Null ASN1Null} being decoded.
 * <p>
 * 
 * This class enforces as an invariant that inner types have the same tagging as
 * the type itself. For instance: <blockquote>
 * 
 * <pre>
 * ASN1OpenType ot;
 * ASN1Integer n;
 * n = new Integer(&quot;42&quot;);
 * n.setExplicit(true);
 * ot = new OpenType(new FooResolver());
 * ot.setExplicit(false);
 * ot.setInnerType(n);
 * </pre>
 * 
 * </blockquote> will cause the tagging method of <code>n</code> to be changed
 * into EXPLICIT upon the call to <code>ot.setInnerType()</code>.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1OpenType.java,v 1.4 2004/08/06 15:13:01 flautens Exp $"
 */
public class ASN1OpenType extends ASN1AbstractType {
    private static final String NO_INNER = "No inner type defined!";

    private ASN1Type inner_;

    protected Resolver resolver_;

    public ASN1OpenType() {
	super();
    }

    /**
     * Creates an instance that attempts to resolve the actual type on decoding
     * using the given {@link Resolver Resolver}.
     * 
     * @param resolver
     *                The instance that is asked to deliver the type to decode.
     */
    public ASN1OpenType(Resolver resolver) {
	resolver_ = resolver;
    }

    /**
     * This constructor corresponds to the superseded ANY DEFINED BY type. The
     * open type attempts to resolve the type to decode right before decoding by
     * a call to the given registry with the given OID as the argument. The
     * exact OID instance is used that is passed to this method as the argument.
     * If this instance is decoded before the open type is decoded (because the
     * OID is encountered earlier in a decoded stream) then the open type can
     * determine the exact type to decode by a call to the registry.
     * 
     * @param oid
     *                The OID that is passed to the given registry on resolving.
     */
    public ASN1OpenType(OIDRegistry registry, ASN1ObjectIdentifier oid) {
	resolver_ = new DefinedByResolver(registry, oid);
    }

    public ASN1OpenType(ASN1ObjectIdentifier oid) {
	resolver_ = new DefinedByResolver(oid);
    }

    /**
     * Returns the inner ASN.1 type. If the inner type is not set and a
     * {@link Resolver Resolver} is set then the Resolver is asked to resolve
     * the inner type. The resulting type is then returned.
     * <p>
     * 
     * This method may return <code>null</code> if the resolver cannot
     * determine the inner type of the open type. In particular, if the Resolver
     * is <code>null</code> and no inner type is already set then
     * <code>null</code> is returned.
     * 
     * @return The inner ASN.1 type.
     */
    public ASN1Type getInnerType() throws ResolverException {
	if (inner_ != null) {
	    return inner_;
	}
	if (resolver_ == null) {
	    return null;
	}
	inner_ = resolver_.resolve(this);

	return inner_;
    }

    /**
     * Sets the inner type. The inner type inherits the tagging of this type.
     * 
     * @param t
     *                The type to set as the inner type.
     * @throws NullPointerException
     *                 if the given type is <code>null</code>.
     */
    protected void setInnerType(ASN1Type t) {
	inner_ = t;
	inner_.setExplicit(isExplicit());
    }

    /**
     * Returns the tag of the inner type.
     * 
     * @return The tag of the inner type.
     * @throws IllegalStateException
     *                 if the inner type is not yet initialized.
     */
    public int getTag() {
	if (inner_ == null) {
	    throw new IllegalStateException(NO_INNER);
	}
	return inner_.getTag();
    }

    /**
     * Returns the tag class of the inner type.
     * 
     * @return The tag class of the inner type.
     * @throws IllegalStateException
     *                 if the inner type is not yet initialized.
     */
    public int getTagClass() {
	if (inner_ == null) {
	    throw new IllegalStateException(NO_INNER);
	}
	return inner_.getTagClass();
    }

    /**
     * Returns the value of the inner type. The default inner type is
     * {@link ASN1Null ASN1Null}. This method calls
     * {@link ASN1Type#getValue getValue} on the inner type and returns the
     * result.
     * 
     * @return The value of the inner type.
     * @throws IllegalStateException
     *                 if the inner type is not yet initialized.
     */
    public Object getValue() {
	if (inner_ == null) {
	    throw new IllegalStateException(NO_INNER);
	}
	return inner_.getValue();
    }

    /**
     * Sets the tagging to either EXPLICIT or IMPLICIT. If this type already has
     * an inner type set then the tagging of the inner type is set to the same
     * tagging.
     * 
     * @param explicit
     *                <code>true</code> if this type shall be tagged EXPLICIT
     *                and <code>false</code> if it shall be encoded IMPLICIT.
     * @throws IllegalStateException
     *                 if the inner type is not yet initialized.
     */
    public void setExplicit(boolean explicit) {
	super.setExplicit(explicit);

	if (inner_ != null) {
	    inner_.setExplicit(explicit);
	}
    }

    /**
     * Sets the {@link Constraint Constraint} of the inner type. For instance an
     * ASN.1 INTEGER might be constrained to a certain range such as INTEGER
     * (0..99). <code>null</code> can be passed as a constraint which disables
     * constraint checking.
     * 
     * @param constraint
     *                The {@link Constraint Constraint} of this type.
     * @throws IllegalStateException
     *                 if the inner type is not yet initialized.
     */
    public void setConstraint(Constraint constraint) {
	if (inner_ == null) {
	    throw new IllegalStateException(NO_INNER);
	}
	inner_.setConstraint(constraint);
    }

    /**
     * Checks the constraint on the inner type if it is set. Otherwise this
     * method returns silently.
     * 
     * @throws ConstraintException
     *                 if this type is not in the appropriate range of values.
     * @throws IllegalStateException
     *                 if the inner type is not yet initialized.
     */
    public void checkConstraints() throws ConstraintException {
	if (inner_ == null) {
	    throw new IllegalStateException(NO_INNER);
	}
	inner_.checkConstraints();
    }

    /**
     * This method compares the given tag and tag class with the tag and tag
     * class of the resolved type.
     * <p>
     * If an exception is thrown by the {@link Resolver Resolver} upon resolving
     * the inner type of this type then <code>false</code> is returned in
     * order to provoke a decoding error.
     * <p>
     * 
     * If no inner type can be resolved then <code>true
     * </code> is returned. In
     * that case this type behaves like the ANY type known from previous ASN.1
     * versions.
     * 
     * @param tag
     *                The tag to match.
     * @param tagclass
     *                The tag class to match.
     * @return <code>true</code> iff the given tag and tag class match one of
     *         the alternative types represented by this variable type.
     */
    public boolean isType(int tag, int tagclass) {
	if (inner_ != null) {
	    return inner_.isType(tag, tagclass);
	}
	try {
	    if (resolver_ != null) {
		inner_ = resolver_.resolve(this);
	    }
	} catch (ResolverException e) {
	    return false;
	}

	/*
	 * If no inner type could be resolved then we behave like the ANY type.
	 */
	if (inner_ == null) {
	    return true;
	}
	return inner_.isType(tag, tagclass);
    }

    /**
     * Encodes the inner typeof this open type using the given
     * {@link Encoder Encoder}. If the inner type is not yet initialized then
     * an exception is thrown.
     * 
     * @param enc
     *                The {@link Encoder Encoder} to use for encoding the inner
     *                type.
     * @throws IllegalStateException
     *                 if the inner type is not yet initialized.
     */
    public void encode(Encoder enc) throws ASN1Exception, IOException {
	if (inner_ == null) {
	    throw new IllegalStateException(NO_INNER);
	}
	enc.writeType(inner_);
    }

    /**
     * Decodes the inner type to the given {@link Decoder decoder}. If a
     * {@link Resolver resolver} was specified then it is asked to provide an
     * ASN.1 type to decode.
     * 
     * @param dec
     *                The decoder to decode to.
     * @throws IllegalStateException
     *                 if the open type cannot be resolved on runtime.
     */
    public void decode(Decoder dec) throws ASN1Exception, IOException {
	if (resolver_ != null && inner_ == null) {
	    inner_ = resolver_.resolve(this);
	}
	if (inner_ == null) {
	    inner_ = dec.readType();
	} else {
	    inner_.decode(dec);
	}
	inner_.setExplicit(isExplicit());
    }

    /**
     * Returns the string representation of this instance.
     * 
     * @return The string representation of this instance.
     */
    public String toString() {
	if (inner_ == null) {
	    return "Open Type <NOT InitializED>";
	}
	return "(Open Type) " + inner_.toString();
    }
}

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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;

/**
 * The basic interface for Java objects representing primitive ASN.1 types
 * according to ITU-T Recommendation X.680.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1AbstractType.java,v 1.4 2005/03/23 12:45:51 flautens Exp $"
 */
public abstract class ASN1AbstractType extends Object implements ASN1Type,
	Cloneable, Externalizable {
    /**
     * Holds the ASN.1 optional state. Optional types may be present in an
     * encoding but they need not be.
     */
    private boolean optional_ = false;

    /**
     * This flag defines the tagging variant which should be used by the
     * decoder/encoder while processing this type.
     */
    private boolean explicit_ = true;

    /**
     * Holds the constraint which should be checked if the internal value of a
     * sub class is modified.
     */
    private Constraint constraint_;

    /**
     * This abstract method should return the value wrapped by the ASN1Type.
     * 
     * @return the internal value
     */
    public abstract Object getValue();

    /**
     * Returns the corresponding ASN.1 tag.
     * 
     * @return the corresponding ASN.1 tag
     */
    public abstract int getTag();

    public abstract void encode(Encoder enc) throws ASN1Exception, IOException;

    public abstract void decode(Decoder dec) throws ASN1Exception, IOException;

    /*
     * Method declarations with default implementation.
     */
    public ASN1AbstractType() {
	super();
    }

    /**
     * Optional types may be present in an encoding but they need not be.
     * 
     * @param optional
     *                <code>true</code> iff this type is optional.
     */
    public void setOptional(boolean optional) {
	optional_ = optional;
    }

    /**
     * @return <code>true</code> if this type is optional.
     */
    public boolean isOptional() {
	return optional_;
    }

    /**
     * This default implementation returns {@link ASN1#CLASS_UNIVERSAL
     * UNIVERSAL}.
     * 
     * @return The class of the ASN.1 tag.
     */
    public int getTagClass() {
	return ASN1.CLASS_UNIVERSAL;
    }

    /**
     * Sets the tagging of this type as either EXPLICIT or IMPLICIT. The default
     * is EXPLICIT. Encoders skip the encoding of identifier octets for types
     * that are declared as IMPLICIT.
     * 
     * @param explicit
     *                <code>true</code> if this type shall be tagged EXPLICIT
     *                and <code>false</code> if it shall be encoded IMPLICIT.
     */
    public void setExplicit(boolean explicit) {
	explicit_ = explicit;
    }

    /**
     * Returns code>true</code> if this type is tagged EXPLICIT and <code>false</code>
     * otherwise.
     * 
     * @return <code>true</code> if this type is tagged EXPLICIT and <code>false</code>
     *         if it is tagged IMPLICIT.
     */
    public boolean isExplicit() {
	return explicit_;
    }

    /**
     * Returns <code>true</code> if the given tag and tag class matches the
     * tag and tag class of this instance. This method is used primarily by
     * decoders and variable types such as {@link ASN1Choice ASN1Choice} and
     * {@link ASN1OpenType ASN1OpenType}. It enables decoders to query a
     * variable type whether a decoded type is accepted.
     * <p>
     * 
     * This method provides a default implementation that matches the given tag
     * and tag class against the values returned by {@link #getTag getTag} and
     * {@link #getTagClass getTagClass} respectively.
     * 
     * @param tag
     *                The tag to compare with.
     * @param tagclass
     *                The tag class to compare with.
     * @return <code>true</code> if the given tag and tag class matches this
     *         type and <code>false</code> otherwise.
     */
    public boolean isType(int tag, int tagclass) {
	if ((getTag() == tag) && (getTagClass() == tagclass)) {
	    return true;
	}

	return false;
    }

    /**
     * Sets the {@link Constraint Constraint} of this type. For instance an
     * ASN.1 INTEGER might be constrained to a certain range such as INTEGER
     * (0..99). <code>null</code> can be passed as a constraint which disables
     * constraint checking.
     * 
     * @param constraint
     *                The {@link Constraint Constraint} of this type.
     */
    public void setConstraint(Constraint constraint) {
	constraint_ = constraint;
    }

    /**
     * Returns the {@link Constraint Constraint} of this type or
     * <code>null</code> if there is none.
     * 
     * @return The Constraint or <code>null</code>.
     */
    public Constraint getConstraint() {
	return constraint_;
    }

    /**
     * Checks the constraint on this type if it is set. Otherwise this method
     * returns silently.
     * 
     * @throws ConstraintException
     *                 if this type is not in the appropriate range of values.
     */
    public void checkConstraints() throws ConstraintException {
	if (constraint_ != null) {
	    constraint_.constrain(this);
	}
    }

    /**
     * The writeExternal and readExternal methods of the Externalizable
     * interface are implemented by a class to give the serializable class
     * complete control over the format and contents of the stream for an object
     * and its supertypes.
     * 
     * @param out -
     *                the stream to write the object to
     * @throws IOException
     *                 if an I/0 error has occured
     * @see java.io.Externalizable
     */
    public void writeExternal(ObjectOutput out) throws IOException {
	byte[] res = null;

	ByteArrayOutputStream baos = new ByteArrayOutputStream();

	try {
	    encode(new DEREncoder(baos));
	    res = baos.toByteArray();
	    baos.close();
	    out.write(res);
	} catch (ASN1Exception e) {
	    throw new RuntimeException(e.toString());
	}
    }

    /**
     * The writeExternal and readExternal methods of the Externalizable
     * interface are implemented by a class to give the serializable class
     * complete control over the format and contents of the stream for an object
     * and its supertypes.
     * 
     * @param in -
     *                the stream to read data from in order to restore the
     *                object
     * @throws IOException
     *                 if an I/0 error has occured
     * @see java.io.Externalizable
     */
    public void readExternal(ObjectInput in) throws IOException {
	try {
	    decode(new DERDecoder((ObjectInputStream) in));
	} catch (ASN1Exception e) {
	    throw new RuntimeException(e.toString());
	}
    }
}

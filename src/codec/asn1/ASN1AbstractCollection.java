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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents an abstract collection of ASN.1 types such as a SEQUENCE or a SET.
 * Since this class inherits from the Collection framework class ArrayList,
 * ASN.1 types may be added conveniently just as object instances are added to a
 * list.
 * <p>
 * 
 * Please note that constraints of collections are validated before encoding and
 * after decoding. Invalid modification of a collection type can be detected on
 * importing and exporting abstract collections. On DER encoding a collection
 * its constraint is validated twice since the DER encoding is a two-pass
 * process.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1AbstractCollection.java,v 1.5 2005/03/22 16:12:45 flautens
 *          Exp $"
 */
public abstract class ASN1AbstractCollection extends ArrayList implements
	ASN1Collection, Cloneable, Externalizable {
    /**
     * DOCUMENT ME!
     */
    private boolean optional_ = false;

    /**
     * DOCUMENT ME!
     */
    private boolean explicit_ = true;

    /**
     * DOCUMENT ME!
     */
    private Constraint constraint_;

    /**
     * Abstract method declarations.
     * 
     * @return corresponding ASN1 tag
     */
    public abstract int getTag();

    /**
     * Method declarations with default implementation.
     */
    public ASN1AbstractCollection() {
	super();
    }

    /**
     * Creates an instance with the given capacity.
     * 
     * @param capacity
     *                The capacity.
     */
    public ASN1AbstractCollection(int capacity) {
	super(capacity);
    }

    /**
     * Returns the Java type that corresponds to this ASN.1 type.
     * 
     * @return The collection used internally for storing the elements in this
     *         constructed ASN.1 type.
     */
    public Object getValue() {
	return this;
    }

    /**
     * Returns the Java type that corresponds to this ASN.1 type.
     * 
     * @return The collection used internally for storing the elements in this
     *         constructed ASN.1 type.
     */
    public Collection getCollection() {
	return this;
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
     * @return <code>true</code> iff this type is optional.
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
     * 
     * @return <code>true</code> if this type is tagged EXPLICIT and
     *         <code>false</code> if it is tagged IMPLICIT.
     */
    public boolean isExplicit() {
	return explicit_;
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
     * Writes this collection to the given {@link Encoder encoder}.
     * 
     * @param enc
     *                The encoder to write this type to.
     */
    public void encode(Encoder enc) throws ASN1Exception, IOException {
	checkConstraints();
	enc.writeCollection(this);
    }

    /**
     * Reads this collection from the given {@link Decoder Decoder}. This type
     * is initialized with the decoded data. The components of the decoded
     * collection must match the components of this collection. If they do then
     * the components are also initialized with the decoded values. Otherwise an
     * exception is thrown.
     * 
     * @param dec
     *                The decoder to read from.
     */
    public void decode(Decoder dec) throws ASN1Exception, IOException {
	dec.readCollection(this);
	checkConstraints();
    }

    /**
     * Prints this collection. This default implementation derives a descriptive
     * name from the name of the fully qualified name of this class (or that of
     * the respective subclass). The last component of the class name is
     * extracted and a prefix of &quot;ASN1&quot; is removed from it. Then the
     * elements contained in this collection are printed.
     * 
     * @return The string representation of this ASN.1 collection.
     */
    public String toString() {
	StringBuffer buf;
	Iterator i;
	String s;

	s = removePackageName(getClass());

	buf = new StringBuffer();
	buf.append(s);

	if (isOptional()) {
	    buf.append(" OPTIONAL");
	}

	if (this instanceof ASN1CollectionOf) {
	    buf.append(" SEQUENCE OF "
		    + removePackageName(((ASN1CollectionOf) this)
			    .getElementType()));
	} else {
	    buf.append(" SEQUENCE ");
	}
	buf.append(" {\n");

	for (i = iterator(); i.hasNext();) {
	    buf.append(i.next().toString());
	    buf.append("\n");
	}
	buf.append("}");
	return buf.toString();
    }

    /**
     * This method removes the package information from the qualified class
     * name. If the remaining class name starts with 'ASN1' then this prefix is
     * also removed.
     * 
     * @param clazz
     *                The class to handle
     * @return the shortened class name
     */
    private String removePackageName(Class clazz) {
	String s = clazz.getName();
	int n = s.lastIndexOf('.');

	if (n < 0) {
	    n = -1;
	}

	s = s.substring(n + 1);
	if (s.startsWith("ASN1")) {
	    s = s.substring(4);
	}
	return s;
    }

    /**
     * The writeExternal and readExternal methods of the Externalizable
     * interface are implemented by a class to give the serializable class
     * complete control over the format and contents of the stream for an object
     * and its supertypes.
     * 
     * @param out -
     *                the stream to write the object to
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

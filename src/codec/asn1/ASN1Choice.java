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
import java.util.Iterator;

/**
 * This type represents the ASN.1 CHOICE type as specified in ITU-T
 * Recommendation X.680. On decoding, the decoder must be able to decide
 * umambiguously which alternative choice it has to decode. For this reason all
 * elements in a CHOICE type must have distinctive tags.
 * <p>
 * 
 * This class does not enforce the distinctive tag rule. Instead, the
 * alternative with the first matching tag should be chosen by decoders. The
 * application that builds the CHOICE type must take care not to produce
 * ambiguous sets of alternatives.
 * <p>
 * 
 * This class distinguishes alternative choices and an inner type. Upon
 * decoding, the inner type is selected from the list of choices based on the
 * identifier octets encountered in the encoded stream. This type is then
 * {@link #setInnerType set as the inner type} of this instance. Unless an inner
 * type is set (either explicitly or by means of decoding) the state of the
 * choice is undefined.
 * <p>
 * 
 * This instance always mimicks its inner type. The methods
 * {@link ASN1Type#getTag getTag}, {@link ASN1Type#getTagClass getTagClass},
 * {@link ASN1Type#getValue getValue} all return the appropriate results of the
 * corresponding method of the inner type. On encoding an instance of this class
 * the inner type is encoded.
 * <p>
 * 
 * No nested CHOICE classes are supported. In principle this is easily supported
 * but it is not good style to build such structures.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1Choice.java,v 1.3 2004/08/06 15:01:20 flautens Exp $"
 */
public class ASN1Choice extends ASN1AbstractType {
    private static final String NO_INNER = "No inner type defined!";

    private ASN1Type inner_;

    private ArrayList choices_;

    /**
     * Creates an instance with an initial capacity of 2.
     */
    public ASN1Choice() {
	choices_ = new ArrayList(2);
    }

    /**
     * Creates an instance with the given initial capacity. The capacity
     * determines the number of choices to store. This instance is backed by an
     * ArrayList, hence the capacity is increased dynamically as required. Use
     * the {@link #trimToSize() trimToSize()} method to trim the internal list
     * to the number of stored choices in order to reclaim memory.
     * 
     * @param capacity
     *                The initial capacity for storing choices.
     * @throws IllegalArgumentException
     *                 if the capacity is less than 1.
     */
    public ASN1Choice(int capacity) {
	if (capacity < 1)
	    throw new IllegalArgumentException(
		    "capacity must be greater than zero!");

	choices_ = new ArrayList(capacity);
    }

    /**
     * Adds the given type as an alternative choice to the collection of
     * choices. The caller has to take care that no ambiguous choices are added.
     * Each added type must have a distinctive tag.
     * <p>
     * 
     * CHOICE elements must neither be OPTIONAL nor tagged IMPLICIT. For safety,
     * this method calls {@link ASN1Type#setOptional setOptional}(false) and
     * {@link ASN1Type#setExplicit setExplicit}(true) on the given type.
     * Callers must not alter this setting after adding a type to this choice.
     * However, the CHOICE itself can be declared OPTIONAL.
     * 
     * @param t
     *                The ASN.1 type to add as a choice.
     * @throws NullPointerException
     *                 if the given type is <code>null</code>.
     * @throws IllegalArgumentException
     *                 if the given type is a ASN1Choice type.
     */
    public void addType(ASN1Type t) {
	if (t == null)
	    throw new NullPointerException("Choice is null!");

	if (t instanceof ASN1Choice)
	    throw new IllegalArgumentException(
		    "No nested CHOICE types are allowed!");

	t.setOptional(false);
	t.setExplicit(true);

	choices_.add(t);
    }

    /**
     * Returns the choice with the given tag and tagclass if it exists,
     * otherwise <code>null</code> is returned. This method is called by the
     * decoder in order to determine the appropriate type to decode. The
     * returned type is set up as the inner type by the decoder.
     * 
     * @param tag
     *                The tag of the type encountered in the encoded stream. The
     *                tags of the various primitive ASN.1 types are defined in
     *                class {@link ASN1 ASN1}.
     * @param tagclass
     *                The tag class of the type encountered in the encoded
     *                stream. The tag class identifiers are defined in class
     *                {@link ASN1 ASN1}. See for instance
     *                {@link ASN1#CLASS_UNIVERSAL CLASS_UNIVERSAL}.
     * @return The choice with matching tag and tag class or <code>null</code>
     *         if no matching choice is found.
     */
    public ASN1Type getType(int tag, int tagclass) {
	Iterator i;
	ASN1Type t;

	for (i = choices_.iterator(); i.hasNext();) {
	    t = (ASN1Type) i.next();
	    if (t.getTag() != tag)
		continue;
	    if (t.getTagClass() == tagclass)
		return t;
	}
	return null;
    }

    public boolean isType(int tag, int tagclass) {
	if (getType(tag, tagclass) != null)
	    return true;

	return false;
    }

    /**
     * Trims the internal list of choices to the actual number of choices stored
     * in it.
     */
    public void trimToSize() {
	choices_.trimToSize();
    }

    /**
     * Clears the internal list of choices. The inner type remains unaffected if
     * it is already set.
     * 
     * number of choices stored in it.
     */
    public void clear() {
	choices_.clear();
    }

    /**
     * Returns the inner ASN.1 type.
     * 
     * @return The inner ASN.1 type.
     */
    public ASN1Type getInnerType() {
	return inner_;
    }

    /**
     * Sets the inner type.
     * 
     * @param t
     *                The type to set as the inner type.
     * @throws NullPointerException
     *                 if the given type is <code>null</code>.
     */
    public void setInnerType(ASN1Type t) {
	if (t == null)
	    throw new NullPointerException("No type given!");

	inner_ = t;
    }

    /**
     * Returns the tag of the inner type.
     * 
     * @return The tag of the inner type.
     * @throws IllegalStateException
     *                 if the inner type is not set.
     */
    public int getTag() {
	if (inner_ == null)
	    throw new IllegalStateException(NO_INNER);

	return inner_.getTag();
    }

    /**
     * Returns the tag class of the inner type.
     * 
     * @return The tag class of the inner type.
     * @throws IllegalStateException
     *                 if the inner type is not set.
     */
    public int getTagClass() {
	if (inner_ == null)
	    throw new IllegalStateException(NO_INNER);

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
     *                 if the inner type is not set.
     */
    public Object getValue() {
	if (inner_ == null)
	    throw new IllegalStateException(NO_INNER);

	return inner_.getValue();
    }

    /**
     * Sets the tagging of the inner type as either EXPLICIT or IMPLICIT. The
     * default is EXPLICIT. Encoders skip the encoding of identifier octets for
     * types that are declared as IMPLICIT.
     * 
     * @param explicit
     *                <code>true</code> if this type shall be tagged EXPLICIT
     *                and <code>false</code> if it shall be encoded IMPLICIT.
     * @throws IllegalStateException
     *                 if the inner type is not set.
     */
    public void setExplicit(boolean explicit) {
	if (!explicit)
	    throw new IllegalArgumentException(
		    "CHOICE types must be tagged EXPLICIT!");
    }

    /**
     * Returns the tagging of the inner type.
     * 
     * @return <code>true</code> if the inner type is tagged EXPLICIT and
     *         <code>false</code> if it is tagged IMPLICIT.
     * @throws IllegalStateException
     *                 if the inner type is not set.
     */
    public boolean isExplicit() {
	return true;
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
     *                 if the inner type is not set.
     */
    public void setConstraint(Constraint constraint) {
	if (inner_ == null)
	    throw new IllegalStateException(NO_INNER);

	inner_.setConstraint(constraint);
    }

    /**
     * Checks the constraint on the inner type if it is set. Otherwise this
     * method returns silently.
     * 
     * @throws ConstraintException
     *                 if this type is not in the appropriate range of values.
     * @throws IllegalStateException
     *                 if the inner type is not set.
     */
    public void checkConstraints() throws ConstraintException {
	if (inner_ == null)
	    throw new IllegalStateException(NO_INNER);

	inner_.checkConstraints();
    }

    /**
     * Encodes this type to the given encoder. Before this method is called, the
     * inner type must be set. Otherwise an IllegalStateException is thrown.
     * <p>
     * 
     * If this method is declared OPTIONAL then still an exception is thrown.
     * The OPTIONAL flag is checked only by {@link Encoder encoders} and
     * {@link Decoder decoders}. Transparent handling of CHOICE types can be
     * achieved by calling <code>{@link Encoder#writeType
     * writeType}(ASN1Choice choice)</code>
     * on the encoder. The encoder's method checks if its argument is OPTIONAL.
     * 
     * @param enc
     *                The {@link Encoder Encoder} to use for encoding.
     * @throws IllegalStateException
     *                 if the inner type is not set.
     */
    public void encode(Encoder enc) throws ASN1Exception, IOException {
	if (inner_ == null)
	    throw new IllegalStateException(NO_INNER);

	enc.writeType(inner_);
    }

    /**
     * Decodes the inner type to the given {@link Decoder decoder}.
     * 
     * @param dec
     *                The decoder to decode to.
     * @throws IllegalStateException
     *                 if the open type cannot be resolved on runtime.
     */
    public void decode(Decoder dec) throws ASN1Exception, IOException {
	dec.readChoice(this);
    }

    /**
     * Returns a string representation of this type.
     * 
     * @return The string representation.
     */
    public String toString() {
	if (inner_ == null)
	    return "CHOICE <NOT InitializED>";

	return "(CHOICE) " + inner_.toString();
    }
}

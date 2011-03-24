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
 * The basic interface for Java objects representing primitive ASN.1 types
 * according to ITU-T Recommendation X.680. A special feature are
 * {@link Constraint constraints}. With constraints the range of valid values
 * of an ASN.1 type can be limited. Constraints are validated for most types in
 * the setter methods allowing initialization with Java types.
 * <p>
 * 
 * An abstract implementation of most of the methods declared in this interface
 * can be found in {@link ASN1AbstractType ASN1AbstractType}.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1Type.java,v 1.2 2000/12/06 17:47:26 vroth Exp $"
 */
public interface ASN1Type {

    public Object getValue();

    public void setOptional(boolean optional);

    public boolean isOptional();

    public int getTag();

    public int getTagClass();

    public void setExplicit(boolean explicit);

    public boolean isExplicit();

    /**
     * Returns <code>true</code> if this type matches the given tag and
     * tagclass. This method is primarily used by decoders in order to verify
     * the tag and tag class of a decoded type. Basic types need not implement
     * this method since {@link ASN1AbstractType ASN1AbstractType} provides a
     * default implementation. Certain variable types such as {@link ASN1Choice
     * ASN1Choice} and {@link ASN1OpenType ASN1OpenType} implement this method.
     * This helps decoders to determine if a decoded type matches a given ASN.1
     * structure.
     * 
     * @param tag
     *                The tag to match.
     * @param tagclass
     *                The tag class to match.
     * @return <code>true</code> if this type matches the given tag and tag
     *         class.
     */
    public boolean isType(int tag, int tagclass);

    public void encode(Encoder enc) throws ASN1Exception, IOException;

    public void decode(Decoder dec) throws ASN1Exception, IOException;

    /**
     * Sets a {@link Constraint constraint} for this type. Constraints are
     * checked by setter methods and as the last operation of a call to the
     * {@link ASN1Type#decode decode()} method.
     * 
     * A number of constraints can be defined in ASN.1; one example is the SIZE
     * constraint on string types. For instance,
     * <tt>foo IA5String (SIZE 10..20)</tt> means the string <tt>foo</tt>
     * can be 10 to 20 characters long. Strings can also be constrained with
     * regard to the character sets. The constraint model of this package allows
     * to add arbitrary constraints on types.
     * <p>
     * 
     * @param o
     *                The constraint to set.
     */
    public void setConstraint(Constraint o);

    /**
     * Returns the {@link Constraint Constraint} of this type or
     * <code>null</code> if there is none.
     * 
     * @return The Constraint or <code>null</code>.
     */
    public Constraint getConstraint();

    /**
     * Checks the {@link Constraint constraints} registered with this instance.
     * 
     * @see Constraint
     * @see ConstraintCollection
     */
    public void checkConstraints() throws ConstraintException;

}

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
 * Represents an ASN.1 SEQUENCE OF type as specified in ITU-T Recommendation
 * X.680. The SequenceOf and SetOf types do not have default constructors in
 * contrast to all the other ASN1Types. The reason is that these types are never
 * created directly on decoding ASN.1 structures. The decoding process always
 * decodes Sequence and Set types because creating the appropriate SequenceOf or
 * SetOf type requires explicit knowledge of the syntactic structure definition.
 * On the other hand, if an explicit structure is given for decoding then the
 * SequenceOf and SetOf types are decoded properly (because they do not have to
 * be created and hence the decoder need not know the component type).
 * <p>
 * 
 * Constraints are checked only after decoding by a call to method
 * {@link #decode decode}.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1SequenceOf.java,v 1.3 2004/08/06 15:16:08 flautens Exp $"
 */
public class ASN1SequenceOf extends ASN1Sequence implements ASN1CollectionOf {
    /**
     * The {@link ASN1Type ASN1Type} from which the component types of this
     * collection are created.
     */
    private Resolver resolver_;

    /**
     * Creates an instance with the given capacity. This constructor is provided
     * for subclasses that wish to handle creation of new elements themselves
     * and do not rely on an application-provided element type.
     * 
     * @param capacity
     *                The initial capacity of the set.
     */
    protected ASN1SequenceOf(int capacity) {
	super(capacity);
    }

    /**
     * Creates an instance that keeps elements of the given type. The type must
     * be a valid {@link ASN1Type ASN1Type}. The given class must be public and
     * it must have a public default constructor.
     * 
     * @param type
     *                The class that represents the component type of this SET
     *                OF.
     * @throws IllegalArgumentException
     *                 if the given class does not implement ASN1Type.
     * @throws NullPointerException
     *                 if <code>type</code> is <code>null</code>.
     */
    public ASN1SequenceOf(Class type) {
	if (type == null)
	    throw new NullPointerException("Need a class!");

	resolver_ = new ClassInstanceResolver(type);
    }

    /**
     * Creates an instance with the given capacity.
     * 
     * @param capacity
     *                The capacity.
     */
    public ASN1SequenceOf(Class type, int capacity) {
	super(capacity);

	if (type == null)
	    throw new NullPointerException("Need a class!");

	resolver_ = new ClassInstanceResolver(type);
    }

    /**
     * Creates an instance that uses the given {@link Resolver Resolver} to
     * create new elements.
     * 
     * @param resolver
     *                The resolver to use for generating elements.
     */
    public ASN1SequenceOf(Resolver resolver) {
	if (resolver == null) {
	    throw new NullPointerException("Need a resolver!");
	}
	resolver_ = resolver;
    }

    /**
     * Returns the Java class representing the ASN.1 type of the elements in
     * this collection or <code>ASN1Type.class
     * </code> if the type cannot be
     * determined.
     * 
     * @return The ASN.1 type of the elements in this collection.
     */
    public Class getElementType() {
	if (resolver_ instanceof ClassInstanceResolver) {
	    return ((ClassInstanceResolver) resolver_).getFactoryClass();
	}
	return ASN1Type.class;
    }

    /**
     * Creates and returns a new instance of the element type of this instance.
     * The freshly created instance is added to this instance automatically.
     * <p>
     * 
     * New instances are created by invoking the <code>Resolver</code>
     * instance set in this instance.
     * <p>
     * 
     * If no new instance can be created then an IllegalStateException is
     * thrown.
     * <p>
     * 
     * <b>{@link Decoder Decoders} should call this method in order to create
     * additional elements on decoding.</b> Subclasses may use this method to
     * keep track on elements added to them.
     * 
     * @return A new instance of the element type of this set.
     * @throws IllegalStateException
     *                 if no new instance could be created.
     */
    public ASN1Type newElement() {
	try {
	    ASN1Type o;

	    o = resolver_.resolve(this);
	    add(o);

	    return o;
	} catch (Exception e) {
	    throw new IllegalStateException("Caught " + e.getClass().getName()
		    + "(\"" + e.getMessage() + "\")");
	}
    }

    /**
     * Reads this collection from the given {@link Decoder Decoder}.
     * 
     * @param dec
     *                The decoder to read from.
     */
    public void decode(Decoder dec) throws ASN1Exception, IOException {
	dec.readCollectionOf(this);
	checkConstraints();
    }
}

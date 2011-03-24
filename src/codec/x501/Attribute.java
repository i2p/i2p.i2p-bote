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
package codec.x501;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import codec.asn1.ASN1Exception;
import codec.asn1.ASN1Null;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1OpenType;
import codec.asn1.ASN1RegisteredType;
import codec.asn1.ASN1Sequence;
import codec.asn1.ASN1Set;
import codec.asn1.ASN1SetOf;
import codec.asn1.ASN1Type;
import codec.asn1.Decoder;
import codec.asn1.DefinedByResolver;
import codec.asn1.OIDRegistry;

/**
 * This class represents an <code>Attribute</code> as defined in X.501
 * standard. The ASN.1 definition of this structure is
 * <p>
 * 
 * <pre>
 * Attribute ::= SEQUENCE {
 *   type         AttributeType,
 *   values       SET OF AttributeValue
 * }
 * AttributeType ::= ObjectIdentifier
 * AttributeValue ::= ANY
 * </pre>
 * 
 * @author Volker Roth
 * @version "$Id: Attribute.java,v 1.4 2007/08/30 08:45:05 pebinger Exp $"
 */

public class Attribute extends ASN1Sequence implements ASN1RegisteredType {
    /**
     * The Object Identifier specifying the attribute type.
     */
    protected ASN1ObjectIdentifier type_;

    /**
     * The List of Attribute values.
     */
    protected ASN1Set values_;

    /**
     * Creates an instance ready for parsing. Any type of ASN.1 structure will
     * be accepted as the values of this attribute. An <code>ASN1OpenType</code>
     * is used for this.
     */
    public Attribute() {
	super(2);

	type_ = new ASN1ObjectIdentifier();
	values_ = new ASN1SetOf(ASN1OpenType.class);

	add(type_);
	add(values_);
    }

    /**
     * Creates an instance ready for parsing. The given
     * {@link OIDRegistry OIDRegistry} is used to resolve the attribute type. If
     * the attribute type cannot be resolved upon decoding then an exception is
     * thrown.
     * 
     * @param registry
     *                The <code>OIDRegistry</code> to use for resolving
     *                attribute value types, or <code>null
     *   </code> if the
     *                global registry shall be used.
     */
    public Attribute(OIDRegistry registry) {
	super(2);

	if (registry == null) {
	    registry = OIDRegistry.getGlobalOIDRegistry();
	}
	type_ = new ASN1ObjectIdentifier();
	values_ = new ASN1SetOf(new DefinedByResolver(registry, type_));

	add(type_);
	add(values_);
    }

    /**
     * Creates a new instance that is initialized with the given OID and value.
     * <b>Note:</b> the given values are not cloned or copied, they are used
     * directly. Hence, the given types must not be modified after hereafter in
     * order to avoid side effects.
     * <p>
     * 
     * The OID must not be <code>null</code>. The <code>
     * value</code> can be
     * <code>null</code> and is replaced by {@link ASN1Null ASN1Null} in that
     * case.
     * 
     * @param oid
     *                The OID that identifies the given value.
     * @param value
     *                The ASN.1 type.
     */
    public Attribute(ASN1ObjectIdentifier oid, ASN1Type value) {
	super(2);

	if (oid == null) {
	    throw new NullPointerException("Need an OID!");
	}
	if (value == null) {
	    value = new ASN1Null();
	}
	type_ = oid;
	values_ = new ASN1Set(1);

	values_.add(value);

	add(oid);
	add(values_);
    }

    /**
     * The arguments passed to this constructor are set up directly for parsing.
     * They are not cloned! The OID of the Attribute is the OID returned by the
     * registered type.
     * 
     * @param value
     *                The registered ASN.1 type.
     */
    public Attribute(ASN1RegisteredType value) {
	super(2);

	if (value == null) {
	    throw new NullPointerException("Need a value!");
	}
	type_ = value.getOID();

	if (type_ == null) {
	    throw new NullPointerException("Value does not provide an OID!");
	}
	values_ = new ASN1Set(1);
	values_.add(value);

	add(type_);
	add(values_);
    }

    /**
     * This method returns the OID of this Attribute.
     * 
     * @return The OID
     */
    public ASN1ObjectIdentifier getOID() {
	return type_;
    }

    /**
     * This method returns an unmodifiable view of the list of values of this
     * Attribute.
     * 
     * @return The unmodifiable view of the list of attribute values.
     */
    public List valueList() {
	return Collections.unmodifiableList(values_);
    }

    /**
     * returns the number of values in this attribute.
     * 
     * @return The number of values.
     */
    public int valueCount() {
	return values_.size();
    }

    /**
     * Returns the value at the given position where position is between 0 and
     * <code>valueCount()-1</code>.
     * 
     * @return The value at the given position.
     * @throws ArrayIndexOutOfBoundsException
     *                 if the given position is not within the bounds of the
     *                 list of attribute values.
     */
    public ASN1Type valueAt(int index) {
	return (ASN1Type) values_.get(index);
    }

    /**
     * Decodes this instance. If the internal storage object of attributes is a
     * <code>ASN1SetOf</code> then that set is transformed into a
     * <code>ASN1Set</code>, and any <code>ASN1OpenType</code> instances
     * are stripped away. This makes a number of internal objects available for
     * garbage collection.
     * <p>
     * 
     * Consequently, after decoding this instance contains a set with the pure
     * attribute values.
     * 
     * @param dec
     *                The decoder to use.
     */
    public void decode(Decoder dec) throws IOException, ASN1Exception {
	super.decode(dec);

	if (!(values_ instanceof ASN1SetOf)) {
	    return;
	}
	ArrayList list;
	ASN1Type o;
	Iterator i;

	try {
	    list = new ArrayList(values_.size());

	    for (i = values_.iterator(); i.hasNext();) {
		o = (ASN1Type) i.next();

		if (o instanceof ASN1OpenType) {
		    o = ((ASN1OpenType) o).getInnerType();
		}
		list.add(o);
	    }
	    values_.clear();
	    values_.addAll(list);
	} catch (ClassCastException e) {
	    throw new ASN1Exception("Unexpected type in SET OF!");
	} catch (NullPointerException e) {
	    throw new ASN1Exception("NULL in SET OF!");
	}
    }
}

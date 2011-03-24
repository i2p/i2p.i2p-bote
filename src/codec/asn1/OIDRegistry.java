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

import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class maps ASN.1 object identifiers onto ASN.1 types suitable for
 * decoding the structure defined by the given OID. It is modelled along the
 * lines of the ClassLoader and provides a hierarchy and top-level OID
 * registries.
 * 
 * @author Volker Roth
 * @version "$Id: OIDRegistry.java,v 1.4 2004/08/09 08:32:22 flautens Exp $"
 */
public class OIDRegistry extends Object {

    /**
     * The list of global OID registries.
     */
    private static Set registries_ = Collections.synchronizedSet(new HashSet());

    /**
     * The global instance of OID registries.
     */
    private static OIDRegistry global_ = new OIDRegistry();

    /**
     * The parent OID registry.
     */
    private OIDRegistry parent_ = null;

    /**
     * Creates an OID registry.
     */
    private OIDRegistry() {
    }

    /**
     * This method returns the global OIDRegistry instance that may be used for
     * querying
     * 
     * @return The global OID registry.
     */
    final static public OIDRegistry getGlobalOIDRegistry() {
	return global_;
    }

    /**
     * Adds a registry to the set of globally known ones unless it is already in
     * the global set. This method checks the permission
     * <p>
     * 
     * {@link ASN1Permission ASN1Permission}, &quot; OIDRegistry.add&quot;
     * <p>
     * 
     * The reference to the parent registry of the given registry is cleared
     * before it is added.
     * 
     * @param r
     *                The registry to add.
     * @throws SecurityException
     *                 iff the caller has no right to add registries to the
     *                 global ones.
     */
    final public static void addOIDRegistry(OIDRegistry r) {
	if (r == null) {
	    return;
	}
	AccessController.checkPermission(new ASN1Permission("OIDRegistry.add"));

	r.parent_ = null;

	registries_.add(r);
    }

    /**
     * Removes the given OID registry from the set of globally known ones. This
     * method checks the permission
     * <p>
     * 
     * {@link ASN1Permission ASN1Permission}, &quot; OIDRegistry.remove&quot;
     * 
     * @param r
     *                The registry to remove.
     * @throws SecurityException
     *                 iff the caller has no right to remove OID registries.
     */
    final public static void removeOIDRegistry(OIDRegistry r) {
	if (r == null) {
	    return;
	}
	AccessController.checkPermission(new ASN1Permission(
		"OIDRegistry.remove"));

	registries_.remove(r);
    }

    /**
     * Creates an OID registry with the given parent. If an OID is not found by
     * this registry then the search is delegated to the parent registry.
     * 
     * @param parent
     *                The parent OID registry.
     */
    public OIDRegistry(OIDRegistry parent) {
	parent_ = parent;
    }

    /**
     * Retrieves an ASN.1 type based on the given OID. If no type is found then
     * <code>null</code> is returned. This method first calls {@link
     * #getLocalASN1Type(ASN1ObjectIdentifier) getLocalASN1Type}. If no ASN.1
     * type is found for the given OID then <code>getASN1Type</code> is called
     * for the parent OIDRegistry.
     * 
     * @param oid
     *                The registered OID of the desired type.
     * @return The type or <code>null</code> if no type with the given OID is
     *         known.
     */
    final public ASN1Type getASN1Type(ASN1ObjectIdentifier oid) {
	ASN1Type o;

	o = getLocalASN1Type(oid);

	if (o == null && parent_ != null) {
	    return parent_.getASN1Type(oid);
	}
	return o;
    }

    /**
     * Retrieves an ASN.1 type for the given OID or <code>null</code> if no
     * such type was found.
     * <p>
     * 
     * This method should be overridden by subclasses. Subclasses should
     * retrieve a pointer to their private synchronized Map with OID to String
     * (or Class) mappings, and call the method with the same name but which
     * takes an additional Map.
     * <p>
     * 
     * This implementation searches the global registries for a matching entry.
     */
    protected ASN1Type getLocalASN1Type(ASN1ObjectIdentifier oid) {
	Iterator i;
	ASN1Type o;
	OIDRegistry r;

	for (i = registries_.iterator(); i.hasNext();) {
	    r = (OIDRegistry) i.next();
	    o = r.getASN1Type(oid);

	    if (o != null) {
		return o;
	    }
	}
	return null;
    }

    /**
     * Retrieves an ASN.1 type for the given OID or <code>null</code> if no
     * such type was found. Strings in the given <code>Map</code> are replaced
     * by the resolved classes.
     * <p>
     * 
     * This is a convenience method that can be called by subclasses with a Map
     * that is specific to the subclass.
     * 
     * @param oid
     *                The OID that is resolved.
     * @param map
     *                The <code>Map</code> that holds the OID to class (name)
     *                mapping. The <code>Map</code> values consist either of
     *                strings (class names) or of the resolved class objects.
     */
    protected ASN1Type getLocalASN1Type(ASN1ObjectIdentifier oid, Map map) {
	Object o;
	Class c;

	if (oid == null || map == null) {
	    throw new NullPointerException("oid or map");
	}
	o = map.get(oid);

	if (o == null) {
	    return null;
	}
	try {
	    if (o instanceof String) {
		c = Class.forName((String) o);

		map.put(new ASN1ObjectIdentifier(oid.getOID()), c);

		o = c;
	    }
	    c = (Class) o;

	    return (ASN1Type) c.newInstance();
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    return null;
	}
    }

    /**
     * This method returns the default OID registry. Subclasses should override
     * this method and provide a static instance of an OID registry that
     * delegates to the global OID registry if a requested OID could not be
     * found locally. This implementation returns the global OID registry by
     * default.
     * 
     * @return The default OID registry.
     */
    static public OIDRegistry getDefaultRegistry() {
	return OIDRegistry.getGlobalOIDRegistry();
    }

    /**
     * An OIDRegistry equals another iff both are of the same class.
     * 
     * @return <code>true</code> if both registries are of the same class.
     */
    public boolean equals(Object o) {
	if (getClass() == o.getClass()) {
	    return true;
	}
	return false;
    }

    /**
     * The hash code of an instance is the hash code of its class. This is
     * required to be consistent with the {@link #equals equals()} method.
     */
    public int hashCode() {
	return getClass().hashCode();
    }

}

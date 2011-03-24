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

/**
 * This interface is used by the {@link ASN1OpenType ASN1OpenType} in order to
 * resolve the ASN.1 type to decode at runtime. Concrete implementations of this
 * interface can be used to model references to type classes as well or to
 * compensate for the superseded ASN.1 ANY DEFINED BY type.
 * <p>
 * 
 * Implementations shall determine and return the correct ASN.1 type to be
 * decoded in the defined method.
 * 
 * @author Volker Roth
 * @version "$Id: DefinedByResolver.java,v 1.2 2000/12/06 17:47:28 vroth Exp $"
 */
public class DefinedByResolver extends Object implements Resolver {
    private OIDRegistry registry_;
    private ASN1ObjectIdentifier oid_;

    /**
     * Creates an instance that attempts to resolve the given OID against the
     * given registry upon calling {@link #resolve resolve}. The OID instance
     * used or resolving is the one passed to this constructor. Hence, an OID
     * can be added to a compound ASN.1 type and an {@link ASN1OpenType
     * ASN1OpenType} can be initialized with this. If the OID is decoded before
     * the open type then the open type is resolved against the given registry
     * and the decoded OID. In other words the ASN.1 ANY DEFINED BY type can be
     * modelled with an ASN1OpenType and an instance of this resolver class.
     * 
     * @param registry
     *                The registry to resolve the given OID against.
     * @param oid
     *                The oid instance to use when resolving.
     */
    public DefinedByResolver(OIDRegistry registry, ASN1ObjectIdentifier oid) {
	if (registry == null || oid == null)
	    throw new NullPointerException("Registry or OID is null!");

	registry_ = registry;
	oid_ = oid;
    }

    /**
     * Creates an instance that resolves the given OID against the
     * {@link OIDRegistry#getGlobalOIDRegistry global OID registry}.
     * 
     * @param oid
     *                The OID to resolve.
     */
    public DefinedByResolver(ASN1ObjectIdentifier oid) {
	if (oid == null)
	    throw new NullPointerException("OID is null!");

	registry_ = OIDRegistry.getGlobalOIDRegistry();
	oid_ = oid;
    }

    /**
     * Looks up the private OID in the private registry and returns the resolved
     * ASN.1 type. If the OID cannot be resolved against the registry then an
     * exception is thrown.
     * 
     * @param caller
     *                The calling ASN.1 type.
     * @throws ResolverException
     *                 if the private OID cannot be mapped onto an ASN.1 type by
     *                 the private registry.
     */
    public ASN1Type resolve(ASN1Type caller) throws ResolverException {
	ASN1Type t;

	t = registry_.getASN1Type(oid_);
	if (t == null) {
	    throw new ResolverException("Cannot resolve " + oid_);
	}
	return t;
    }
}

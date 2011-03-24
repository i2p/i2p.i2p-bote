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
package codec.util;

import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A number of invariants must hold for the properties defined by the installed
 * providers such that this class can work properly:
 * <ul>
 * <li> Aliases must be mapped to standard JCA/JCE names whenever possible. All
 * Aliases for an engine must map to the same name.
 * <li> Two OID mappings with the same OID must map to the same name.
 * <li> The slashed form of signature names must be set up as an alias.
 * <li> Signature engines that do not have a corresponding cipher engine still
 * require a reverse OID mapping of the form Alg.Alias.Cipher.OID.<i>oid</i> =
 * <i>name</i>, where <i>name</i> is the cipher name component of a slashed
 * form alias for that signature engine.
 * </ul>
 * 
 * @author Volker Roth
 * @version "$Id: JCA.java,v 1.5 2004/08/11 14:33:49 flautens Exp $"
 */
public class JCA extends Object {
    /**
     * The digest/cipher name to signature algorithm name mapping.
     */
    private static Map dc2s_ = new HashMap();

    /**
     * The signature algorithm name to digest/cipher name mapping.
     */
    private static Map s2dc_ = new HashMap();

    /**
     * The root alias map. Each map entry consists of the lower case engine name
     * mapped to another map that holds the aliases for that engine.
     */
    protected static Map aliases_ = initAliasLookup();

    /**
     * Let no-one create an instance.
     * 
     */
    private JCA() {
    }

    /**
     * Reads the properties of the installed providers and builds an optimized
     * alias lookup table. All entries of the form
     * <ol>
     * <li> &quot;Alg.Alias.&quot;+&lt;engine&gt;+&quot;.&quot;+&lt;alias&gt; =
     * &lt;value&gt;
     * <li> &quot;Alg.Alias.&quot;+&lt;engine&gt;+&quot;.OID.&quot;+&lt;oid&gt; =
     * &lt;value&gt;
     * <li> &quot;Alg.Alias.&quot;+&lt;engine&gt;+&quot;.&quot;+&lt;oid&gt; =
     * &lt;value&gt;
     * </ol>
     * are transformed and stored in a hashmap which is used by this class in
     * order to do quick lookups of aliases and OID mappings. The stored entries
     * are of the form:
     * <ol>
     * <li> &lt;engine&gt;+&quot;.&quot;+&lt;alias&gt; = &lt;value&gt;
     * <li> &quot;oid.&quot;+&lt;value&gt; = &lt;oid&gt;
     * <li> &quot;oid.&quot;+&lt;oid&gt; = &lt;value&gt;
     * </ol>
     * In case multiple providers define mappings for the same keys the mapping
     * of the first registered provider wins.
     */
    static private Map initAliasLookup() {
	Enumeration e;
	Provider[] provider;
	String k; // key
	String v; // value
	String s; // string
	String p; // previous mapping
	Map map;
	int i;
	int j;

	map = new HashMap();
	provider = Security.getProviders();

	/*
	 * We start from the last provider and work our way to the first one
	 * such that aliases of preferred providers overwrite entries of less
	 * favoured providers.
	 */
	for (i = provider.length - 1; i >= 0; i--) {
	    e = provider[i].propertyNames();

	    while (e.hasMoreElements()) {
		k = (String) e.nextElement();
		v = provider[i].getProperty(k);

		if (!k.startsWith("Alg.Alias.")) {
		    continue;
		}
		/*
		 * Truncate k to <engine>.<alias>
		 */
		k = k.substring(10).toLowerCase();
		j = k.indexOf('.');

		if (j < 1) {
		    continue;
		}
		/*
		 * Copy <engine> to s Truncate k to <alias>
		 */
		s = k.substring(0, j);
		k = k.substring(j + 1);

		if (k.length() < 1) {
		    continue;
		}
		/*
		 * If <alias> starts with a digit then we assume it is an OID.
		 * OIDs are uniquely defined, hence we ommit <engine> in the oid
		 * mappings. But we also include the alias mapping for this oid.
		 */
		if (Character.isDigit(k.charAt(0))) {
		    p = (String) map.get("oid." + k);

		    if (p != null && p.length() >= v.length()) {
			continue;
		    }
		    map.put("oid." + k, v);
		    map.put(s + "." + k, v);
		}
		/*
		 * If <alias> starts with the string "OID." then we found a
		 * reverse mapping. In that case we swap <alias> and the value
		 * of the mapping, and make an entry of the form "oid."+<value> =
		 * <oid>
		 */
		else if (k.startsWith("oid.")) {
		    k = k.substring(4);
		    v = v.toLowerCase();

		    map.put("oid." + v, k);
		}
		/*
		 * In all other cases we make an entry of the form <engine>+"."+<alias> =
		 * <value> as is defined in the providers.
		 */
		else {
		    map.put(s + "." + k, v);
		}
	    }
	}
	// System.out.println("MAP : "+map);
	return map;
    }

    /*
     * static private void dump(Map map) { Iterator i; Map.Entry entry;
     * 
     * for (i=map.entrySet().iterator(); i.hasNext();) { entry =
     * (Map.Entry)i.next(); System.out.println( entry.getKey()+" =
     * "+entry.getValue()); } }
     */

    /**
     * Returns the JCA standard name for a given OID. The OID must be a string
     * of numbers separated by dots, and can be preceded by the prefix
     * &quot;OID.&quot;. If the OID is not defined in a mapping of some
     * registered provider then <code>null</code> is returned.
     * <p>
     * 
     * OID mappings are unambigous; no engine type is required for the mapping
     * and no engine type is returned as part of the result. The returned string
     * consists only of the name of the algorithm.
     * 
     * @param oid
     *                The string with the OID that shall be resolved.
     * @return The standard JCA engine name for the given OID or
     *         <code>null</code> if no such OID is defined.
     * @throws NullPointerException
     *                 if the oid is <code>null</code>.
     */
    public static String getName(String oid) {
	if (oid == null) {
	    throw new NullPointerException("OID is null!");
	}
	if (oid.startsWith("OID.") || oid.startsWith("oid.")) {
	    oid = oid.substring(4);
	}
	return (String) aliases_.get("oid." + oid);
    }

    /**
     * Resolves the given alias to the standard JCA name for the given engine
     * type. If no appropriate mapping is defined then <code>null</code> is
     * returned. If the given alias is actually an OID string and there is an
     * appropriate alias mapping defined for that OID by some provider then the
     * corresponding JCA name is returned.
     * 
     * @param engine
     *                The JCA engine type name.
     * @param alias
     *                The alias to resolve for the given engine type.
     * @return The standard JCA name or <code>null</code> if no appropriate
     *         mapping could be found.
     * @throws IllegalArgumentException
     *                 if the alias is an empty string.
     * @throws NullPointerException
     *                 if the alias or engine name is <code>null</code>.
     */
    public static String resolveAlias(String engine, String alias) {
	if (alias == null || engine == null) {
	    throw new NullPointerException("Engine or alias is null!");
	}
	if (alias.length() < 1) {
	    throw new IllegalArgumentException("Zero-length alias!");
	}
	return (String) aliases_.get(engine.toLowerCase() + "."
		+ alias.toLowerCase());
    }

    /**
     * Returns the OID of the given algorithm name. The given name must be the
     * JCA standard name of the algorithm and not an alias. Use
     * {@link #resolveAlias resolveAlias} to map aliases onto their standard
     * names.
     * 
     * @param algorithm
     *                The JCA standard name of the algorithm for which the OID
     *                should be returned.
     * @return The OID or <code>null</code> if no appropriate mapping could be
     *         found.
     * @throws NullPointerException
     *                 if engine or algorithm is <code>null</code>.
     */
    public static String getOID(String algorithm) {
	if (algorithm == null) {
	    throw new NullPointerException("Algorithm is null!");
	}
	if (algorithm.length() < 1) {
	    throw new IllegalArgumentException("Algorithm name is empty!");
	}
	if (Character.isDigit(algorithm.charAt(0))) {
	    return algorithm;
	}
	return (String) aliases_.get("oid." + algorithm.toLowerCase());
    }

    /**
     * Returns the OID of the given algorithm name. The given engine name is
     * taken as a hint if the given algorithm name is a non-standard name. In
     * that case one shot is given to alias resolving before a second attempt is
     * made to map the algorithm to an OID. Alias resolving is done by means of
     * the {@link #resolveAlias resolveAlias} method.
     * 
     * @param algorithm
     *                The JCA standard name of the algorithm for which the OID
     *                should be returned.
     * @param engine
     *                The engine name that is taken as a hint for alias
     *                resolving if the algorithm name cannot be resolved in the
     *                first attempt.
     * @return The OID or <code>null</code> if no appropriate mapping could be
     *         found.
     * @throws NullPointerException
     *                 if engine or algorithm is <code>null</code>.
     */
    public static String getOID(String algorithm, String engine) {
	String oid;

	oid = getOID(algorithm);

	if (oid != null) {
	    return oid;
	}
	algorithm = resolveAlias(engine, algorithm);

	if (algorithm == null) {
	    return null;
	}
	return getOID(algorithm);
    }

    /**
     * This method maps a given digest algorithm OID and cipher algorithm OID
     * onto the standard name of the combined signature algorithm. For this to
     * work the aliases must be well defined such as described below:
     * <dl>
     * <dt> Digest Algorithm
     * <dd> Alg.Alias.MessageDigest.<i>oid</i><sub>1</sub> = <i>digestAlg</i>
     * <dt> Cipher Algorithm
     * <dd> Alg.Alias.Cipher.<i>oid</i><sub>2</sub> = <i>cipherAlg</i>
     * <dt> Signature Algorithm
     * <dd> Alg.Alias.Signature.<i>digestAlg</i>/<i>cipherAlg</i> =
     * <i>signatureAlg</i>
     * </dl>
     * The <i>oid</i> denotes the sequence of OID numbers separated by dots but
     * without a leading &quot;OID.&quot;. In some cases, such as the DSA, there
     * is no cipher engine corresponding to <i>oid</i><sub>2</sub>. In this
     * case, <i>oid</i><sub>2</sub> must be mapped to the corresponding name
     * by other engine types, such as a KeyFactory.
     * <p>
     * 
     * All found mappings are cached for future use, as well as the reverse
     * mapping, which is much more complicated to synthesise.
     * 
     * @param doid
     *                The string representation of the digest algorithm OID. The
     *                OID must have a &quot;OID.&quot; prefix.
     * @return The standard JCE name of the signature algorithm or
     *         <code>null</code> if no mapping could be found.
     */
    public static String getSignatureName(String doid, String coid) {
	String dn;
	String cn;
	String sn;
	String dc;

	dn = getName(doid);
	cn = getName(coid);

	if (dn == null || cn == null) {
	    return null;
	}
	dc = dn + "/" + cn;

	synchronized (dc2s_) {
	    sn = (String) dc2s_.get(dc);

	    if (sn != null) {
		return sn;
	    }
	}
	sn = resolveAlias("signature", dc);

	if (sn != null) {
	    synchronized (dc2s_) {
		cn = dc.toLowerCase();

		if (!dc2s_.containsKey(cn)) {
		    dc2s_.put(cn, sn);
		}
	    }
	    synchronized (s2dc_) {
		cn = sn.toLowerCase();

		if (!s2dc_.containsKey(cn)) {
		    s2dc_.put(cn, dc);
		}
	    }
	}
	return sn;
    }

    /**
     * This method maps the standard signature algorithm name to the
     * <i>digestAlg</i>/<i>cipherAlg</i> format. This format can be used to
     * retrieve the OID of the digest algorithm and cipher algorithm
     * respectively. For this to work the aliases must be well defined such as
     * described below:
     * <dl>
     * <dt> Signature Algorithm
     * <dd> Alg.Alias.Signature.<i>d</i>/<i>c</i> = <i>sigAlg</i> where
     * <i>d</i> denotes the digest algorithm and <i>c</i> the cipher
     * algorithm. <i>sigAlg</i> must be the name under which the algorithm
     * engine is published.
     * </dl>
     * 
     * If <code>sigAlg</code> contains a &quot;/&quot; then we assume that the
     * given algorithm name is already of the desired form and return
     * <code>sigAlg</code>.
     * 
     * @param sigAlg
     *                The standard signature algorithm name.
     * @return The <i>digestAlg</i>/<i>cipherAlg</i> format of the given
     *         signature algorithm name or <code>
     *   null</code> if no suitable
     *         mapping could be found.
     */
    public static String getSlashedForm(String sigAlg) {
	String v;

	if (sigAlg.indexOf("/") > 0) {
	    return sigAlg;
	}
	sigAlg = sigAlg.toLowerCase();

	synchronized (s2dc_) {
	    v = (String) s2dc_.get(sigAlg);

	    if (v != null) {
		return v;
	    }
	}
	Iterator i;
	String k;
	int m;

	for (i = aliases_.keySet().iterator(); i.hasNext();) {
	    k = (String) i.next();

	    if (!k.startsWith("signature.")) {
		continue;
	    }
	    v = (String) aliases_.get(k);

	    if (!v.equalsIgnoreCase(sigAlg)) {
		continue;
	    }
	    k = k.substring(10);
	    m = k.indexOf("/");

	    if (m < 0) {
		continue;
	    }
	    synchronized (s2dc_) {
		if (!s2dc_.containsKey(sigAlg)) {
		    s2dc_.put(sigAlg, k);
		}
	    }
	    return k;
	}
	return null;
    }

    /**
     * This method maps the given standard signature algorithm name to the
     * string representation of the OID associated with the digest algorithm of
     * the given signature algorithm.
     * 
     * @param sigAlg
     *                The standard signature algorithm name.
     * @return The string representation of the OID associated with the digest
     *         alorithm used for <code>sigAlg</code>.
     */
    public static String getDigestOID(String sigAlg) {
	int n;
	String v;
	String h;
	String r;

	v = getSlashedForm(sigAlg);

	if (v == null) {
	    return null;
	}
	n = v.indexOf("/");

	if (n < 0) {
	    return null;
	}
	h = v.substring(0, n);
	r = getOID(h);

	if (r != null) {
	    return r;
	}
	/*
	 * We now try to "repair" the bad algorithm name if we find a fitting
	 * alias instead.
	 */
	h = resolveAlias("MessageDigest", h);

	if (h == null) {
	    return null;
	}
	r = getOID(h);

	if (r != null) {
	    v = h + "/" + v.substring(n + 1);

	    synchronized (s2dc_) {
		s2dc_.put(sigAlg, v);
	    }
	}
	return r;
    }

    /**
     * This method maps the given standard signature algorithm name to the
     * string representation of the OID associated with the cipher algorithm of
     * the given signature algorithm.
     * <p>
     * This conversion is a bit tricky. In cases such as DSA, no corresponding
     * Cipher engine exists, since DSA is not designed to be used as a cipher.
     * In such cases, some provider needs to set up a bogus alias of the form:
     * <dl>
     * <dt> Signature Algorithm
     * <dd> Alg.Alias.Cipher.OID.<i>oid</i> = DSA
     * </dl>
     * 
     * The <i>oid</i> denotes the sequence of OID numbers separated by dots but
     * without a leading &quot;OID.&quot;.
     * 
     * @param sigAlg
     *                The standard signature algorithm name.
     * @return The string representation of the OID associated with the cipher
     *         alorithm used for <code>sigAlg</code>.
     */
    public static String getCipherOID(String sigAlg) {
	int n;
	String s;
	String v;
	String r;

	v = getSlashedForm(sigAlg);

	if (v == null) {
	    return null;
	}
	n = v.indexOf("/");

	if (n < 0) {
	    return null;
	}
	s = v.substring(n + 1);
	r = getOID(s);

	if (r != null) {
	    return r;
	}
	/*
	 * We now try to "repair" the bad algorithm name if we find a fitting
	 * alias instead.
	 */
	s = resolveAlias("Signature", s);

	if (s == null) {
	    return null;
	}
	r = getOID(s);

	if (r != null) {
	    v = v.substring(0, n) + "/" + s;

	    synchronized (s2dc_) {
		s2dc_.put(sigAlg, v);
	    }
	}
	return r;
    }

}

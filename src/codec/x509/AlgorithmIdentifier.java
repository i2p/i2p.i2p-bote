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
package codec.x509;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import codec.CorruptedCodeException;
import codec.InconsistentStateException;
import codec.asn1.ASN1;
import codec.asn1.ASN1Exception;
import codec.asn1.ASN1Null;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1Opaque;
import codec.asn1.ASN1Sequence;
import codec.asn1.ASN1Type;
import codec.asn1.DEREncoder;
import codec.pkcs8.PrivateKeyInfo;
import codec.util.JCA;

/**
 * This class represents the ASN.1/DER value of the AlgorithmIdentifier defined
 * in Annex D to Recommendation X.509. This structure is extensively used for
 * instance in the PKCS standards of RSA Inc. The ASN.1 definition of this
 * structure is as given below:
 * <p>
 * 
 * <pre>
 * AlgorithmIdentifier  ::= SEQUENCE{
 *   algorithm  OBJECT IDENTIFIER,
 *   parameters ANY DEFINED BY algorithm OPTIONAL
 * }
 * </pre>
 * 
 * <p>
 * For this class to work properly, providers need to define the following
 * algorithm aliases for the {@link java.security.AlgorithmParameters
 * AlgorithmParameters} implementations they provide:
 * <ol>
 * <li> AlgorithmParameters.MyAlg = <i>class</i>
 * <li> Alg.Alias.AlgorithmParameters.1.2.3.4 = MyAlg
 * <li> Alg.Alias.AlgorithmParameters.OID.1.2.3.4 = MyAlg
 * </ol>
 * The first property defined the mapping of the JCE compliant standard name of
 * the algorithm to the implementing class. The second provider entry allows
 * mapping OID to those algorithm names while the third allows mapping those
 * names on corresponding OID.
 * <p>
 * 
 * The alias definitions are used by this class in order to find an
 * AlgorithmParameters implementation for the OID embedded in the X.509
 * AlgorithmIdentifier structure, and to create the OID for a given
 * AlgorithmParameters instances. This is done by means of the {@link JCA JCA}
 * class, which operates on the engine and alias definitions of the installed
 * providers.
 * <p>
 * 
 * 
 * 
 * 
 * @author Volker Roth
 * @version "$Id: AlgorithmIdentifier.java,v 1.3 2004/08/13 11:37:03 pebinger
 *          Exp $"
 * @see JCA
 */
public class AlgorithmIdentifier extends ASN1Sequence {

    /**
     * The algorithm parameters of the algorithm specified by this algorithm
     * identifier.
     */
    protected ASN1Opaque parameters_;

    /**
     * The OID of the algorithm.
     */
    protected ASN1ObjectIdentifier algorithm_;

    /**
     * Creates an {@link AlgorithmIdentifier AlgorithmIdentifier} from the given
     * key. The key must be either a public key or a private key. No secret
     * (symmetric) keys are accepted.
     * <p>
     * 
     * The keys encoding must be either a a {@link PrivateKeyInfo
     * PrivateKeyInfo} or a {@link SubjectPublicKeyInfo SubjectPublicKeyInfo}.
     * 
     * @param key
     *                The key from which the AlgorithmIdentifier shall be
     *                extracted.
     * @throws IllegalArgumentException
     *                 if the given key is neither a PublicKey or a PrivateKey.
     * @throws CorruptedCodeException
     *                 if an exception was caught while decoding the key's
     *                 encoding.
     */
    public static AlgorithmIdentifier createAlgorithmIdentifier(Key key)
	    throws CorruptedCodeException {
	try {
	    if (key instanceof PublicKey) {
		return new SubjectPublicKeyInfo((PublicKey) key)
			.getAlgorithmIdentifier();
	    }

	    if (key instanceof PrivateKey) {
		return new PrivateKeyInfo((PrivateKey) key)
			.getAlgorithmIdentifier();
	    }

	    throw new IllegalArgumentException("Key type not supported!");

	} catch (InvalidKeyException e) {
	    throw new CorruptedCodeException("Error decoding key!");
	}
    }

    /**
     * This method builds the tree of ASN.1 objects used for decoding this
     * structure.
     */
    public AlgorithmIdentifier() {
	super(2);

	algorithm_ = new ASN1ObjectIdentifier();
	parameters_ = new ASN1Opaque();
	parameters_.setOptional(true);
	add(algorithm_);
	add(parameters_);
    }

    /**
     * Creates an instance initialized to the given algorithm. The algorithm
     * must not have parameters since this constructor does not take a parameter
     * argument.
     * 
     * @param algorithm
     *                The JCE standard algorithm name.
     * @throws NoSuchAlgorithmException
     *                 if the name cannot be resolved to an OID or the OID has a
     *                 bad syntax.
     * @throws NullPointerException
     *                 if <code>algorithm</code> is <code>null</code>.
     */
    public AlgorithmIdentifier(String algorithm)
	    throws NoSuchAlgorithmException {
	super(2);

	if (algorithm == null)
	    throw new NullPointerException("Need an algorithm name!");

	String oid;

	oid = JCA.getOID(algorithm);
	if (oid == null)
	    throw new NoSuchAlgorithmException("No OID alias for algorithm "
		    + algorithm);

	try {
	    algorithm_ = new ASN1ObjectIdentifier(oid);
	} catch (IllegalArgumentException e) {
	    throw new NoSuchAlgorithmException("Bad OID alias for algorithm "
		    + algorithm);
	}
	parameters_ = new ASN1Opaque(ASN1.TAG_NULL, ASN1.CLASS_UNIVERSAL,
		new byte[0]);

	add(algorithm_);
	add(parameters_);
    }

    /**
     * Creates an instance with the given OID and opaque algorithm parameter
     * representation. Both the given OID and the parameter encoding is cloned
     * or copied. No side effects occur if these arguments are modified after
     * completition of this constructor.
     * 
     * @param oid
     *                The algorithm object identifier.
     * @param b
     *                The opaque DER encoding of the parameters for the
     *                algorithm known under the given OID. If no parameters are
     *                required then <code>null</code> might be passed. In that
     *                case {@link ASN1Null ASN.1 NULL} is encoded.
     * @throws ASN1Exception
     *                 if the opaque representation does not contain a valid DER
     *                 header and contents octets.
     */
    public AlgorithmIdentifier(ASN1ObjectIdentifier oid, byte[] b)
	    throws ASN1Exception {
	super(2);

	if (oid == null)
	    throw new NullPointerException("Need an OID!");

	algorithm_ = (ASN1ObjectIdentifier) oid.clone();

	if (b == null) {
	    /*
	     * Usually, we'd define the following type as OPTIONAl. However, in
	     * case no parameters are given a NULL is set instead.
	     */
	    parameters_ = new ASN1Opaque(ASN1.TAG_NULL, ASN1.CLASS_UNIVERSAL,
		    new byte[0]);
	} else
	    parameters_ = new ASN1Opaque(b);

	add(algorithm_);
	add(parameters_);
    }

    /**
     * Creates an instance that is initialized from the given
     * AlgorithmParameters instance. This method attempts to map the algorithm
     * name to an ASN.1 OID by calling {@link JCA#getOID(String) JCA#getOID}.
     * <p>
     * 
     * @param alg
     *                The name of the algorithm.
     * @param params
     *                The AlgorithmParameters.
     * @throws NullPointerException
     *                 if <code>alg</code> is <code>null</code>.
     * @throws InvalidAlgorithmParameterException
     *                 if the given parameters have a bad encoding, or the OID
     *                 of the algorithm cannot be determined.
     */
    public AlgorithmIdentifier(String alg, AlgorithmParameters params)
	    throws InvalidAlgorithmParameterException {
	super(2);

	String s;

	if (alg == null)
	    throw new NullPointerException("Algorithm is null!");

	s = JCA.getOID(alg);

	if (s == null) {
	    s = "1.3.14.3.2.7"; // DES_CBC
	}

	try {
	    algorithm_ = new ASN1ObjectIdentifier(s);
	} catch (IllegalArgumentException e) {
	    throw new InvalidAlgorithmParameterException(
		    "Bad OID alias for algorithm " + params.getAlgorithm());
	}

	try {
	    if (params == null) {
		parameters_ = new ASN1Opaque(ASN1.TAG_NULL,
			ASN1.CLASS_UNIVERSAL, new byte[0]);
	    } else {
		parameters_ = new ASN1Opaque(params.getEncoded());
	    }
	} catch (IOException e) {
	    throw new InvalidAlgorithmParameterException(
		    "Error during parameter encoding!");
	} catch (ASN1Exception e) {
	    throw new InvalidAlgorithmParameterException(
		    "Parameter encoding is not ASN.1/DER!");
	}

	add(algorithm_);
	add(parameters_);
    }

    /**
     * Creates an instance with the given OID and parameters. The parameters are
     * encoded according to DER and stored by means of an opaque type. If the
     * given parameters are <code>null</code> then an ASN.1 NULL is encoded.
     * 
     * @param oid
     *                The OID to use.
     * @param params
     *                The ASN.1 type of which the parameters consist.
     * @throws InconsistentStateException
     *                 if an internal error occurs; this should never happen.
     * @throws ASN1Exception
     *                 if the given parameters cannot be encoded. This should
     *                 rarely happen.
     */
    public AlgorithmIdentifier(ASN1ObjectIdentifier oid, ASN1Type params)
	    throws ASN1Exception {
	super(2);

	DEREncoder enc;
	ByteArrayOutputStream bos;

	if (oid == null)
	    throw new NullPointerException("Need an OID!");

	algorithm_ = (ASN1ObjectIdentifier) oid.clone();

	try {
	    if (params == null || (params instanceof ASN1Null))
		parameters_ = new ASN1Opaque(ASN1.TAG_NULL,
			ASN1.CLASS_UNIVERSAL, new byte[0]);
	    else {
		bos = new ByteArrayOutputStream();
		enc = new DEREncoder(bos);
		params.encode(enc);

		parameters_ = new ASN1Opaque(bos.toByteArray());
		bos.close();
	    }
	    add(algorithm_);
	    add(parameters_);
	} catch (IOException e) {
	    throw new InconsistentStateException(
		    "Internal, caught IOException!");
	}
    }

    /**
     * This method locates a suitable {@link java.security.AlgorithmParameters
     * AlgorithmParameters} implementation if it is available from the JCE
     * compliant security providers that are installed locally.
     * <p>
     * 
     * Such providers need to specify the following aliases for this to work:
     * <ul>
     * <li> AlgorithmParameters.MyAlg = <i>class</i>
     * <li> Alg.Alias.AlgorithmParameters.1.2.3.4 = MyAlg
     * </ul>
     * If you ever want to test a provider for compliance with the JCE and
     * <i>cleverness</i>, test it against the FhG-IGD PKCS package. If it
     * doesn't work then better demand fixes from the provider's vendor.
     * <p>
     * 
     * This method may be called only if this instance is initialized properly
     * either by specifying AlgorithmParameters in a constructor or by parsing a
     * valid ASN.1/DER encoding.
     * 
     * @throws NoSuchAlgorithmException
     *                 if no matching AlgorithmParameters engine is found.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters cannot be decoded properly.
     * @return The AlgorithmParameters or <code>null</code> if none are
     *         enclosed in this structure.
     */
    public AlgorithmParameters getParameters() throws NoSuchAlgorithmException,
	    InvalidAlgorithmParameterException {
	AlgorithmParameters params;
	String s;

	if (parameters_.isOptional())
	    return null;

	if (parameters_.getTag() == ASN1.TAG_NULL
		&& parameters_.getTagClass() == ASN1.CLASS_UNIVERSAL)
	    return null;

	/*
	 * Since alias resolution is Provider-local and hardly any Provider does
	 * it right and complete, we have to resolve aliases on our own.
	 */
	s = JCA.getName(algorithm_.toString());

	if (s == null)
	    throw new NoSuchAlgorithmException("Cannot resolve "
		    + algorithm_.toString());

	/*
	 * If we resolve cipher names then we have to remove the trailing
	 * padding string, if present.
	 */
	int n;

	n = s.indexOf("/");

	if (n > 0)
	    s = s.substring(0, n);

	params = AlgorithmParameters.getInstance(s);

	try {
	    params.init(parameters_.getEncoded());
	} catch (IOException e) {
	    throw new InvalidAlgorithmParameterException(
		    "Caught IOException(\"" + e.getMessage() + "\")");
	} catch (ASN1Exception e) {
	    throw new InvalidAlgorithmParameterException(
		    "Caught ASN1Exception(\"" + e.getMessage() + "\")");
	}
	return params;
    }

    /**
     * This method returns the OID of the algorithm represented by this
     * AlgorithmIdentifier. The OID returned is the one used internally. Do not
     * modify the returned OID! Otherwise, side effects occur.
     * 
     * @return The algorithm OID.
     */
    public ASN1ObjectIdentifier getAlgorithmOID() {
	return algorithm_;
    }

    /**
     * This method returns the JCE standard name of the algorithm specified in
     * this AlgorithmIdentifier. However, for this to work a proper alias for
     * the algorithm must be defined by some provider. See the general
     * documentation of this class for details on that.
     * <p>
     * 
     * This method calls {@link JCA#getName(String) JCA.getName()} with the
     * string representation of this instance's
     * {@link #algorithm_ object identifier}.
     * <p>
     * 
     * If you are {@link #getParameters retrieving the parameters} anyway then
     * avoid calling this method and call
     * {@link java.security.AlgorithmParameters#getAlgorithm getAlgorithm} on
     * the parameter instance instead.
     */
    public String getAlgorithmName() {
	return JCA.getName(algorithm_.toString());
    }

    /**
     * Returns a string representation of this object.
     * 
     * @return The string representation.
     */
    public String toString() {
	String s;
	String t;

	t = "X.509 AlgorithmIdentifier " + algorithm_.toString();
	s = getAlgorithmName();

	if (s != null)
	    return t + " (" + s + ")";

	return t;
    }

    /**
     * This method returns <code>true</code> if the given object is an
     * instance of this class or a subclass thereof and the algorithm OID of the
     * given object equals this object's algorithm OID.
     * 
     * @return <code>true</code> if the given object equals this one.
     */
    public boolean equals(Object o) {
	if (!(o instanceof AlgorithmIdentifier))
	    return false;

	return algorithm_.equals(((AlgorithmIdentifier) o).getAlgorithmOID());
    }

    public int hashCode() {
	return algorithm_.hashCode();
    }

    /**
     * Returns a clone. The clone is a deep copy of this instance except from
     * the constraints. Constraints are copied by reference.
     * 
     * @return The clone.
     */
    public Object clone() {
	AlgorithmIdentifier aid;

	aid = (AlgorithmIdentifier) super.clone();
	aid.clear();
	aid.algorithm_ = (ASN1ObjectIdentifier) algorithm_.clone();
	aid.parameters_ = (ASN1Opaque) parameters_.clone();

	aid.add(algorithm_);
	aid.add(parameters_);

	return aid;
    }

}

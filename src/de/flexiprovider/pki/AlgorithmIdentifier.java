/* Copyright 2000 Fraunhofer Gesellschaft
 * Leonrodstr. 54, 80636 Munich, Germany.
 * All rights reserved.
 *
 * You shall use this software only in accordance with
 * the terms of the license agreement you entered into
 * with Fraunhofer Gesellschaft.
 */
package de.flexiprovider.pki;

import java.io.IOException;

import codec.asn1.ASN1;
import codec.asn1.ASN1Exception;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1Type;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.api.parameters.AlgorithmParameters;

/**
 * This class represents the ASN.1/DER value of the AlgorithmIdentifier defined
 * in Annex D to Recommendation X.509. This structure is extensively used for
 * instance in the PKCS standards of RSA Inc. The ASN.1 definition of this
 * structure is as given below:
 * 
 * <pre>
 * AlgorithmIdentifier  ::= SEQUENCE{
 *   algorithm  OBJECT IDENTIFIER,
 *   parameters ANY DEFINED BY algorithm OPTIONAL
 * }
 * </pre>
 * 
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
 * The alias definitions are used by this class in order to find an
 * AlgorithmParameters implementation for the OID embedded in the X.509
 * AlgorithmIdentifier structure, and to create the OID for a given
 * AlgorithmParameters instances. This is done by means of the
 * {@link codec.util.JCA JCA} class, which operates on the engine and alias
 * definitions of the installed providers.
 * 
 * @author Volker Roth
 * @see codec.util.JCA
 */
public class AlgorithmIdentifier extends codec.x509.AlgorithmIdentifier {

    /**
     * This method builds the tree of ASN.1 objects used for decoding this
     * structure.
     */
    public AlgorithmIdentifier() {
	super();
    }

    /**
     * Creates an instance with the given OID and opaque algorithm parameter
     * representation. Both the given OID and the parameter encoding is cloned
     * or copied. No side effects occur if these arguments are modified after
     * completion of this constructor.
     * 
     * @param oid
     *                The algorithm object identifier.
     * @param b
     *                The opaque DER encoding of the parameters for the
     *                algorithm known under the given OID. If no parameters are
     *                required then <tt>null</tt> might be passed. In that
     *                case {@link codec.asn1.ASN1Null ASN.1NULL} is encoded.
     * @throws ASN1Exception
     *                 if the opaque representation does not contain a valid DER
     *                 header and contents octets.
     */
    public AlgorithmIdentifier(ASN1ObjectIdentifier oid, byte[] b)
	    throws ASN1Exception {
	super(oid, b);
    }

    /**
     * Creates an instance with the given OID and parameters. The parameters are
     * encoded according to DER and stored by means of an opaque type. If the
     * given parameters are <tt>null</tt> then an ASN.1 NULL is encoded.
     * 
     * @param oid
     *                The OID to use.
     * @param params
     *                The ASN.1 type of which the parameters consist.
     * @throws ASN1Exception
     *                 if the given parameters cannot be encoded. This should
     *                 rarely happen.
     */
    public AlgorithmIdentifier(ASN1ObjectIdentifier oid, ASN1Type params)
	    throws ASN1Exception {
	super(oid, params);
    }

    /**
     * This method locates a suitable {@link AlgorithmParameters} implementation
     * if it is available from the JCE compliant security providers that are
     * installed locally.
     * <p>
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
     * This method may be called only if this instance is initialised properly
     * either by specifying AlgorithmParameters in a constructor or by parsing a
     * valid ASN.1/DER encoding.
     * 
     * @throws NoSuchAlgorithmException
     *                 if no matching AlgorithmParameters engine is found.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters cannot be decoded properly.
     * @return The AlgorithmParameters or <tt>null</tt> if none are enclosed
     *         in this structure.
     */
    public AlgorithmParameters getParams() throws NoSuchAlgorithmException,
	    InvalidAlgorithmParameterException {
	AlgorithmParameters params;

	if (parameters_.isOptional()) {
	    return null;
	}

	if (parameters_.getTag() == ASN1.TAG_NULL
		&& parameters_.getTagClass() == ASN1.CLASS_UNIVERSAL) {
	    return null;
	}

	params = Registry.getAlgParams(algorithm_.toString());

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

}

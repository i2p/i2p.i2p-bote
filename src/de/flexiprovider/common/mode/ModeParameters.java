/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt 
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */
package de.flexiprovider.common.mode;

import java.io.IOException;

import javax.crypto.spec.IvParameterSpec;

import codec.asn1.ASN1Exception;
import codec.asn1.ASN1OctetString;
import de.flexiprovider.api.exceptions.InvalidParameterSpecException;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.api.parameters.AlgorithmParameters;
import de.flexiprovider.common.util.ASN1Tools;
import de.flexiprovider.common.util.ByteUtils;

/**
 * This class is used as an opaque representation of initialization vectors used
 * as mode parameters. ASN.1/DER encoding and decoding are supported. Parameters
 * are encoded as
 * 
 * <pre>
 *   ModeParameters = OCTET STRING (SIZE 8) IV
 * </pre>
 * 
 * @author Norbert Trummel
 * @author Sylvain Franke
 */
public class ModeParameters extends AlgorithmParameters {

    private byte[] iv;

    /**
     * JCA adapter for FlexiAPI method {@link #init(AlgorithmParameterSpec)}:
     * initialize this parameters object using the parameters specified in
     * <tt>paramSpec</tt>. This method overrides the corresponding method of
     * {@link AlgorithmParameters} in order to provide support for
     * {@link IvParameterSpec}.
     * 
     * @param params
     *                the parameter specification
     * @throws java.security.spec.InvalidParameterSpecException
     *                 if <tt>paramSpec</tt> is inappropriate for
     *                 initialization.
     */
    protected void engineInit(java.security.spec.AlgorithmParameterSpec params)
	    throws java.security.spec.InvalidParameterSpecException {

	if (params == null) {
	    throw new java.security.spec.InvalidParameterSpecException();
	}
	if (!(params instanceof AlgorithmParameterSpec)) {
	    if (params instanceof IvParameterSpec) {
		iv = ((IvParameterSpec) params).getIV();
		return;
	    }
	    throw new java.security.spec.InvalidParameterSpecException();
	}

	init((AlgorithmParameterSpec) params);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #getParameterSpec(Class)}: return
     * a (transparent) specification of this parameters object. This method
     * overrides the corresponding method of {@link AlgorithmParameters} in
     * order to provide support for {@link IvParameterSpec}.
     * 
     * @param paramSpec
     *                the the specification class in which the parameters should
     *                be returned
     * @return the parameter specification
     * @throws java.security.spec.InvalidParameterSpecException
     *                 if the requested parameter specification is inappropriate
     *                 for this parameter object.
     */
    protected java.security.spec.AlgorithmParameterSpec engineGetParameterSpec(
	    Class paramSpec)
	    throws java.security.spec.InvalidParameterSpecException {

	if (!(AlgorithmParameterSpec.class.isAssignableFrom(paramSpec))) {
	    if (paramSpec == IvParameterSpec.class) {
		return getParameterSpec(ModeParameterSpec.class);
	    }
	    throw new java.security.spec.InvalidParameterSpecException(
		    "Unsupported parameter specification.");
	}

	return getParameterSpec(paramSpec);
    }

    /**
     * Initialize this parameters object using the parameters specified in
     * <tt>paramSpec</tt>.
     * 
     * @param paramSpec
     *                the parameter specification
     * @throws InvalidParameterSpecException
     *                 if the given parameter specification is inappropriate for
     *                 the initialization of this parameter object.
     */
    public final void init(AlgorithmParameterSpec paramSpec)
	    throws InvalidParameterSpecException {

	if (paramSpec == null) {
	    throw new InvalidParameterSpecException("Null parameters.");
	}

	if (!(paramSpec instanceof ModeParameterSpec)) {
	    throw new InvalidParameterSpecException(
		    "Unsupported parameter specification.");
	}
	iv = ((ModeParameterSpec) paramSpec).getIV();
    }

    /**
     * Import the specified parameters and decodes them according to the primary
     * decoding format for parameters. The primary decoding format for
     * parameters is ASN.1.
     * 
     * @param encParams
     *                the encoded parameters
     * @throws IOException
     *                 on decoding errors
     */
    public final void init(byte[] encParams) throws IOException {
	ASN1OctetString asn1IV = new ASN1OctetString();
	try {
	    ASN1Tools.derDecode(encParams, asn1IV);
	} catch (ASN1Exception e) {
	    throw new IOException("Illegal encoding.");
	}
	iv = asn1IV.getByteArray();
    }

    /**
     * Import the specified parameters and decodes them according to the
     * specified decoding format. Only "ASN.1" is supported at the moment.
     * 
     * @param params
     *                the encoded parameters.
     * @param format
     *                the name of the decoding format.
     * @throws IOException
     *                 if <tt>format</tt> is not equal to "ASN.1".
     */
    public final void init(byte[] params, String format) throws IOException {
	if (!(format == "ASN.1")) {
	    throw new IOException("Unsupported encoding format.");
	}
	init(params);
    }

    /**
     * @return the ASN.1 encoded parameters
     */
    public final byte[] getEncoded() {
	return ASN1Tools.derEncode(new ASN1OctetString(iv));
    }

    /**
     * Return the parameters encoded in the specified format. Only "ASN.1" is
     * supported at the moment.
     * 
     * @param format
     *                the encoding format
     * @return the encoded parameters
     * @throws IOException
     *                 if <tt>format</tt> is not equal to "ASN.1".
     */
    public final byte[] getEncoded(String format) throws IOException {
	if (!(format == "ASN.1")) {
	    throw new IOException("Unsupported encoding format.");
	}
	return getEncoded();
    }

    /**
     * Return a (transparent) specification of this parameters object.
     * <tt>paramSpec</tt> identifies the specification class in which the
     * parameters should be returned. Only {@link ModeParameterSpec} is
     * supported.
     * 
     * @param paramSpec
     *                the specification class in which the parameters should be
     *                returned
     * @return the parameter specification
     * @throws InvalidParameterSpecException
     *                 if the requested parameter specification is inappropriate
     *                 for this parameters object.
     */
    public final AlgorithmParameterSpec getParameterSpec(Class paramSpec)
	    throws InvalidParameterSpecException {
	if (!(paramSpec.isAssignableFrom(ModeParameterSpec.class))) {
	    throw new InvalidParameterSpecException(
		    "Unsupported parameter specification.");
	}
	return new ModeParameterSpec(iv);
    }

    /**
     * @return a formatted string describing the parameters
     */
    public final String toString() {
	return "IV: " + ByteUtils.toHexString(iv);
    }

}

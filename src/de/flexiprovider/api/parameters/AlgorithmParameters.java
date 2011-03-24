package de.flexiprovider.api.parameters;

import java.io.IOException;

import de.flexiprovider.api.exceptions.InvalidParameterSpecException;

/**
 * This class defines the interface used to manage algorithm parameters.
 * 
 * @see de.flexiprovider.api.parameters.AlgorithmParameterSpec
 */
public abstract class AlgorithmParameters extends
	java.security.AlgorithmParametersSpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * JCA adapter for FlexiAPI method {@link #init(AlgorithmParameterSpec)}:
     * initialize this parameters object using the parameters specified in
     * <tt>paramSpec</tt>.
     * 
     * @param params
     *                the parameter specification
     * @throws java.security.spec.InvalidParameterSpecException
     *                 if <tt>paramSpec</tt> is inappropriate for
     *                 initialization.
     */
    protected void engineInit(java.security.spec.AlgorithmParameterSpec params)
	    throws java.security.spec.InvalidParameterSpecException {

	if ((params == null) || !(params instanceof AlgorithmParameterSpec)) {
	    throw new java.security.spec.InvalidParameterSpecException();
	}
	init((AlgorithmParameterSpec) params);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #init(byte[])}: import the
     * specified parameters and decode them according to the primary decoding
     * format for parameters. The primary decoding format for parameters is
     * ASN.1, if an ASN.1 specification for this type of parameters exists.
     * 
     * @param params
     *                the encoded parameters
     * @throws IOException
     *                 on decoding errors.
     */
    protected final void engineInit(byte[] params) throws IOException {
	init(params);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #init(byte[], String)}: import
     * the specified parameters and decode them according to the specified
     * decoding format. If <tt>format</tt> is null, the primary decoding
     * format for parameters is used. The primary decoding format is ASN.1, if
     * an ASN.1 specification for these parameters exists.
     * 
     * @param params
     *                the encoded parameters
     * @param format
     *                the decoding format
     * @throws IOException
     *                 on decoding errors.
     */
    protected final void engineInit(byte[] params, String format)
	    throws IOException {
	init(params, format);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #getEncoded()}: return the
     * parameters in their primary encoding format. The primary encoding format
     * for parameters is ASN.1, if an ASN.1 specification for this type of
     * parameters exists.
     * 
     * @return the parameters encoded in their primary encoding format
     * @throws IOException
     *                 on encoding errors.
     */
    protected final byte[] engineGetEncoded() throws IOException {
	return getEncoded();
    }

    /**
     * JCA adapter for FlexiAPI method {@link #getEncoded(String)}: return the
     * parameters in the specified encoding format. If <tt>format</tt> is
     * null, the primary decoding format is used. The primary encoding format
     * for parameters is ASN.1, if an ASN.1 specification for this type of
     * parameters exists.
     * 
     * @param format
     *                the encoding format
     * @return the parameters encoded in the specified encoding format
     * @throws IOException
     *                 on encoding errors.
     */
    protected final byte[] engineGetEncoded(String format) throws IOException {
	return getEncoded(format);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #getParameterSpec(Class)}: return
     * a (transparent) specification of this parameters object.
     * <tt>paramSpec</tt> identifies the specification class in which the
     * parameters should be returned. It could, for example, be
     * {@link java.security.spec.DSAParameterSpec}<tt>.class</tt> , to
     * indicate that the parameters should be returned in an instance of the
     * {@link java.security.spec.DSAParameterSpec} class.
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
	    throw new java.security.spec.InvalidParameterSpecException(
		    "Unsupported parameter specification.");
	}
	return getParameterSpec(paramSpec);
    }

    /**
     * JCA adapter for FlexiAPI method {@link #toString()}.
     * 
     * @return a human readable form of this parameters object
     */
    protected final String engineToString() {
	return toString();
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Initialize the parameters with the given parameter specification.
     * 
     * @param paramSpec
     *                the parameter specification
     * @throws InvalidParameterSpecException
     *                 if the parameter specification is <tt>null</tt> or of
     *                 an unsupported type.
     */
    public abstract void init(AlgorithmParameterSpec paramSpec)
	    throws InvalidParameterSpecException;

    /**
     * Import the given encoded parameters and decode them according to the
     * primary encoding format (ASN.1).
     * 
     * @param encParams
     *                the encoded parameters
     * @throws IOException
     *                 on decoding errors.
     */
    public abstract void init(byte[] encParams) throws IOException;

    /**
     * Import the given encoded parameters and decode them according to the
     * specified encoding format.
     * 
     * @param encParams
     *                the encoded parameters
     * @param format
     *                the encoding format
     * @throws IOException
     *                 on decoding errors or if the encoding format is
     *                 <tt>null</tt> or not supported.
     */
    public abstract void init(byte[] encParams, String format)
	    throws IOException;

    /**
     * Encode the parameters according to the primary encoding format (ASN.1).
     * 
     * @return the encoded parameters
     * @throws IOException
     *                 on encoding errors.
     */
    public abstract byte[] getEncoded() throws IOException;

    /**
     * Encode the parameters according to the specified encoding format.
     * 
     * @param format
     *                the encoding format
     * @return the encoded parameters
     * @throws IOException
     *                 on encoding errors or if the encoding format is
     *                 <tt>null</tt> or not supported.
     */
    public abstract byte[] getEncoded(String format) throws IOException;

    /**
     * Return a transparent specification of the parameters.
     * 
     * @param paramSpec
     *                the desired parameter specification type
     * @return the parameter specification
     * @throws InvalidParameterSpecException
     *                 if the parameter specification type is <tt>null</tt> or
     *                 or not supported
     */
    public abstract AlgorithmParameterSpec getParameterSpec(Class paramSpec)
	    throws InvalidParameterSpecException;

    /**
     * @return a human readable form of the parameters
     */
    public abstract String toString();

}

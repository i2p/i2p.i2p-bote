package de.flexiprovider.api.parameters;

import de.flexiprovider.api.SecureRandom;
import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.common.mode.ModeParameterSpec;
import de.flexiprovider.common.util.JavaSecureRandomWrapper;

public abstract class AlgorithmParameterGenerator extends
	java.security.AlgorithmParameterGeneratorSpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * JCA adapter for FlexiAPI method {@link #generateParameters()}: generate
     * parameters.
     * 
     * @return the generated parameters
     */
    protected final java.security.AlgorithmParameters engineGenerateParameters() {

	/**
	 * JCA adapter class, used to translate from {@link AlgorithmParameters}
	 * to {@link java.security.AlgorithmParameters}.
	 */
	final class JCAAlgorithmParameters extends
		java.security.AlgorithmParameters {
	    private JCAAlgorithmParameters(AlgorithmParameters params) {
		super(params, null, null);
	    }
	}

	JCAAlgorithmParameters algParams = new JCAAlgorithmParameters(
		getAlgorithmParameters());

	try {
	    algParams.init(generateParameters());
	} catch (java.security.spec.InvalidParameterSpecException ipse) {
	    throw new RuntimeException("InvalidParameterSpecException: "
		    + ipse.getMessage());
	}

	return algParams;
    }

    /**
     * JCA adapter for FlexiAPI methods init(): initialize the parameter
     * generator with a key size and a source of randomness.
     * 
     * @param keySize
     *                the key size
     * @param javaRand
     *                the source of randomness
     */
    protected final void engineInit(int keySize,
	    java.security.SecureRandom javaRand) {
	SecureRandom flexiRand = new JavaSecureRandomWrapper(javaRand);
	init(keySize, flexiRand);
    }

    /**
     * JCA adapter for FlexiAPI methods init(): initialize the parameter
     * generator with a parameter specification and a source of randomness.
     * 
     * @param genParamSpec
     *                the parameter specification
     * @param javaRand
     *                the source of randomness
     * @throws java.security.InvalidAlgorithmParameterException
     *                 if the given parameters are invalid.
     */
    protected final void engineInit(
	    java.security.spec.AlgorithmParameterSpec genParamSpec,
	    java.security.SecureRandom javaRand)
	    throws java.security.InvalidAlgorithmParameterException {
	SecureRandom flexiRand = new JavaSecureRandomWrapper(javaRand);
	ModeParameterSpec paramSpec;
	if (genParamSpec instanceof javax.crypto.spec.IvParameterSpec) {
	    paramSpec = new ModeParameterSpec(
		    (javax.crypto.spec.IvParameterSpec) genParamSpec);
	    init(paramSpec, flexiRand);
	} else {
	    if (!(genParamSpec instanceof AlgorithmParameterSpec)) {
		throw new java.security.InvalidAlgorithmParameterException();
	    }
	    init((AlgorithmParameterSpec) genParamSpec, flexiRand);
	}
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * @return an instance of the {@link AlgorithmParameters} class
     *         corresponding to the generated parameters
     */
    protected abstract AlgorithmParameters getAlgorithmParameters();

    /**
     * Initialize the parameter generator with a key size and a source of
     * randomness.
     * 
     * @param keySize
     *                the key size
     * @param random
     *                the source of randomness
     */
    public abstract void init(int keySize, SecureRandom random);

    /**
     * Initialize the parameter generator with a parameter specification and a
     * source of randomness.
     * 
     * @param genParamSpec
     *                the parameter specification
     * @param random
     *                the source of randomness
     * @throws InvalidAlgorithmParameterException
     *                 if the given parameters are invalid.
     */
    public abstract void init(AlgorithmParameterSpec genParamSpec,
	    SecureRandom random) throws InvalidAlgorithmParameterException;

    /**
     * Generate parameters.
     * 
     * @return the generated parameters
     */
    public abstract AlgorithmParameterSpec generateParameters();

}

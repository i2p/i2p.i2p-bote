package de.flexiprovider.common.mode;

import de.flexiprovider.api.Registry;
import de.flexiprovider.api.SecureRandom;
import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.parameters.AlgorithmParameterGenerator;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.api.parameters.AlgorithmParameters;

/**
 * This class is used to generate initialization vectors (IVs) used by the modes
 * CBC, CFB, OFB, and CTR.
 * 
 * @author Martin Döring
 */
public class ModeParameterGenerator extends AlgorithmParameterGenerator {

    // the length of the IV
    private int ivLength;

    // the source of randomness
    private SecureRandom random;

    // flag indicating whether the parameter generator has been initialized
    private boolean initialized;

    /**
     * @return an instance of the {@link AlgorithmParameters} class
     *         corresponding to the generated parameters
     */
    protected AlgorithmParameters getAlgorithmParameters() {
	return new ModeParameters();
    }

    /**
     * Initialize the parameter generator with parameters and a source of
     * randomness. If the parameters are <tt>null</tt>, the
     * {@link ModeParamGenParameterSpec#ModeParamGenParameterSpec() default parameters}
     * are used.
     * 
     * @param genParams
     *                the parameters
     * @param random
     *                the source of randomness
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are not an instance of
     *                 {@link ModeParamGenParameterSpec}.
     */
    public void init(AlgorithmParameterSpec genParams, SecureRandom random)
	    throws InvalidAlgorithmParameterException {

	ModeParamGenParameterSpec modeGenParams;
	if (genParams == null) {
	    modeGenParams = new ModeParamGenParameterSpec();
	} else if (genParams instanceof ModeParamGenParameterSpec) {
	    modeGenParams = (ModeParamGenParameterSpec) genParams;
	} else {
	    throw new InvalidAlgorithmParameterException("unsupported type");
	}

	ivLength = modeGenParams.getIVLength();
	this.random = random != null ? random : Registry.getSecureRandom();

	initialized = true;
    }

    /**
     * Initialize the parameter generator with the desired length of the IV in
     * bytes and the source of randomness used to generate the IV.
     * 
     * @param ivLength
     *                the length of the IV in bytes
     * @param random
     *                the source of randomness
     */
    public void init(int ivLength, SecureRandom random) {
	ModeParamGenParameterSpec genParams = new ModeParamGenParameterSpec(
		ivLength);
	try {
	    init(genParams, random);
	} catch (InvalidAlgorithmParameterException e) {
	    // the parameters are correct and must be accepted
	    throw new RuntimeException("internal error");
	}
    }

    private void initDefault() {
	ModeParamGenParameterSpec defaultGenParams = new ModeParamGenParameterSpec();
	try {
	    init(defaultGenParams, random);
	} catch (InvalidAlgorithmParameterException e) {
	    // the parameters are correct and must be accepted
	    throw new RuntimeException("internal error");
	}
    }

    /**
     * Generate a new IV using the length and source of randomness specified
     * during initialization.
     * 
     * @return the generated IV encapsulated in an instance of
     *         {@link ModeParameterSpec}
     */
    public AlgorithmParameterSpec generateParameters() {
	if (!initialized) {
	    initDefault();
	}

	byte[] iv = new byte[ivLength];
	random.nextBytes(iv);
	return new ModeParameterSpec(iv);
    }

}

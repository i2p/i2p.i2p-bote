package de.flexiprovider.common.mode;

import de.flexiprovider.api.parameters.AlgorithmParameterSpec;

/**
 * This class specifies parameters used for initializing the
 * {@link ModeParameterGenerator}. The parameters consist of the byte length of
 * the initialization vector.
 * 
 * @author Martin Döring
 */
public class ModeParamGenParameterSpec implements AlgorithmParameterSpec {

    /**
     * The default length of the IV (8 bytes)
     */
    public static final int DEFAULT_LENGTH = 8;

    private int ivLength;

    /**
     * Construct the default mode parameter generation parameters. Set the
     * length of the IV to {@link #DEFAULT_LENGTH}. The default length is
     * chosen (somewhat arbitrarily) as 8 bytes.
     */
    public ModeParamGenParameterSpec() {
	this(DEFAULT_LENGTH);
    }

    /**
     * Construct new parameters from the desired length of the initialization
     * vector (IV) in bytes.
     * 
     * @param ivLength
     *                the length of the IV in bytes
     */
    public ModeParamGenParameterSpec(int ivLength) {
	this.ivLength = ivLength;
    }

    /**
     * @return the length of the IV in bytes
     */
    public int getIVLength() {
	return ivLength;
    }

}

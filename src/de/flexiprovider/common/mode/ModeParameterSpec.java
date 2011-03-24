package de.flexiprovider.common.mode;

import de.flexiprovider.api.parameters.AlgorithmParameterSpec;

/**
 * This interface groups (and provides type safety for) all mode parameter
 * specifications. All mode parameter specifications must implement this
 * interface.
 */
public class ModeParameterSpec extends javax.crypto.spec.IvParameterSpec
	implements AlgorithmParameterSpec {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * Converts a JCA IvParameterSpec in a Flexi IvParameterSpec.
     * 
     * @param params
     *                the JCA IvParameterSpec.
     */
    public ModeParameterSpec(javax.crypto.spec.IvParameterSpec params) {
	super(params.getIV());
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Constructor. Set the initialization vector (IV). The IV may be
     * <tt>null</tt>.
     * 
     * @param iv
     *                the IV
     */
    public ModeParameterSpec(byte[] iv) {
	super(iv);
    }

    /**
     * Constructor. Set the initialization vector (IV). The IV must not be null.
     * 
     * @param iv
     *                the byte array containing the IV
     * @param offset
     *                the offset where the IV starts
     * @param length
     *                the length of the IV
     */
    public ModeParameterSpec(byte[] iv, int offset, int length) {
	super(iv, offset, length);
    }

}

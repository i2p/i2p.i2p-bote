package de.flexiprovider.api.keys;

import de.flexiprovider.api.SecureRandom;
import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.InvalidParameterException;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.common.util.JavaSecureRandomWrapper;

public abstract class KeyPairGenerator extends
	java.security.KeyPairGeneratorSpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * JCA adapter for FlexiAPI method
     * {@link #initialize(AlgorithmParameterSpec, SecureRandom)}: initialize
     * the key pair generator using the specified parameter set and source of
     * randomness.
     * 
     * @param params
     *                the parameter set used to generate the keys
     * @param javaRand
     *                the source of randomness for this generator
     * @throws java.security.InvalidAlgorithmParameterException
     *                 if the given parameters are inappropriate for this key
     *                 pair generator.
     */
    public void initialize(java.security.spec.AlgorithmParameterSpec params,
	    java.security.SecureRandom javaRand)
	    throws java.security.InvalidAlgorithmParameterException {

	if (params != null && !(params instanceof AlgorithmParameterSpec)) {
	    throw new java.security.InvalidAlgorithmParameterException();
	}
	SecureRandom flexiRand = new JavaSecureRandomWrapper(javaRand);
	initialize((AlgorithmParameterSpec) params, flexiRand);
    }

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * Initialize the key pair generator for a certain key size using a default
     * parameter set and the <tt>SecureRandom</tt> implementation of the
     * highest-priority installed provider as the source of randomness. (If none
     * of the installed providers supply an implementation of
     * <tt>SecureRandom</tt>, a system-provided source of randomness is
     * used.)
     * 
     * @param keysize
     *                the keysize. This is an algorithm-specific metric, such as
     *                modulus length, specified in number of bits.
     * @param javaRand
     *                the source of randomness for this generator
     * @throws InvalidParameterException
     *                 if the <tt>keysize</tt> is not supported by this
     *                 KeyPairGenerator object.
     */
    public final void initialize(int keysize,
	    java.security.SecureRandom javaRand)
	    throws InvalidParameterException {
	SecureRandom flexiRand = new JavaSecureRandomWrapper(javaRand);
	initialize(keysize, flexiRand);
    }

    /**
     * JCA adapter to FlexiAPI method {@link #genKeyPair()}: generate a key
     * pair. Unless an initialization method is called using a KeyPairGenerator
     * interface, algorithm-specific defaults will be used. This will generate a
     * new key pair every time it is called.
     * 
     * @return a newly generated <tt>KeyPair</tt>
     */
    public final java.security.KeyPair generateKeyPair() {
	return genKeyPair().pair;
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Initialize the key pair generator using the specified parameter set and
     * the <tt>SecureRandom</tt> implementation of the highest-priority
     * installed provider as the source of randomness. (If none of the installed
     * providers supply an implementation of <tt>SecureRandom</tt>, a
     * system-provided source of randomness is used.).
     * 
     * @param params
     *                the parameter set used to generate the keys
     * @param random
     *                a source of randomness
     * @throws InvalidAlgorithmParameterException
     *                 if the given parameters are inappropriate for this key
     *                 pair generator.
     */
    public abstract void initialize(AlgorithmParameterSpec params,
	    SecureRandom random) throws InvalidAlgorithmParameterException;

    /**
     * Initialize the key pair generator for a certain keysize using a default
     * parameter set and the <tt>SecureRandom</tt> implementation of the
     * highest-priority installed provider as the source of randomness. (If none
     * of the installed providers supply an implementation of
     * <tt>SecureRandom</tt>, a system-provided source of randomness is
     * used.)
     * 
     * @param keysize
     *                the keysize. This is an algorithm-specific metric, such as
     *                modulus length, specified in number of bits.
     * @param random
     *                the source of randomness for this generator
     * @throws InvalidParameterException
     *                 if the <tt>keysize</tt> is not supported by this
     *                 KeyPairGenerator object.
     */
    public abstract void initialize(int keysize, SecureRandom random)
	    throws InvalidParameterException;

    /**
     * Generate a key pair. Unless an initialization method is called using a
     * KeyPairGenerator interface, algorithm-specific defaults will be used.
     * This will generate a new key pair every time it is called.
     * 
     * @return a newly generated {@link KeyPair}
     */
    public abstract KeyPair genKeyPair();

}

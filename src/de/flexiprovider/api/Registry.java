package de.flexiprovider.api;

import java.util.Hashtable;

import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.api.exceptions.RegistrationException;
import de.flexiprovider.api.keys.KeyPairGenerator;
import de.flexiprovider.api.parameters.AlgorithmParameters;
import de.flexiprovider.common.util.DefaultPRNG;

/**
 * This class is responsible for the registration and instantiation of all
 * cryptographic algorithms of the FlexiProvider. It provides methods for adding
 * registrations of algorithms and methods for instantiating registered
 * algorithms.
 * 
 * @author Johannes M�ller
 * @author Martin D�ring
 */
public abstract class Registry {

	/* algorithm type constants */

	/**
	 * Constant for asymmetric block ciphers
	 */
	public static final int ASYMMETRIC_BLOCK_CIPHER = 0;

	/**
	 * Constant for asymmetric hybrid ciphers
	 */
	public static final int ASYMMETRIC_HYBRID_CIPHER = 1;

	/**
	 * Constant for symmetric block ciphers
	 */
	public static final int BLOCK_CIPHER = 2;

	/**
	 * Constant for modes of operation
	 */
	public static final int MODE = 3;

	/**
	 * Constant for padding schemes
	 */
	public static final int PADDING_SCHEME = 4;

	/**
	 * Constant for generic ciphers
	 */
	public static final int CIPHER = 5;

	/**
	 * Constant for message authentication codes (MACs)
	 */
	public static final int MAC = 6;

	/**
	 * Constant for message digests (hash functions)
	 */
	public static final int MESSAGE_DIGEST = 7;

	/**
	 * Constant for PRNGs
	 */
	public static final int SECURE_RANDOM = 8;

	/**
	 * Constant for digital signatures
	 */
	public static final int SIGNATURE = 9;

	/**
	 * Constant for algorithm parameter specifications
	 */
	public static final int ALG_PARAM_SPEC = 10;

	/**
	 * Constant for algorithm parameters (used to encode and decode parameter
	 * specifications)
	 */
	public static final int ALG_PARAMS = 11;

	/**
	 * Constant for algorithm parameter generators
	 */
	public static final int ALG_PARAM_GENERATOR = 12;

	/**
	 * Constant for secret key generators
	 */
	public static final int SECRET_KEY_GENERATOR = 13;

	/**
	 * Constant for key pair generators
	 */
	public static final int KEY_PAIR_GENERATOR = 14;

	/**
	 * Constant for secret key factories
	 */
	public static final int SECRET_KEY_FACTORY = 15;

	/**
	 * Constant for key factories
	 */
	public static final int KEY_FACTORY = 16;

	/**
	 * Constant for key derivations
	 */
	public static final int KEY_DERIVATION = 17;

	/**
	 * Constant for key agreements
	 */
	public static final int KEY_AGREEMENT = 18;

	/* hash tables for the different algorithm types */

	private static final Hashtable asymBlockCiphers = new Hashtable();
	private static final Hashtable asymHybridCiphers = new Hashtable();
	private static final Hashtable blockCiphers = new Hashtable();
	private static final Hashtable modes = new Hashtable();
	private static final Hashtable paddingSchemes = new Hashtable();
	private static final Hashtable ciphers = new Hashtable();
	private static final Hashtable macs = new Hashtable();
	private static final Hashtable messageDigests = new Hashtable();
	private static final Hashtable secureRandoms = new Hashtable();
	private static final Hashtable signatures = new Hashtable();
	private static final Hashtable algParamSpecs = new Hashtable();
	private static final Hashtable algParams = new Hashtable();
	private static final Hashtable algParamGenerators = new Hashtable();
	private static final Hashtable secretKeyGenerators = new Hashtable();
	private static final Hashtable keyPairGenerators = new Hashtable();
	private static final Hashtable secretKeyFactories = new Hashtable();
	private static final Hashtable keyFactories = new Hashtable();
	private static final Hashtable keyDerivations = new Hashtable();
	private static final Hashtable keyAgreements = new Hashtable();

	// array holding all hash tables (indexed by algorithm type)
	private static final Hashtable[] hashtables = { asymBlockCiphers,
			asymHybridCiphers, blockCiphers, modes, paddingSchemes, ciphers,
			macs, messageDigests, secureRandoms, signatures, algParamSpecs,
			algParams, algParamGenerators, secretKeyGenerators,
			keyPairGenerators, secretKeyFactories, keyFactories,
			keyDerivations, keyAgreements };

	// hash table for standard algorithm parameters
	private static final Hashtable standardAlgParams = new Hashtable();

	/**
	 * Register an algorithm of the given type under the given names.
	 * 
	 * @param type
	 *            the algorithm type
	 * @param algClass
	 *            the class implementing the algorithm
	 * @param algNames
	 *            the names for the algorithm
	 * @throws RegistrationException
	 *             if the expected and actual algorithm types do not match or an
	 *             algorithm is already registered under one of the given names.
	 */
	public static final void add(int type, Class algClass, String[] algNames) {
		Hashtable table = getHashtable(type);
		// trivial cases
		if ((table == null) || (algClass == null) || (algNames == null)
				|| (algNames.length == 0)) {
			return;
		}

		// register first name
		table.put(algNames[0], algClass);

		// register additional names (aliases)
		for (int i = 1; i < algNames.length; i++) {
			table.put(algNames[i], algNames[0]);
		}
	}

	/**
	 * Return an instance of the specified message digest.
	 * 
	 * @param algName
	 *            the name of the message digest
	 * @return a new {@link MessageDigest} object implementing the chosen
	 *         algorithm
	 * @throws NoSuchAlgorithmException
	 *             if the message digest cannot be found.
	 */
	public static final MessageDigest getMessageDigest(String algName)
			throws NoSuchAlgorithmException {
		return (MessageDigest) getInstance(messageDigests, algName);
	}

	/**
	 * Return an instance of the specified source of randomness.
	 * 
	 * @param algName
	 *            the name of the source of randomness
	 * @return a new {@link SecureRandom} object implementing the chosen
	 *         algorithm
	 * @throws NoSuchAlgorithmException
	 *             if the source of randomness cannot be found.
	 */
	public static final SecureRandom getSecureRandom(String algName)
			throws NoSuchAlgorithmException {
		return (SecureRandom) getInstance(secureRandoms, algName);
	}

	/**
	 * @return the default secure random
	 * @throws RuntimeException
	 *             if the default secure random cannot be instantiated.
	 */
	public static final SecureRandom getSecureRandom() {
		return new DefaultPRNG();
	}

	/**
	 * Return an instance of the specified algorithm parameters.
	 * 
	 * @param algName
	 *            the name of the algorithm parameters
	 * @return a new {@link AlgorithmParameters} object implementing the chosen
	 *         algorithm
	 * @throws NoSuchAlgorithmException
	 *             if the algorithm parameters cannot be found.
	 */
	public static final AlgorithmParameters getAlgParams(String algName)
			throws NoSuchAlgorithmException {
		return (AlgorithmParameters) getInstance(algParams, algName);
	}

	/**
	 * Return an instance of the specified key pair generator.
	 * 
	 * @param algName
	 *            the name of the key pair generator
	 * @return a new {@link KeyPairGenerator} object implementing the chosen
	 *         algorithm
	 * @throws NoSuchAlgorithmException
	 *             if the key pair generator cannot be found.
	 */
	public static final KeyPairGenerator getKeyPairGenerator(String algName)
			throws NoSuchAlgorithmException {
		return (KeyPairGenerator) getInstance(keyPairGenerators, algName);
	}

	private static Hashtable getHashtable(int type) {
		if (type > hashtables.length) {
			return null;
		}
		return hashtables[type];
	}

	private static Object resolveAlias(Hashtable table, String name) {
		Object value = name;
		do {
			String algName = (String) value;
			value = table.get(algName);
		} while (value != null && (value instanceof String));
		return value;
	}

	/**
	 * Try to find an algorithm with the specified name inside the corresponding
	 * hashtable and return an instance of the algorithm.
	 * 
	 * @param table
	 *            hashtable containing the algorithm
	 * @param name
	 *            the algorithm name
	 * @return a new object implementing the chosen algorithm, or <tt>null</tt>
	 *         if the algorithm name is <tt>null</tt>
	 * @throws NoSuchAlgorithmException
	 *             if the algorithm cannot be found.
	 */
	private static Object getInstance(Hashtable table, String name)
			throws NoSuchAlgorithmException {
		if (name == null) {
			return null;
		}
		Class algClass = (Class) resolveAlias(table, name);
		if (algClass == null) {
			throw new NoSuchAlgorithmException(name);
		}
		try {
			return algClass.newInstance();
		} catch (InstantiationException e) {
			throw new RegistrationException("Instantiation exception: "
					+ e.getMessage());
		} catch (IllegalAccessException e) {
			throw new RegistrationException("Illegal access exception: "
					+ e.getMessage());
		}
	}

}

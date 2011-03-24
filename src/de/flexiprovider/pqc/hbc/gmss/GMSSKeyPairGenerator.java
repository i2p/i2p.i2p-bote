package de.flexiprovider.pqc.hbc.gmss;

import java.util.Vector;

import de.flexiprovider.api.MessageDigest;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.SecureRandom;
import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.api.exceptions.SignatureException;
import de.flexiprovider.api.keys.KeyPair;
import de.flexiprovider.api.keys.KeyPairGenerator;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.core.CoreRegistry;

/**
 * This class implements key pair generation of the generalized Merkle signature
 * scheme (GMSS). The class extends the KeyPairGeneratorSpi class.
 * <p>
 * The GMSSKeyPairGenerator can be used as follows:
 * <p>
 * 1. get instance of GMSS key pair generator:<br/>
 * <code>KeyPairGenerator kpg =
 * KeyPairGenerator.getInstance("GMSSwithSHA1",
 * "FlexiPQC");</code><br/>
 * 2. initialize the KPG with the desired Parameterset<br/>
 * <code>kpg.initialize(parameterset);</code><br/>
 * 3. create GMSS key pair:<br/>
 * <code>KeyPair keyPair = kpg.generateKeyPair();</code><br/>
 * 4. get the encoded private and public keys from the key pair:<br/>
 * <code>encodedPublicKey = keyPair.getPublic().getEncoded();<br/>
 * encodedPrivateKey = keyPair.getPrivate().getEncoded();</code>
 * 
 * <p>
 * The key pair generator can be initialized with an integer value as well. For
 * this purpose call <code>kpg.initialize(keySize);</code>. The integer
 * <code>keySize</code> determindes the number of signatures that can be
 * created. A value less than 10 creates 2^10 signatures, between 11 and 20
 * creates 2^20 and a keySize greater than 20 creates 2^40 signatures.
 * 
 * <p>
 * To generate an own parameterSpec for the use with GMSS use the following:
 * 
 * <p>
 * 1. define int arrays of the desired parameters (defh for the height of the
 * single layers of the GMSS tree, w for the Winternitz parameters for each
 * layer, K for the parameter for the AuthPath computation)<br/>
 * <code> int[] defh = {10, 10, 10, 10};</code><br/>
 * <code> int[] defw = {9, 9, 9, 3};</code><br/>
 * <code> int[] defk = {2, 2, 2, 2};</code><br/>
 * 2. create a parameterspec<br/>
 * <code> gps = new GMSSParameterSpec(defh.length, defh, defw, defk);</code><br/>
 * 3. initialize the KPG with the desired Parameterset<br/>
 * <code>kpg.initialize(parameterset);</code><br/>
 * 
 * @author Michael Schneider, Sebastian Blume
 * @see GMSSSignature
 * @see GMSSPrivateKey
 * @see GMSSPublicKey
 */
public class GMSSKeyPairGenerator extends KeyPairGenerator {
	/*
	 * Inner classes providing concrete implementations of GMSSKeyPairGenerator
	 * with a variety of message digests.
	 */

	/**
	 * GMSSKeyPairGenerator with SHA1
	 */
	public static class GMSSwithSHA1 extends GMSSKeyPairGenerator {

		/**
		 * The OID of the algorithm.
		 */
		public static final String OID = GMSSKeyFactory.OID + ".1";

		/**
		 * Constructor.
		 */
		public GMSSwithSHA1() {
			super(OID, "SHA1", "FlexiCore");
		}
	}

	/**
	 * GMSSKeyPairGenerator with SHA224
	 */
	public static class GMSSwithSHA224 extends GMSSKeyPairGenerator {

		/**
		 * The OID of the algorithm.
		 */
		public static final String OID = GMSSKeyFactory.OID + ".2";

		/**
		 * Constructor.
		 */
		public GMSSwithSHA224() {
			super(OID, "SHA224", "FlexiCore");
		}
	}

	/**
	 * GMSSKeyPairGenerator with SHA256
	 */
	public static class GMSSwithSHA256 extends GMSSKeyPairGenerator {

		/**
		 * The OID of the algorithm.
		 */
		public static final String OID = GMSSKeyFactory.OID + ".3";

		/**
		 * Constructor.
		 */
		public GMSSwithSHA256() {
			super(OID, "SHA256", "FlexiCore");
		}
	}

	/**
	 * GMSSKeyPairGenerator with SHA384
	 */
	public static class GMSSwithSHA384 extends GMSSKeyPairGenerator {

		/**
		 * The OID of the algorithm.
		 */
		public static final String OID = GMSSKeyFactory.OID + ".4";

		/**
		 * Constructor.
		 */
		public GMSSwithSHA384() {
			super(OID, "SHA384", "FlexiCore");
		}
	}

	/**
	 * GMSSKeyPairGenerator with SHA512
	 */
	public static class GMSSwithSHA512 extends GMSSKeyPairGenerator {

		/**
		 * The OID of the algorithm.
		 */
		public static final String OID = GMSSKeyFactory.OID + ".5";

		/**
		 * Constructor.
		 */
		public GMSSwithSHA512() {
			super(OID, "SHA512", "FlexiCore");
		}
	}

	// //////////////////////////////////////////////////////////////////////////////

	/**
	 * The source of randomness for OTS private key generation
	 */
	private GMSSRandom gmssRandom;

	/**
	 * The hash function used for the construction of the authentication trees
	 */
	private MessageDigest messDigestTree;

	/**
	 * An array of the seeds for the PRGN (for main tree, and all current
	 * subtrees)
	 */
	private byte[][] currentSeeds;

	/**
	 * An array of seeds for the PRGN (for all subtrees after next)
	 */
	private byte[][] nextNextSeeds;

	/**
	 * An array of the RootSignatures
	 */
	private byte[][] currentRootSigs;

	/**
	 * An array of strings containing the name of the hash function used to
	 * construct the authentication trees and used by the OTS
	 */
	private String[] algNames = new String[2];

	/**
	 * The length of the seed for the PRNG
	 */
	private int mdLength;

	/**
	 * the number of Layers
	 */
	private int numLayer;

	/**
	 * Instance of GMSSParameterSpec
	 */
	private GMSSParameterSpec gmssParameterSpec;

	/**
	 * Flag indicating if the class already has been initialized
	 */
	private boolean initialized = false;

	/**
	 * Instance of GMSSParameterset
	 */
	private GMSSParameterset gmssPS;

	/**
	 * An array of the heights of the authentication trees of each layer
	 */
	private int[] heightOfTrees;

	/**
	 * An array of the Winternitz parameter 'w' of each layer
	 */
	private int[] otsIndex;

	/**
	 * The parameter K needed for the authentication path computation
	 */
	private int[] K;

	/**
	 * The standard constructor tries to generate the GMSS algorithm identifier
	 * with the corresponding OID.
	 * <p>
	 * 
	 * @param oidStr
	 *            string with the oid of the algorithm
	 * @param mdName
	 *            name of the message digest for the construction of the
	 *            authentication trees
	 * @param mdProvName
	 *            provider name of the message digest for the construction of
	 *            the the authentication trees and for the OTS
	 */
	public GMSSKeyPairGenerator(String oidStr, String mdName, String mdProvName) {
		String errorMsg;

		CoreRegistry.registerAlgorithms();
		try {
			messDigestTree = Registry.getMessageDigest(mdName);
			algNames[0] = mdName;

			// set mdLength
			this.mdLength = messDigestTree.getDigestLength();
			// construct randomizer
			this.gmssRandom = new GMSSRandom(messDigestTree);

			return;
		} catch (NoSuchAlgorithmException nsae) {
			errorMsg = "message digest " + mdName + " not found in "
					+ mdProvName + " or key pair generator " + mdName
					+ " not found in " + mdProvName;
		}
		throw new RuntimeException("GMSSKeyPairGenerator error: " + errorMsg);

	}

	/**
	 * Generates the GMSS key pair. The public key is an instance of
	 * GMSSPublicKey, the private key is an instance of GMSSPrivateKey.
	 * 
	 * @return Key pair containing a GMSSPublicKey and a GMSSPrivateKey
	 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKey
	 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPublicKey
	 */
	public KeyPair genKeyPair() {
		if (!initialized)
			initializeDefault();

		// initialize authenticationPaths and treehash instances
		byte[][][] currentAuthPaths = new byte[numLayer][][];
		byte[][][] nextAuthPaths = new byte[numLayer - 1][][];
		Treehash[][] currentTreehash = new Treehash[numLayer][];
		Treehash[][] nextTreehash = new Treehash[numLayer - 1][];

		Vector[] currentStack = new Vector[numLayer];
		Vector[] nextStack = new Vector[numLayer - 1];

		Vector[][] currentRetain = new Vector[numLayer][];
		Vector[][] nextRetain = new Vector[numLayer - 1][];

		for (int i = 0; i < numLayer; i++) {
			currentAuthPaths[i] = new byte[heightOfTrees[i]][mdLength];
			currentTreehash[i] = new Treehash[heightOfTrees[i] - K[i]];

			if (i > 0) {
				nextAuthPaths[i - 1] = new byte[heightOfTrees[i]][mdLength];
				nextTreehash[i - 1] = new Treehash[heightOfTrees[i] - K[i]];
			}

			currentStack[i] = new Vector();
			if (i > 0)
				nextStack[i - 1] = new Vector();
		}

		// initialize roots
		byte[][] currentRoots = new byte[numLayer][mdLength];
		byte[][] nextRoots = new byte[numLayer - 1][mdLength];
		// initialize seeds
		byte[][] seeds = new byte[numLayer][mdLength];
		// initialize seeds[] by copying starting-seeds of first trees of each
		// layer
		for (int i = 0; i < numLayer; i++) {
			System.arraycopy(currentSeeds[i], 0, seeds[i], 0, mdLength);
		}

		// initialize rootSigs
		currentRootSigs = new byte[numLayer - 1][mdLength];

		// -------------------------
		// -------------------------
		// --- calculation of current authpaths and current rootsigs (AUTHPATHS,
		// SIG)------
		// from bottom up to the root
		for (int h = numLayer - 1; h >= 0; h--) {
			GMSSRootCalc tree = new GMSSRootCalc(this.heightOfTrees[h],
					this.K[h], this.algNames);
			try {
				// on lowest layer no lower root is available, so just call
				// the method with null as first parameter
				if (h == numLayer - 1)
					tree = this.generateCurrentAuthpathAndRoot(null,
							currentStack[h], seeds[h], h);
				else
					// otherwise call the method with the former computed root
					// value
					tree = this.generateCurrentAuthpathAndRoot(
							currentRoots[h + 1], currentStack[h], seeds[h], h);

			} catch (SignatureException e1) {
				e1.printStackTrace();
			}

			// set initial values needed for the private key construction
			for (int i = 0; i < heightOfTrees[h]; i++) {
				System.arraycopy(tree.getAuthPath()[i], 0,
						currentAuthPaths[h][i], 0, mdLength);
			}
			currentRetain[h] = tree.getRetain();
			currentTreehash[h] = tree.getTreehash();
			System.arraycopy(tree.getRoot(), 0, currentRoots[h], 0, mdLength);
		}

		// --- calculation of next authpaths and next roots (AUTHPATHS+, ROOTS+)
		// ------
		for (int h = numLayer - 2; h >= 0; h--) {
			GMSSRootCalc tree = new GMSSRootCalc(this.heightOfTrees[h + 1],
					this.K[h + 1], this.algNames);

			tree = this.generateNextAuthpathAndRoot(nextStack[h], seeds[h + 1],
					h + 1);

			// set initial values needed for the private key construction
			for (int i = 0; i < heightOfTrees[h + 1]; i++) {
				System.arraycopy(tree.getAuthPath()[i], 0, nextAuthPaths[h][i],
						0, mdLength);
			}
			nextRetain[h] = tree.getRetain();
			nextTreehash[h] = tree.getTreehash();
			System.arraycopy(tree.getRoot(), 0, nextRoots[h], 0, mdLength);

			// create seed for the Merkle tree after next (nextNextSeeds)
			// SEEDs++
			System.arraycopy(seeds[h + 1], 0, this.nextNextSeeds[h], 0,
					mdLength);
		}
		// ------------

		// generate GMSSPublicKey
		GMSSPublicKey publicKey = new GMSSPublicKey(currentRoots[0], gmssPS);

		// generate the GMSSPrivateKey
		GMSSPrivateKey privateKey = new GMSSPrivateKey(currentSeeds,
				nextNextSeeds, currentAuthPaths, nextAuthPaths,
				currentTreehash, nextTreehash, currentStack, nextStack,
				currentRetain, nextRetain, nextRoots, currentRootSigs, gmssPS,
				algNames);

		// return the KeyPair
		return (new KeyPair(publicKey, privateKey));
	}

	/**
	 * calculates the authpath for tree in layer h which starts with seed[h]
	 * additionally computes the rootSignature of underlaying root
	 * 
	 * @param currentStack
	 *            stack used for the treehash instance created by this method
	 * @param lowerRoot
	 *            stores the root of the lower tree
	 * @param seeds
	 *            starting seeds
	 * @param h
	 *            actual layer
	 * @throws SignatureException
	 *             if the OTS verifying goes wrong
	 */
	private GMSSRootCalc generateCurrentAuthpathAndRoot(byte[] lowerRoot,
			Vector currentStack, byte[] seed, int h) throws SignatureException {
		byte[] help = new byte[mdLength];

		byte[] OTSseed = new byte[mdLength];
		OTSseed = gmssRandom.nextSeed(seed);

		WinternitzOTSignature ots;

		// data structure that constructs the whole tree and stores
		// the initial values for treehash, Auth and retain
		GMSSRootCalc treeToConstruct = new GMSSRootCalc(this.heightOfTrees[h],
				this.K[h], this.algNames);

		treeToConstruct.initialize(currentStack);

		// generate the first leaf
		if (h == numLayer - 1) {
			ots = new WinternitzOTSignature(OTSseed, algNames, otsIndex[h]);
			help = ots.getPublicKey();
		} else {
			// for all layers except the lowest, generate the signature of the
			// underlying root
			// and reuse this signature to compute the first leaf of acual layer
			// more efficiently (by verifiing the signature)
			ots = new WinternitzOTSignature(OTSseed, algNames, otsIndex[h]);
			currentRootSigs[h] = ots.getSignature(lowerRoot);
			WinternitzOTSVerify otsver = new WinternitzOTSVerify(algNames,
					otsIndex[h]);
			help = otsver.Verify(lowerRoot, currentRootSigs[h]);
		}
		// update the tree with the first leaf
		treeToConstruct.update(help);

		int seedForTreehashIndex = 3;
		int count = 0;

		// update the tree 2^(H) - 1 times, from the second to the last leaf
		for (int i = 1; i < (1 << this.heightOfTrees[h]); i++) {
			// initialize the seeds for the leaf generation with index 3 * 2^h
			if (i == seedForTreehashIndex
					&& count < this.heightOfTrees[h] - this.K[h]) {
				treeToConstruct.initializeTreehashSeed(seed, count);
				seedForTreehashIndex *= 2;
				count++;
			}

			OTSseed = gmssRandom.nextSeed(seed);
			ots = new WinternitzOTSignature(OTSseed, algNames, otsIndex[h]);
			treeToConstruct.update(ots.getPublicKey());
		}

		if (treeToConstruct.wasFinished()) {
			return treeToConstruct;
		}
		System.err.println("Baum noch nicht fertig konstruiert!!!");
		return null;
	}

	/**
	 * calculates the authpath and root for tree in layer h which starts with
	 * seed[h]
	 * 
	 * @param nextStack
	 *            stack used for the treehash instance created by this method
	 * @param seeds
	 *            starting seeds
	 * @param h
	 *            actual layer
	 * 
	 */
	private GMSSRootCalc generateNextAuthpathAndRoot(Vector nextStack,
			byte[] seed, int h) {
		byte[] OTSseed = new byte[numLayer];
		WinternitzOTSignature ots;

		// data structure that constructs the whole tree and stores
		// the initial values for treehash, Auth and retain
		GMSSRootCalc treeToConstruct = new GMSSRootCalc(this.heightOfTrees[h],
				this.K[h], this.algNames);
		treeToConstruct.initialize(nextStack);

		int seedForTreehashIndex = 3;
		int count = 0;

		// update the tree 2^(H) times, from the first to the last leaf
		for (int i = 0; i < (1 << this.heightOfTrees[h]); i++) {
			// initialize the seeds for the leaf generation with index 3 * 2^h
			if (i == seedForTreehashIndex
					&& count < this.heightOfTrees[h] - this.K[h]) {
				treeToConstruct.initializeTreehashSeed(seed, count);
				seedForTreehashIndex *= 2;
				count++;
			}

			OTSseed = gmssRandom.nextSeed(seed);
			ots = new WinternitzOTSignature(OTSseed, algNames, otsIndex[h]);
			treeToConstruct.update(ots.getPublicKey());
		}

		if (treeToConstruct.wasFinished())
			return treeToConstruct;
		System.err.println("Nächster Baum noch nicht fertig konstruiert!!!");
		return null;
	}

	/**
	 * This method initializes the GMSS KeyPairGenerator using an integer value
	 * <code>keySize</code> as input. It provides a simple use of the GMSS for
	 * testing demands.
	 * <p>
	 * A given <code>keysize</code> of less than 10 creates an amount 2^10
	 * signatures. A keySize between 10 and 20 creates 2^20 signatures. Given an
	 * integer greater than 20 the key pair generator creates 2^40 signatures.
	 * 
	 * @param keySize
	 *            Assigns the parameters used for the GMSS signatures. There are
	 *            3 choices:<br/>
	 *            1. keysize <= 10: creates 2^10 signatures using the
	 *            parameterset<br/>
	 *            P = (2, (5, 5), (3, 3), (3, 3))<br/>
	 *            2. keysize > 10 and <= 20: creates 2^20 signatures using the
	 *            parameterset<br/>
	 *            P = (2, (10, 10), (5, 4), (2, 2))<br/>
	 *            3. keysize > 20: creates 2^40 signatures using the
	 *            parameterset<br/>
	 *            P = (2, (10, 10, 10, 10), (9, 9, 9, 3), (2, 2, 2, 2))
	 * 
	 * @param secureRandom
	 *            not used by GMSS, the SHA1PRNG of the SUN Provider is always
	 *            used
	 */
	public void initialize(int keySize, SecureRandom secureRandom) {

		GMSSParameterSpec gps;
		if (keySize <= 10) { // create 2^10 keys
			int[] defh = { 10 };
			int[] defw = { 3 };
			int[] defk = { 2 };
			gps = new GMSSParameterSpec(defh.length, defh, defw, defk);
		} else if (keySize <= 20) { // create 2^20 keys
			int[] defh = { 10, 10 };
			int[] defw = { 5, 4 };
			int[] defk = { 2, 2 };
			gps = new GMSSParameterSpec(defh.length, defh, defw, defk);
		} else { // create 2^40 keys, keygen lasts around 80 seconds
			int[] defh = { 10, 10, 10, 10 };
			int[] defw = { 9, 9, 9, 3 };
			int[] defk = { 2, 2, 2, 2 };
			gps = new GMSSParameterSpec(defh.length, defh, defw, defk);
		}

		// call the initializer with the chosen parameters
		try {
			this.initialize(gps);
		} catch (InvalidAlgorithmParameterException ae) {
		}
	}

	/**
	 * Initalizes the key pair generator using a parameter set as input
	 * 
	 * @param algParamSpec
	 *            an instance of <a
	 *            href="GMSSParameterSpec.html">GMSSParameterSpec</a>
	 * @param secureRandom
	 *            not used in GMSS
	 * @see de.flexiprovider.pqc.hbc.gmss.GMSSParameterSpec
	 */

	public void initialize(AlgorithmParameterSpec algParamSpec,
			SecureRandom secureRandom)
			throws InvalidAlgorithmParameterException {
		this.initialize(algParamSpec);
	}

	/**
	 * Initalizes the key pair generator using a parameter set as input
	 * 
	 * @param algParamSpec
	 *            an instance of <a
	 *            href="GMSSParameterSpec.html">GMSSParameterSpec</a>
	 * @see de.flexiprovider.pqc.hbc.gmss.GMSSParameterSpec
	 */
	public void initialize(AlgorithmParameterSpec algParamSpec)
			throws InvalidAlgorithmParameterException {

		if (!(algParamSpec instanceof GMSSParameterSpec)) {
			throw new InvalidAlgorithmParameterException(
					"in GMSSKeyPairGenerator: initialize: params is not "
							+ "an instance of GMSSParameterSpec");
		}
		this.gmssParameterSpec = (GMSSParameterSpec) algParamSpec;

		// generate GMSSParameterset
		this.gmssPS = new GMSSParameterset(gmssParameterSpec.getNumOfLayers(),
				gmssParameterSpec.getHeightOfTrees(), gmssParameterSpec
						.getWinternitzParameter(), gmssParameterSpec.getK());

		this.numLayer = gmssPS.getNumOfLayers();
		this.heightOfTrees = gmssPS.getHeightOfTrees();
		this.otsIndex = gmssPS.getWinternitzParameter();
		this.K = gmssPS.getK();

		// seeds
		this.currentSeeds = new byte[numLayer][mdLength];
		this.nextNextSeeds = new byte[numLayer - 1][mdLength];

		byte[] seed;
		// construct SecureRandom for initial seed generation
		SecureRandom secRan = Registry.getSecureRandom();

		// generation of initial seeds
		for (int i = 0; i < numLayer; i++) {
			seed = secRan.generateSeed(mdLength);
			System.arraycopy(seed, 0, currentSeeds[i], 0, mdLength);
			gmssRandom.nextSeed(currentSeeds[i]);
		}

		this.initialized = true;
	}

	/**
	 * This method is called by generateKeyPair() in case that no other
	 * initialization method has been called by the user
	 */
	private void initializeDefault() {
		int[] defh = { 10, 10, 10, 10 };
		int[] defw = { 3, 3, 3, 3 };
		int[] defk = { 2, 2, 2, 2 };

		GMSSParameterSpec gps = new GMSSParameterSpec(defh.length, defh, defw,
				defk);

		try {
			this.initialize(gps);
		} catch (InvalidAlgorithmParameterException ae) {
		}
	}
}

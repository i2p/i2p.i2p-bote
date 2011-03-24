package de.flexiprovider.pqc.hbc.gmss;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.flexiprovider.api.MessageDigest;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.SecureRandom;
import de.flexiprovider.api.Signature;
import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.api.exceptions.SignatureException;
import de.flexiprovider.api.keys.PrivateKey;
import de.flexiprovider.api.keys.PublicKey;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;
import de.flexiprovider.common.util.ByteUtils;
import de.flexiprovider.core.CoreRegistry;

/**
 * This class implements the GMSS signature scheme. The class extends the
 * SignatureSpi class.
 * <p>
 * The GMSSSignature can be used as follows:
 * <p>
 * <b>Signature generation:</b>
 * <p>
 * 1. generate KeySpec from encoded GMSS private key:<br/>
 * <code>KeySpec privateKeySpec = new PKCS8EncodedKeySpec(encPrivateKey);</code><br/>
 * 2. get instance of GMSS key factory:<br/>
 * <code>KeyFactory keyFactory = KeyFactory.getInstance("GMSS","FlexiPQC");</code><br/>
 * 3. decode GMSS private key:<br/>
 * <code>PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);</code><br/>
 * 4. get instance of a GMSS signature:<br/> <code>Signature cmmsSig =
 * Signature.getInstance("GMSSwithSHA1","FlexiPQC");</code><br/>
 * 5. initialize signing:<br/> <code>gmssSig.initSign(privateKey);</code><br/>
 * 6. sign message:<br/> <code>gmssSig.update(message.getBytes());<br/>
 * signature = gmssSig.sign();<br/>
 * return signature;</code>
 * <p>
 * <b>Signature verification:</b>
 * <p>
 * 1. generate KeySpec from encoded GMSS public key:<br/>
 * <code>KeySpec publicKeySpec = new X509EncodedKeySpec(encPublicKey);</code><br/>
 * 2. decode GMSS public key:<br/>
 * <code>PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);</code><br/>
 * 3. initialize verifying:<br/> <code>gmssSig.initVerify(publicKey);</code><br/>
 * 4. Verify the signature:<br/> <code>gmssSig.update(message.getBytes());<br/>
 * return gmssSig.verify(signature);</code>
 * 
 * @author Michael Schneider, Sebastian Blume
 * @see GMSSKeyPairGenerator
 */
public class GMSSSignature extends Signature {
    /*
     * Inner classes providing concrete implementations of MerkleOTSSignature
     * with a variety of message digests.
     */

    // //////////////////////////////////////////////////////////////////////////////
    /**
     * GMSSSignature with SHA1 message digest
     */
    public static class GMSSwithSHA1 extends GMSSSignature {

	/**
	 * The OID of the algorithm.
	 */
	public static final String OID = GMSSKeyPairGenerator.GMSSwithSHA1.OID;

	/**
	 * Constructor.
	 */
	public GMSSwithSHA1() {
	    super(OID, "SHA1", "FlexiCore");
	}
    }

    /**
     * GMSSSignature with SHA224 message digest
     */
    public static class GMSSwithSHA224 extends GMSSSignature {

	/**
	 * The OID of the algorithm.
	 */
	public static final String OID = GMSSKeyPairGenerator.GMSSwithSHA224.OID;

	/**
	 * Constructor.
	 */
	public GMSSwithSHA224() {
	    super(OID, "SHA224", "FlexiCore");
	}
    }

    /**
     * GMSSSignature with SHA256 message digest
     */
    public static class GMSSwithSHA256 extends GMSSSignature {

	/**
	 * The OID of the algorithm.
	 */
	public static final String OID = GMSSKeyPairGenerator.GMSSwithSHA256.OID;

	/**
	 * Constructor.
	 */
	public GMSSwithSHA256() {
	    super(OID, "SHA256", "FlexiCore");
	}
    }

    /**
     * GMSSSignature with SHA384 message digest
     */
    public static class GMSSwithSHA384 extends GMSSSignature {

	/**
	 * The OID of the algorithm.
	 */
	public static final String OID = GMSSKeyPairGenerator.GMSSwithSHA384.OID;

	/**
	 * Constructor.
	 */
	public GMSSwithSHA384() {
	    super(OID, "SHA384", "FlexiCore");
	}
    }

    /**
     * GMSSSignature with SHA512 message digest
     */
    public static class GMSSwithSHA512 extends GMSSSignature {

	/**
	 * The OID of the algorithm.
	 */
	public static final String OID = GMSSKeyPairGenerator.GMSSwithSHA512.OID;

	/**
	 * Constructor.
	 */
	public GMSSwithSHA512() {
	    super(OID, "SHA512", "FlexiCore");
	}
    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Instance of GMSSParameterSpec
     */
    private GMSSParameterSpec gmssParameterSpec;

    /**
     * Instance of GMSSUtilities
     */
    private GMSSUtilities gmssUtil = new GMSSUtilities();

    /**
     * The GMSS private key
     */
    private GMSSPrivateKey gmssPrivateKey;

    /**
     * The GMSS public key
     */
    private GMSSPublicKey gmssPublicKey;

    /**
     * The raw GMSS public key
     */
    private byte[] pubKeyBytes;

    /**
     * Hash function for the construction of the authentication trees
     */
    private MessageDigest messDigestTrees;

    /**
     * The length of the hash function output
     */
    private int mdLength;

    /**
     * The number of tree layers
     */
    private int numLayer;

    /**
     * The hash function used by the OTS
     */
    private MessageDigest messDigestOTS;

    /**
     * An instance of the Winternitz one-time signature
     */
    private WinternitzOTSignature ots;

    /**
     * Array of strings containing the name of the hash function used by the OTS
     * and the corresponding provider name
     */
    private String[] algNames = new String[2];

    /**
     * The current main tree and subtree indices
     */
    private int[] index;

    /**
     * Array of the authentication paths for the current trees of all layers
     */
    private byte[][][] currentAuthPaths;

    /**
     * The one-time signature of the roots of the current subtrees
     */
    private byte[][] subtreeRootSig;

    /**
     * The ByteArrayOutputStream holding the messages
     */
    private ByteArrayOutputStream baos;

    /**
     * The GMSSParameterset
     */
    private GMSSParameterset gmssPS;

    /**
     * The PRNG
     */
    private GMSSRandom gmssRandom;

    /**
     * The standard constructor tries to generate the MerkleTree Algorithm
     * identifier with the corresponding OID.
     * 
     * @param oidStr
     *                string with the oid of the algorithm
     * @param mdName
     *                name of the message digest for the authentication trees
     * @param mdProvName
     *                provider name of the message digest for the authentication
     *                trees
     */
    public GMSSSignature(String oidStr, String mdName, String mdProvName) {
	algNames[0] = mdName;
	algNames[1] = mdProvName;
	CoreRegistry.registerAlgorithms();

	// construct message digest
	try {
	    messDigestTrees = Registry.getMessageDigest(mdName);
	    messDigestOTS = messDigestTrees;
	    mdLength = messDigestTrees.getDigestLength();
	    gmssRandom = new GMSSRandom(messDigestTrees);
	} catch (NoSuchAlgorithmException nsae) {
	    throw new RuntimeException("message digest " + mdName
		    + " not found in " + mdProvName + " or signature " + mdName
		    + " not found in " + mdProvName);
	}

    }

    /**
     * Feeds a message byte to the message digest.
     * 
     * @param data
     *                array of message bytes
     * @throws SignatureException
     *                 if the signature object is not initialized correctly.
     */
    public void update(byte data) throws SignatureException {
	baos.write(data);
    }

    /**
     * Feeds message bytes to the message digest.
     * 
     * @param data
     *                array of message bytes
     * @param offset
     *                index of message start
     * @param length
     *                number of message bytes
     * @throws SignatureException
     *                 if the signature object is not initialized correctly.
     */

    public void update(byte[] data, int offset, int length)
	    throws SignatureException {
	baos.write(data, offset, length);
    }

    /**
     * This method returns the date contained in the ByteArrayOutputStream
     * closes the stream
     */
    private byte[] getData() {
	byte[] data = baos.toByteArray();

	try {
	    baos.close();
	} catch (IOException ioe) {
	    System.out.println("Can not close ByteArrayOutputStream");
	}
	baos.reset();
	return data;
    }

    /**
     * This method creates a ByteArrayOutputStream
     */
    private void initValues() {

	/*
	 * int logValue = merkleOperations.lookupLog(digestLength)+4;
	 * logValue/=8; logValue++; keySize = (digestLength+3*logValue)<<2;
	 * helpSize = digestLength;
	 */

	baos = new ByteArrayOutputStream();

    }

    /**
     * Initializes the signature algorithm for signing a message.
     * <p>
     * 
     * @param privateKey
     *                the private key of the signer.
     * @throws InvalidKeyException
     *                 if the key is not an instance of OTSPrivateKey.
     */
    public void initSign(PrivateKey privateKey, SecureRandom sr)
	    throws InvalidKeyException {
	if (privateKey instanceof GMSSPrivateKey) {
	    messDigestTrees.reset();
	    initValues();
	    // set private key and take from it ots key, auth, tree and key
	    // counter, rootSign
	    gmssPrivateKey = (GMSSPrivateKey) privateKey;

	    // check if last signature has been generated
	    if (gmssPrivateKey.getIndex(0) >= gmssPrivateKey.getNumLeafs(0)) {
		throw new RuntimeException(
			"No more signatures can be generated");
	    }

	    // get Parameterset
	    this.gmssPS = gmssPrivateKey.getParameterset();
	    // get numLayer
	    this.numLayer = gmssPS.getNumOfLayers();

	    // get OTS Instance of lowest layer
	    byte[] seed = gmssPrivateKey.getCurrentSeeds()[numLayer - 1];
	    byte[] OTSSeed = new byte[mdLength];
	    byte[] dummy = new byte[mdLength];
	    System.arraycopy(seed, 0, dummy, 0, mdLength);
	    OTSSeed = gmssRandom.nextSeed(dummy); // secureRandom.nextBytes(currentSeeds[currentSeeds.length-1]);secureRandom.nextBytes(OTSseed);
	    this.ots = new WinternitzOTSignature(OTSSeed, algNames, gmssPS
		    .getWinternitzParameter()[numLayer - 1]);

	    byte[][][] helpCurrentAuthPaths = gmssPrivateKey
		    .getCurrentAuthPaths();
	    currentAuthPaths = new byte[numLayer][][];

	    // copy the main tree authentication path
	    for (int j = 0; j < numLayer; j++) {
		currentAuthPaths[j] = new byte[helpCurrentAuthPaths[j].length][mdLength];
		for (int i = 0; i < helpCurrentAuthPaths[j].length; i++) {
		    System.arraycopy(helpCurrentAuthPaths[j][i], 0,
			    currentAuthPaths[j][i], 0, mdLength);
		}
	    }

	    // copy index
	    index = new int[numLayer];
	    System.arraycopy(gmssPrivateKey.getIndex(), 0, index, 0, numLayer);

	    // copy subtreeRootSig
	    byte[] helpSubtreeRootSig;
	    subtreeRootSig = new byte[numLayer - 1][];
	    for (int i = 0; i < numLayer - 1; i++) {
		helpSubtreeRootSig = gmssPrivateKey.getSubtreeRootSig(i);
		subtreeRootSig[i] = new byte[helpSubtreeRootSig.length];
		System.arraycopy(helpSubtreeRootSig, 0, subtreeRootSig[i], 0,
			helpSubtreeRootSig.length);
	    }

	    // change private key for next signature
	    gmssPrivateKey.nextKey(numLayer - 1);

	} else
	    throw new InvalidKeyException("Key is not a GMSSPrivateKey.");
    }

    /**
     * Signs a message.
     * <p>
     * 
     * @return the signature.
     * @throws SignatureException
     *                 if the signature is not initialized properly.
     */
    public byte[] sign() throws SignatureException {

	byte[] message;
	byte[] otsSig = new byte[mdLength];
	byte[] authPathBytes;
	byte[] indexBytes;

	// --- first part of this signature
	// get the data which should be signed
	message = getData();

	otsSig = ots.getSignature(message);

	// get concatenated lowest layer tree authentication path
	authPathBytes = gmssUtil
		.concatenateArray(currentAuthPaths[numLayer - 1]);

	// put lowest layer index into a byte array
	indexBytes = gmssUtil.intToBytesLittleEndian(index[numLayer - 1]);

	// create first part of GMSS signature
	byte[] gmssSigFirstPart = new byte[indexBytes.length + otsSig.length
		+ authPathBytes.length];
	System.arraycopy(indexBytes, 0, gmssSigFirstPart, 0, indexBytes.length);
	System.arraycopy(otsSig, 0, gmssSigFirstPart, indexBytes.length,
		otsSig.length);
	System.arraycopy(authPathBytes, 0, gmssSigFirstPart,
		(indexBytes.length + otsSig.length), authPathBytes.length);
	// --- end first part

	// --- next parts of the signature
	// create initial array with length 0 for iteration
	byte[] gmssSigNextPart = new byte[0];

	for (int i = numLayer - 1 - 1; i >= 0; i--) {

	    // get concatenated next tree authentication path
	    authPathBytes = gmssUtil.concatenateArray(currentAuthPaths[i]);

	    // put next tree index into a byte array
	    indexBytes = gmssUtil.intToBytesLittleEndian(index[i]);

	    // create next part of GMSS signature

	    // create help array and copy actual gmssSig into it
	    byte[] helpGmssSig = new byte[gmssSigNextPart.length];
	    System.arraycopy(gmssSigNextPart, 0, helpGmssSig, 0,
		    gmssSigNextPart.length);
	    // adjust length of gmssSigNextPart for adding next part
	    gmssSigNextPart = new byte[helpGmssSig.length + indexBytes.length
		    + subtreeRootSig[i].length + authPathBytes.length];

	    // copy old data (help array) and new data in gmssSigNextPart
	    System.arraycopy(helpGmssSig, 0, gmssSigNextPart, 0,
		    helpGmssSig.length);
	    System.arraycopy(indexBytes, 0, gmssSigNextPart,
		    helpGmssSig.length, indexBytes.length);
	    System.arraycopy(subtreeRootSig[i], 0, gmssSigNextPart,
		    (helpGmssSig.length + indexBytes.length),
		    subtreeRootSig[i].length);
	    System
		    .arraycopy(
			    authPathBytes,
			    0,
			    gmssSigNextPart,
			    (helpGmssSig.length + indexBytes.length + subtreeRootSig[i].length),
			    authPathBytes.length);

	}
	// --- end next parts

	// concatenate the two parts of the GMSS signature
	byte[] gmssSig = new byte[gmssSigFirstPart.length
		+ gmssSigNextPart.length];
	System.arraycopy(gmssSigFirstPart, 0, gmssSig, 0,
		gmssSigFirstPart.length);
	System.arraycopy(gmssSigNextPart, 0, gmssSig, gmssSigFirstPart.length,
		gmssSigNextPart.length);

	// return the GMSS signature
	return gmssSig;
    }

    /**
     * Initializes the signature algorithm for verifying a signature.
     * 
     * @param publicKey
     *                the public key of the signer.
     * @throws InvalidKeyException
     *                 if the public key is not an instance of GMSSPublicKey.
     */
    public void initVerify(PublicKey publicKey) throws InvalidKeyException {
	if (publicKey instanceof GMSSPublicKey) {
	    messDigestTrees.reset();

	    gmssPublicKey = (GMSSPublicKey) publicKey;
	    pubKeyBytes = gmssPublicKey.getPublicKeyBytes();
	    gmssPS = gmssPublicKey.getParameterset();
	    // get numLayer
	    this.numLayer = gmssPS.getNumOfLayers();
	    initValues();

	}

	else
	    throw new InvalidKeyException("Key is not a GMSSPublicKey");
    }

    /**
     * Verifies a signature.
     * 
     * @param signature
     *                the signature to be verified.
     * @return <code>TRUE</code> if the signature is correct,
     *         <code>FALSE</code> otherwise
     */
    public boolean verify(byte[] signature) throws SignatureException {

	boolean success = false;
	// int halfSigLength = signature.length >>> 1;
	messDigestOTS.reset();
	WinternitzOTSVerify otsVerify;
	int otsSigLength;

	// get the message that was signed
	byte[] help = getData();

	byte[] message;
	byte[] otsSig;
	byte[] otsPublicKey;
	byte[][] authPath;
	byte[] dest;
	int nextEntry = 0;
	int index;
	// Verify signature

	// --- begin with message = 'message that was signed'
	// and then in each step message = subtree root
	for (int j = numLayer - 1; j >= 0; j--) {
	    otsVerify = new WinternitzOTSVerify(algNames, gmssPS
		    .getWinternitzParameter()[j]);
	    otsSigLength = otsVerify.getSignatureLength();

	    message = help;
	    // get the subtree index
	    index = gmssUtil.bytesToIntLittleEndian(signature, nextEntry);

	    // 4 is the number of bytes in integer
	    nextEntry += 4;

	    // get one-time signature
	    otsSig = new byte[otsSigLength];
	    System.arraycopy(signature, nextEntry, otsSig, 0, otsSigLength);
	    nextEntry += otsSigLength;

	    // compute public OTS key from the one-time signature
	    otsPublicKey = otsVerify.Verify(message, otsSig);

	    // test if OTSsignature is correct
	    if (otsPublicKey == null) {
		System.err
			.println("OTS Public Key is null in GMSSSignature.verify");
		return false;
	    }

	    // get authentication path from the signature
	    authPath = new byte[gmssPS.getHeightOfTrees()[j]][mdLength];
	    for (int i = 0; i < authPath.length; i++) {
		System
			.arraycopy(signature, nextEntry, authPath[i], 0,
				mdLength);
		nextEntry = nextEntry + mdLength;
	    }

	    // compute the root of the subtree from the authentication path
	    help = new byte[mdLength];

	    help = otsPublicKey;

	    int count = 1 << authPath.length;
	    count = count + index;

	    for (int i = 0; i < authPath.length; i++) {
		dest = new byte[mdLength << 1];

		if ((count % 2) == 0) {
		    System.arraycopy(help, 0, dest, 0, mdLength);
		    System.arraycopy(authPath[i], 0, dest, mdLength, mdLength);
		    count = count / 2;
		} else {
		    System.arraycopy(authPath[i], 0, dest, 0, mdLength);
		    System.arraycopy(help, 0, dest, mdLength, help.length);
		    count = (count - 1) / 2;
		}
		messDigestTrees.update(dest);
		help = messDigestTrees.digest();
	    }
	}

	// now help contains the root of the maintree

	// test if help is equal to the GMSS public key
	if (ByteUtils.equals(pubKeyBytes, help)) {
	    success = true;
	}

	return success;
    }

    /**
     * Sets the parameters for GMSSSignature
     * 
     * @param algParamSpec
     *                parameter specification for MerkleTree.
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSParameterSpec
     */
    public void setParameters(AlgorithmParameterSpec algParamSpec)
	    throws InvalidAlgorithmParameterException {
	if (algParamSpec == null) {
	    return;
	}

	if (!(algParamSpec instanceof GMSSParameterSpec)) {
	    throw new InvalidAlgorithmParameterException(
		    "in GMSSSignature: initialize: params is not "
			    + "an instance of GMSSParameterSpec");
	}
	this.gmssParameterSpec = (GMSSParameterSpec) algParamSpec;
    }

}
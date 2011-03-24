package de.flexiprovider.pqc.hbc.gmss;

import de.flexiprovider.api.MessageDigest;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.common.util.ByteUtils;
import de.flexiprovider.core.CoreRegistry;

/**
 * This class implements the distributed computation of the public key of the
 * Winternitz one-time signature scheme (OTSS). The class is used by the GMSS
 * classes for calculation of upcoming leafs.
 * 
 * @author Sebastian Blume, Michael Schneider
 * 
 */
public class GMSSLeaf {

    /**
     * The hash function used by the OTS and the PRNG
     */
    private MessageDigest messDigestOTS;

    /**
     * The length of the message digest and private key
     */
    private int mdsize, keysize;

    /**
     * The source of randomness for OTS private key generation
     */
    private GMSSRandom gmssRandom;

    /**
     * Byte array for distributed coputation of the upcoming leaf
     */
    private byte[] leaf;

    /**
     * Byte array for storing the concatenated hashes of private key parts
     */
    private byte[] concHashs;

    /**
     * indices for distributed computation
     */
    private int i, j;

    /**
     * storing 2^w
     */
    private int two_power_w;

    /**
     * Winternitz parameter w
     */
    private int w;

    /**
     * the amount of distributed computation steps when updateLeaf is called
     */
    private int steps;

    /**
     * the internal seed
     */
    private byte[] seed;

    /**
     * the OTS privateKey parts
     */
    byte[] privateKeyOTS;

    /**
     * This constructor regenerates a prior GMSSLeaf object
     * 
     * @param name
     *                an array of strings, containing the name of the used hash
     *                function and PRNG and the name of the corresponding
     *                provider
     * @param statByte
     *                status bytes
     * @param statInt
     *                status ints
     */
    public GMSSLeaf(String[] name, byte[][] statByte, int[] statInt) {
	this.i = statInt[0];
	this.j = statInt[1];
	this.steps = statInt[2];
	this.w = statInt[3];

	CoreRegistry.registerAlgorithms();
	try {
	    messDigestOTS = Registry.getMessageDigest(name[0]);
	} catch (NoSuchAlgorithmException nsae) {
	    throw new RuntimeException(": message digest " + name[0]
		    + " not found in " + name[1]);
	}

	gmssRandom = new GMSSRandom(messDigestOTS);

	// calulate keysize for private key and the help array
	mdsize = messDigestOTS.getDigestLength();
	int mdsizeBit = mdsize << 3;
	int messagesize = (int) Math.ceil((double) (mdsizeBit) / (double) w);
	int checksumsize = getLog((messagesize << w) + 1);
	this.keysize = messagesize
		+ (int) Math.ceil((double) checksumsize / (double) w);
	this.two_power_w = 1 << w;

	// calculate steps
	// ((2^w)-1)*keysize + keysize + 1 / (2^h -1)

	// initialize arrays
	this.privateKeyOTS = statByte[0];
	this.seed = statByte[1];
	this.concHashs = statByte[2];
	this.leaf = statByte[3];
    }

    /**
     * The constructor precomputes some needed variables for ditributed leaf
     * calculation
     * 
     * @param name
     *                an array of strings, containing the name of the used hash
     *                function and PRNG and the name of the corresponding
     *                provider
     * @param w
     *                the winterniz parameter of that tree the leaf is computed
     *                for
     * @param numLeafs
     *                the number of leafs of the tree from where the distributed
     *                computation is called
     */
    public GMSSLeaf(String[] name, int w, int numLeafs) {
	this.w = w;

	CoreRegistry.registerAlgorithms();
	try {
	    messDigestOTS = Registry.getMessageDigest(name[0]);
	} catch (NoSuchAlgorithmException nsae) {
	    throw new RuntimeException(": message digest " + name[0]
		    + " not found in " + name[1]);
	}

	gmssRandom = new GMSSRandom(messDigestOTS);

	// calulate keysize for private key and the help array
	mdsize = messDigestOTS.getDigestLength();
	int mdsizeBit = mdsize << 3;
	int messagesize = (int) Math.ceil((double) (mdsizeBit) / (double) w);
	int checksumsize = getLog((messagesize << w) + 1);
	this.keysize = messagesize
		+ (int) Math.ceil((double) checksumsize / (double) w);
	this.two_power_w = 1 << w;

	// calculate steps
	// ((2^w)-1)*keysize + keysize + 1 / (2^h -1)
	this.steps = (int) Math
		.ceil((double) (((1 << w) - 1) * keysize + 1 + keysize)
			/ (double) (numLeafs));

	// initialize arrays
	this.seed = new byte[mdsize];
	this.leaf = new byte[mdsize];
	this.privateKeyOTS = new byte[mdsize];
	this.concHashs = new byte[mdsize * keysize];
    }

    /**
     * initialize the distributed leaf calculation reset i,j and compute OTSseed
     * with seed0
     * 
     * @param seed0
     *                the starting seed
     */
    public void initLeafCalc(byte[] seed0) {
	this.i = 0;
	this.j = 0;
	byte[] dummy = new byte[mdsize];
	System.arraycopy(seed0, 0, dummy, 0, seed.length);
	this.seed = gmssRandom.nextSeed(dummy);
    }

    /**
     * Processes <code>steps</code> steps of distributed leaf calculation
     * 
     * @return true if leaf is completed, else false
     */
    public boolean updateLeafCalc() {
	// steps times do
	for (int s = 0; s < steps; s++) {

	    if (i == keysize && j == two_power_w - 1) { // [3] at last hash the
		// concatenation
		messDigestOTS.update(concHashs);
		leaf = messDigestOTS.digest();
		return true; // leaf fineshed
	    } else if (i == 0 || j == two_power_w - 1) { // [1] at the
		// beginning and
		// when [2] is
		// finished: get the
		// next private key
		// part
		i++;
		j = 0;
		// get next privKey part
		this.privateKeyOTS = gmssRandom.nextSeed(seed);
	    } else { // [2] hash the privKey part
		messDigestOTS.update(privateKeyOTS);
		privateKeyOTS = messDigestOTS.digest();
		j++;
		if (j == two_power_w - 1) { // after w hashes add to the
		    // concatenated array
		    System.arraycopy(privateKeyOTS, 0, concHashs, mdsize
			    * (i - 1), mdsize);
		}
	    }
	}

	return false; // leaf not finished yet
    }

    /**
     * Returns the leaf value.
     * 
     * @return the leaf value
     */
    public byte[] getLeaf() {
	return leaf;
    }

    /**
     * This method returns the least integer that is greater or equal to the
     * logarithm to the base 2 of an integer <code>intValue</code>.
     * 
     * @param intValue
     *                an integer
     * @return The least integer greater or equal to the logarithm to the base 2
     *         of <code>intValue</code>
     */
    private int getLog(int intValue) {
	int log = 1;
	int i = 2;
	while (i < intValue) {
	    i <<= 1;
	    log++;
	}
	return log;
    }

    /**
     * Returns the status byte array used by the GMSSPrivateKeyASN.1 class
     * 
     * @return The status bytes
     */
    public byte[][] getStatByte() {

	byte[][] statByte = new byte[4][];
	statByte[0] = new byte[mdsize];
	statByte[1] = new byte[mdsize];
	statByte[2] = new byte[mdsize * keysize];
	statByte[3] = new byte[mdsize];
	statByte[0] = privateKeyOTS;
	statByte[1] = seed;
	statByte[2] = concHashs;
	statByte[3] = leaf;

	return statByte;
    }

    /**
     * Returns the status int array used by the GMSSPrivateKeyASN.1 class
     * 
     * @return The status ints
     */
    public int[] getStatInt() {

	int[] statInt = new int[4];
	statInt[0] = i;
	statInt[1] = j;
	statInt[2] = steps;
	statInt[3] = w;
	return statInt;
    }

    /**
     * Returns a String representation of the main part of this element
     * 
     * @return a String representation of the main part of this element
     */
    public String toString() {
	String out = "";

	for (int i = 0; i < 4; i++) {
	    out = out + this.getStatInt()[i] + " ";
	}
	out = out + " " + this.mdsize + " " + this.keysize + " "
		+ this.two_power_w + " ";

	byte[][] temp = this.getStatByte();
	for (int i = 0; i < 4; i++) {
	    if (temp[i] != null) {
		out = out + ByteUtils.toHexString(temp[i]) + " ";
	    } else
		out = out + "null ";
	}
	return out;
    }

}

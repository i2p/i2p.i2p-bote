package de.flexiprovider.pqc.hbc.gmss;

import de.flexiprovider.api.MessageDigest;

/**
 * This class provides a PRNG for GMSS
 * 
 * @author Sebastian Blume, Michael Schneider
 */
public class GMSSRandom {

    /**
     * Hash function for the construction of the authentication trees
     */
    private MessageDigest messDigestTree;

    /**
     * Constructor
     * 
     * @param messDigestTree
     */
    public GMSSRandom(MessageDigest messDigestTree) {

	this.messDigestTree = messDigestTree;
    }

    /**
     * computes the next seed value, returns a random byte array and sets
     * outseed to the next value
     * 
     * @param outseed
     *                byte array in which ((1 + SEEDin +RAND) mod 2^n) will be
     *                stored
     * @return byte array of H(SEEDin)
     */
    public byte[] nextSeed(byte[] outseed) {

	// byte array value "1"
	byte[] one = new byte[outseed.length];
	one[0] = 1;
	for (int i = 1; i < outseed.length; i++) {
	    one[i] = 0;
	}
	// RAND <-- H(SEEDin)
	byte[] rand = new byte[outseed.length];
	messDigestTree.update(outseed);
	rand = messDigestTree.digest();

	// SEEDout <-- (1 + SEEDin +RAND) mod 2^n
	addByteArrays(outseed, rand);
	addOne(outseed);

	// System.arraycopy(outseed, 0, outseed, 0, outseed.length);

	return rand;
    }

    private void addByteArrays(byte[] a, byte[] b) {

	byte overflow = 0;
	int temp;

	for (int i = 0; i < a.length; i++) {
	    temp = (0xFF & a[i]) + (0xFF & b[i]) + overflow;
	    a[i] = (byte) temp;
	    overflow = (byte) (temp >> 8);
	}
    }

    private void addOne(byte[] a) {

	byte overflow = 1;
	int temp;

	for (int i = 0; i < a.length; i++) {
	    temp = (0xFF & a[i]) + overflow;
	    a[i] = (byte) temp;
	    overflow = (byte) (temp >> 8);
	}
    }

}

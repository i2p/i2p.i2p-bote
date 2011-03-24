package de.flexiprovider.pqc.hbc.gmss;

import de.flexiprovider.api.exceptions.InvalidParameterException;

/**
 * This class provides a specification for the GMSS parameters that are used by
 * the GMSSKeyPairGenerator and GMSSSignature classes.
 * 
 * @author Sebastian Blume, Michael Schneider
 * 
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSKeyPairGenerator
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSSignature
 */
public class GMSSParameterset {

    /**
     * The number of authentication tree layers.
     */
    private int numOfLayers;

    /**
     * The height of the authentication trees of each layer.
     */
    private int[] heightOfTrees;

    /**
     * The Winternitz Parameter 'w' of each layer.
     */
    private int[] winternitzParameter;

    /**
     * The parameter K needed for the authentication path computation
     */
    private int[] K;

    /**
     * The constructor for the parameters of the GMSSKeyPairGenerator.
     * <p>
     * 
     * @param layers
     *                the number of authentication tree layers
     * @param heightOfTrees
     *                the height of the authentication trees
     * @param winternitzParameter
     *                the Winternitz Parameter 'w' of each layer
     * @param K
     *                parameter for authpath computation
     */
    public GMSSParameterset(int layers, int[] heightOfTrees,
	    int[] winternitzParameter, int[] K)
	    throws InvalidParameterException {
	boolean valid = true;
	String errMsg = "";
	this.numOfLayers = layers;
	if ((numOfLayers != winternitzParameter.length)
		|| (numOfLayers != heightOfTrees.length)
		|| (numOfLayers != K.length)) {
	    valid = false;
	    errMsg = "Unexpected parameterset format";
	}
	for (int i = 0; i < numOfLayers; i++) {
	    if ((K[i] < 2) || ((heightOfTrees[i] - K[i]) % 2 != 0)) {
		valid = false;
		errMsg = "Wrong parameter K (K >= 2 and H-K even required)!";
	    }

	    if ((heightOfTrees[i] < 4) || (winternitzParameter[i] < 2)) {
		valid = false;
		errMsg = "Wrong parameter H or w (H > 3 and w > 1 required)!";
	    }
	}

	if (valid) {
	    this.heightOfTrees = heightOfTrees;
	    this.winternitzParameter = winternitzParameter;
	    this.K = K;
	} else
	    throw new InvalidParameterException(errMsg);
    }

    /**
     * Returns the number of levels of the authentication trees.
     * 
     * @return The number of levels of the authentication trees.
     */
    public int getNumOfLayers() {
	return numOfLayers;
    }

    /**
     * Returns the array of height (for each layer) of the authentication trees
     * 
     * @return The array of height (for each layer) of the authentication trees
     */
    public int[] getHeightOfTrees() {
	return heightOfTrees;
    }

    /**
     * Returns the array of WinternitzParameter (for each layer) of the
     * authentication trees
     * 
     * @return The array of WinternitzParameter (for each layer) of the
     *         authentication trees
     */
    public int[] getWinternitzParameter() {
	return winternitzParameter;
    }

    /**
     * Returns the parameter K needed for authentication path computation
     * 
     * @return The parameter K needed for authentication path computation
     */
    public int[] getK() {
	return K;
    }
}

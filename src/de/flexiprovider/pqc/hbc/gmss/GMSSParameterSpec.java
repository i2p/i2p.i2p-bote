package de.flexiprovider.pqc.hbc.gmss;

import de.flexiprovider.api.parameters.AlgorithmParameterSpec;

/**
 * This class provides a specification for the GMSS parameters that are used by
 * the GMSSKeyPairGenerator and GMSSSignature classes.
 * 
 * @author Sebastian Blume, Michael Schneider
 * 
 * @see GMSSKeyPairGenerator
 * @see GMSSSignature
 */
public class GMSSParameterSpec implements AlgorithmParameterSpec {

    /**
     * The number of authentication tree layers. numOfLevels is then equal to
     * the length of heightOfTrees and to the length winternitzParameter
     */
    private int numberOfLayers;

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
    public GMSSParameterSpec(int layers, int[] heightOfTrees,
	    int[] winternitzParameter, int[] K) {
	this.numberOfLayers = layers;
	this.heightOfTrees = heightOfTrees;
	this.winternitzParameter = winternitzParameter;
	this.K = K;
    }

    /**
     * The constructor
     * 
     * @param gmssParSet
     *                an instance of GMSSParameterset
     */
    public GMSSParameterSpec(GMSSParameterset gmssParSet) {
	this.numberOfLayers = gmssParSet.getNumOfLayers();
	this.heightOfTrees = gmssParSet.getHeightOfTrees();
	this.winternitzParameter = gmssParSet.getWinternitzParameter();
	this.K = gmssParSet.getK();
    }

    /**
     * Returns the number of layers of the authentication trees.
     * 
     * @return The number of layers of the authentication trees.
     */
    public int getNumOfLayers() {
	return numberOfLayers;
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
     * Returns the array of Winternitz parameters (for each layer) of the
     * authentication trees
     * 
     * @return The array of Winternitz parameters (for each layer) of the
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

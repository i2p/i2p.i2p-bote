package de.flexiprovider.pqc.hbc.gmss;

import java.util.Vector;

import de.flexiprovider.api.keys.KeySpec;

/**
 * This class provides a specification for a GMSS private key.
 * 
 * @author Michael Schneider, Sebastian Blume;
 * 
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKey
 */
public class GMSSPrivateKeySpec implements KeySpec {

    private int[] index;

    private byte[][] currentSeed;
    private byte[][] nextNextSeed;

    private byte[][][] currentAuthPath;
    private byte[][][] nextAuthPath;

    private Treehash[][] currentTreehash;
    private Treehash[][] nextTreehash;

    private Vector[] currentStack;
    private Vector[] nextStack;

    private Vector[][] currentRetain;
    private Vector[][] nextRetain;

    private byte[][][] keep;

    private GMSSLeaf[] nextNextLeaf;
    private GMSSLeaf[] upperLeaf;
    private GMSSLeaf[] upperTreehashLeaf;

    private int[] minTreehash;

    private GMSSParameterset gmssPS;

    private byte[][] nextRoot;
    private GMSSRootCalc[] nextNextRoot;

    private byte[][] currentRootSig;
    private GMSSRootSig[] nextRootSig;

    private String[] algNames;

    /**
     * /**
     * 
     * @param index
     *                tree indices
     * @param currentSeed
     *                seed for the generation of private OTS keys for the
     *                current subtrees (TREE)
     * @param nextNextSeed
     *                seed for the generation of private OTS keys for the
     *                subtrees after next (TREE++)
     * @param currentAuthPath
     *                array of current authentication paths (AUTHPATH)
     * @param nextAuthPath
     *                array of next authentication paths (AUTHPATH+)
     * @param keep
     *                keep array for the authPath algorithm
     * @param currentTreehash
     *                treehash for authPath algorithm of current tree
     * @param nextTreehash
     *                treehash for authPath algorithm of next tree (TREE+)
     * @param currentStack
     *                shared stack for authPath algorithm of current tree
     * @param nextStack
     *                shared stack for authPath algorithm of next tree (TREE+)
     * @param currentRetain
     *                retain stack for authPath algorithm of current tree
     * @param nextRetain
     *                retain stack for authPath algorithm of next tree (TREE+)
     * @param nextNextLeaf
     *                array of upcoming leafs of the tree after next (LEAF++) of
     *                each layer
     * @param upperLeaf
     *                needed for precomputation of upper nodes
     * @param upperTreehashLeaf
     *                needed for precomputation of upper treehash nodes
     * @param minTreehash
     *                index of next treehash instance to receive an update
     * @param nextRoot
     *                the roots of the next trees (ROOT+)
     * @param nextNextRoot
     *                the roots of the tree after next (ROOT++)
     * @param currentRootSig
     *                array of signatures of the roots of the current subtrees
     *                (SIG)
     * @param nextRootSig
     *                array of signatures of the roots of the next subtree
     *                (SIG+)
     * @param gmssParameterset
     *                the GMSS Parameterset
     * @param algNames
     *                An array of strings, containing the name of the used hash
     *                function and the name of the corresponding provider
     */
    public GMSSPrivateKeySpec(int[] index, byte[][] currentSeed,
	    byte[][] nextNextSeed, byte[][][] currentAuthPath,
	    byte[][][] nextAuthPath, Treehash[][] currentTreehash,
	    Treehash[][] nextTreehash, Vector[] currentStack,
	    Vector[] nextStack, Vector[][] currentRetain,
	    Vector[][] nextRetain, byte[][][] keep, GMSSLeaf[] nextNextLeaf,
	    GMSSLeaf[] upperLeaf, GMSSLeaf[] upperTreehashLeaf,
	    int[] minTreehash, byte[][] nextRoot, GMSSRootCalc[] nextNextRoot,
	    byte[][] currentRootSig, GMSSRootSig[] nextRootSig,
	    GMSSParameterset gmssParameterset, String[] algNames) {

	this.index = index;
	this.currentSeed = currentSeed;
	this.nextNextSeed = nextNextSeed;
	this.currentAuthPath = currentAuthPath;
	this.nextAuthPath = nextAuthPath;
	this.currentTreehash = currentTreehash;
	this.nextTreehash = nextTreehash;
	this.currentStack = currentStack;
	this.nextStack = nextStack;
	this.currentRetain = currentRetain;
	this.nextRetain = nextRetain;
	this.keep = keep;
	this.nextNextLeaf = nextNextLeaf;
	this.upperLeaf = upperLeaf;
	this.upperTreehashLeaf = upperTreehashLeaf;
	this.minTreehash = minTreehash;
	this.nextRoot = nextRoot;
	this.nextNextRoot = nextNextRoot;
	this.currentRootSig = currentRootSig;
	this.nextRootSig = nextRootSig;
	this.gmssPS = gmssParameterset;
	this.algNames = algNames;
    }

    public int[] getIndex() {
	return index;
    }

    public byte[][] getCurrentSeed() {
	return currentSeed;
    }

    public byte[][] getNextNextSeed() {
	return nextNextSeed;
    }

    public byte[][][] getCurrentAuthPath() {
	return currentAuthPath;
    }

    public byte[][][] getNextAuthPath() {
	return nextAuthPath;
    }

    public Treehash[][] getCurrentTreehash() {
	return currentTreehash;
    }

    public Treehash[][] getNextTreehash() {
	return nextTreehash;
    }

    public byte[][][] getKeep() {
	return keep;
    }

    public Vector[] getCurrentStack() {
	return currentStack;
    }

    public Vector[] getNextStack() {
	return nextStack;
    }

    public Vector[][] getCurrentRetain() {
	return currentRetain;
    }

    public Vector[][] getNextRetain() {
	return nextRetain;
    }

    public GMSSLeaf[] getNextNextLeaf() {
	return nextNextLeaf;
    }

    public GMSSLeaf[] getUpperLeaf() {
	return upperLeaf;
    }

    public GMSSLeaf[] getUpperTreehashLeaf() {
	return upperTreehashLeaf;
    }

    public int[] getMinTreehash() {
	return minTreehash;
    }

    public GMSSRootSig[] getNextRootSig() {
	return nextRootSig;
    }

    public GMSSParameterset getGmssPS() {
	return gmssPS;
    }

    public byte[][] getNextRoot() {
	return nextRoot;
    }

    public GMSSRootCalc[] getNextNextRoot() {
	return nextNextRoot;
    }

    public byte[][] getCurrentRootSig() {
	return currentRootSig;
    }

    public String[] getAlgNames() {
	return algNames;
    }
}
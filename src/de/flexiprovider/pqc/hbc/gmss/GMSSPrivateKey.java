package de.flexiprovider.pqc.hbc.gmss;

import java.util.Vector;

import codec.asn1.ASN1Null;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1Type;
import de.flexiprovider.api.MessageDigest;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.api.exceptions.SignatureException;
import de.flexiprovider.api.keys.PrivateKey;
import de.flexiprovider.common.util.ASN1Tools;
import de.flexiprovider.common.util.ByteUtils;

/**
 * This class implements a GMSS private key and is usually initiated by the <a
 * href="GMSSKeyPairGenerator.html">GMSSKeyPairGenerator</a>.
 * 
 * @author Michael Schneider, Sebastian Blume
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSKeyPairGenerator
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKeySpec
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKeyASN1
 */
public class GMSSPrivateKey extends PrivateKey {

    /**
     * An array of tree index of each layer
     */
    private int[] index;

    /**
     * Array of the seeds for the current trees of each layer
     */
    private byte[][] currentSeeds;

    /**
     * Array of the seeds for the trees after next (TREE++) of each layer
     */
    private byte[][] nextNextSeeds;

    /**
     * Array of the authentication paths of the current trees of each layer
     */
    private byte[][][] currentAuthPaths;

    /**
     * Array of the authentication paths of the next trees (TREE+) of each layer
     */
    private byte[][][] nextAuthPaths;

    /**
     * TREEHASH instances for the authentication path algorithm
     */
    private Treehash[][] currentTreehash;

    /**
     * TREEHASH instances for the authentication path algorithm used for the
     * following tree (TREE+)
     */
    private Treehash[][] nextTreehash;

    /**
     * The KEEP arrays for the authentication path algorithm
     */
    private byte[][][] keep;

    /**
     * The stack for the authentication path algorithm
     */
    private Vector[] currentStack;

    /**
     * The stack for the authentication path algorithm used for the following
     * tree (TREE+)
     */
    private Vector[] nextStack;

    /**
     * The RETAIN stacks for the authentication path algorithm
     */
    private Vector[][] currentRetain;

    /**
     * The RETAIN stacks for the authentication path algorithm used for the
     * following tree (TREE+)
     */
    private Vector[][] nextRetain;

    /**
     * An array of the upcoming leaf of the tree after next (TREE++) of each
     * layer (LEAF++)
     */
    private GMSSLeaf[] nextNextLeaf;

    /**
     * An array of the upcoming leaf of the tree over the actual tree
     */
    private GMSSLeaf[] upperLeaf;

    /**
     * An array of the leafs of the upcoming treehashs of the tree over the
     * actual tree
     */
    private GMSSLeaf[] upperTreehashLeaf;

    /**
     * For each layer, this array depicts which treehash instance is the next
     * one to receive an update.
     */
    private int[] minTreehash;

    /**
     * An array of the upcoming signature of the root of next tree (TREE+) of
     * each layer (SIG+)
     */
    private GMSSRootSig[] nextRootSig;

    /**
     * The GMSS Parameterset
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
     * the number of Layers
     */
    private int numLayer;

    /**
     * An array of the roots of the next subtrees (ROOTS+)
     */
    private byte[][] nextRoot;

    /**
     * An array of the roots of the subtrees after next (ROOTS ++)
     */
    private GMSSRootCalc[] nextNextRoot;

    /**
     * An array of the signatures of the roots of the current subtrees (SIG)
     */
    private byte[][] currentRootSig;

    /**
     * The hash function used to construct the authentication trees
     */
    private MessageDigest messDigestTrees;

    /**
     * The message digest length
     */
    private int mdLength;

    /**
     * The number of leafs of one tree of each layer
     */
    private int[] numLeafs;

    /**
     * An array of strings containing the name of the hash function used to
     * construct the authentication trees and used by the OTS.
     */
    private String[] algNames = new String[2];

    /**
     * The PRNG used for private key generation
     */
    private GMSSRandom gmssRandom;

    /**
     * Generates a new GMSS private key
     * 
     * @param currentSeed
     *                seed for the generation of private OTS keys for the
     *                current subtrees
     * @param nextNextSeed
     *                seed for the generation of private OTS keys for the next
     *                subtrees
     * @param currentAuthPath
     *                array of current authentication paths
     * @param nextAuthPath
     *                array of next authentication paths
     * @param currentTreehash
     *                array of current treehash instances
     * @param nextTreehash
     *                array of next treehash instances
     * @param currentStack
     *                array of current shared stacks
     * @param nextStack
     *                array of next shared stacks
     * @param currentRetain
     *                array of current retain stacks
     * @param nextRetain
     *                array of next retain stacks
     * @param nextRoot
     *                the roots of the next subtree
     * @param currentRootSig
     *                array of signatures of the roots of the current subtrees
     * @param gmssParameterset
     *                the GMSS Parameterset
     * @param algNames
     *                An array of strings, containing the name of the used hash
     *                function and the name of the corresponding provider
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSKeyPairGenerator
     */

    protected GMSSPrivateKey(byte[][] currentSeed, byte[][] nextNextSeed,
	    byte[][][] currentAuthPath, byte[][][] nextAuthPath,
	    Treehash[][] currentTreehash, Treehash[][] nextTreehash,
	    Vector[] currentStack, Vector[] nextStack,
	    Vector[][] currentRetain, Vector[][] nextRetain, byte[][] nextRoot,
	    byte[][] currentRootSig, GMSSParameterset gmssParameterset,
	    String[] algNames) {
	this(null, currentSeed, nextNextSeed, currentAuthPath, nextAuthPath,
		null, currentTreehash, nextTreehash, currentStack, nextStack,
		currentRetain, nextRetain, null, null, null, null, nextRoot,
		null, currentRootSig, null, gmssParameterset, algNames);
    }

    /**
     * Constructor
     * 
     * @param gmssPrivKeySpec
     *                a valid GMSS privateKeySpec
     */
    protected GMSSPrivateKey(GMSSPrivateKeySpec gmssPrivKeySpec) {
	this(gmssPrivKeySpec.getIndex(), gmssPrivKeySpec.getCurrentSeed(),
		gmssPrivKeySpec.getNextNextSeed(), gmssPrivKeySpec
			.getCurrentAuthPath(), gmssPrivKeySpec
			.getNextAuthPath(), gmssPrivKeySpec.getKeep(),
		gmssPrivKeySpec.getCurrentTreehash(), gmssPrivKeySpec
			.getNextTreehash(), gmssPrivKeySpec.getCurrentStack(),
		gmssPrivKeySpec.getNextStack(), gmssPrivKeySpec
			.getCurrentRetain(), gmssPrivKeySpec.getNextRetain(),
		gmssPrivKeySpec.getNextNextLeaf(), gmssPrivKeySpec
			.getUpperLeaf(),
		gmssPrivKeySpec.getUpperTreehashLeaf(), gmssPrivKeySpec
			.getMinTreehash(), gmssPrivKeySpec.getNextRoot(),
		gmssPrivKeySpec.getNextNextRoot(), gmssPrivKeySpec
			.getCurrentRootSig(), gmssPrivKeySpec.getNextRootSig(),
		gmssPrivKeySpec.getGmssPS(), gmssPrivKeySpec.getAlgNames());
    }

    /**
     * Generates a new GMSS private key
     * 
     * @param index
     *                tree indices
     * @param currentSeeds
     *                seed for the generation of private OTS keys for the
     *                current subtrees (TREE)
     * @param nextNextSeeds
     *                seed for the generation of private OTS keys for the
     *                subtrees after next (TREE++)
     * @param currentAuthPaths
     *                array of current authentication paths (AUTHPATH)
     * @param nextAuthPaths
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
    protected GMSSPrivateKey(int[] index, byte[][] currentSeeds,
	    byte[][] nextNextSeeds, byte[][][] currentAuthPaths,
	    byte[][][] nextAuthPaths, byte[][][] keep,
	    Treehash[][] currentTreehash, Treehash[][] nextTreehash,
	    Vector[] currentStack, Vector[] nextStack,
	    Vector[][] currentRetain, Vector[][] nextRetain,
	    GMSSLeaf[] nextNextLeaf, GMSSLeaf[] upperLeaf,
	    GMSSLeaf[] upperTreehashLeaf, int[] minTreehash, byte[][] nextRoot,
	    GMSSRootCalc[] nextNextRoot, byte[][] currentRootSig,
	    GMSSRootSig[] nextRootSig, GMSSParameterset gmssParameterset,
	    String[] algNames) {

	// construct message digest
	try {
	    this.messDigestTrees = Registry.getMessageDigest(algNames[0]);
	    this.mdLength = messDigestTrees.getDigestLength();

	} catch (NoSuchAlgorithmException nsae) {
	    String errorMsg = "message digest " + algNames[0]
		    + " not found in " + algNames[1] + " or secure random "
		    + algNames[4] + " not found in " + algNames[5];
	    throw new RuntimeException(errorMsg);
	}

	// Parameter
	this.gmssPS = gmssParameterset;
	this.otsIndex = gmssParameterset.getWinternitzParameter();
	this.K = gmssParameterset.getK();
	this.heightOfTrees = gmssParameterset.getHeightOfTrees();
	// initialize numLayer
	this.numLayer = gmssPS.getNumOfLayers();

	// initialize index if null
	if (index == null) {
	    this.index = new int[numLayer];
	    for (int i = 0; i < numLayer; i++) {
		this.index[i] = 0;
	    }
	} else {
	    this.index = index;
	}

	this.currentSeeds = currentSeeds;
	this.nextNextSeeds = nextNextSeeds;

	this.currentAuthPaths = currentAuthPaths;
	this.nextAuthPaths = nextAuthPaths;

	// initialize keep if null
	if (keep == null) {
	    this.keep = new byte[numLayer][][];
	    for (int i = 0; i < numLayer; i++) {
		this.keep[i] = new byte[(int) Math.floor(heightOfTrees[i] / 2)][mdLength];
	    }
	} else {
	    this.keep = keep;
	}

	// initialize stack if null
	if (currentStack == null) {
	    this.currentStack = new Vector[numLayer];
	    for (int i = 0; i < numLayer; i++) {
		this.currentStack[i] = new Vector();
	    }
	} else {
	    this.currentStack = currentStack;
	}

	// initialize nextStack if null
	if (nextStack == null) {
	    this.nextStack = new Vector[numLayer - 1];
	    for (int i = 0; i < numLayer - 1; i++) {
		this.nextStack[i] = new Vector();
	    }
	} else {
	    this.nextStack = nextStack;
	}

	this.currentTreehash = currentTreehash;
	this.nextTreehash = nextTreehash;

	this.currentRetain = currentRetain;
	this.nextRetain = nextRetain;

	this.nextRoot = nextRoot;

	this.algNames = algNames;

	if (nextNextRoot == null) {
	    this.nextNextRoot = new GMSSRootCalc[numLayer - 1];
	    for (int i = 0; i < numLayer - 1; i++) {
		this.nextNextRoot[i] = new GMSSRootCalc(
			this.heightOfTrees[i + 1], this.K[i + 1], this.algNames);
	    }
	} else {
	    this.nextNextRoot = nextNextRoot;
	}
	this.currentRootSig = currentRootSig;

	// calculate numLeafs
	numLeafs = new int[numLayer];
	for (int i = 0; i < numLayer; i++) {
	    numLeafs[i] = 1 << heightOfTrees[i];
	}
	// construct PRNG
	this.gmssRandom = new GMSSRandom(messDigestTrees);

	if (numLayer > 1) {
	    // construct the nextNextLeaf (LEAFs++) array for upcoming leafs in
	    // tree after next (TREE++)
	    if (nextNextLeaf == null) {
		this.nextNextLeaf = new GMSSLeaf[numLayer - 2];
		for (int i = 0; i < numLayer - 2; i++) {
		    this.nextNextLeaf[i] = new GMSSLeaf(algNames,
			    otsIndex[i + 1], numLeafs[i + 2]);
		    this.nextNextLeaf[i].initLeafCalc(this.nextNextSeeds[i]);
		}
	    } else {
		this.nextNextLeaf = nextNextLeaf;
	    }
	} else {
	    this.nextNextLeaf = new GMSSLeaf[0];
	}

	// construct the upperLeaf array for upcoming leafs in tree over the
	// actual
	if (upperLeaf == null) {
	    this.upperLeaf = new GMSSLeaf[numLayer - 1];
	    for (int i = 0; i < numLayer - 1; i++) {
		this.upperLeaf[i] = new GMSSLeaf(algNames, otsIndex[i],
			numLeafs[i + 1]);
		this.upperLeaf[i].initLeafCalc(this.currentSeeds[i]);
	    }
	} else {
	    this.upperLeaf = upperLeaf;
	}

	// construct the leafs for upcoming leafs in treehashs in tree over the
	// actual
	if (upperTreehashLeaf == null) {
	    this.upperTreehashLeaf = new GMSSLeaf[numLayer - 1];
	    for (int i = 0; i < numLayer - 1; i++) {
		this.upperTreehashLeaf[i] = new GMSSLeaf(algNames, otsIndex[i],
			numLeafs[i + 1]);
	    }
	} else {
	    this.upperTreehashLeaf = upperTreehashLeaf;
	}

	if (minTreehash == null) {
	    this.minTreehash = new int[numLayer - 1];
	    for (int i = 0; i < numLayer - 1; i++) {
		this.minTreehash[i] = -1;
	    }
	} else {
	    this.minTreehash = minTreehash;
	}

	// construct the nextRootSig (RootSig++)
	byte[] dummy = new byte[mdLength];
	byte[] OTSseed = new byte[mdLength];
	if (nextRootSig == null) {
	    this.nextRootSig = new GMSSRootSig[numLayer - 1];
	    for (int i = 0; i < numLayer - 1; i++) {
		System.arraycopy(currentSeeds[i], 0, dummy, 0, mdLength);
		gmssRandom.nextSeed(dummy);
		OTSseed = gmssRandom.nextSeed(dummy);
		this.nextRootSig[i] = new GMSSRootSig(algNames, otsIndex[i],
			heightOfTrees[i + 1]);
		this.nextRootSig[i].initSign(OTSseed, nextRoot[i]);
	    }
	} else {
	    this.nextRootSig = nextRootSig;
	}
    }

    /**
     * This method updates the GMSS private key for the next signature
     * 
     * @param layer
     *                the layer where the next key is processed
     * 
     */
    public void nextKey(int layer) {
	// only for lowest layer ( other layers indices are raised in nextTree()
	// method )
	if (layer == numLayer - 1) {
	    index[layer]++;
	} // else System.out.println(" --- nextKey on layer " + layer + "
	// index is now : " + index[layer]);

	// if tree of this layer is depleted
	if (index[layer] == numLeafs[layer]) {
	    if (numLayer != 1) {
		nextTree(layer);
		index[layer] = 0;
	    }
	} else {
	    updateKey(layer);
	}
    }

    /**
     * Switch to next subtree if the current one is depleted
     * 
     * @param layer
     *                the layer where the next tree is processed
     * 
     */
    private void nextTree(int layer) {
	// System.out.println("NextTree method called on layer " + layer);
	// dont create next tree for the top layer
	if (layer > 0) {
	    // raise index for upper layer
	    index[layer - 1]++;

	    // test if it is already the last tree
	    boolean lastTree = true;
	    int z = layer;
	    do {
		z--;
		if (index[z] < numLeafs[z]) {
		    lastTree = false;
		}
	    } while (lastTree && (z > 0));

	    // only construct next subtree if last one is not already in use
	    if (!lastTree) {
		gmssRandom.nextSeed(currentSeeds[layer]);

		// last step of distributed signature calculation
		try {
		    nextRootSig[layer - 1].updateSign();
		} catch (SignatureException se) {
		}

		// last step of distributed leaf calculation for nextNextLeaf
		if (layer > 1) {
		    nextNextLeaf[layer - 1 - 1].updateLeafCalc();
		}

		// last step of distributed leaf calculation for upper leaf
		upperLeaf[layer - 1].updateLeafCalc();

		// last step of distributed leaf calculation for all treehashs

		if (minTreehash[layer - 1] >= 0) {
		    this.upperTreehashLeaf[layer - 1].updateLeafCalc();
		    byte[] leaf = this.upperTreehashLeaf[layer - 1].getLeaf();
		    // if update is required use the precomputed leaf to update
		    // treehash
		    try {
			currentTreehash[layer - 1][minTreehash[layer - 1]]
				.update(this.gmssRandom, leaf);
			// System.out.println("UUUpdated TH " +
			// minTreehash[layer - 1]);
			if (currentTreehash[layer - 1][minTreehash[layer - 1]]
				.wasFinished()) {
			    // System.out.println("FFFinished TH " +
			    // minTreehash[layer - 1]);
			}
		    } catch (Exception e) {
			System.out.println(e);
		    }
		}

		// last step of nextNextAuthRoot calculation
		this.updateNextNextAuthRoot(layer);

		// ******************************************************** /

		// NOW: advance to next tree on layer 'layer'

		// NextRootSig --> currentRootSigs
		this.currentRootSig[layer - 1] = nextRootSig[layer - 1]
			.getSig();

		// -----------------------

		// nextTreehash --> currentTreehash
		// nextNextTreehash --> nextTreehash
		for (int i = 0; i < heightOfTrees[layer] - K[layer]; i++) {
		    this.currentTreehash[layer][i] = this.nextTreehash[layer - 1][i];
		    this.nextTreehash[layer - 1][i] = this.nextNextRoot[layer - 1]
			    .getTreehash()[i];
		}

		// NextAuthPath --> currentAuthPath
		// nextNextAuthPath --> nextAuthPath
		for (int i = 0; i < heightOfTrees[layer]; i++) {
		    System.arraycopy(nextAuthPaths[layer - 1][i], 0,
			    currentAuthPaths[layer][i], 0, mdLength);
		    System.arraycopy(nextNextRoot[layer - 1].getAuthPath()[i],
			    0, nextAuthPaths[layer - 1][i], 0, mdLength);
		}

		// nextRetain --> currentRetain
		// nextNextRetain --> nextRetain
		for (int i = 0; i < K[layer] - 1; i++) {
		    this.currentRetain[layer][i] = this.nextRetain[layer - 1][i];
		    this.nextRetain[layer - 1][i] = this.nextNextRoot[layer - 1]
			    .getRetain()[i];
		}

		// nextStack --> currentStack
		this.currentStack[layer] = this.nextStack[layer - 1];
		// nextNextStack --> nextStack
		this.nextStack[layer - 1] = this.nextNextRoot[layer - 1]
			.getStack();

		// nextNextRoot --> nextRoot
		this.nextRoot[layer - 1] = this.nextNextRoot[layer - 1]
			.getRoot();
		// -----------------------

		// -----------------
		byte[] OTSseed = new byte[mdLength];
		byte[] dummy = new byte[mdLength];
		// gmssRandom.setSeed(currentSeeds[layer]);
		System
			.arraycopy(currentSeeds[layer - 1], 0, dummy, 0,
				mdLength);
		OTSseed = gmssRandom.nextSeed(dummy); // only need OTSSeed
		OTSseed = gmssRandom.nextSeed(dummy);
		OTSseed = gmssRandom.nextSeed(dummy);
		// nextWinSig[layer-1]=new
		// GMSSWinSig(OTSseed,algNames,otsIndex[layer-1],heightOfTrees[layer],nextRoot[layer-1]);
		nextRootSig[layer - 1].initSign(OTSseed, nextRoot[layer - 1]);

		// nextKey for upper layer
		nextKey(layer - 1);
	    }
	}
    }

    /**
     * This method computes the authpath (AUTH) for the current tree,
     * Additionally the root signature for the next tree (SIG+), the authpath
     * (AUTH++) and root (ROOT++) for the tree after next in layer
     * <code>layer</code>, and the LEAF++^1 for the next next tree in the
     * layer above are updated This method is used by nextKey()
     * 
     * @param layer
     */
    private void updateKey(int layer) {
	// ----------current tree processing of actual layer---------
	// compute upcoming authpath for current Tree (AUTH)
	computeAuthPaths(layer);

	// -----------distributed calculations part------------
	// not for highest tree layer
	if (layer > 0) {

	    // compute (partial) next leaf on TREE++ (not on layer 1 and 0)
	    if (layer > 1) {
		nextNextLeaf[layer - 1 - 1].updateLeafCalc();
	    }

	    // compute (partial) next leaf on tree above (not on layer 0)
	    upperLeaf[layer - 1].updateLeafCalc();

	    // compute (partial) next leaf for all treehashs on tree above (not
	    // on layer 0)

	    int t = (int) Math
		    .floor((double) (this.getNumLeafs(layer) * 2)
			    / (double) (this.heightOfTrees[layer - 1] - this.K[layer - 1]));

	    if (index[layer] % t == 1) {
		// System.out.println(" layer: " + layer + " index: " +
		// index[layer] + " t : " + t);

		// take precomputed node for treehash update
		// ------------------------------------------------
		if (index[layer] > 1 && minTreehash[layer - 1] >= 0) {
		    byte[] leaf = this.upperTreehashLeaf[layer - 1].getLeaf();
		    // if update is required use the precomputed leaf to update
		    // treehash
		    try {
			currentTreehash[layer - 1][minTreehash[layer - 1]]
				.update(this.gmssRandom, leaf);
			// System.out.println("Updated TH " + minTreehash[layer
			// - 1]);
			if (currentTreehash[layer - 1][minTreehash[layer - 1]]
				.wasFinished()) {
			    // System.out.println("Finished TH " +
			    // minTreehash[layer - 1]);
			}
		    } catch (Exception e) {
			System.out.println(e);
		    }
		    // ------------------------------------------------
		}

		// initialize next leaf precomputation
		// ------------------------------------------------

		// get lowest index of treehashs
		this.minTreehash[layer - 1] = getMinTreehashIndex(layer - 1);

		if (this.minTreehash[layer - 1] >= 0) {
		    // initialize leaf
		    byte[] seed = this.currentTreehash[layer - 1][this.minTreehash[layer - 1]]
			    .getSeedActive();
		    this.upperTreehashLeaf[layer - 1] = new GMSSLeaf(
			    this.algNames, this.otsIndex[layer - 1], t);
		    this.upperTreehashLeaf[layer - 1].initLeafCalc(seed);
		    this.upperTreehashLeaf[layer - 1].updateLeafCalc();
		    // System.out.println("restarted treehashleaf (" + (layer -
		    // 1) + "," + this.minTreehash[layer - 1] + ")");
		}
		// ------------------------------------------------

	    } else {
		// update the upper leaf for the treehash one step
		if (this.minTreehash[layer - 1] >= 0) {
		    this.upperTreehashLeaf[layer - 1].updateLeafCalc();
		    // if (minTreehash[layer - 1] > 3)
		    // System.out.print("#");
		}
	    }

	    // compute (partial) the signature of ROOT+ (RootSig+) (not on top
	    // layer)
	    try {
		nextRootSig[layer - 1].updateSign();
	    } catch (SignatureException se) {
	    }

	    // compute (partial) AUTHPATH++ & ROOT++ (not on top layer)
	    if (index[layer] == 1) {
		// init root and authpath calculation for tree after next
		// (AUTH++, ROOT++)
		this.nextNextRoot[layer - 1].initialize(new Vector());
	    }

	    // update root and authpath calculation for tree after next (AUTH++,
	    // ROOT++)
	    this.updateNextNextAuthRoot(layer);
	}
	// ----------- end distributed calculations part-----------------
    }

    /**
     * This method returns the index of the next Treehash instance that should
     * receive an update
     * 
     * @param layer
     *                the layer of the GMSS tree
     * @return index of the treehash instance that should get the update
     */
    private int getMinTreehashIndex(int layer) {
	int minTreehash = -1;
	for (int h = 0; h < heightOfTrees[layer] - K[layer]; h++) {
	    if (currentTreehash[layer][h].wasInitialized()
		    && !currentTreehash[layer][h].wasFinished()) {
		if (minTreehash == -1) {
		    minTreehash = h;
		} else if (currentTreehash[layer][h].getLowestNodeHeight() < currentTreehash[layer][minTreehash]
			.getLowestNodeHeight()) {
		    minTreehash = h;
		}
	    }
	}
	return minTreehash;
    }

    /**
     * Computes the upcoming currentAuthpath of layer <code>layer</code> using
     * the revisited authentication path computation of Dahmen/Schneider 2008
     * 
     * @param layer
     *                the actual layer
     */
    private void computeAuthPaths(int layer) {

	int Phi = index[layer];
	int H = heightOfTrees[layer];
	int K = this.K[layer];

	// update all nextSeeds for seed scheduling
	for (int i = 0; i < H - K; i++) {
	    currentTreehash[layer][i].updateNextSeed(gmssRandom);
	}

	// STEP 1 of Algorithm
	int Tau = heightOfPhi(Phi);

	byte[] OTSseed = new byte[mdLength];
	OTSseed = gmssRandom.nextSeed(currentSeeds[layer]);

	// STEP 2 of Algorithm
	// if phi's parent on height tau + 1 if left node, store auth_tau
	// in keep_tau.
	// TODO check it, formerly was
	// int L = Phi / (int) Math.floor(Math.pow(2, Tau + 1));
	// L %= 2;
	int L = (Phi >>> (Tau + 1)) & 1;

	byte[] tempKeep = new byte[mdLength];
	// store the keep node not in keep[layer][tau/2] because it might be in
	// use
	// wait until the space is freed in step 4a
	if (Tau < H - 1 && L == 0) {
	    System.arraycopy(currentAuthPaths[layer][Tau], 0, tempKeep, 0,
		    mdLength);
	}

	byte[] help = new byte[mdLength];
	// STEP 3 of Algorithm
	// if phi is left child, compute and store leaf for next currentAuthPath
	// path,
	// (obtained by veriying current signature)
	if (Tau == 0) {
	    // LEAFCALC !!!
	    if (layer == numLayer - 1) { // lowest layer computes the
		// necessary leaf completely at this
		// time
		WinternitzOTSignature ots = new WinternitzOTSignature(OTSseed,
			algNames, otsIndex[layer]);
		help = ots.getPublicKey();
	    } else { // other layers use the precomputed leafs in
		// nextNextLeaf
		byte[] dummy = new byte[mdLength];
		System.arraycopy(currentSeeds[layer], 0, dummy, 0, mdLength);
		gmssRandom.nextSeed(dummy);
		help = upperLeaf[layer].getLeaf();
		this.upperLeaf[layer].initLeafCalc(dummy);

		// WinternitzOTSVerify otsver = new
		// WinternitzOTSVerify(algNames, otsIndex[layer]);
		// byte[] help2 = otsver.Verify(currentRoot[layer],
		// currentRootSig[layer]);
		// System.out.println(" --- " + layer + " " +
		// ByteUtils.toHexString(help) + " " +
		// ByteUtils.toHexString(help2));
	    }
	    System.arraycopy(help, 0, currentAuthPaths[layer][0], 0, mdLength);
	} else {
	    // STEP 4a of Algorithm
	    // get new left currentAuthPath node on height tau
	    byte[] toBeHashed = new byte[mdLength << 1];
	    System.arraycopy(currentAuthPaths[layer][Tau - 1], 0, toBeHashed,
		    0, mdLength);
	    // free the shared keep[layer][tau/2]
	    System.arraycopy(keep[layer][(int) Math.floor((Tau - 1) / 2)], 0,
		    toBeHashed, mdLength, mdLength);
	    messDigestTrees.update(toBeHashed);
	    currentAuthPaths[layer][Tau] = messDigestTrees.digest();

	    // STEP 4b and 4c of Algorithm
	    // copy right nodes to currentAuthPath on height 0..Tau-1
	    for (int i = 0; i < Tau; i++) {

		// STEP 4b of Algorithm
		// 1st: copy from treehashs
		if (i < H - K) {
		    if (currentTreehash[layer][i].wasFinished()) {
			System.arraycopy(currentTreehash[layer][i]
				.getFirstNode(), 0, currentAuthPaths[layer][i],
				0, mdLength);
			currentTreehash[layer][i].destroy();
		    } else {
			System.err
				.println("Treehash ("
					+ layer
					+ ","
					+ i
					+ ") not finished when needed in AuthPathComputation");
		    }
		}

		// 2nd: copy precomputed values from Retain
		if (i < H - 1 && i >= H - K) {
		    if (currentRetain[layer][i - (H - K)].size() > 0) {
			// pop element from retain
			System.arraycopy(currentRetain[layer][i - (H - K)]
				.lastElement(), 0, currentAuthPaths[layer][i],
				0, mdLength);
			currentRetain[layer][i - (H - K)]
				.removeElementAt(currentRetain[layer][i
					- (H - K)].size() - 1);
		    }
		}

		// STEP 4c of Algorithm
		// initialize new stack at heights 0..Tau-1
		if (i < H - K) {
		    // create stacks anew
		    int startPoint = Phi + 3 * (1 << i);
		    if (startPoint < numLeafs[layer]) {
			// if (layer < 2) {
			// System.out.println("initialized TH " + i + " on layer
			// " + layer);
			// }
			currentTreehash[layer][i].initialize();
		    }
		}
	    }
	}

	// now keep space is free to use
	if (Tau < H - 1 && L == 0) {
	    System.arraycopy(tempKeep, 0,
		    keep[layer][(int) Math.floor(Tau / 2)], 0, mdLength);
	}

	// only update empty stack at height h if all other stacks have
	// tailnodes with height >h
	// finds active stack with lowest node height, choses lower index in
	// case of tie

	// on the lowest layer leafs must be computed at once, no precomputation
	// is possible. So all treehash updates are done at once here
	if (layer == numLayer - 1) {
	    for (int tmp = 1; tmp <= (H - K) / 2; tmp++) {
		// index of the treehash instance that receives the next update
		int minTreehash = getMinTreehashIndex(layer);

		// if active treehash is found update with a leaf
		if (minTreehash >= 0) {
		    try {
			byte[] seed = new byte[mdLength];
			System.arraycopy(
				this.currentTreehash[layer][minTreehash]
					.getSeedActive(), 0, seed, 0, mdLength);
			byte[] seed2 = gmssRandom.nextSeed(seed);
			WinternitzOTSignature ots = new WinternitzOTSignature(
				seed2, this.algNames, this.otsIndex[layer]);
			byte[] leaf = ots.getPublicKey();
			currentTreehash[layer][minTreehash].update(
				this.gmssRandom, leaf);
		    } catch (Exception e) {
			System.out.println(e);
		    }
		}
	    }
	} else { // on higher layers the updates are done later
	    this.minTreehash[layer] = getMinTreehashIndex(layer);
	}
    }

    /**
     * Returns the largest h such that 2^h | Phi
     * 
     * @param Phi
     *                the leaf index
     * @return The largest <code>h</code> with <code>2^h | Phi</code> if
     *         <code>Phi!=0</code> else return <code>-1</code>
     */
    private int heightOfPhi(int Phi) {
	if (Phi == 0) {
	    return -1;
	}
	int Tau = 0;
	int modul = 1;
	while (Phi % modul == 0) {
	    modul *= 2;
	    Tau += 1;
	}
	return Tau - 1;
    }

    /**
     * Updates the authentication path and root calculation for the tree after
     * next (AUTH++, ROOT++) in layer <code>layer</code>
     * 
     * @param layer
     */
    private void updateNextNextAuthRoot(int layer) {

	byte[] OTSseed = new byte[mdLength];
	OTSseed = gmssRandom.nextSeed(nextNextSeeds[layer - 1]);

	// get the necessary leaf
	if (layer == numLayer - 1) { // lowest layer computes the necessary
	    // leaf completely at this time
	    WinternitzOTSignature ots = new WinternitzOTSignature(OTSseed,
		    algNames, otsIndex[layer]);
	    this.nextNextRoot[layer - 1].update(nextNextSeeds[layer - 1], ots
		    .getPublicKey());
	} else { // other layers use the precomputed leafs in nextNextLeaf
	    this.nextNextRoot[layer - 1].update(nextNextSeeds[layer - 1],
		    nextNextLeaf[layer - 1].getLeaf());
	    this.nextNextLeaf[layer - 1].initLeafCalc(nextNextSeeds[layer - 1]);
	}
    }

    /**
     * @return The name of the algorithm
     */
    public String getAlgorithm() {
	return "GMSS";
    }

    /**
     * @return The detailed name of the algorithm
     */
    public String[] getName() {

	return algNames;
    }

    /**
     * @return the OID to encode in the SubjectPublicKeyInfo structure
     */
    protected ASN1ObjectIdentifier getOID() {
	return new ASN1ObjectIdentifier(GMSSKeyFactory.OID);
    }

    /**
     * @return the algorithm parameters to encode in the SubjectPublicKeyInfo
     *         structure
     */
    protected ASN1Type getAlgParams() {
	return new ASN1Null();
    }

    /**
     * @return the keyData to encode in the SubjectPublicKeyInfo structure
     */
    protected byte[] getKeyData() {
	GMSSPrivateKeyASN1 mtsPrivateKey = new GMSSPrivateKeyASN1(index,
		currentSeeds, nextNextSeeds, currentAuthPaths, nextAuthPaths,
		keep, currentTreehash, nextTreehash, currentStack, nextStack,
		currentRetain, nextRetain, nextNextLeaf, upperLeaf,
		upperTreehashLeaf, minTreehash, nextRoot, nextNextRoot,
		currentRootSig, nextRootSig, gmssPS, algNames);
	return ASN1Tools.derEncode(mtsPrivateKey);
    }

    /***************************************************************************
     * / Returns an initialized one-time signature object
     * 
     * @return An initialized <code>WinternitzOTSignature</code> object
     */
    /*
     * public WinternitzOTSignature getOTSInstance() { //forwards currentSeed
     * //gmssRandom.setSeed(currentSeeds[currentSeeds.length-1]);//secureRandom.setSeed(currentSeeds[currentSeeds.length-1]);
     * byte[] OTSseed = new byte[mdLength]; byte[] dummy = new byte[mdLength];
     * System.arraycopy(currentSeeds[numLayer-1], 0, dummy, 0, mdLength);
     * OTSseed =
     * gmssRandom.nextSeed(dummy);//secureRandom.nextBytes(currentSeeds[currentSeeds.length-1]);secureRandom.nextBytes(OTSseed);
     * 
     * WinternitzOTSignature ots = new WinternitzOTSignature( OTSseed, algNames,
     * gmssPS.getWinternitzParameter()[currentSeeds.length-1]); //update 'first'
     * every second time if (index[numLayer-1] % 2 == 0) { } return ots; }
     */

    /**
     * @return The current indices array
     */
    protected int[] getIndex() {
	return index;
    }

    /**
     * @return The current index of layer i
     */
    protected int getIndex(int i) {
	return index[i];
    }

    /**
     * @return The array of current seeds
     */
    protected byte[][] getCurrentSeeds() {
	return currentSeeds;
    }

    /**
     * @return The array of seeds after next (SEED++)
     */
    protected byte[][] getNextNextSeeds() {
	return nextNextSeeds;
    }

    /**
     * @return The current authentication path array
     */
    protected byte[][][] getCurrentAuthPaths() {
	return currentAuthPaths;
    }

    /**
     * @return The next authentication path array
     */
    protected byte[][][] getNextAuthPaths() {
	return nextAuthPaths;
    }

    /**
     * @return The current treehash instances
     */
    protected Treehash[][] getCurrentTreehash() {
	return currentTreehash;
    }

    /**
     * @return The next treehash instances
     */
    protected Treehash[][] getNextTreehash() {
	return nextTreehash;
    }

    /**
     * @return The current treehash instances
     */
    protected Vector[] getCurrentStack() {
	return currentStack;
    }

    /**
     * @return The next treehash instances
     */
    protected Vector[] getNextStack() {
	return nextStack;
    }

    /**
     * @return The current treehash instances
     */
    protected Vector[][] getCurrentRetain() {
	return currentRetain;
    }

    /**
     * @return The next treehash instances
     */
    protected Vector[][] getNextRetain() {
	return nextRetain;
    }

    /**
     * @return The number of leafs of each tree of layer i
     */
    protected int getNumLeafs(int i) {
	return numLeafs[i];
    }

    /**
     * @return The array of number of leafs of a tree of each layer
     */
    protected int[] getNumLeafs() {
	return numLeafs;
    }

    /**
     * @return The stack array <code>keep</code>
     */
    protected byte[][][] getKeep() {
	return keep;
    }

    /**
     * @return An array of the GMSSLeafs of the tree after next of each layer
     *         (LEAF++)
     */
    protected GMSSLeaf[] getNextNextLeaf() {
	return nextNextLeaf;
    }

    /**
     * @return An array of the GMSSLeafs of the tree after next of each layer
     *         (LEAF++)
     */
    protected GMSSLeaf[] getUpperLeaf() {
	return upperLeaf;
    }

    /**
     * @return An array of the GMSSLeafs of the tree after next of each layer
     *         (LEAF++)
     */
    protected GMSSLeaf[] getUpperTreehashLeaf() {
	return upperTreehashLeaf;
    }

    /**
     * @return An array of the indices of the next treehashs to receive updates
     */
    protected int[] getMinTreehash() {
	return minTreehash;
    }

    /**
     * @return An array of roots of the next subtree of each layer (ROOT+)
     */
    protected byte[][] getNextRoot() {
	return nextRoot;
    }

    /**
     * @return An array of roots of the subtree after next of each layer
     *         (ROOT++)
     */
    protected GMSSRootCalc[] getNextNextRoot() {
	return nextNextRoot;
    }

    /**
     * @return An array of signatures of the current subtree roots of each layer
     */
    protected byte[][] getCurrentRootSig() {
	return currentRootSig;
    }

    /**
     * @return the GMSSParameterset
     */
    protected GMSSParameterset getParameterset() {
	return gmssPS;
    }

    /**
     * @return The one-time signature of the root of the current subtree
     */
    protected byte[] getSubtreeRootSig(int i) {
	return currentRootSig[i];
    }

    /**
     * @return The one-time signatures of the next root (SIG+)
     */

    protected GMSSRootSig[] getNextRootSig() {
	return nextRootSig;
    }

    /**
     * @return A human readable representation of main part of the key
     */
    public String toString() {
	GMSSUtilities gmssUtil = new GMSSUtilities();

	String out = "";
	out = out + "tree indices           : \n";
	for (int i = 0; i < index.length; i++) {
	    out = out + "  " + i + ". index           : " + index[i] + "\n";
	}
	out = out + "current tree seeds            : \n";
	for (int i = 0; i < currentSeeds.length; i++) {
	    out = out + "  " + i + ". currentSeed           : "
		    + ByteUtils.toHexString(currentSeeds[i]) + "\n";
	}
	out = out + "next next tree seeds            : \n";
	for (int i = 0; i < nextNextSeeds.length; i++) {
	    out = out + "  " + i + ". nextNextSeed           : "
		    + ByteUtils.toHexString(nextNextSeeds[i]) + "\n";
	}
	out = out + "current tree authPaths            : \n";
	for (int i = 0; i < currentAuthPaths.length; i++) {
	    out = out
		    + "  "
		    + i
		    + ". currentAuthPath           : "
		    + ByteUtils.toHexString(gmssUtil
			    .concatenateArray(currentAuthPaths[i])) + "\n";
	}
	out = out + "next tree authPaths            : \n";
	for (int i = 0; i < nextAuthPaths.length; i++) {
	    out = out
		    + "  "
		    + i
		    + ". nextAuthPath           : "
		    + ByteUtils.toHexString(gmssUtil
			    .concatenateArray(nextAuthPaths[i])) + "\n";
	}
	out = out + "currentTreehash      :\n";
	for (int i = 0; i < currentTreehash.length; i++) {
	    for (int j = 0; j < currentTreehash[i].length; j++) {
		out = out + "  (" + i + "," + j + ")."
			+ currentTreehash[i][j].toString() + "\n";
	    }
	}
	out = out + "nextTreehash      :\n";
	for (int i = 0; i < nextTreehash.length; i++) {
	    for (int j = 0; j < nextTreehash[i].length; j++) {
		out = out + "  (" + i + "," + j + ")."
			+ nextTreehash[i][j].toString() + "\n";
	    }
	}
	out = out + "current tree stack            : \n";
	for (int i = 0; i < currentStack.length; i++) {
	    out = out + "  " + i;
	    for (int j = 0; j < currentStack[i].size(); j++) {
		out = out
			+ " "
			+ j
			+ ". currentStack         : "
			+ ByteUtils.toHexString((byte[]) currentStack[i]
				.elementAt(j));
	    }
	    out = out + "\n";
	}
	out = out + "next tree stack            : \n";
	for (int i = 0; i < nextStack.length; i++) {
	    out = out + "  " + i;
	    for (int j = 0; j < nextStack[i].size(); j++) {
		out = out
			+ " "
			+ j
			+ ". nextStack         : "
			+ ByteUtils.toHexString((byte[]) nextStack[i]
				.elementAt(j));
	    }
	    out = out + "\n";
	}
	String help;
	out = out + "current tree retain            : \n";
	for (int i = 0; i < currentRetain.length; i++) {
	    for (int j = 0; j < currentRetain[i].length; j++) {
		help = "";
		for (int k = 0; k < currentRetain[i][j].size(); k++) {
		    help = help
			    + ByteUtils
				    .toHexString((byte[]) currentRetain[i][j]
					    .elementAt(k));
		}
		out = out + "  (" + i + "," + j + "). currentRetain         : "
			+ help + "\n";
	    }
	}
	out = out + "next tree retain            : \n";
	for (int i = 0; i < nextRetain.length; i++) {
	    for (int j = 0; j < nextRetain[i].length; j++) {
		help = "";
		for (int k = 0; k < nextRetain[i][j].size(); k++) {
		    help = help
			    + ByteUtils.toHexString((byte[]) nextRetain[i][j]
				    .elementAt(k));
		}
		out = out + "  (" + i + "," + j + "). nextRetain         : "
			+ help + "\n";
	    }
	}
	out = out + "keep             : \n";
	for (int i = 0; i < keep.length; i++) {
	    out = out + "  " + i + ". keep           : "
		    + ByteUtils.toHexString(gmssUtil.concatenateArray(keep[i]))
		    + "\n";
	}
	out = out + "subtree Roots             : \n";
	for (int i = 0; i < currentRootSig.length; i++) {
	    out = out + "  " + i + ". currentRootSig           : "
		    + ByteUtils.toHexString(currentRootSig[i]) + "\n";
	}
	out = out + "next next Roots (distributed)         : \n";
	for (int i = 0; i < nextNextRoot.length; i++) {
	    out = out + "  " + nextNextRoot[i].toString() + "\n";
	}
	out = out + "Name of        " + "\n" + "  message digest (trees)  : "
		+ algNames[0] + " [" + algNames[1] + "]" + "\n";

	out = out + "nextNextLeaf      :\n";
	for (int i = 0; i < nextNextLeaf.length; i++) {
	    out = out + "  " + i + ".    " + nextNextLeaf[i].toString() + "\n";
	}
	out = out + "upperLeaf      :\n";
	for (int i = 0; i < upperLeaf.length; i++) {
	    out = out + "  " + i + ".    " + upperLeaf[i].toString() + "\n";
	}
	out = out + "upperTreehashLeaf      :\n";
	for (int i = 0; i < upperTreehashLeaf.length; i++) {
	    out = out + "  " + i + ".    " + upperTreehashLeaf[i].toString()
		    + "\n";
	}
	out = out + "minTreehash           : \n";
	for (int i = 0; i < minTreehash.length; i++) {
	    out = out + "  " + i + ". minTreehash           : "
		    + minTreehash[i] + "\n";
	}
	out = out + "nextRootSig           : \n";
	for (int i = 0; i < nextRootSig.length; i++) {
	    out = out + "  " + i + ".  " + nextRootSig[i].toString() + "\n";
	}

	return out;
    }
}

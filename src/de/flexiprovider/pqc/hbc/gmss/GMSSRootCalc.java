package de.flexiprovider.pqc.hbc.gmss;

import java.util.Vector;

import de.flexiprovider.api.MessageDigest;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.common.util.ByteUtils;
import de.flexiprovider.core.CoreRegistry;

/**
 * This class computes a whole Merkle tree and saves the needed values for
 * AuthPath computation. It is used for precomputation of the root of a
 * following tree. After initialization, 2^H updates are required to complete
 * the root. Every update requires one leaf value as parameter. While computing
 * the root all initial values for the authentication path algorithm (treehash,
 * auth, retain) are stored for later use.
 * 
 * @author Michael Schneider
 */
class GMSSRootCalc {

    /**
     * max height of the tree
     */
    private int heightOfTree;

    /**
     * length of the messageDigest
     */
    private int mdLength;

    /**
     * the treehash instances of the tree
     */
    private Treehash[] treehash;

    /**
     * stores the retain nodes for authPath computation
     */
    private Vector[] retain;

    /**
     * finally stores the root of the tree when finished
     */
    private byte[] root;

    /**
     * stores the authentication path y_1(i), i = 0..H-1
     */
    private byte[][] AuthPath;

    /**
     * the value K for the authentication path computation
     */
    private int K;

    /**
     * Vector element that stores the nodes on the stack
     */
    private Vector tailStack;

    /**
     * stores the height of all nodes laying on the tailStack
     */
    private Vector heightOfNodes;
    /**
     * The hash function used for the construction of the authentication trees
     */
    private MessageDigest messDigestTree;

    /**
     * An array of strings containing the name of the hash function used to
     * construct the authentication trees and used by the OTS.
     */
    private String[] algNames = new String[2];

    /**
     * stores the index of the current node on each height of the tree
     */
    private int[] index;

    /**
     * true if instance was already initialized, false otherwise
     */
    private boolean isInitialized;

    /**
     * true it instance was finished
     */
    private boolean isFinished;

    /**
     * Integer that stores the index of the next seed that has to be omitted to
     * the treehashs
     */
    private int indexForNextSeed;

    /**
     * temporary integer that stores the height of the next treehash instance
     * that gets initialized with a seed
     */
    private int heightOfNextSeed;

    /**
     * This constructor regenerates a prior treehash object
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
    public GMSSRootCalc(String[] name, byte[][] statByte, int[] statInt,
	    Treehash[] treeH, Vector[] ret) {
	// decode name
	CoreRegistry.registerAlgorithms();
	try {
	    messDigestTree = Registry.getMessageDigest(name[0]);
	} catch (NoSuchAlgorithmException nsae) {
	    System.err.println(": message digest " + name[0] + " not found in "
		    + name[1]);
	}

	this.algNames = name;
	// decode statInt
	this.heightOfTree = statInt[0];
	this.mdLength = statInt[1];
	this.K = statInt[2];
	this.indexForNextSeed = statInt[3];
	this.heightOfNextSeed = statInt[4];
	if (statInt[5] == 1)
	    this.isFinished = true;
	else
	    this.isFinished = false;
	if (statInt[6] == 1)
	    this.isInitialized = true;
	else
	    this.isInitialized = false;

	int tailLength = statInt[7];

	this.index = new int[heightOfTree];
	for (int i = 0; i < heightOfTree; i++) {
	    this.index[i] = statInt[8 + i];
	}

	this.heightOfNodes = new Vector();
	for (int i = 0; i < tailLength; i++) {
	    this.heightOfNodes.addElement(new Integer(statInt[8 + heightOfTree
		    + i]));
	}

	// decode statByte
	this.root = statByte[0];

	this.AuthPath = new byte[heightOfTree][mdLength];
	for (int i = 0; i < heightOfTree; i++) {
	    this.AuthPath[i] = statByte[1 + i];
	}

	this.tailStack = new Vector();
	for (int i = 0; i < tailLength; i++) {
	    this.tailStack.addElement(statByte[1 + heightOfTree + i]);
	}

	// decode treeH
	this.treehash = treeH;

	// decode ret
	this.retain = ret;
    }

    /**
     * Constructor
     * 
     * @param heightOfTree
     *                maximal height of the tree
     * @param algNames
     *                an array of strings, containing the name of the used hash
     *                function and PRNG and the name of the corresponding
     *                provider
     */
    public GMSSRootCalc(int heightOfTree, int K, String[] algNames) {
	this.heightOfTree = heightOfTree;
	CoreRegistry.registerAlgorithms();
	try {
	    messDigestTree = Registry.getMessageDigest(algNames[0]);
	} catch (NoSuchAlgorithmException nsae) {
	    System.err.println(": message digest " + algNames[0]
		    + " not found in " + algNames[1]);
	}
	this.algNames = algNames;
	this.mdLength = messDigestTree.getDigestLength();
	this.K = K;
	this.index = new int[heightOfTree];
	this.AuthPath = new byte[heightOfTree][mdLength];
	this.root = new byte[mdLength];
	// this.treehash = new Treehash[this.heightOfTree - this.K];
	this.retain = new Vector[this.K - 1];
	for (int i = 0; i < K - 1; i++)
	    this.retain[i] = new Vector();

    }

    /**
     * Initializes the calculation of a new root
     * 
     * @param sharedStack
     *                the stack shared by all treehash instances of this tree
     */
    public void initialize(Vector sharedStack) {
	this.treehash = new Treehash[this.heightOfTree - this.K];
	for (int i = 0; i < this.heightOfTree - this.K; i++)
	    this.treehash[i] = new Treehash(sharedStack, i, this.algNames);

	this.index = new int[heightOfTree];
	this.AuthPath = new byte[heightOfTree][mdLength];
	this.root = new byte[mdLength];

	this.tailStack = new Vector();
	this.heightOfNodes = new Vector();
	this.isInitialized = true;
	this.isFinished = false;

	for (int i = 0; i < heightOfTree; i++)
	    this.index[i] = -1;

	this.retain = new Vector[this.K - 1];
	for (int i = 0; i < K - 1; i++)
	    this.retain[i] = new Vector();

	this.indexForNextSeed = 3;
	this.heightOfNextSeed = 0;
    }

    /**
     * updates the root with one leaf and stores needed values in retain,
     * treehash or authpath. Additionally counts the seeds used. This method is
     * used when performing the updates for TREE++.
     * 
     * @param seed
     *                the initial seed for treehash: seedNext
     * @param leaf
     *                the height of the treehash
     */
    public void update(byte[] seed, byte[] leaf) {
	if (this.heightOfNextSeed < (this.heightOfTree - this.K)
		&& this.indexForNextSeed - 2 == index[0]) {
	    this.initializeTreehashSeed(seed, this.heightOfNextSeed);
	    this.heightOfNextSeed++;
	    this.indexForNextSeed *= 2;
	}
	// now call the simple update
	this.update(leaf);
    }

    /**
     * Updates the root with one leaf and stores the needed values in retain,
     * treehash or authpath
     */
    public void update(byte[] leaf) {

	if (isFinished) {
	    System.out.print("Too much updates for Tree!!");
	    return;
	}
	if (!isInitialized) {
	    System.err.println("GMSSRootCalc not initialized!");
	    return;
	}

	// a new leaf was omitted, so raise index on lowest layer
	index[0]++;

	// store the nodes on the lowest layer in treehash or authpath
	if (index[0] == 1) {
	    System.arraycopy(leaf, 0, AuthPath[0], 0, mdLength);
	} else if (index[0] == 3) {
	    // store in treehash only if K < H
	    if (heightOfTree > K)
		treehash[0].setFirstNode(leaf);
	}

	if ((index[0] - 3) % 2 == 0 && index[0] >= 3) {
	    // store in retain if K = H
	    if (heightOfTree == K)
		// TODO: check it
		retain[0].insertElementAt(leaf, 0);
	}

	// if first update to this tree is made
	if (index[0] == 0) {
	    tailStack.addElement(leaf);
	    heightOfNodes.addElement(new Integer(0));
	} else {

	    byte[] help = new byte[mdLength];
	    byte[] toBeHashed = new byte[mdLength << 1];

	    // store the new leaf in help
	    System.arraycopy(leaf, 0, help, 0, mdLength);
	    int helpHeight = 0;
	    // while top to nodes have same height
	    while (tailStack.size() > 0
		    && helpHeight == ((Integer) heightOfNodes.lastElement())
			    .intValue()) {

		// help <-- hash(stack top element || help)
		System.arraycopy(tailStack.lastElement(), 0, toBeHashed, 0,
			mdLength);
		tailStack.removeElementAt(tailStack.size() - 1);
		heightOfNodes.removeElementAt(heightOfNodes.size() - 1);
		System.arraycopy(help, 0, toBeHashed, mdLength, mdLength);

		messDigestTree.update(toBeHashed);
		help = messDigestTree.digest();

		// the new help node is one step higher
		helpHeight++;
		if (helpHeight < heightOfTree) {
		    index[helpHeight]++;

		    // add index 1 element to initial authpath
		    if (index[helpHeight] == 1) {
			System.arraycopy(help, 0, AuthPath[helpHeight], 0,
				mdLength);
		    }

		    if (helpHeight >= heightOfTree - K) {
			if (helpHeight == 0)
			    System.out.println("MÖÖÖP");
			// add help element to retain stack if it is a right
			// node
			// and not stored in treehash
			if ((index[helpHeight] - 3) % 2 == 0
				&& index[helpHeight] >= 3)
			    // TODO: check it
			    retain[helpHeight - (heightOfTree - K)]
				    .insertElementAt(help, 0);
		    } else {
			// if element is third in his line add it to treehash
			if (index[helpHeight] == 3) {
			    treehash[helpHeight].setFirstNode(help);
			}
		    }
		}
	    }
	    // push help element to the stack
	    tailStack.addElement(help);
	    heightOfNodes.addElement(new Integer(helpHeight));

	    // is the root calculation finished?
	    if (helpHeight == heightOfTree) {
		isFinished = true;
		isInitialized = false;
		root = (byte[]) tailStack.lastElement();
	    }
	}

    }

    /**
     * initializes the seeds for the treehashs of the tree precomputed by this
     * class
     * 
     * @param seed
     *                the initial seed for treehash: seedNext
     * @param index
     *                the height of the treehash
     */
    public void initializeTreehashSeed(byte[] seed, int index) {
	treehash[index].initializeSeed(seed);
    }

    /**
     * Method to check whether the instance has been initialized or not
     * 
     * @return true if treehash was already initialized
     */
    public boolean wasInitialized() {
	return isInitialized;
    }

    /**
     * Method to check whether the instance has been finished or not
     * 
     * @return true if tree has reached its maximum height
     */
    public boolean wasFinished() {
	return isFinished;
    }

    /**
     * returns the authentication path of the first leaf of the tree
     * 
     * @return the authentication path of the first leaf of the tree
     */
    public byte[][] getAuthPath() {
	return AuthPath;
    }

    /**
     * returns the initial treehash instances, storing value y_3(i)
     * 
     * @return the initial treehash instances, storing value y_3(i)
     */
    public Treehash[] getTreehash() {
	return treehash;
    }

    /**
     * returns the retain stacks storing all right nodes near to the root
     * 
     * @return the retain stacks storing all right nodes near to the root
     */
    public Vector[] getRetain() {
	return retain;
    }

    /**
     * returns the finished root value
     * 
     * @return the finished root value
     */
    public byte[] getRoot() {
	return root;
    }

    /**
     * returns the shared stack
     * 
     * @return the shared stack
     */
    public Vector getStack() {
	return tailStack;
    }

    /**
     * Returns the status byte array used by the GMSSPrivateKeyASN.1 class
     * 
     * @return The status bytes
     */
    public byte[][] getStatByte() {

	int tailLength;
	if (tailStack == null)
	    tailLength = 0;
	else
	    tailLength = tailStack.size();
	byte[][] statByte = new byte[1 + heightOfTree + tailLength][messDigestTree
		.getDigestLength()];
	statByte[0] = root;

	for (int i = 0; i < heightOfTree; i++) {
	    statByte[1 + i] = AuthPath[i];
	}
	for (int i = 0; i < tailLength; i++) {
	    statByte[1 + heightOfTree + i] = (byte[]) tailStack.elementAt(i);
	}

	return statByte;
    }

    /**
     * Returns the status int array used by the GMSSPrivateKeyASN.1 class
     * 
     * @return The status ints
     */
    public int[] getStatInt() {

	int tailLength;
	if (tailStack == null) {
		tailLength = 0;
	} else {
		tailLength = tailStack.size();
	}
	int[] statInt = new int[8 + heightOfTree + tailLength];
	statInt[0] = heightOfTree;
	statInt[1] = mdLength;
	statInt[2] = K;
	statInt[3] = indexForNextSeed;
	statInt[4] = heightOfNextSeed;
	if (isFinished)
	    statInt[5] = 1;
	else
	    statInt[5] = 0;
	if (isInitialized)
	    statInt[6] = 1;
	else
	    statInt[6] = 0;
	statInt[7] = tailLength;

	for (int i = 0; i < heightOfTree; i++) {
	    statInt[8 + i] = index[i];
	}
	for (int i = 0; i < tailLength; i++) {
	    statInt[8 + heightOfTree + i] = ((Integer) heightOfNodes
		    .elementAt(i)).intValue();
	}

	return statInt;
    }

    /**
     * @return a human readable version of the structure
     */
    public String toString() {
	String out = "";
	int tailLength;
	if (tailStack == null)
	    tailLength = 0;
	else
	    tailLength = tailStack.size();

	for (int i = 0; i < 8 + heightOfTree + tailLength; i++) {
	    out = out + getStatInt()[i] + " ";
	}
	for (int i = 0; i < 1 + heightOfTree + tailLength; i++) {
	    out = out + ByteUtils.toHexString(getStatByte()[i]) + " ";
	}
	out = out + "  " + algNames[0] + "( " + algNames[1] + " )  " + mdLength;
	return out;
    }
}

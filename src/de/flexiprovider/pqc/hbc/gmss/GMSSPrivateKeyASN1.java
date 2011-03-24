package de.flexiprovider.pqc.hbc.gmss;

import java.util.Vector;

import codec.asn1.ASN1IA5String;
import codec.asn1.ASN1Integer;
import codec.asn1.ASN1OctetString;
import codec.asn1.ASN1Sequence;
import codec.asn1.ASN1SequenceOf;
import codec.asn1.ASN1Type;
import de.flexiprovider.common.util.ASN1Tools;

/**
 * This class represents an ASN.1 encoded <code>GMSSPrivateKey</code>.
 * <p>
 * The ASN.1 definition of this structure is
 * <p>
 * 
 * <pre>
 *     GMSSPrivateKey		::= SEQUENCE {
 * <p>
 * 			algorithm		OBJECT IDENTIFIER
 * 			index			SEQUENCE OF INTEGER
 * 			curSeeds		SEQUENCE OF OCTET STRING
 * 			nextNextSeeds	SEQUENCE OF OCTET STRING
 * 			curAuth			SEQUENCE OF AuthPath
 * 			nextAuth		SEQUENCE OF AuthPath
 * 		curTreehash		SEQUENCE OF TreehashStack
 * 		nextTreehash	SEQUENCE OF TreehashStack
 * 			StackKeep		SEQUENCE OF Stack
 * 
 * 		curStack		SEQUENCE OF Stack
 * 		nextStack		SEQUENCE OF Stack
 * 		curRetain		SEQUENCE OF Retain
 * 		nextRetain		SEQUENCE OF Retain
 * 
 * 			nextNextLeaf	SEQUENCE OF DistrLeaf
 * 			upperLeaf		SEQUENCE OF DistrLeaf
 * 			upperTHLeaf		SEQUENCE OF DistrLeaf
 * 			minTreehash		SEQUENCE OF INTEGER
 * 
 * 			nextRoot		SEQUENCE OF OCTET STRING
 * 			nextNextRoot	SEQUENCE OF DistrRoot
 * 		curRootSig		SEQUENCE OF OCTET STRING
 * 			nextRootSig		SEQUENCE OF DistrRootSig
 * 
 * 			Parameterset	ParSet
 * 			names			SEQUENCE OF ASN1IA5String
 * 		}
 * 
 * 		DistrLeaf		::= SEQUENCE {
 * 			name			SEQUENCE OF ASN1IA5String
 * 		statBytes		SEQUENCE OF OCTET STRING
 * 			statInts		SEQUENCE OF INTEGER
 * 		}
 * 		DistrRootSig	::= SEQUENCE {
 * 			name			SEQUENCE OF ASN1IA5String
 * 		statBytes		SEQUENCE OF OCTET STRING
 * 			statInts		SEQUENCE OF INTEGER
 * 		}
 * 		DistrRoot		::= SEQUENCE {
 * 			name			SEQUENCE OF ASN1IA5String
 * 		statBytes		SEQUENCE OF OCTET STRING
 * 			statInts		SEQUENCE OF INTEGER
 * 			treeH			SEQUENCE OF Treehash
 * 		ret				SEQUENCE OF Retain
 * 		}
 * 		TreehashStack	::= SEQUENCE OF Treehash
 * 		Treehash		::= SEQUENCE {
 * 			name			SEQUENCE OF ASN1IA5String
 * 		statBytes		SEQUENCE OF OCTET STRING
 * 			statInts		SEQUENCE OF INTEGER
 * 		}
 * 
 * 		ParSet			::= SEQUENCE {
 * 			T				INTEGER
 * 			h				SEQUENCE OF INTEGER
 * 			w				SEQUENCE OF INTEGER		
 * 			K				SEQUENCE OF INTEGER
 * 		}
 * 		Retain			::= SEQUENCE OF Stack
 * 		AuthPath		::= SEQUENCE OF OCTET STRING
 * 		Stack			::= SEQUENCE OF OCTET STRING
 * </pre>
 * 
 * @author Michael Schneider, Sebastian Blume
 */
public class GMSSPrivateKeyASN1 extends ASN1Sequence {

    private GMSSPrivateKeySpec keySpec;

    /**
     * The Constructor
     * 
     * @param encoded
     *                The key in binary representation
     */
    public GMSSPrivateKeyASN1(ASN1Type encoded) {

	ASN1Sequence mtsPrivateKey = (ASN1Sequence) encoded;

	// --- Decode <index>.
	ASN1Sequence indexPart = (ASN1Sequence) mtsPrivateKey.get(0);
	int[] index = new int[indexPart.size()];
	for (int i = 0; i < indexPart.size(); i++) {
	    index[i] = ASN1Tools.getFlexiBigInt((ASN1Integer) indexPart.get(i))
		    .intValue();
	}

	// --- Decode <curSeeds>.
	ASN1Sequence curSeedsPart = (ASN1Sequence) mtsPrivateKey.get(1);
	byte[][] curSeeds = new byte[curSeedsPart.size()][];
	for (int i = 0; i < curSeeds.length; i++) {
	    curSeeds[i] = ((ASN1OctetString) curSeedsPart.get(i))
		    .getByteArray();
	}

	// --- Decode <nextNextSeeds>.
	ASN1Sequence nextNextSeedsPart = (ASN1Sequence) mtsPrivateKey.get(2);
	byte[][] nextNextSeeds = new byte[nextNextSeedsPart.size()][];
	for (int i = 0; i < nextNextSeeds.length; i++) {
	    nextNextSeeds[i] = ((ASN1OctetString) nextNextSeedsPart.get(i))
		    .getByteArray();
	}

	// --- Decode <curAuth>.
	ASN1Sequence curAuthPart0 = (ASN1Sequence) mtsPrivateKey.get(3);
	ASN1Sequence curAuthPart1;

	byte[][][] curAuth = new byte[curAuthPart0.size()][][];
	for (int i = 0; i < curAuth.length; i++) {
	    curAuthPart1 = (ASN1Sequence) curAuthPart0.get(i);
	    curAuth[i] = new byte[curAuthPart1.size()][];
	    for (int j = 0; j < curAuth[i].length; j++) {
		curAuth[i][j] = ((ASN1OctetString) curAuthPart1.get(j))
			.getByteArray();
	    }
	}

	// --- Decode <nextAuth>.
	ASN1Sequence nextAuthPart0 = (ASN1Sequence) mtsPrivateKey.get(4);
	ASN1Sequence nextAuthPart1;

	byte[][][] nextAuth = new byte[nextAuthPart0.size()][][];
	for (int i = 0; i < nextAuth.length; i++) {
	    nextAuthPart1 = (ASN1Sequence) nextAuthPart0.get(i);
	    nextAuth[i] = new byte[nextAuthPart1.size()][];
	    for (int j = 0; j < nextAuth[i].length; j++) {
		nextAuth[i][j] = ((ASN1OctetString) nextAuthPart1.get(j))
			.getByteArray();
	    }
	}

	// --- Decode <curTreehash>.
	ASN1Sequence seqOfcurTreehash0 = (ASN1Sequence) mtsPrivateKey.get(5);
	ASN1Sequence seqOfcurTreehash1;
	ASN1Sequence seqOfcurTreehashStat;
	ASN1Sequence seqOfcurTreehashBytes;
	ASN1Sequence seqOfcurTreehashInts;
	ASN1Sequence seqOfcurTreehashString;

	Treehash[][] curTreehash = new Treehash[seqOfcurTreehash0.size()][];

	for (int i = 0; i < curTreehash.length; i++) {
	    seqOfcurTreehash1 = (ASN1Sequence) seqOfcurTreehash0.get(i);
	    curTreehash[i] = new Treehash[seqOfcurTreehash1.size()];
	    for (int j = 0; j < curTreehash[i].length; j++) {
		seqOfcurTreehashStat = (ASN1Sequence) seqOfcurTreehash1.get(j);
		seqOfcurTreehashString = (ASN1Sequence) seqOfcurTreehashStat
			.get(0);
		seqOfcurTreehashBytes = (ASN1Sequence) seqOfcurTreehashStat
			.get(1);
		seqOfcurTreehashInts = (ASN1Sequence) seqOfcurTreehashStat
			.get(2);

		String[] name = new String[2];
		name[0] = ((ASN1IA5String) seqOfcurTreehashString.get(0))
			.getString();
		name[1] = ((ASN1IA5String) seqOfcurTreehashString.get(1))
			.getString();

		int tailLength = ASN1Tools.getFlexiBigInt(
			(ASN1Integer) seqOfcurTreehashInts.get(1)).intValue();
		byte[][] statByte = new byte[3 + tailLength][];
		statByte[0] = ((ASN1OctetString) seqOfcurTreehashBytes.get(0))
			.getByteArray();
		if (statByte[0].length == 0) { // if null was encoded
		    statByte[0] = null;
		}

		statByte[1] = ((ASN1OctetString) seqOfcurTreehashBytes.get(1))
			.getByteArray();
		statByte[2] = ((ASN1OctetString) seqOfcurTreehashBytes.get(2))
			.getByteArray();
		for (int k = 0; k < tailLength; k++) {
		    statByte[3 + k] = ((ASN1OctetString) seqOfcurTreehashBytes
			    .get(3 + k)).getByteArray();
		}
		int[] statInt = new int[6 + tailLength];
		statInt[0] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfcurTreehashInts.get(0))).intValue();
		statInt[1] = tailLength;
		statInt[2] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfcurTreehashInts.get(2))).intValue();
		statInt[3] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfcurTreehashInts.get(3))).intValue();
		statInt[4] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfcurTreehashInts.get(4))).intValue();
		statInt[5] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfcurTreehashInts.get(5))).intValue();
		for (int k = 0; k < tailLength; k++) {
		    statInt[6 + k] = ASN1Tools.getFlexiBigInt(
			    ((ASN1Integer) seqOfcurTreehashInts.get(6 + k)))
			    .intValue();
		}
		curTreehash[i][j] = new Treehash(name, statByte, statInt);
	    }
	}

	// --- Decode <nextTreehash>.
	ASN1Sequence seqOfNextTreehash0 = (ASN1Sequence) mtsPrivateKey.get(6);
	ASN1Sequence seqOfNextTreehash1;
	ASN1Sequence seqOfNextTreehashStat;
	ASN1Sequence seqOfNextTreehashBytes;
	ASN1Sequence seqOfNextTreehashInts;
	ASN1Sequence seqOfNextTreehashString;

	Treehash[][] nextTreehash = new Treehash[seqOfNextTreehash0.size()][];

	for (int i = 0; i < nextTreehash.length; i++) {
	    seqOfNextTreehash1 = (ASN1Sequence) seqOfNextTreehash0.get(i);
	    nextTreehash[i] = new Treehash[seqOfNextTreehash1.size()];
	    for (int j = 0; j < nextTreehash[i].length; j++) {
		seqOfNextTreehashStat = (ASN1Sequence) seqOfNextTreehash1
			.get(j);
		seqOfNextTreehashString = (ASN1Sequence) seqOfNextTreehashStat
			.get(0);
		seqOfNextTreehashBytes = (ASN1Sequence) seqOfNextTreehashStat
			.get(1);
		seqOfNextTreehashInts = (ASN1Sequence) seqOfNextTreehashStat
			.get(2);

		String[] name = new String[2];
		name[0] = ((ASN1IA5String) seqOfNextTreehashString.get(0))
			.getString();
		name[1] = ((ASN1IA5String) seqOfNextTreehashString.get(1))
			.getString();

		int tailLength = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfNextTreehashInts.get(1)))
			.intValue();
		byte[][] statByte = new byte[3 + tailLength][];
		statByte[0] = ((ASN1OctetString) seqOfNextTreehashBytes.get(0))
			.getByteArray();
		if (statByte[0].length == 0) { // if null was encoded
		    statByte[0] = null;
		}

		statByte[1] = ((ASN1OctetString) seqOfNextTreehashBytes.get(1))
			.getByteArray();
		statByte[2] = ((ASN1OctetString) seqOfNextTreehashBytes.get(2))
			.getByteArray();
		for (int k = 0; k < tailLength; k++) {
		    statByte[3 + k] = ((ASN1OctetString) seqOfNextTreehashBytes
			    .get(3 + k)).getByteArray();
		}
		int[] statInt = new int[6 + tailLength];
		statInt[0] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfNextTreehashInts.get(0)))
			.intValue();
		statInt[1] = tailLength;
		statInt[2] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfNextTreehashInts.get(2)))
			.intValue();
		statInt[3] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfNextTreehashInts.get(3)))
			.intValue();
		statInt[4] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfNextTreehashInts.get(4)))
			.intValue();
		statInt[5] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfNextTreehashInts.get(5)))
			.intValue();
		for (int k = 0; k < tailLength; k++) {
		    statInt[6 + k] = ASN1Tools.getFlexiBigInt(
			    ((ASN1Integer) seqOfNextTreehashInts.get(6 + k)))
			    .intValue();
		}
		nextTreehash[i][j] = new Treehash(name, statByte, statInt);
	    }
	}

	// --- Decode <keep>.
	ASN1Sequence keepPart0 = (ASN1Sequence) mtsPrivateKey.get(7);
	ASN1Sequence keepPart1;

	byte[][][] keep = new byte[keepPart0.size()][][];
	for (int i = 0; i < keep.length; i++) {
	    keepPart1 = (ASN1Sequence) keepPart0.get(i);
	    keep[i] = new byte[keepPart1.size()][];
	    for (int j = 0; j < keep[i].length; j++) {
		keep[i][j] = ((ASN1OctetString) keepPart1.get(j))
			.getByteArray();
	    }
	}

	// --- Decode <curStack>.
	ASN1Sequence curStackPart0 = (ASN1Sequence) mtsPrivateKey.get(8);
	ASN1Sequence curStackPart1;

	Vector[] curStack = new Vector[curStackPart0.size()];
	for (int i = 0; i < curStack.length; i++) {
	    curStackPart1 = (ASN1Sequence) curStackPart0.get(i);
	    curStack[i] = new Vector();
	    for (int j = 0; j < curStackPart1.size(); j++) {
		curStack[i].addElement(((ASN1OctetString) curStackPart1.get(j))
			.getByteArray());
	    }
	}

	// --- Decode <nextStack>.
	ASN1Sequence nextStackPart0 = (ASN1Sequence) mtsPrivateKey.get(9);
	ASN1Sequence nextStackPart1;

	Vector[] nextStack = new Vector[nextStackPart0.size()];
	for (int i = 0; i < nextStack.length; i++) {
	    nextStackPart1 = (ASN1Sequence) nextStackPart0.get(i);
	    nextStack[i] = new Vector();
	    for (int j = 0; j < nextStackPart1.size(); j++) {
		nextStack[i].addElement(((ASN1OctetString) nextStackPart1
			.get(j)).getByteArray());
	    }
	}

	// --- Decode <curRetain>.
	ASN1Sequence curRetainPart0 = (ASN1Sequence) mtsPrivateKey.get(10);
	ASN1Sequence curRetainPart1;
	ASN1Sequence curRetainPart2;

	Vector[][] curRetain = new Vector[curRetainPart0.size()][];
	for (int i = 0; i < curRetain.length; i++) {
	    curRetainPart1 = (ASN1Sequence) curRetainPart0.get(i);
	    curRetain[i] = new Vector[curRetainPart1.size()];
	    for (int j = 0; j < curRetain[i].length; j++) {
		curRetainPart2 = (ASN1Sequence) curRetainPart1.get(j);
		curRetain[i][j] = new Vector();
		for (int k = 0; k < curRetainPart2.size(); k++) {
		    curRetain[i][j]
			    .addElement(((ASN1OctetString) curRetainPart2
				    .get(k)).getByteArray());
		}
	    }
	}

	// --- Decode <nextRetain>.
	ASN1Sequence nextRetainPart0 = (ASN1Sequence) mtsPrivateKey.get(11);
	ASN1Sequence nextRetainPart1;
	ASN1Sequence nextRetainPart2;

	Vector[][] nextRetain = new Vector[nextRetainPart0.size()][];
	for (int i = 0; i < nextRetain.length; i++) {
	    nextRetainPart1 = (ASN1Sequence) nextRetainPart0.get(i);
	    nextRetain[i] = new Vector[nextRetainPart1.size()];
	    for (int j = 0; j < nextRetain[i].length; j++) {
		nextRetainPart2 = (ASN1Sequence) nextRetainPart1.get(j);
		nextRetain[i][j] = new Vector();
		for (int k = 0; k < nextRetainPart2.size(); k++) {
		    nextRetain[i][j]
			    .addElement(((ASN1OctetString) nextRetainPart2
				    .get(k)).getByteArray());
		}
	    }
	}

	// --- Decode <nextNextLeaf>.
	ASN1Sequence seqOfLeafs = (ASN1Sequence) mtsPrivateKey.get(12);
	ASN1Sequence seqOfLeafStat;
	ASN1Sequence seqOfLeafBytes;
	ASN1Sequence seqOfLeafInts;
	ASN1Sequence seqOfLeafString;

	GMSSLeaf[] nextNextLeaf = new GMSSLeaf[seqOfLeafs.size()];

	for (int i = 0; i < nextNextLeaf.length; i++) {
	    seqOfLeafStat = (ASN1Sequence) seqOfLeafs.get(i);
	    // nextNextAuth[i]= new byte[nextNextAuthPart1.size()][];
	    seqOfLeafString = (ASN1Sequence) seqOfLeafStat.get(0);
	    seqOfLeafBytes = (ASN1Sequence) seqOfLeafStat.get(1);
	    seqOfLeafInts = (ASN1Sequence) seqOfLeafStat.get(2);

	    String[] name = new String[2];
	    name[0] = ((ASN1IA5String) seqOfLeafString.get(0)).getString();
	    name[1] = ((ASN1IA5String) seqOfLeafString.get(1)).getString();
	    byte[][] statByte = new byte[4][];
	    statByte[0] = ((ASN1OctetString) seqOfLeafBytes.get(0))
		    .getByteArray();
	    statByte[1] = ((ASN1OctetString) seqOfLeafBytes.get(1))
		    .getByteArray();
	    statByte[2] = ((ASN1OctetString) seqOfLeafBytes.get(2))
		    .getByteArray();
	    statByte[3] = ((ASN1OctetString) seqOfLeafBytes.get(3))
		    .getByteArray();
	    int[] statInt = new int[4];
	    statInt[0] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfLeafInts.get(0))).intValue();
	    statInt[1] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfLeafInts.get(1))).intValue();
	    statInt[2] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfLeafInts.get(2))).intValue();
	    statInt[3] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfLeafInts.get(3))).intValue();
	    nextNextLeaf[i] = new GMSSLeaf(name, statByte, statInt);
	}

	// --- Decode <upperLeaf>.
	ASN1Sequence seqOfUpperLeafs = (ASN1Sequence) mtsPrivateKey.get(13);
	ASN1Sequence seqOfUpperLeafStat;
	ASN1Sequence seqOfUpperLeafBytes;
	ASN1Sequence seqOfUpperLeafInts;
	ASN1Sequence seqOfUpperLeafString;

	GMSSLeaf[] upperLeaf = new GMSSLeaf[seqOfUpperLeafs.size()];

	for (int i = 0; i < upperLeaf.length; i++) {
	    seqOfUpperLeafStat = (ASN1Sequence) seqOfUpperLeafs.get(i);
	    seqOfUpperLeafString = (ASN1Sequence) seqOfUpperLeafStat.get(0);
	    seqOfUpperLeafBytes = (ASN1Sequence) seqOfUpperLeafStat.get(1);
	    seqOfUpperLeafInts = (ASN1Sequence) seqOfUpperLeafStat.get(2);

	    String[] name = new String[2];
	    name[0] = ((ASN1IA5String) seqOfUpperLeafString.get(0)).getString();
	    name[1] = ((ASN1IA5String) seqOfUpperLeafString.get(1)).getString();
	    byte[][] statByte = new byte[4][];
	    statByte[0] = ((ASN1OctetString) seqOfUpperLeafBytes.get(0))
		    .getByteArray();
	    statByte[1] = ((ASN1OctetString) seqOfUpperLeafBytes.get(1))
		    .getByteArray();
	    statByte[2] = ((ASN1OctetString) seqOfUpperLeafBytes.get(2))
		    .getByteArray();
	    statByte[3] = ((ASN1OctetString) seqOfUpperLeafBytes.get(3))
		    .getByteArray();
	    int[] statInt = new int[4];
	    statInt[0] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfUpperLeafInts.get(0))).intValue();
	    statInt[1] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfUpperLeafInts.get(1))).intValue();
	    statInt[2] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfUpperLeafInts.get(2))).intValue();
	    statInt[3] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfUpperLeafInts.get(3))).intValue();
	    upperLeaf[i] = new GMSSLeaf(name, statByte, statInt);
	}

	// --- Decode <upperTreehashLeaf>.
	ASN1Sequence seqOfUpperTHLeafs = (ASN1Sequence) mtsPrivateKey.get(14);
	ASN1Sequence seqOfUpperTHLeafStat;
	ASN1Sequence seqOfUpperTHLeafBytes;
	ASN1Sequence seqOfUpperTHLeafInts;
	ASN1Sequence seqOfUpperTHLeafString;

	GMSSLeaf[] upperTHLeaf = new GMSSLeaf[seqOfUpperTHLeafs.size()];

	for (int i = 0; i < upperTHLeaf.length; i++) {
	    seqOfUpperTHLeafStat = (ASN1Sequence) seqOfUpperTHLeafs.get(i);
	    seqOfUpperTHLeafString = (ASN1Sequence) seqOfUpperTHLeafStat.get(0);
	    seqOfUpperTHLeafBytes = (ASN1Sequence) seqOfUpperTHLeafStat.get(1);
	    seqOfUpperTHLeafInts = (ASN1Sequence) seqOfUpperTHLeafStat.get(2);

	    String[] name = new String[2];
	    name[0] = ((ASN1IA5String) seqOfUpperTHLeafString.get(0))
		    .getString();
	    name[1] = ((ASN1IA5String) seqOfUpperTHLeafString.get(1))
		    .getString();
	    byte[][] statByte = new byte[4][];
	    statByte[0] = ((ASN1OctetString) seqOfUpperTHLeafBytes.get(0))
		    .getByteArray();
	    statByte[1] = ((ASN1OctetString) seqOfUpperTHLeafBytes.get(1))
		    .getByteArray();
	    statByte[2] = ((ASN1OctetString) seqOfUpperTHLeafBytes.get(2))
		    .getByteArray();
	    statByte[3] = ((ASN1OctetString) seqOfUpperTHLeafBytes.get(3))
		    .getByteArray();
	    int[] statInt = new int[4];
	    statInt[0] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfUpperTHLeafInts.get(0))).intValue();
	    statInt[1] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfUpperTHLeafInts.get(1))).intValue();
	    statInt[2] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfUpperTHLeafInts.get(2))).intValue();
	    statInt[3] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfUpperTHLeafInts.get(3))).intValue();
	    upperTHLeaf[i] = new GMSSLeaf(name, statByte, statInt);
	}

	// --- Decode <minTreehash>.
	ASN1Sequence minTreehashPart = (ASN1Sequence) mtsPrivateKey.get(15);
	int[] minTreehash = new int[minTreehashPart.size()];
	for (int i = 0; i < minTreehashPart.size(); i++) {
	    minTreehash[i] = ASN1Tools.getFlexiBigInt(
		    (ASN1Integer) minTreehashPart.get(i)).intValue();
	}

	// --- Decode <nextRoot>.
	ASN1Sequence seqOfnextRoots = (ASN1Sequence) mtsPrivateKey.get(16);
	byte[][] nextRoot = new byte[seqOfnextRoots.size()][];
	for (int i = 0; i < nextRoot.length; i++) {
	    nextRoot[i] = ((ASN1OctetString) seqOfnextRoots.get(i))
		    .getByteArray();
	}

	// --- Decode <nextNextRoot>.
	ASN1Sequence seqOfnextNextRoot = (ASN1Sequence) mtsPrivateKey.get(17);
	ASN1Sequence seqOfnextNextRootStat;
	ASN1Sequence seqOfnextNextRootBytes;
	ASN1Sequence seqOfnextNextRootInts;
	ASN1Sequence seqOfnextNextRootString;
	ASN1Sequence seqOfnextNextRootTreeH;
	ASN1Sequence seqOfnextNextRootRetain;

	GMSSRootCalc[] nextNextRoot = new GMSSRootCalc[seqOfnextNextRoot.size()];

	for (int i = 0; i < nextNextRoot.length; i++) {
	    seqOfnextNextRootStat = (ASN1Sequence) seqOfnextNextRoot.get(i);
	    seqOfnextNextRootString = (ASN1Sequence) seqOfnextNextRootStat
		    .get(0);
	    seqOfnextNextRootBytes = (ASN1Sequence) seqOfnextNextRootStat
		    .get(1);
	    seqOfnextNextRootInts = (ASN1Sequence) seqOfnextNextRootStat.get(2);
	    seqOfnextNextRootTreeH = (ASN1Sequence) seqOfnextNextRootStat
		    .get(3);
	    seqOfnextNextRootRetain = (ASN1Sequence) seqOfnextNextRootStat
		    .get(4);

	    // decode treehash of nextNextRoot
	    // ---------------------------------
	    ASN1Sequence seqOfnextNextRootTreeHStat;
	    ASN1Sequence seqOfnextNextRootTreeHBytes;
	    ASN1Sequence seqOfnextNextRootTreeHInts;
	    ASN1Sequence seqOfnextNextRootTreeHString;

	    Treehash[] nnRTreehash = new Treehash[seqOfnextNextRootTreeH.size()];

	    for (int k = 0; k < nnRTreehash.length; k++) {
		seqOfnextNextRootTreeHStat = (ASN1Sequence) seqOfnextNextRootTreeH
			.get(k);
		seqOfnextNextRootTreeHString = (ASN1Sequence) seqOfnextNextRootTreeHStat
			.get(0);
		seqOfnextNextRootTreeHBytes = (ASN1Sequence) seqOfnextNextRootTreeHStat
			.get(1);
		seqOfnextNextRootTreeHInts = (ASN1Sequence) seqOfnextNextRootTreeHStat
			.get(2);

		String[] name = new String[2];
		name[0] = ((ASN1IA5String) seqOfnextNextRootTreeHString.get(0))
			.getString();
		name[1] = ((ASN1IA5String) seqOfnextNextRootTreeHString.get(1))
			.getString();

		int tailLength = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfnextNextRootTreeHInts.get(1)))
			.intValue();
		byte[][] statByte = new byte[3 + tailLength][];
		statByte[0] = ((ASN1OctetString) seqOfnextNextRootTreeHBytes
			.get(0)).getByteArray();
		if (statByte[0].length == 0) { // if null was encoded
		    statByte[0] = null;
		}

		statByte[1] = ((ASN1OctetString) seqOfnextNextRootTreeHBytes
			.get(1)).getByteArray();
		statByte[2] = ((ASN1OctetString) seqOfnextNextRootTreeHBytes
			.get(2)).getByteArray();
		for (int j = 0; j < tailLength; j++) {
		    statByte[3 + j] = ((ASN1OctetString) seqOfnextNextRootTreeHBytes
			    .get(3 + j)).getByteArray();
		}
		int[] statInt = new int[6 + tailLength];
		statInt[0] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfnextNextRootTreeHInts.get(0)))
			.intValue();
		statInt[1] = tailLength;
		statInt[2] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfnextNextRootTreeHInts.get(2)))
			.intValue();
		statInt[3] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfnextNextRootTreeHInts.get(3)))
			.intValue();
		statInt[4] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfnextNextRootTreeHInts.get(4)))
			.intValue();
		statInt[5] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfnextNextRootTreeHInts.get(5)))
			.intValue();
		for (int j = 0; j < tailLength; j++) {
		    statInt[6 + j] = ASN1Tools.getFlexiBigInt(
			    ((ASN1Integer) seqOfnextNextRootTreeHInts
				    .get(6 + j))).intValue();
		}
		nnRTreehash[k] = new Treehash(name, statByte, statInt);
	    }
	    // ---------------------------------

	    // decode retain of nextNextRoot
	    // ---------------------------------
	    // ASN1Sequence seqOfnextNextRootRetainPart0 =
	    // (ASN1Sequence)seqOfnextNextRootRetain.get(0);
	    ASN1Sequence seqOfnextNextRootRetainPart1;

	    Vector[] nnRRetain = new Vector[seqOfnextNextRootRetain.size()];
	    for (int j = 0; j < nnRRetain.length; j++) {
		seqOfnextNextRootRetainPart1 = (ASN1Sequence) seqOfnextNextRootRetain
			.get(j);
		nnRRetain[j] = new Vector();
		for (int k = 0; k < seqOfnextNextRootRetainPart1.size(); k++) {
		    nnRRetain[j]
			    .addElement(((ASN1OctetString) seqOfnextNextRootRetainPart1
				    .get(k)).getByteArray());
		}
	    }
	    // ---------------------------------

	    String[] name = new String[2];
	    name[0] = ((ASN1IA5String) seqOfnextNextRootString.get(0))
		    .getString();
	    name[1] = ((ASN1IA5String) seqOfnextNextRootString.get(1))
		    .getString();

	    int heightOfTree = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnextNextRootInts.get(0))).intValue();
	    int tailLength = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnextNextRootInts.get(7))).intValue();
	    byte[][] statByte = new byte[1 + heightOfTree + tailLength][];
	    statByte[0] = ((ASN1OctetString) seqOfnextNextRootBytes.get(0))
		    .getByteArray();
	    for (int j = 0; j < heightOfTree; j++) {
		statByte[1 + j] = ((ASN1OctetString) seqOfnextNextRootBytes
			.get(1 + j)).getByteArray();
	    }
	    for (int j = 0; j < tailLength; j++) {
		statByte[1 + heightOfTree + j] = ((ASN1OctetString) seqOfnextNextRootBytes
			.get(1 + heightOfTree + j)).getByteArray();
	    }
	    int[] statInt = new int[8 + heightOfTree + tailLength];
	    statInt[0] = heightOfTree;
	    statInt[1] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnextNextRootInts.get(1))).intValue();
	    statInt[2] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnextNextRootInts.get(2))).intValue();
	    statInt[3] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnextNextRootInts.get(3))).intValue();
	    statInt[4] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnextNextRootInts.get(4))).intValue();
	    statInt[5] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnextNextRootInts.get(5))).intValue();
	    statInt[6] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnextNextRootInts.get(6))).intValue();
	    statInt[7] = tailLength;
	    for (int j = 0; j < heightOfTree; j++) {
		statInt[8 + j] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfnextNextRootInts.get(8 + j)))
			.intValue();
	    }
	    for (int j = 0; j < tailLength; j++) {
		statInt[8 + heightOfTree + j] = ASN1Tools.getFlexiBigInt(
			((ASN1Integer) seqOfnextNextRootInts.get(8
				+ heightOfTree + j))).intValue();
	    }
	    nextNextRoot[i] = new GMSSRootCalc(name, statByte, statInt,
		    nnRTreehash, nnRRetain);
	}

	// --- Decode <curRootSig>.
	ASN1Sequence seqOfcurRootSig = (ASN1Sequence) mtsPrivateKey.get(18);
	byte[][] curRootSig = new byte[seqOfcurRootSig.size()][];
	for (int i = 0; i < curRootSig.length; i++) {
	    curRootSig[i] = ((ASN1OctetString) seqOfcurRootSig.get(i))
		    .getByteArray();
	}

	// --- Decode <nextRootSig>.
	ASN1Sequence seqOfnextRootSigs = (ASN1Sequence) mtsPrivateKey.get(19);
	ASN1Sequence seqOfnRSStats;
	ASN1Sequence seqOfnRSStrings;
	ASN1Sequence seqOfnRSInts;
	ASN1Sequence seqOfnRSBytes;

	GMSSRootSig[] nextRootSig = new GMSSRootSig[seqOfnextRootSigs.size()];

	for (int i = 0; i < nextRootSig.length; i++) {
	    seqOfnRSStats = (ASN1Sequence) seqOfnextRootSigs.get(i);
	    // nextNextAuth[i]= new byte[nextNextAuthPart1.size()][];
	    seqOfnRSStrings = (ASN1Sequence) seqOfnRSStats.get(0);
	    seqOfnRSBytes = (ASN1Sequence) seqOfnRSStats.get(1);
	    seqOfnRSInts = (ASN1Sequence) seqOfnRSStats.get(2);

	    String[] name = new String[2];
	    name[0] = ((ASN1IA5String) seqOfnRSStrings.get(0)).getString();
	    name[1] = ((ASN1IA5String) seqOfnRSStrings.get(1)).getString();
	    byte[][] statByte = new byte[5][];
	    statByte[0] = ((ASN1OctetString) seqOfnRSBytes.get(0))
		    .getByteArray();
	    statByte[1] = ((ASN1OctetString) seqOfnRSBytes.get(1))
		    .getByteArray();
	    statByte[2] = ((ASN1OctetString) seqOfnRSBytes.get(2))
		    .getByteArray();
	    statByte[3] = ((ASN1OctetString) seqOfnRSBytes.get(3))
		    .getByteArray();
	    statByte[4] = ((ASN1OctetString) seqOfnRSBytes.get(4))
		    .getByteArray();
	    int[] statInt = new int[9];
	    statInt[0] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(0))).intValue();
	    statInt[1] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(1))).intValue();
	    statInt[2] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(2))).intValue();
	    statInt[3] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(3))).intValue();
	    statInt[4] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(4))).intValue();
	    statInt[5] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(5))).intValue();
	    statInt[6] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(6))).intValue();
	    statInt[7] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(7))).intValue();
	    statInt[8] = ASN1Tools.getFlexiBigInt(
		    ((ASN1Integer) seqOfnRSInts.get(8))).intValue();
	    nextRootSig[i] = new GMSSRootSig(name, statByte, statInt);
	}

	// --- Decode <parameterset>.

	ASN1Sequence seqOfParams = (ASN1Sequence) mtsPrivateKey.get(20);
	int numLayer = ASN1Tools.getFlexiBigInt(
		((ASN1Integer) seqOfParams.get(0))).intValue();
	ASN1Sequence seqOfPSh = (ASN1Sequence) seqOfParams.get(1);
	ASN1Sequence seqOfPSw = (ASN1Sequence) seqOfParams.get(2);
	ASN1Sequence seqOfPSK = (ASN1Sequence) seqOfParams.get(3);

	int[] h = new int[seqOfPSh.size()];
	int[] w = new int[seqOfPSw.size()];
	int[] K = new int[seqOfPSK.size()];

	for (int i = 0; i < h.length; i++) {
	    h[i] = ASN1Tools.getFlexiBigInt(((ASN1Integer) seqOfPSh.get(i)))
		    .intValue();
	    w[i] = ASN1Tools.getFlexiBigInt(((ASN1Integer) seqOfPSw.get(i)))
		    .intValue();
	    K[i] = ASN1Tools.getFlexiBigInt(((ASN1Integer) seqOfPSK.get(i)))
		    .intValue();
	}
	GMSSParameterset parSet = new GMSSParameterset(numLayer, h, w, K);

	// --- Decode <name>.
	ASN1Sequence namePart = (ASN1Sequence) mtsPrivateKey.get(21);
	String[] name = new String[namePart.size()];
	for (int i = 0; i < name.length; i++) {
	    name[i] = ((ASN1IA5String) namePart.get(i)).getString();
	}

	// --- Store the key spec.
	keySpec = new GMSSPrivateKeySpec(index, curSeeds, nextNextSeeds,
		curAuth, nextAuth, curTreehash, nextTreehash, curStack,
		nextStack, curRetain, nextRetain, keep, nextNextLeaf,
		upperLeaf, upperTHLeaf, minTreehash, nextRoot, nextNextRoot,
		curRootSig, nextRootSig, parSet, name);
    }

    /**
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
    public GMSSPrivateKeyASN1(int[] index, byte[][] currentSeeds,
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

	// --- Encode <index>.
	ASN1SequenceOf indexPart = new ASN1SequenceOf(ASN1Integer.class);
	for (int i = 0; i < index.length; i++) {
	    indexPart.add(new ASN1Integer(index[i]));
	}
	add(indexPart);

	// --- Encode <curSeeds>.
	ASN1SequenceOf curSeedsPart = new ASN1SequenceOf(ASN1OctetString.class);
	for (int i = 0; i < currentSeeds.length; i++) {
	    curSeedsPart.add(new ASN1OctetString(currentSeeds[i]));
	}
	add(curSeedsPart);

	// --- Encode <nextNextSeeds>.
	ASN1SequenceOf nextNextSeedsPart = new ASN1SequenceOf(
		ASN1OctetString.class);
	for (int i = 0; i < nextNextSeeds.length; i++) {
	    nextNextSeedsPart.add(new ASN1OctetString(nextNextSeeds[i]));
	}
	add(nextNextSeedsPart);

	// --- Encode <curAuth>.
	ASN1SequenceOf curAuthPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	ASN1SequenceOf curAuthPart1 = new ASN1SequenceOf(ASN1Sequence.class);
	for (int i = 0; i < currentAuthPaths.length; i++) {
	    for (int j = 0; j < currentAuthPaths[i].length; j++) {
		curAuthPart0.add(new ASN1OctetString(currentAuthPaths[i][j]));
	    }
	    curAuthPart1.add(curAuthPart0);
	    curAuthPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	}
	add(curAuthPart1);

	// --- Encode <nextAuth>.
	ASN1SequenceOf nextAuthPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	ASN1SequenceOf nextAuthPart1 = new ASN1SequenceOf(ASN1Sequence.class);
	for (int i = 0; i < nextAuthPaths.length; i++) {
	    for (int j = 0; j < nextAuthPaths[i].length; j++) {
		nextAuthPart0.add(new ASN1OctetString(nextAuthPaths[i][j]));
	    }
	    nextAuthPart1.add(nextAuthPart0);
	    nextAuthPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	}
	add(nextAuthPart1);

	// --- Encode <curTreehash>.
	ASN1SequenceOf seqOfTreehash0 = new ASN1SequenceOf(ASN1Sequence.class);
	ASN1SequenceOf seqOfTreehash1 = new ASN1SequenceOf(ASN1Sequence.class);
	ASN1SequenceOf seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	ASN1SequenceOf seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);
	ASN1SequenceOf seqOfInt = new ASN1SequenceOf(ASN1Integer.class);
	ASN1SequenceOf seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	for (int i = 0; i < currentTreehash.length; i++) {
	    for (int j = 0; j < currentTreehash[i].length; j++) {
		seqOfString.add(new ASN1IA5String(algNames[0]));
		seqOfString.add(new ASN1IA5String(""));
		seqOfStat.add(seqOfString);
		seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

		int tailLength = currentTreehash[i][j].getStatInt()[1];

		seqOfByte.add(new ASN1OctetString(currentTreehash[i][j]
			.getStatByte()[0]));
		seqOfByte.add(new ASN1OctetString(currentTreehash[i][j]
			.getStatByte()[1]));
		seqOfByte.add(new ASN1OctetString(currentTreehash[i][j]
			.getStatByte()[2]));
		for (int k = 0; k < tailLength; k++) {
		    seqOfByte.add(new ASN1OctetString(currentTreehash[i][j]
			    .getStatByte()[3 + k]));
		}
		seqOfStat.add(seqOfByte);
		seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);

		seqOfInt.add(new ASN1Integer(
			currentTreehash[i][j].getStatInt()[0]));
		seqOfInt.add(new ASN1Integer(tailLength));
		seqOfInt.add(new ASN1Integer(
			currentTreehash[i][j].getStatInt()[2]));
		seqOfInt.add(new ASN1Integer(
			currentTreehash[i][j].getStatInt()[3]));
		seqOfInt.add(new ASN1Integer(
			currentTreehash[i][j].getStatInt()[4]));
		seqOfInt.add(new ASN1Integer(
			currentTreehash[i][j].getStatInt()[5]));
		for (int k = 0; k < tailLength; k++) {
		    seqOfInt.add(new ASN1Integer(currentTreehash[i][j]
			    .getStatInt()[6 + k]));
		}
		seqOfStat.add(seqOfInt);
		seqOfInt = new ASN1SequenceOf(ASN1Integer.class);

		seqOfTreehash1.add(seqOfStat);
		seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	    }
	    seqOfTreehash0.add(seqOfTreehash1);
	    seqOfTreehash1 = new ASN1SequenceOf(ASN1Sequence.class);
	}
	add(seqOfTreehash0);

	// --- Encode <nextTreehash>.
	seqOfTreehash0 = new ASN1SequenceOf(ASN1Sequence.class);
	seqOfTreehash1 = new ASN1SequenceOf(ASN1Sequence.class);
	seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);
	seqOfInt = new ASN1SequenceOf(ASN1Integer.class);
	seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	for (int i = 0; i < nextTreehash.length; i++) {
	    for (int j = 0; j < nextTreehash[i].length; j++) {
		seqOfString.add(new ASN1IA5String(algNames[0]));
		seqOfString.add(new ASN1IA5String(""));
		seqOfStat.add(seqOfString);
		seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

		int tailLength = nextTreehash[i][j].getStatInt()[1];

		seqOfByte.add(new ASN1OctetString(nextTreehash[i][j]
			.getStatByte()[0]));
		seqOfByte.add(new ASN1OctetString(nextTreehash[i][j]
			.getStatByte()[1]));
		seqOfByte.add(new ASN1OctetString(nextTreehash[i][j]
			.getStatByte()[2]));
		for (int k = 0; k < tailLength; k++) {
		    seqOfByte.add(new ASN1OctetString(nextTreehash[i][j]
			    .getStatByte()[3 + k]));
		}
		seqOfStat.add(seqOfByte);
		seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);

		seqOfInt
			.add(new ASN1Integer(nextTreehash[i][j].getStatInt()[0]));
		seqOfInt.add(new ASN1Integer(tailLength));
		seqOfInt
			.add(new ASN1Integer(nextTreehash[i][j].getStatInt()[2]));
		seqOfInt
			.add(new ASN1Integer(nextTreehash[i][j].getStatInt()[3]));
		seqOfInt
			.add(new ASN1Integer(nextTreehash[i][j].getStatInt()[4]));
		seqOfInt
			.add(new ASN1Integer(nextTreehash[i][j].getStatInt()[5]));
		for (int k = 0; k < tailLength; k++) {
		    seqOfInt.add(new ASN1Integer(nextTreehash[i][j]
			    .getStatInt()[6 + k]));
		}
		seqOfStat.add(seqOfInt);
		seqOfInt = new ASN1SequenceOf(ASN1Integer.class);

		seqOfTreehash1.add(seqOfStat);
		seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	    }
	    seqOfTreehash0.add(seqOfTreehash1);
	    seqOfTreehash1 = new ASN1SequenceOf(ASN1Sequence.class);
	}
	add(seqOfTreehash0);

	// --- Encode <keep>.
	ASN1SequenceOf keepPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	ASN1SequenceOf keepPart1 = new ASN1SequenceOf(ASN1Sequence.class);
	for (int i = 0; i < keep.length; i++) {
	    for (int j = 0; j < keep[i].length; j++) {
		keepPart0.add(new ASN1OctetString(keep[i][j]));
	    }
	    keepPart1.add(keepPart0);
	    keepPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	}
	add(keepPart1);

	// --- Encode <curStack>.
	ASN1SequenceOf curStackPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	ASN1SequenceOf curStackPart1 = new ASN1SequenceOf(ASN1Sequence.class);
	for (int i = 0; i < currentStack.length; i++) {
	    for (int j = 0; j < currentStack[i].size(); j++) {
		curStackPart0.add(new ASN1OctetString((byte[]) currentStack[i]
			.elementAt(j)));
	    }
	    curStackPart1.add(curStackPart0);
	    curStackPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	}
	add(curStackPart1);

	// --- Encode <nextStack>.
	ASN1SequenceOf nextStackPart0 = new ASN1SequenceOf(
		ASN1OctetString.class);
	ASN1SequenceOf nextStackPart1 = new ASN1SequenceOf(ASN1Sequence.class);
	for (int i = 0; i < nextStack.length; i++) {
	    for (int j = 0; j < nextStack[i].size(); j++) {
		nextStackPart0.add(new ASN1OctetString((byte[]) nextStack[i]
			.elementAt(j)));
	    }
	    nextStackPart1.add(nextStackPart0);
	    nextStackPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	}
	add(nextStackPart1);

	// --- Encode <curRetain>.
	ASN1SequenceOf currentRetainPart0 = new ASN1SequenceOf(
		ASN1OctetString.class);
	ASN1SequenceOf currentRetainPart1 = new ASN1SequenceOf(
		ASN1Sequence.class);
	ASN1SequenceOf currentRetainPart2 = new ASN1SequenceOf(
		ASN1Sequence.class);
	for (int i = 0; i < currentRetain.length; i++) {
	    for (int j = 0; j < currentRetain[i].length; j++) {
		for (int k = 0; k < currentRetain[i][j].size(); k++) {
		    currentRetainPart0.add(new ASN1OctetString(
			    (byte[]) currentRetain[i][j].elementAt(k)));
		}
		currentRetainPart1.add(currentRetainPart0);
		currentRetainPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	    }
	    currentRetainPart2.add(currentRetainPart1);
	    currentRetainPart1 = new ASN1SequenceOf(ASN1OctetString.class);
	}
	add(currentRetainPart2);

	// --- Encode <nextRetain>.
	ASN1SequenceOf nextRetainPart0 = new ASN1SequenceOf(
		ASN1OctetString.class);
	ASN1SequenceOf nextRetainPart1 = new ASN1SequenceOf(ASN1Sequence.class);
	ASN1SequenceOf nextRetainPart2 = new ASN1SequenceOf(ASN1Sequence.class);
	for (int i = 0; i < nextRetain.length; i++) {
	    for (int j = 0; j < nextRetain[i].length; j++) {
		for (int k = 0; k < nextRetain[i][j].size(); k++) {
		    nextRetainPart0.add(new ASN1OctetString(
			    (byte[]) nextRetain[i][j].elementAt(k)));
		}
		nextRetainPart1.add(nextRetainPart0);
		nextRetainPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	    }
	    nextRetainPart2.add(nextRetainPart1);
	    nextRetainPart1 = new ASN1SequenceOf(ASN1OctetString.class);
	}
	add(nextRetainPart2);

	// --- Encode <nextNextLeaf>.
	ASN1SequenceOf seqOfLeaf = new ASN1SequenceOf(ASN1Sequence.class);
	seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);
	seqOfInt = new ASN1SequenceOf(ASN1Integer.class);
	seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	for (int i = 0; i < nextNextLeaf.length; i++) {
	    seqOfString.add(new ASN1IA5String(algNames[0]));
	    seqOfString.add(new ASN1IA5String(""));
	    seqOfStat.add(seqOfString);
	    seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	    byte[][] tempByte = nextNextLeaf[i].getStatByte();
	    seqOfByte.add(new ASN1OctetString(tempByte[0]));
	    seqOfByte.add(new ASN1OctetString(tempByte[1]));
	    seqOfByte.add(new ASN1OctetString(tempByte[2]));
	    seqOfByte.add(new ASN1OctetString(tempByte[3]));
	    seqOfStat.add(seqOfByte);
	    seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);

	    int[] tempInt = nextNextLeaf[i].getStatInt();
	    seqOfInt.add(new ASN1Integer(tempInt[0]));
	    seqOfInt.add(new ASN1Integer(tempInt[1]));
	    seqOfInt.add(new ASN1Integer(tempInt[2]));
	    seqOfInt.add(new ASN1Integer(tempInt[3]));
	    seqOfStat.add(seqOfInt);
	    seqOfInt = new ASN1SequenceOf(ASN1Integer.class);

	    seqOfLeaf.add(seqOfStat);
	    seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	}
	add(seqOfLeaf);

	// --- Encode <upperLEAF>.
	ASN1SequenceOf seqOfUpperLeaf = new ASN1SequenceOf(ASN1Sequence.class);
	seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);
	seqOfInt = new ASN1SequenceOf(ASN1Integer.class);
	seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	for (int i = 0; i < upperLeaf.length; i++) {
	    seqOfString.add(new ASN1IA5String(algNames[0]));
	    seqOfString.add(new ASN1IA5String(""));
	    seqOfStat.add(seqOfString);
	    seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	    byte[][] tempByte = upperLeaf[i].getStatByte();
	    seqOfByte.add(new ASN1OctetString(tempByte[0]));
	    seqOfByte.add(new ASN1OctetString(tempByte[1]));
	    seqOfByte.add(new ASN1OctetString(tempByte[2]));
	    seqOfByte.add(new ASN1OctetString(tempByte[3]));
	    seqOfStat.add(seqOfByte);
	    seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);

	    int[] tempInt = upperLeaf[i].getStatInt();
	    seqOfInt.add(new ASN1Integer(tempInt[0]));
	    seqOfInt.add(new ASN1Integer(tempInt[1]));
	    seqOfInt.add(new ASN1Integer(tempInt[2]));
	    seqOfInt.add(new ASN1Integer(tempInt[3]));
	    seqOfStat.add(seqOfInt);
	    seqOfInt = new ASN1SequenceOf(ASN1Integer.class);

	    seqOfUpperLeaf.add(seqOfStat);
	    seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	}
	add(seqOfUpperLeaf);

	// encode <upperTreehashLeaf>
	ASN1SequenceOf seqOfUpperTreehashLeaf = new ASN1SequenceOf(
		ASN1Sequence.class);
	seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);
	seqOfInt = new ASN1SequenceOf(ASN1Integer.class);
	seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	for (int i = 0; i < upperTreehashLeaf.length; i++) {
	    seqOfString.add(new ASN1IA5String(algNames[0]));
	    seqOfString.add(new ASN1IA5String(""));
	    seqOfStat.add(seqOfString);
	    seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	    byte[][] tempByte = upperTreehashLeaf[i].getStatByte();
	    seqOfByte.add(new ASN1OctetString(tempByte[0]));
	    seqOfByte.add(new ASN1OctetString(tempByte[1]));
	    seqOfByte.add(new ASN1OctetString(tempByte[2]));
	    seqOfByte.add(new ASN1OctetString(tempByte[3]));
	    seqOfStat.add(seqOfByte);
	    seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);

	    int[] tempInt = upperTreehashLeaf[i].getStatInt();
	    seqOfInt.add(new ASN1Integer(tempInt[0]));
	    seqOfInt.add(new ASN1Integer(tempInt[1]));
	    seqOfInt.add(new ASN1Integer(tempInt[2]));
	    seqOfInt.add(new ASN1Integer(tempInt[3]));
	    seqOfStat.add(seqOfInt);
	    seqOfInt = new ASN1SequenceOf(ASN1Integer.class);

	    seqOfUpperTreehashLeaf.add(seqOfStat);
	    seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	}
	add(seqOfUpperTreehashLeaf);

	// --- Encode <minTreehash>.
	ASN1SequenceOf minTreehashPart = new ASN1SequenceOf(ASN1Integer.class);
	for (int i = 0; i < minTreehash.length; i++) {
	    minTreehashPart.add(new ASN1Integer(minTreehash[i]));
	}
	add(minTreehashPart);

	// --- Encode <nextRoot>.
	ASN1SequenceOf nextRootPart = new ASN1SequenceOf(ASN1OctetString.class);
	for (int i = 0; i < nextRoot.length; i++) {
	    nextRootPart.add(new ASN1OctetString(nextRoot[i]));
	}
	add(nextRootPart);

	// --- Encode <nextNextRoot>.
	ASN1SequenceOf seqOfnextNextRoot = new ASN1SequenceOf(
		ASN1Sequence.class);
	ASN1SequenceOf seqOfnnRStats = new ASN1SequenceOf(ASN1Sequence.class);
	ASN1SequenceOf seqOfnnRStrings = new ASN1SequenceOf(ASN1IA5String.class);
	ASN1SequenceOf seqOfnnRBytes = new ASN1SequenceOf(ASN1OctetString.class);
	ASN1SequenceOf seqOfnnRInts = new ASN1SequenceOf(ASN1Integer.class);
	ASN1SequenceOf seqOfnnRTreehash = new ASN1SequenceOf(ASN1Integer.class);
	ASN1SequenceOf seqOfnnRRetain = new ASN1SequenceOf(ASN1Integer.class);

	for (int i = 0; i < nextNextRoot.length; i++) {
	    seqOfnnRStrings.add(new ASN1IA5String(algNames[0]));
	    seqOfnnRStrings.add(new ASN1IA5String(""));
	    seqOfnnRStats.add(seqOfnnRStrings);
	    seqOfnnRStrings = new ASN1SequenceOf(ASN1IA5String.class);

	    int heightOfTree = nextNextRoot[i].getStatInt()[0];
	    int tailLength = nextNextRoot[i].getStatInt()[7];

	    seqOfnnRBytes.add(new ASN1OctetString(
		    nextNextRoot[i].getStatByte()[0]));
	    for (int j = 0; j < heightOfTree; j++) {
		seqOfnnRBytes.add(new ASN1OctetString(nextNextRoot[i]
			.getStatByte()[1 + j]));
	    }
	    for (int j = 0; j < tailLength; j++) {
		seqOfnnRBytes.add(new ASN1OctetString(nextNextRoot[i]
			.getStatByte()[1 + heightOfTree + j]));
	    }

	    seqOfnnRStats.add(seqOfnnRBytes);
	    seqOfnnRBytes = new ASN1SequenceOf(ASN1OctetString.class);

	    seqOfnnRInts.add(new ASN1Integer(heightOfTree));
	    seqOfnnRInts.add(new ASN1Integer(nextNextRoot[i].getStatInt()[1]));
	    seqOfnnRInts.add(new ASN1Integer(nextNextRoot[i].getStatInt()[2]));
	    seqOfnnRInts.add(new ASN1Integer(nextNextRoot[i].getStatInt()[3]));
	    seqOfnnRInts.add(new ASN1Integer(nextNextRoot[i].getStatInt()[4]));
	    seqOfnnRInts.add(new ASN1Integer(nextNextRoot[i].getStatInt()[5]));
	    seqOfnnRInts.add(new ASN1Integer(nextNextRoot[i].getStatInt()[6]));
	    seqOfnnRInts.add(new ASN1Integer(tailLength));
	    for (int j = 0; j < heightOfTree; j++) {
		seqOfnnRInts.add(new ASN1Integer(
			nextNextRoot[i].getStatInt()[8 + j]));
	    }
	    for (int j = 0; j < tailLength; j++) {
		seqOfnnRInts.add(new ASN1Integer(nextNextRoot[i].getStatInt()[8
			+ heightOfTree + j]));
	    }

	    seqOfnnRStats.add(seqOfnnRInts);
	    seqOfnnRInts = new ASN1SequenceOf(ASN1Integer.class);

	    // add treehash of nextNextRoot object
	    // ----------------------------
	    seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
	    seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);
	    seqOfInt = new ASN1SequenceOf(ASN1Integer.class);
	    seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

	    if (nextNextRoot[i].getTreehash() != null) {
		for (int j = 0; j < nextNextRoot[i].getTreehash().length; j++) {
		    seqOfString.add(new ASN1IA5String(algNames[0]));
		    seqOfString.add(new ASN1IA5String(""));
		    seqOfStat.add(seqOfString);
		    seqOfString = new ASN1SequenceOf(ASN1IA5String.class);

		    tailLength = nextNextRoot[i].getTreehash()[j].getStatInt()[1];

		    seqOfByte.add(new ASN1OctetString(nextNextRoot[i]
			    .getTreehash()[j].getStatByte()[0]));
		    seqOfByte.add(new ASN1OctetString(nextNextRoot[i]
			    .getTreehash()[j].getStatByte()[1]));
		    seqOfByte.add(new ASN1OctetString(nextNextRoot[i]
			    .getTreehash()[j].getStatByte()[2]));
		    for (int k = 0; k < tailLength; k++) {
			seqOfByte.add(new ASN1OctetString(nextNextRoot[i]
				.getTreehash()[j].getStatByte()[3 + k]));
		    }
		    seqOfStat.add(seqOfByte);
		    seqOfByte = new ASN1SequenceOf(ASN1OctetString.class);

		    seqOfInt.add(new ASN1Integer(
			    nextNextRoot[i].getTreehash()[j].getStatInt()[0]));
		    seqOfInt.add(new ASN1Integer(tailLength));
		    seqOfInt.add(new ASN1Integer(
			    nextNextRoot[i].getTreehash()[j].getStatInt()[2]));
		    seqOfInt.add(new ASN1Integer(
			    nextNextRoot[i].getTreehash()[j].getStatInt()[3]));
		    seqOfInt.add(new ASN1Integer(
			    nextNextRoot[i].getTreehash()[j].getStatInt()[4]));
		    seqOfInt.add(new ASN1Integer(
			    nextNextRoot[i].getTreehash()[j].getStatInt()[5]));
		    for (int k = 0; k < tailLength; k++) {
			seqOfInt.add(new ASN1Integer(nextNextRoot[i]
				.getTreehash()[j].getStatInt()[6 + k]));
		    }
		    seqOfStat.add(seqOfInt);
		    seqOfInt = new ASN1SequenceOf(ASN1Integer.class);

		    seqOfnnRTreehash.add(seqOfStat);
		    seqOfStat = new ASN1SequenceOf(ASN1Sequence.class);
		}
	    }
	    // ----------------------------
	    seqOfnnRStats.add(seqOfnnRTreehash);
	    seqOfnnRTreehash = new ASN1SequenceOf(ASN1Integer.class);

	    // encode retain of nextNextRoot
	    // ----------------------------
	    // --- Encode <curRetain>.
	    currentRetainPart0 = new ASN1SequenceOf(ASN1OctetString.class);
	    if (nextNextRoot[i].getRetain() != null) {
		for (int j = 0; j < nextNextRoot[i].getRetain().length; j++) {
		    for (int k = 0; k < nextNextRoot[i].getRetain()[j].size(); k++) {
			currentRetainPart0.add(new ASN1OctetString(
				(byte[]) nextNextRoot[i].getRetain()[j]
					.elementAt(k)));
		    }
		    seqOfnnRRetain.add(currentRetainPart0);
		    currentRetainPart0 = new ASN1SequenceOf(
			    ASN1OctetString.class);
		}
	    }
	    // ----------------------------
	    seqOfnnRStats.add(seqOfnnRRetain);
	    seqOfnnRRetain = new ASN1SequenceOf(ASN1Integer.class);

	    seqOfnextNextRoot.add(seqOfnnRStats);
	    seqOfnnRStats = new ASN1SequenceOf(ASN1Sequence.class);
	}
	add(seqOfnextNextRoot);

	// --- Encode <curRootSig>.
	ASN1SequenceOf curRootSigPart = new ASN1SequenceOf(
		ASN1OctetString.class);
	for (int i = 0; i < currentRootSig.length; i++) {
	    curRootSigPart.add(new ASN1OctetString(currentRootSig[i]));
	}
	add(curRootSigPart);

	// --- Encode <nextRootSig>.
	ASN1SequenceOf seqOfnextRootSigs = new ASN1SequenceOf(
		ASN1Sequence.class);
	ASN1SequenceOf seqOfnRSStats = new ASN1SequenceOf(ASN1Sequence.class);
	ASN1SequenceOf seqOfnRSStrings = new ASN1SequenceOf(ASN1IA5String.class);
	ASN1SequenceOf seqOfnRSBytes = new ASN1SequenceOf(ASN1OctetString.class);
	ASN1SequenceOf seqOfnRSInts = new ASN1SequenceOf(ASN1Integer.class);

	for (int i = 0; i < nextRootSig.length; i++) {
	    seqOfnRSStrings.add(new ASN1IA5String(algNames[0]));
	    seqOfnRSStrings.add(new ASN1IA5String(""));
	    seqOfnRSStats.add(seqOfnRSStrings);
	    seqOfnRSStrings = new ASN1SequenceOf(ASN1IA5String.class);

	    seqOfnRSBytes.add(new ASN1OctetString(
		    nextRootSig[i].getStatByte()[0]));
	    seqOfnRSBytes.add(new ASN1OctetString(
		    nextRootSig[i].getStatByte()[1]));
	    seqOfnRSBytes.add(new ASN1OctetString(
		    nextRootSig[i].getStatByte()[2]));
	    seqOfnRSBytes.add(new ASN1OctetString(
		    nextRootSig[i].getStatByte()[3]));
	    seqOfnRSBytes.add(new ASN1OctetString(
		    nextRootSig[i].getStatByte()[4]));

	    seqOfnRSStats.add(seqOfnRSBytes);
	    seqOfnRSBytes = new ASN1SequenceOf(ASN1OctetString.class);

	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[0]));
	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[1]));
	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[2]));
	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[3]));
	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[4]));
	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[5]));
	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[6]));
	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[7]));
	    seqOfnRSInts.add(new ASN1Integer(nextRootSig[i].getStatInt()[8]));

	    seqOfnRSStats.add(seqOfnRSInts);
	    seqOfnRSInts = new ASN1SequenceOf(ASN1Integer.class);

	    seqOfnextRootSigs.add(seqOfnRSStats);
	    seqOfnRSStats = new ASN1SequenceOf(ASN1Sequence.class);
	}
	add(seqOfnextRootSigs);

	// --- Encode <parameterset>.
	ASN1SequenceOf parSetPart0 = new ASN1SequenceOf(ASN1Sequence.class);
	ASN1SequenceOf parSetPart1 = new ASN1SequenceOf(ASN1Integer.class);
	ASN1SequenceOf parSetPart2 = new ASN1SequenceOf(ASN1Integer.class);
	ASN1SequenceOf parSetPart3 = new ASN1SequenceOf(ASN1Integer.class);

	for (int i = 0; i < gmssParameterset.getHeightOfTrees().length; i++) {
	    parSetPart1.add(new ASN1Integer(
		    gmssParameterset.getHeightOfTrees()[i]));
	    parSetPart2.add(new ASN1Integer(gmssParameterset
		    .getWinternitzParameter()[i]));
	    parSetPart3.add(new ASN1Integer(gmssParameterset.getK()[i]));
	}
	parSetPart0.add(new ASN1Integer(gmssParameterset.getNumOfLayers()));
	parSetPart0.add(parSetPart1);
	parSetPart0.add(parSetPart2);
	parSetPart0.add(parSetPart3);
	add(parSetPart0);

	// --- Encode <names>.
	ASN1SequenceOf namesPart = new ASN1SequenceOf(ASN1IA5String.class);

	for (int i = 0; i < algNames.length; i++) {
	    String name = algNames[i];
	    if (name != null) {
		namesPart.add(new ASN1IA5String(algNames[i]));
	    } else {
		namesPart.add(new ASN1IA5String(""));
	    }

	}
	add(namesPart);

    }

    /**
     * @return The <code>GMSSPrivateKeySpec</code>
     */
    public GMSSPrivateKeySpec getKeySpec() {
	return keySpec;
    }
}

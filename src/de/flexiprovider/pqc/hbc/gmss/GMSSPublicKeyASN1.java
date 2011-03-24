package de.flexiprovider.pqc.hbc.gmss;

import codec.asn1.ASN1Integer;
import codec.asn1.ASN1OctetString;
import codec.asn1.ASN1Sequence;
import codec.asn1.ASN1SequenceOf;
import codec.asn1.ASN1Type;
import de.flexiprovider.common.util.ASN1Tools;

/**
 * This class implements an ASN.1 encoded GMSS public key. The ASN.1 definition
 * of this structure is:
 * 
 * <pre>
 *  GMSSPublicKey		::= SEQUENCE{
 *   	publicKey		SEQUENCE OF OCTET STRING 
 *  	heightOfTrees	SEQUENCE OF INTEGER
 *  	Parameterset	ParSet
 * 	}
 * 	ParSet				::= SEQUENCE {
 * 		T				INTEGER
 * 		h				SEQUENCE OF INTEGER
 * 		w				SEQUENCE OF INTEGER		
 * 	K				SEQUENCE OF INTEGER
 * 	}
 * </pre>
 * 
 * @author Sebastian Blume, Michael Schneider
 */
public class GMSSPublicKeyASN1 extends ASN1Sequence {

    /**
     * A key specification of the GMSS public key
     */
    private GMSSPublicKeySpec keySpec;

    /**
     * The Constructor
     * 
     * @param encoded
     *                a ASN.1 encoded GMSS public key
     */
    public GMSSPublicKeyASN1(ASN1Type encoded) {

	ASN1Sequence mtsPublicKey = (ASN1Sequence) encoded;

	// --- Decode <publicKey>.
	byte[] publicKey = ((ASN1OctetString) mtsPublicKey.get(0))
		.getByteArray();

	// --- Decode <parameterset>.

	ASN1Sequence seqOfParams = (ASN1Sequence) mtsPublicKey.get(1);
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

	keySpec = new GMSSPublicKeySpec(publicKey, parSet);
    }

    /**
     * The Constructor
     * 
     * @param publicKey
     *                a raw GMSS public key
     * @param parSet
     *                GMSS parameter set
     */
    public GMSSPublicKeyASN1(byte[] publicKey, GMSSParameterset parSet) {

	// --- Encode <publicKey>.
	add(new ASN1OctetString(publicKey));

	// --- Encode <parameterset>.
	ASN1SequenceOf parSetPart0 = new ASN1SequenceOf(ASN1Sequence.class);
	ASN1SequenceOf parSetPart1 = new ASN1SequenceOf(ASN1Integer.class);
	ASN1SequenceOf parSetPart2 = new ASN1SequenceOf(ASN1Integer.class);
	ASN1SequenceOf parSetPart3 = new ASN1SequenceOf(ASN1Integer.class);

	for (int i = 0; i < parSet.getHeightOfTrees().length; i++) {
	    parSetPart1.add(new ASN1Integer(parSet.getHeightOfTrees()[i]));
	    parSetPart2
		    .add(new ASN1Integer(parSet.getWinternitzParameter()[i]));
	    parSetPart3.add(new ASN1Integer(parSet.getK()[i]));
	}
	parSetPart0.add(new ASN1Integer(parSet.getNumOfLayers()));
	parSetPart0.add(parSetPart1);
	parSetPart0.add(parSetPart2);
	parSetPart0.add(parSetPart3);
	add(parSetPart0);
    }

    /**
     * Returns the GMSS public key specification
     * 
     * @return the GMSS public key specification
     */
    public GMSSPublicKeySpec getKeySpec() {
	return keySpec;
    }
}

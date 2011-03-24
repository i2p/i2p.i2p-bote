package de.flexiprovider.pqc.hbc.gmss;

import codec.asn1.ASN1Null;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1Type;
import de.flexiprovider.api.keys.PublicKey;
import de.flexiprovider.common.util.ASN1Tools;
import de.flexiprovider.common.util.ByteUtils;

/**
 * This class implements the GMSS public key and is usually initiated by the <a
 * href="GMSSKeyPairGenerator">GMSSKeyPairGenerator</a>.
 * 
 * @author Sebastian Blume
 * 
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSKeyPairGenerator
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPublicKeySpec
 */
public class GMSSPublicKey extends PublicKey {

    /**
     * The GMSS public key
     */
    private byte[] publicKeyBytes;

    /**
     * The GMSSParameterset
     */
    private GMSSParameterset gmssParameterset;

    /**
     * The constructor
     * 
     * @param pub
     *                a raw GMSS public key
     * 
     * @param gmssParameterset
     *                an instance of GMSS Parameterset
     * 
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSKeyPairGenerator
     */
    protected GMSSPublicKey(byte[] pub, GMSSParameterset gmssParameterset) {

	this.gmssParameterset = gmssParameterset;
	this.publicKeyBytes = pub;
    }

    /**
     * The constructor
     * 
     * @param keySpec
     *                a GMSS key specification
     */
    protected GMSSPublicKey(GMSSPublicKeySpec keySpec) {
	this(keySpec.getPublicKey(), keySpec.getGMSSParameterset());
    }

    /**
     * Returns the name of the algorithm
     * 
     * @return "GMSS"
     */
    public String getAlgorithm() {
	return "GMSS";
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
	GMSSPublicKeyASN1 key = new GMSSPublicKeyASN1(publicKeyBytes,
		gmssParameterset);
	return ASN1Tools.derEncode(key);
    }

    /**
     * @return The GMSS public key byte array
     */
    protected byte[] getPublicKeyBytes() {
	return publicKeyBytes;
    }

    /**
     * @return The GMSS Parameterset
     */
    protected GMSSParameterset getParameterset() {
	return gmssParameterset;
    }

    /**
     * Returns a human readable form of the GMSS public key
     * 
     * @return A human readable form of the GMSS public key
     */
    public String toString() {
	String out = "GMSS public key : "
		+ ByteUtils.toHexString(publicKeyBytes) + "\n"
		+ "Height of Trees: \n";

	for (int i = 0; i < gmssParameterset.getHeightOfTrees().length; i++) {
	    out = out + "Layer " + i + " : "
		    + gmssParameterset.getHeightOfTrees()[i]
		    + " WinternitzParameter: "
		    + gmssParameterset.getWinternitzParameter()[i] + " K: "
		    + gmssParameterset.getK()[i] + "\n";
	}
	return out;
    }

}

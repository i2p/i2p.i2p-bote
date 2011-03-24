package de.flexiprovider.api.keys;

import codec.asn1.ASN1Exception;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1Type;
import codec.pkcs8.PrivateKeyInfo;
import de.flexiprovider.common.util.ASN1Tools;
import de.flexiprovider.pki.AlgorithmIdentifier;

public abstract class PrivateKey implements Key, java.security.PrivateKey {

    /**
     * Return the encoding format, PKCS #8.
     * 
     * @return "PKCS#8"
     */
    public final String getFormat() {
	return "PKCS#8";
    }

    /**
     * Return the key in its primary encoding format, PKCS #8.
     * 
     * @return the PKCS #8 encoded key.
     */
    public final byte[] getEncoded() {
	AlgorithmIdentifier aid;
	try {
	    aid = new AlgorithmIdentifier(getOID(), getAlgParams());
	} catch (ASN1Exception asn1e) {
	    throw new RuntimeException("ASN1Exception: " + asn1e.getMessage());
	}
	PrivateKeyInfo spki = new PrivateKeyInfo(aid, getKeyData());
	return ASN1Tools.derEncode(spki);
    }

    /**
     * @return the OID to encode in the SubjectPublicKeyInfo structure
     */
    protected abstract ASN1ObjectIdentifier getOID();

    /**
     * @return the algorithm parameters to encode in the SubjectPublicKeyInfo
     *         structure
     */
    protected abstract ASN1Type getAlgParams();

    /**
     * @return the keyData to encode in the SubjectPublicKeyInfo structure
     */
    protected abstract byte[] getKeyData();

}

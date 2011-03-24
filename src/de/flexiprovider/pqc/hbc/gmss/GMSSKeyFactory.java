package de.flexiprovider.pqc.hbc.gmss;

import java.io.ByteArrayInputStream;

import codec.CorruptedCodeException;
import codec.asn1.ASN1Type;
import codec.asn1.DERDecoder;
import codec.pkcs8.PrivateKeyInfo;
import codec.x509.SubjectPublicKeyInfo;
import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.exceptions.InvalidKeySpecException;
import de.flexiprovider.api.keys.Key;
import de.flexiprovider.api.keys.KeyFactory;
import de.flexiprovider.api.keys.KeySpec;
import de.flexiprovider.api.keys.PrivateKey;
import de.flexiprovider.api.keys.PublicKey;
import de.flexiprovider.pki.PKCS8EncodedKeySpec;
import de.flexiprovider.pki.X509EncodedKeySpec;

/**
 * This class transforms GMSS keys and GMSS key specifications into a form that
 * can be used with the FlexiPQCProvider.
 * 
 * @author Sebastian Blume, Michael Schneider
 * 
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKey
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKeySpec
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPublicKey
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPublicKeySpec
 */
public class GMSSKeyFactory extends KeyFactory {

    /**
     * The GMSS OID.
     */
    public static final String OID = "1.3.6.1.4.1.8301.3.1.3.3";

    /**
     * Converts, if possible, a key specification into a GMSSPublicKey.
     * Currently the following key specs are supported: GMSSPublicKeySpec,
     * X509EncodedKeySpec.
     * <p>
     * 
     * @param keySpec
     *                the key specification
     * @return A GMSS public key
     * @throws InvalidKeySpecException
     *                 if the KeySpec is not supported
     */
    public PublicKey generatePublic(KeySpec keySpec)
	    throws InvalidKeySpecException {
	if (keySpec instanceof GMSSPublicKeySpec) {
	    GMSSPublicKeySpec gmssPublicKeySpec = (GMSSPublicKeySpec) keySpec;

	    return new GMSSPublicKey(gmssPublicKeySpec.getPublicKey(),
		    gmssPublicKeySpec.getGMSSParameterset());
	} else if (keySpec instanceof X509EncodedKeySpec) {

	    // get the DER-encoded Key according to X.509 from the spec
	    byte[] enc = ((X509EncodedKeySpec) keySpec).getEncoded();

	    // decode the SubjectPublicKeyInfo data structure to the pki object
	    SubjectPublicKeyInfo pki = new SubjectPublicKeyInfo();
	    try {
		ByteArrayInputStream bais = new ByteArrayInputStream(enc);
		pki.decode(new DERDecoder(bais));
		bais.close();
	    } catch (Exception ce) {
		ce.printStackTrace();
		throw new InvalidKeySpecException(
			"Unable to decode X509EncodedKeySpec");
	    }

	    // get the inner type inside the BIT STRING
	    try {
		ASN1Type innerType = pki.getDecodedRawKey();
		GMSSPublicKeyASN1 gmssPublicKey = new GMSSPublicKeyASN1(
			innerType);
		return new GMSSPublicKey(gmssPublicKey.getKeySpec());
	    } catch (CorruptedCodeException cce) {
		throw new InvalidKeySpecException(
			"Unable to decode X509EncodedKeySpec");
	    }
	}
	throw new InvalidKeySpecException("Unknown KeySpec type");

    }

    /**
     * Converts, if possible, a key specification into a GMSSPrivateKey.
     * Currently the following key specs are supported: GMSSPrivateKeySpec.
     * <p>
     * 
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKey
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKeySpec
     * @param keySpec
     *                the key specification
     * @return The GMSS private key
     * @throws InvalidKeySpecException
     *                 if the KeySpec is not supported
     */
    public PrivateKey generatePrivate(KeySpec keySpec)
	    throws InvalidKeySpecException {
	if (keySpec instanceof GMSSPrivateKeySpec) {
	    GMSSPrivateKeySpec gmssPrivKeySpec = (GMSSPrivateKeySpec) keySpec;

	    return new GMSSPrivateKey(gmssPrivKeySpec.getIndex(),
		    gmssPrivKeySpec.getCurrentSeed(), gmssPrivKeySpec
			    .getNextNextSeed(), gmssPrivKeySpec
			    .getCurrentAuthPath(), gmssPrivKeySpec
			    .getNextAuthPath(), gmssPrivKeySpec.getKeep(),
		    gmssPrivKeySpec.getCurrentTreehash(), gmssPrivKeySpec
			    .getNextTreehash(), gmssPrivKeySpec
			    .getCurrentStack(), gmssPrivKeySpec.getNextStack(),
		    gmssPrivKeySpec.getCurrentRetain(), gmssPrivKeySpec
			    .getNextRetain(),
		    gmssPrivKeySpec.getNextNextLeaf(), gmssPrivKeySpec
			    .getUpperLeaf(), gmssPrivKeySpec
			    .getUpperTreehashLeaf(), gmssPrivKeySpec
			    .getMinTreehash(), gmssPrivKeySpec.getNextRoot(),
		    gmssPrivKeySpec.getNextNextRoot(), gmssPrivKeySpec
			    .getCurrentRootSig(), gmssPrivKeySpec
			    .getNextRootSig(), gmssPrivKeySpec.getGmssPS(),
		    gmssPrivKeySpec.getAlgNames());
	} else if (keySpec instanceof PKCS8EncodedKeySpec) {

	    // get the DER-encoded Key according to PKCS#8 from the spec
	    byte[] encKey = ((PKCS8EncodedKeySpec) keySpec).getEncoded();

	    // decode the PKCS#8 data structure to the pki object
	    PrivateKeyInfo pki = new PrivateKeyInfo();
	    try {
		ByteArrayInputStream bais = new java.io.ByteArrayInputStream(
			encKey);
		pki.decode(new DERDecoder(bais));
		bais.close();
	    } catch (Exception ce2) {
		throw new InvalidKeySpecException(
			"Unable to decode PKCS8EncodedKeySpec."
				+ ce2.getMessage());
	    }

	    // get the inner type inside the OCTET STRING
	    try {
		// --- Build and return the actual key.
		ASN1Type innerType = pki.getDecodedRawKey();
		GMSSPrivateKeyASN1 key = new GMSSPrivateKeyASN1(innerType);
		return new GMSSPrivateKey(key.getKeySpec());

	    } catch (CorruptedCodeException cce) {
		throw new InvalidKeySpecException(
			"Unable to decode PKCS8EncodedKeySpec.");
	    }
	}

	throw new InvalidKeySpecException("Unknown KeySpec type.");
    }

    /**
     * Converts a given key into a key specification, if possible. Currently the
     * following specs are supported:
     * <ul>
     * <li> for GMSSPublicKey: X509EncodedKeySpec, GMSSPublicKeySpec
     * <li> for GMSSPrivateKey: PKCS8EncodedKeySpec, GMSSPrivateKeySpec
     * </ul>
     * <p>
     * 
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKey
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSPrivateKeySpec
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSPublicKey
     * @see de.flexiprovider.pqc.hbc.gmss.GMSSPublicKeySpec
     * @param key
     *                the key
     * @param spec
     *                the class of which type the returned class should be
     * @return The specification of the GMSS key
     * @throws InvalidKeySpecException
     *                 if the specification is not supported
     */
    public KeySpec getKeySpec(Key key, Class spec)
	    throws InvalidKeySpecException {
	if (key instanceof GMSSPrivateKey) {
	    if (PKCS8EncodedKeySpec.class.isAssignableFrom(spec)) {
		return new PKCS8EncodedKeySpec(key.getEncoded());
	    } else if (GMSSPrivateKeySpec.class.isAssignableFrom(spec)) {
		GMSSPrivateKey gmssPrivateKey = (GMSSPrivateKey) key;

		return new GMSSPrivateKeySpec(gmssPrivateKey.getIndex(),
			gmssPrivateKey.getCurrentSeeds(), gmssPrivateKey
				.getNextNextSeeds(), gmssPrivateKey
				.getCurrentAuthPaths(), gmssPrivateKey
				.getNextAuthPaths(), gmssPrivateKey
				.getCurrentTreehash(), gmssPrivateKey
				.getNextTreehash(), gmssPrivateKey
				.getCurrentStack(), gmssPrivateKey
				.getNextStack(), gmssPrivateKey
				.getCurrentRetain(), gmssPrivateKey
				.getNextRetain(), gmssPrivateKey.getKeep(),
			gmssPrivateKey.getNextNextLeaf(), gmssPrivateKey
				.getUpperLeaf(), gmssPrivateKey
				.getUpperTreehashLeaf(), gmssPrivateKey
				.getMinTreehash(),
			gmssPrivateKey.getNextRoot(), gmssPrivateKey
				.getNextNextRoot(), gmssPrivateKey
				.getCurrentRootSig(), gmssPrivateKey
				.getNextRootSig(), gmssPrivateKey
				.getParameterset(), gmssPrivateKey.getName());
	    }
	} else if (key instanceof GMSSPublicKey) {
	    if (X509EncodedKeySpec.class.isAssignableFrom(spec)) {
		return new X509EncodedKeySpec(key.getEncoded());
	    } else if (GMSSPublicKeySpec.class.isAssignableFrom(spec)) {
		GMSSPublicKey merkleTreePublicKey = (GMSSPublicKey) key;
		return new GMSSPublicKeySpec(merkleTreePublicKey
			.getPublicKeyBytes(), merkleTreePublicKey
			.getParameterset());
	    }
	}
	throw new InvalidKeySpecException("Unknown KeySpec");
    }

    /**
     * Translates a key into a form known by the FlexiProvider. Currently the
     * following key types are supported: GMSSPrivateKey, GMSSPublicKey.
     * <p>
     * 
     * @param key
     *                the key
     * @return A key of a known key type
     * @throws InvalidKeyException
     *                 if the key is not supported
     */
    public Key translateKey(Key key) throws InvalidKeyException {
	if (key instanceof GMSSPrivateKey) {
	    return key;
	} else if (key instanceof GMSSPublicKey) {
	    return key;
	}

	throw new InvalidKeyException("Unsupported key type");
    }
}

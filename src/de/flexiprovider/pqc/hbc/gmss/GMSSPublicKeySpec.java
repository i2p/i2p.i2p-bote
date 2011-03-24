package de.flexiprovider.pqc.hbc.gmss;

import de.flexiprovider.api.keys.KeySpec;

/**
 * This class provides a specification for a GMSS public key.
 * 
 * @author Sebastian Blume
 * 
 * @see de.flexiprovider.pqc.hbc.gmss.GMSSPublicKey
 */
public class GMSSPublicKeySpec implements KeySpec {

    /**
     * The GMSS public key
     */
    private byte[] gmssPublicKey;

    /**
     * The GMSSParameterset
     */
    private GMSSParameterset gmssParameterset;

    /**
     * The constructor.
     * 
     * @param key
     *                a raw GMSS public key
     * @param gmssParameterset
     *                an instance of GMSSParameterset
     */
    public GMSSPublicKeySpec(byte[] key, GMSSParameterset gmssParameterset) {
	this.gmssPublicKey = key;
	this.gmssParameterset = gmssParameterset;
    }

    /**
     * Returns the GMSS public key
     * 
     * @return The GMSS public key
     */
    public byte[] getPublicKey() {
	return gmssPublicKey;
    }

    /**
     * Returns the GMSS parameterset
     * 
     * @return The GMSS parameterset
     */
    public GMSSParameterset getGMSSParameterset() {
	return gmssParameterset;
    }

}

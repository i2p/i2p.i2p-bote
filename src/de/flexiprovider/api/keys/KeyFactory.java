package de.flexiprovider.api.keys;

import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.exceptions.InvalidKeySpecException;
import de.flexiprovider.pki.PKCS8EncodedKeySpec;
import de.flexiprovider.pki.X509EncodedKeySpec;

public abstract class KeyFactory extends java.security.KeyFactorySpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * JCA adapter for FlexiAPI method generatePublic(): generates a public key
     * object from the provided key specification (key material).
     * 
     * @param keySpec
     *                the specification (key material) of the public key
     * @return the public key
     * @throws java.security.spec.InvalidKeySpecException
     *                 if the given key specification is inappropriate for this
     *                 key factory to produce a public key.
     */
    protected java.security.PublicKey engineGeneratePublic(
	    java.security.spec.KeySpec keySpec)
	    throws java.security.spec.InvalidKeySpecException {

	if (keySpec != null && !(keySpec instanceof KeySpec)) {
	    if (keySpec instanceof java.security.spec.X509EncodedKeySpec) {
		KeySpec encKeySpec = new X509EncodedKeySpec(
			(java.security.spec.X509EncodedKeySpec) keySpec);
		return generatePublic(encKeySpec);
	    }

	    throw new java.security.spec.InvalidKeySpecException();
	}

	return generatePublic((KeySpec) keySpec);
    }

    /**
     * JCA adapter for FlexiAPI method generatePrivate(): generate a private key
     * object from the provided key specification (key material).
     * 
     * @param keySpec
     *                the specification (key material) of the private key
     * @return the private key
     * @throws java.security.spec.InvalidKeySpecException
     *                 if the given key specification is inappropriate for this
     *                 key factory to produce a private key.
     */
    protected java.security.PrivateKey engineGeneratePrivate(
	    java.security.spec.KeySpec keySpec)
	    throws java.security.spec.InvalidKeySpecException {

	if (keySpec != null && !(keySpec instanceof KeySpec)) {
	    if (keySpec instanceof java.security.spec.PKCS8EncodedKeySpec) {
		KeySpec encKeySpec = new PKCS8EncodedKeySpec(
			(java.security.spec.PKCS8EncodedKeySpec) keySpec);
		return generatePrivate(encKeySpec);
	    }

	    throw new java.security.spec.InvalidKeySpecException();
	}

	return generatePrivate((KeySpec) keySpec);
    }

    /**
     * JCA adapter for FlexiAPI method getKeySpec(): return a specification (key
     * material) of the given key object. <tt>keySpec</tt> identifies the
     * specification class in which the key material should be returned. It
     * could, for example, be <tt>DSAPublicKeySpec.class</tt>, to indicate
     * that the key material should be returned in an instance of the
     * <tt>DSAPublicKeySpec</tt> class.
     * 
     * @param key
     *                the key
     * @param keySpec
     *                the specification class in which the key material should
     *                be returned
     * @return the underlying key specification (key material) in an instance of
     *         the requested specification class
     * @throws java.security.spec.InvalidKeySpecException
     *                 if the requested key specification is inappropriate for
     *                 the given key, or the given key cannot be dealt with
     *                 (e.g., the given key has an unrecognized format).
     */
    protected final java.security.spec.KeySpec engineGetKeySpec(
	    java.security.Key key, Class keySpec)
	    throws java.security.spec.InvalidKeySpecException {

	if (!(key instanceof Key)) {
	    throw new java.security.spec.InvalidKeySpecException();
	}

	return getKeySpec((Key) key, keySpec);
    }

    /**
     * JCA adapter for FlexiAPI method translateKey(): translate a key object,
     * whose provider may be unknown or potentially untrusted, into a
     * corresponding key object of this key factory.
     * 
     * @param key
     *                the key whose provider is unknown or untrusted
     * @return the translated key
     * @throws java.security.InvalidKeyException
     *                 if the given key cannot be processed by this key factory.
     */
    protected final java.security.Key engineTranslateKey(java.security.Key key)
	    throws java.security.InvalidKeyException {

	if (!(key instanceof Key)) {
	    throw new java.security.InvalidKeyException();
	}

	return translateKey((Key) key);
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * Generate a public key object from the provided key specification (key
     * material).
     * 
     * @param keySpec
     *                the specification (key material) of the public key
     * @return the public key
     * @throws InvalidKeySpecException
     *                 if the given key specification is inappropriate for this
     *                 key factory to produce a public key.
     */
    public abstract PublicKey generatePublic(KeySpec keySpec)
	    throws InvalidKeySpecException;

    /**
     * Generate a private key object from the provided key specification (key
     * material).
     * 
     * @param keySpec
     *                the specification (key material) of the private key
     * @return the private key
     * @throws InvalidKeySpecException
     *                 if the given key specification is inappropriate for this
     *                 key factory to produce a private key.
     */
    public abstract PrivateKey generatePrivate(KeySpec keySpec)
	    throws InvalidKeySpecException;

    /**
     * Return a specification (key material) of the given key object.
     * <tt>keySpec</tt> identifies the specification class in which the key
     * material should be returned. It could, for example, be
     * <tt>DSAPublicKeySpec.class</tt>, to indicate that the key material
     * should be returned in an instance of the <tt>DSAPublicKeySpec</tt>
     * class.
     * 
     * @param key
     *                the key
     * @param keySpec
     *                the specification class in which the key material should
     *                be returned
     * @return the underlying key specification (key material) in an instance of
     *         the requested specification class
     * @throws InvalidKeySpecException
     *                 if the requested key specification is inappropriate for
     *                 the given key, or the given key cannot be dealt with
     *                 (e.g., the given key has an unrecognized format).
     */
    public abstract KeySpec getKeySpec(Key key, Class keySpec)
	    throws InvalidKeySpecException;

    /**
     * Translate a key object, whose provider may be unknown or potentially
     * untrusted, into a corresponding key object of this key factory.
     * 
     * @param key
     *                the key whose provider is unknown or untrusted
     * @return the translated key
     * @throws InvalidKeyException
     *                 if the given key cannot be processed by this key factory.
     */
    public abstract Key translateKey(Key key) throws InvalidKeyException;

}

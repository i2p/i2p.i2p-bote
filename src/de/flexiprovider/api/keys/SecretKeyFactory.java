package de.flexiprovider.api.keys;

import javax.crypto.SecretKeyFactorySpi;

import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.exceptions.InvalidKeySpecException;

public abstract class SecretKeyFactory extends SecretKeyFactorySpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    protected javax.crypto.SecretKey engineGenerateSecret(
	    java.security.spec.KeySpec keySpec)
	    throws java.security.spec.InvalidKeySpecException {

	if (keySpec == null) {
	    throw new java.security.spec.InvalidKeySpecException();
	}

	if (!(keySpec instanceof KeySpec)) {
	    if (keySpec instanceof javax.crypto.spec.SecretKeySpec) {
		javax.crypto.spec.SecretKeySpec javaSpec = (javax.crypto.spec.SecretKeySpec) keySpec;
		KeySpec secretKeySpec = new SecretKeySpec(
			javaSpec.getEncoded(), javaSpec.getAlgorithm());
		return generateSecret(secretKeySpec);
	    }

	    throw new java.security.spec.InvalidKeySpecException();
	}

	return generateSecret((KeySpec) keySpec);
    }

    protected java.security.spec.KeySpec engineGetKeySpec(
	    javax.crypto.SecretKey key, Class keySpec)
	    throws java.security.spec.InvalidKeySpecException {

	if ((key == null) || (keySpec == null) || !(key instanceof SecretKey)) {
	    throw new java.security.spec.InvalidKeySpecException();
	}
	return getKeySpec((SecretKey) key, keySpec);
    }

    protected javax.crypto.SecretKey engineTranslateKey(
	    javax.crypto.SecretKey key)
	    throws java.security.InvalidKeyException {

	if ((key == null) || !(key instanceof SecretKey)) {
	    throw new java.security.InvalidKeyException();
	}
	return translateKey((SecretKey) key);
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    public abstract SecretKey generateSecret(KeySpec keySpec)
	    throws InvalidKeySpecException;

    public abstract KeySpec getKeySpec(SecretKey key, Class keySpec)
	    throws InvalidKeySpecException;

    public abstract SecretKey translateKey(SecretKey key)
	    throws InvalidKeyException;

}

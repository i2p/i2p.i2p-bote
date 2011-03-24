package de.flexiprovider.api.keys;

/**
 * This class is a simple holder for a key pair (a public key and a private
 * key). It does not enforce any security, and, when initialized, should be
 * treated like a private key.
 */
public final class KeyPair {

    java.security.KeyPair pair;

    /**
     * Construct a key pair from the given public key and private key.
     * <p>
     * Note that this constructor only stores references to the public and
     * private key components in the generated key pair. This is safe, because
     * <tt>Key</tt> objects are immutable.
     * 
     * @param publicKey
     *                the public key.
     * 
     * @param privateKey
     *                the private key.
     */
    public KeyPair(PublicKey publicKey, PrivateKey privateKey) {
	pair = new java.security.KeyPair(publicKey, privateKey);
    }

    /**
     * Return a reference to the public key component of this key pair.
     * 
     * @return a reference to the public key.
     */
    public PublicKey getPublic() {
	return (PublicKey) pair.getPublic();
    }

    /**
     * Return a reference to the private key component of this key pair.
     * 
     * @return a reference to the private key.
     */
    public PrivateKey getPrivate() {
	return (PrivateKey) pair.getPrivate();
    }

}

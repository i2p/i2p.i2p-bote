package de.flexiprovider.api.keys;

public class SecretKeySpec extends javax.crypto.spec.SecretKeySpec implements
	KeySpec {

    public SecretKeySpec(byte[] key, String algorithm) {
	super(key, algorithm);
    }

    public SecretKeySpec(byte[] key, int offset, int len, String algorithm) {
	super(key, offset, len, algorithm);
    }

}

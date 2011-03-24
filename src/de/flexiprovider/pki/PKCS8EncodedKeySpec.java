package de.flexiprovider.pki;

public class PKCS8EncodedKeySpec extends java.security.spec.PKCS8EncodedKeySpec
	implements EncodedKeySpec {

    public PKCS8EncodedKeySpec(byte[] encodedKey) {
	super(encodedKey);
    }

    public PKCS8EncodedKeySpec(
	    java.security.spec.PKCS8EncodedKeySpec javaKeySpec) {
	super(javaKeySpec.getEncoded());
    }

}

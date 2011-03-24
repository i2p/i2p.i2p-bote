package de.flexiprovider.pki;

public class X509EncodedKeySpec extends java.security.spec.X509EncodedKeySpec
	implements EncodedKeySpec {

    public X509EncodedKeySpec(byte[] encodedKey) {
	super(encodedKey);
    }

    public X509EncodedKeySpec(java.security.spec.X509EncodedKeySpec javaKeySpec) {
	super(javaKeySpec.getEncoded());
    }

}

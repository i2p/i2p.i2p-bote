package net.sf.ntru;

public class EncryptionPublicKey {
    private EncryptionParameters params;
    IntegerPolynomial h;

    EncryptionPublicKey(IntegerPolynomial h, EncryptionParameters params) {
        this.h = h;
        this.params = params;
    }
    
    public EncryptionPublicKey(byte[] b, EncryptionParameters params) {
        this.params = params;
        h = IntegerPolynomial.fromBinary(b, params.N, params.q);
    }
    
    public byte[] getEncoded() {
        return h.toBinary(params.q);
    }
}
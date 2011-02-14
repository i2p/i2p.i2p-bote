package net.sf.ntru;

public class SignaturePublicKey {
    private SignatureParameters params;
    IntegerPolynomial h;

    SignaturePublicKey(IntegerPolynomial h, SignatureParameters params) {
        this.h = h;
        this.params = params;
    }
    
    public SignaturePublicKey(byte[] b, SignatureParameters params) {
        h = IntegerPolynomial.fromBinary(b, params.N, params.q);
        this.params = params;
    }
    
    public byte[] getEncoded() {
        return h.toBinary(params.q);
    }
}
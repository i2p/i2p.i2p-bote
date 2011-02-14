package net.sf.ntru;

public class EncryptionPrivateKey {
    private EncryptionParameters params;
    IntegerPolynomial f;

    EncryptionPrivateKey(IntegerPolynomial f, EncryptionParameters params) {
        this.f = f;
        this.params = params;
    }
    
    public EncryptionPrivateKey(byte[] b, EncryptionParameters params) {
        this.params = params;
        f = IntegerPolynomial.fromBinary3Arith(b, params.N);
        f.modCenter(params.q);
    }
    
    public byte[] getEncoded() {
        return f.toBinary3Arith();
    }
}
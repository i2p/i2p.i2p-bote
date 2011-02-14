package net.sf.ntru;

public class SignatureKeyPair {
    public SignaturePrivateKey priv;
    public SignaturePublicKey pub;
    
    public SignatureKeyPair(SignaturePrivateKey priv, SignaturePublicKey pub) {
        this.priv = priv;
        this.pub = pub;
    }
}
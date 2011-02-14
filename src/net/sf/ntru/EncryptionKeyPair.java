package net.sf.ntru;

public class EncryptionKeyPair {
    public EncryptionPrivateKey priv;
    public EncryptionPublicKey pub;
    
    public EncryptionKeyPair(EncryptionPrivateKey priv, EncryptionPublicKey pub) {
        this.priv = priv;
        this.pub = pub;
    }
}
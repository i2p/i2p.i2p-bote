package i2p.bote.io;

import i2p.bote.Util;

/**
 * Contains a symmetric encryption key derived from a password,
 * and the parameters involved in deriving the key (i.e. salt and
 * the number of iterations).
 */
public class DerivedKey {
    byte[] salt;
    int numIterations;
    byte[] key;
    
    DerivedKey(byte[] salt, int numIterations, byte[] key) {
        this.salt = salt;
        this.numIterations = numIterations;
        this.key = key;
    }
    
    void clear() {
        Util.zeroOut(salt);
        numIterations = 0;
        Util.zeroOut(key);
    }
    
    /** Makes a deep copy */
    @Override
    public DerivedKey clone() {
        return new DerivedKey(salt.clone(), numIterations, key.clone());
    }
}
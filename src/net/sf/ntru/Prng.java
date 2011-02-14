package net.sf.ntru;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// deterministic pseudo-random generator
public class Prng {
    private int counter;
    private byte[] seed;
    private MessageDigest hashAlg;
    
    Prng(byte[] seed) throws NoSuchAlgorithmException {
        counter = 0;
        this.seed = seed;
        hashAlg = MessageDigest.getInstance("SHA-512");
    }
    
    byte[] nextBytes(int n) {
        ByteBuffer buf = ByteBuffer.allocate(n);
        
        while (buf.hasRemaining()) {
            ByteBuffer cbuf = ByteBuffer.allocate(seed.length + 4);
            cbuf.put(seed);
            cbuf.putInt(counter);
            byte[] hash = hashAlg.digest(cbuf.array());
            
            if (buf.remaining() < hash.length)
                buf.put(hash, 0, buf.remaining());
            else
                buf.put(hash);
            counter++;
        }
        
        return buf.array();
    }
}
package net.sf.ntru;

public class EncryptionParameters {
    public static final EncryptionParameters EES1087EP2 = new EncryptionParameters(1087, 2048, 120, 170, 256, 13, 25, 14, new byte[] {0, 6, 3});   // security=256, optimized for key size
    public static final EncryptionParameters EES1171EP1 = new EncryptionParameters(1171, 2048, 106, 186, 256, 10, 20, 15, new byte[] {0, 6, 4});   // security=256, key size / speed tradeoff
    public static final EncryptionParameters EES1499EP1 = new EncryptionParameters(1499, 2048, 79, 247, 256, 13, 17, 19, new byte[] {0, 6, 5});   // security=256, optimized for speed
    
    int N, q, df, dr, dg, maxMsgLenBytes, db, bufferLenBits, bufferLenTrits, dm0, pkLen, c, minCallsR, minCallsMask;
    byte[] oid;
    
    public EncryptionParameters(int N, int q, int df, int maxMsgLenBytes, int db, int c, int minCallsR, int minCallsMask, byte[] oid) {
        this.N = N;
        this.q = q;
        this.df = df;
        dr = df;
        dg = N / 3;
        this.maxMsgLenBytes = maxMsgLenBytes;
        this.db = db;
        bufferLenBits = (N*3/2+7)/8*8;   // one byte more than p1363.1 says
        bufferLenTrits = N - 1;
        dm0 = df;
        pkLen = db / 2;
        this.c = c;
        this.minCallsR = minCallsR;
        this.minCallsMask = minCallsMask;
        this.oid = oid;
    }
}
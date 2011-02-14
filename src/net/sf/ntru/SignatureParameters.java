package net.sf.ntru;

public class SignatureParameters {
    // from http://grouper.ieee.org/groups/1363/WorkingGroup/presentations/NTRUSignParams-1363-0411.ps
    public static final SignatureParameters T157 = new SignatureParameters(157, 256, 29, 1, BasisType.TRANSPOSE, 0.38407, 150.02);   // gives less than 80 bits of security
    public static final SignatureParameters T349 = new SignatureParameters(349, 512, 75, 1, BasisType.TRANSPOSE, 0.18543, 368.62);   // gives less than 256 bits of security
    
    public enum BasisType {STANDARD, TRANSPOSE};
    
    int N, q, d, B;
    double betaSq, normBoundSq;
    BasisType basisType;
    
    public SignatureParameters(int N, int q, int d, int B, BasisType basisType, double beta, double normBound) {
        this.N = N;
        this.q = q;
        this.d = d;
        this.B = B;
        this.basisType = basisType;
        this.betaSq = beta * beta;
        this.normBoundSq = normBound * normBound;
    }
}
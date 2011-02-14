package net.sf.ntru;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import net.sf.ntru.SignatureParameters.BasisType;
import net.sf.ntru.SignaturePrivateKey.Basis;

public class NtruSign {
    
    public static SignatureKeyPair generateKeyPair(SignatureParameters params) {
        SignaturePrivateKey priv = new SignaturePrivateKey();
        SignaturePublicKey pub = null;
        for (int k=params.B; k>=0; k--) {
            Basis basis = createBasis(params);
            priv.add(basis);
            if (k == 0)
                pub = new SignaturePublicKey(basis.h, params);
        }
        SignatureKeyPair kp = new SignatureKeyPair(priv, pub);
        return kp;
    }

    public static byte[] sign(byte[] m, SignaturePrivateKey priv, SignaturePublicKey pub, SignatureParameters params) {
        int r = 0;
        IntegerPolynomial s;
        IntegerPolynomial i;
        do {
            r++;
            try {
                i = createMsgRep(m, r, params);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            s = sign(i, priv, pub, params);
        } while (!verify(i, s, pub.h, params));

        byte[] rawSig = s.toBinary(params.q);
        ByteBuffer sbuf = ByteBuffer.allocate(rawSig.length + 4);
        sbuf.put(rawSig);
        sbuf.putInt(r);
        return sbuf.array();
    }
    
    static IntegerPolynomial sign(IntegerPolynomial i, SignaturePrivateKey priv, SignaturePublicKey pub, SignatureParameters params) {
        int N = params.N;
        int q = params.q;
        int perturbationBases = params.B;
        
        IntegerPolynomial s = new IntegerPolynomial(N);
        int iLoop = perturbationBases;
        while (iLoop >= 1) {
            IntegerPolynomial f = priv.getBasis(iLoop).f;
            IntegerPolynomial fPrime = priv.getBasis(iLoop).fPrime;
            
            IntegerPolynomial y = i.mult(f);
            y.div(q);
            y = y.mult(fPrime);
            
            IntegerPolynomial x = i.mult(fPrime);
            x.div(q);
            x = x.mult(f);

            IntegerPolynomial si = y;
            si.sub(x);
            s.add(si);
            
            IntegerPolynomial hi = priv.getBasis(iLoop).h.clone();
            if (iLoop > 1)
                hi.sub(priv.getBasis(iLoop-1).h);
            else
                hi.sub(pub.h);
            i = si.mult(hi, q);
            
            iLoop--;
        }
        
        IntegerPolynomial f = priv.getBasis(0).f;
        IntegerPolynomial fPrime = priv.getBasis(0).fPrime;
        
        IntegerPolynomial y = i.mult(f);
        y.div(q);
        y = y.mult(fPrime);
        
        IntegerPolynomial x = i.mult(fPrime);
        x.div(q);
        x = x.mult(f);

        y.sub(x);
        s.add(y);
        s.modPositive(q);
        return s;
    }
    
    public static boolean verify(byte[] m, byte[] sig, SignaturePublicKey pub, SignatureParameters params) {
        ByteBuffer sbuf = ByteBuffer.wrap(sig);
        byte[] rawSig = new byte[sig.length - 4];
        sbuf.get(rawSig);
        IntegerPolynomial s = IntegerPolynomial.fromBinary(rawSig, params.N, params.q);
        int r = sbuf.getInt();
        try {
            return verify(createMsgRep(m, r, params), s, pub.h, params);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    static boolean verify(IntegerPolynomial i, IntegerPolynomial s, IntegerPolynomial h, SignatureParameters params) {
        int N = params.N;
        int q = params.q;
        double normBoundSq = params.normBoundSq;
        double betaSq = params.betaSq;
        
        IntegerPolynomial t = h.mult(s, q);
        IntegerPolynomial e2 = i.clone();
        e2.sub(t);
        e2.modPositive(q);

        shift(e2, q);
        s = s.clone();
        shift(s, q);
        
        int ssum = 0;
        int e2sum = 0;
        int squaresum = 0;
        for (int j=0; j<N; j++) {
            int sj = s.coeffs[j];
            ssum += sj;
            int e2j = e2.coeffs[j];
            e2sum += e2j;
            squaresum += sj*sj + e2j*e2j;
        }
        int centeredNormSq = squaresum - (ssum*ssum + e2sum*e2sum) / N;
        
        IntegerPolynomial hs = h.mult(s);
        hs.sub(i);
        hs.mod(q);
        centeredNormSq = s.centerNormSq(q) + (int)(betaSq * hs.centerNormSq(q));
        
        return centeredNormSq <= normBoundSq;
    }
    
    private static void shift(IntegerPolynomial p, int q) {
        int[] coeffs = p.coeffs.clone();
        Arrays.sort(coeffs);
        int maxrange = 0;
        int maxrangeStart = 0;
        for (int i=0; i<coeffs.length-1; i++) {
            int range = coeffs[i+1] - coeffs[i];
            maxrange = Math.max(range, maxrange);
            maxrangeStart = coeffs[i];
        }
        
        int pmin = Integer.MAX_VALUE;
        int m = -1;
        int pmax = Integer.MIN_VALUE;
        for (int j=0; j<p.coeffs.length; j++) {
            if (p.coeffs[j] < pmin) {
                pmin = p.coeffs[j];
                m = j;
            }
            pmax = Math.max(p.coeffs[j], pmax);
        }
        
        int j = q - pmax + pmin;
        int shift;
        if (j > maxrange)
//            shift = maxrange;
            shift = pmax+maxrange/2+q/2;
        else
            shift = maxrangeStart+maxrange/2+q/2;
//            shift = j;
        
        p.sub(shift);
        p.mod(q);
for(int i=0; i<coeffs.length; i++) {
    while (p.coeffs[i] < -q/2)
        p.coeffs[i] += q;
    while (p.coeffs[i] >= -q/2)
        p.coeffs[i] -= q;
}
    }
    
    static IntegerPolynomial createMsgRep(byte[] m, int r, SignatureParameters params) throws NoSuchAlgorithmException {
        int N = params.N;
        int q = params.q;
        
        int c = 31 - Integer.numberOfLeadingZeros(q);
        int B = (c+7) / 8;
        IntegerPolynomial i = new IntegerPolynomial(N);
        
        ByteBuffer cbuf = ByteBuffer.allocate(m.length + 4);
        cbuf.put(m);
        cbuf.putInt(r);
        Prng prng = new Prng(cbuf.array());
        
        for (int t=0; t<N; t++) {
            byte[] o = prng.nextBytes(B);
            int hi = o[o.length-1];
            hi >>= 8*B-c;
            hi <<= 8*B-c;
            o[o.length-1] = (byte)hi;
            
            ByteBuffer obuf = ByteBuffer.allocate(4);
            obuf.put(o);
            obuf.rewind();
            // reverse byte order so it matches the endianness of java ints
            i.coeffs[t] = Integer.reverseBytes(obuf.getInt());
        }
        return i;
    }
    
    private static Basis createBasis(SignatureParameters params) {
        int N = params.N;
        int q = params.q;
        int d = params.d;
        BasisType basisType = params.basisType;
        
        IntegerPolynomial f;
        IntegerPolynomial g;
        IntegerPolynomial fq;
        Resultant rf;
        Resultant rg;
        BigIntEuclidean r;
        
        int _2n1 = 2*N+1;
        boolean isPrime = BigInteger.valueOf(N).isProbablePrime(1000) && BigInteger.valueOf(_2n1).isProbablePrime(1000);
        
        do {
            f = IntegerPolynomial.generateRandomSmall(N, d+1, d);
            fq = f.invertFq(q);
        } while (fq == null);
        rf = f.resultant();
        
        do {
            do {
                g = IntegerPolynomial.generateRandomSmall(N, d+1, d);
            } while (isPrime && f.resultant(_2n1).res.equals(ZERO) && g.resultant(_2n1).res.equals(ZERO));
            rg = g.resultant();
            r = BigIntEuclidean.calculate(rf.res, rg.res);
        } while (!r.gcd.equals(ONE));
        
        BigIntPolynomial F = rg.rho;
        F.mult(r.y.negate());
        F.mult(q);
        BigIntPolynomial G = rf.rho;
        G.mult(r.x);
        G.mult(q);
        
        int[] fRevCoeffs = new int[N];
        int[] gRevCoeffs = new int[N];
        fRevCoeffs[0] = f.coeffs[0];
        gRevCoeffs[0] = g.coeffs[0];
        for (int i=1; i<N; i++) {
            fRevCoeffs[i] = f.coeffs[N-i];
            gRevCoeffs[i] = g.coeffs[N-i];
        }
        IntegerPolynomial fRev = new IntegerPolynomial(fRevCoeffs);
        IntegerPolynomial gRev = new IntegerPolynomial(gRevCoeffs);
        
        IntegerPolynomial t = f.mult(fRev);
        t.add(g.mult(gRev));
        Resultant rt = t.resultant();
        BigIntPolynomial c = new BigIntPolynomial(fRev).mult(F);
        c.add(new BigIntPolynomial(gRev).mult(G));
        c = c.mult(rt.rho);
        c.div(rt.res);
        F.sub(c.mult(f));
        G.sub(c.mult(g));

        if (!equalsQ(f, g, F, G, q, N))
            throw new RuntimeException("this shouldn't happen");
        
        BigIntPolynomial fPrime;
        BigIntPolynomial h;
        if (basisType == BasisType.STANDARD) {
            fPrime = F;
            h = new BigIntPolynomial(g).mult(new BigIntPolynomial(fq), BigInteger.valueOf(q));
        }
        else {
            fPrime = new BigIntPolynomial(g);
            h = F.mult(new BigIntPolynomial(fq), BigInteger.valueOf(q));
        }
        IntegerPolynomial h_ = new IntegerPolynomial(h);
        h_.modPositive(q);
        
        return new Basis(f, new IntegerPolynomial(fPrime), h_, params);
    }
    
    // verifies that f*G-g*F=q
    private static boolean equalsQ(IntegerPolynomial f, IntegerPolynomial g, BigIntPolynomial F, BigIntPolynomial G, int q, int N) {
        G = new BigIntPolynomial(java.util.Arrays.copyOf(G.coeffs, N));
        BigIntPolynomial x = new BigIntPolynomial(f).mult(G);
        x.sub(new BigIntPolynomial(g).mult(F));
        boolean equalsQ=true;
        for (int i=1; i<x.coeffs.length-1; i++)
            equalsQ &= ZERO.equals(x.coeffs[i]);
        equalsQ &= x.coeffs[0].equals(BigInteger.valueOf(q));
        return equalsQ;
    }
}
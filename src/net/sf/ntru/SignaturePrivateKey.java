package net.sf.ntru;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.sf.ntru.SignatureParameters.BasisType;

public class SignaturePrivateKey {
    private List<Basis> bases;
    
    public SignaturePrivateKey(byte[] b, SignatureParameters params) {
        bases = new ArrayList<Basis>();
        ByteBuffer buf = ByteBuffer.wrap(b);
        for (int i=0; i<=params.B; i++)
            // include a public key h[i] in all bases except for the first one
            add(new Basis(buf, params, i!=0));
    }
    
    SignaturePrivateKey() {
        bases = new ArrayList<Basis>();
    }
    
    void add(Basis b) {
        bases.add(b);
    }
    
    // Returns the i-th basis
    Basis getBasis(int i) {
        return bases.get(i);
    }
    
    public byte[] getEncoded() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (int i=0; i<bases.size(); i++)
            try {
                // all bases except for the first one contain a public key
                bases.get(i).encode(os, i!=0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        return os.toByteArray();
    }
    
    static class Basis {
        IntegerPolynomial f;
        IntegerPolynomial fPrime;
        IntegerPolynomial h;
        SignatureParameters params;
        
        Basis(IntegerPolynomial f, IntegerPolynomial fPrime, IntegerPolynomial h, SignatureParameters params) {
            this.f = f;
            this.fPrime = fPrime;
            this.h = h;
            this.params = params;
        }
        
        Basis(ByteBuffer buf, SignatureParameters params, boolean include_h) {
            int N = params.N;
            int q = params.q;
            this.params = params;
            
            f = IntegerPolynomial.fromBinary3Arith(buf, N);
            if (params.basisType == BasisType.STANDARD) {
                fPrime = IntegerPolynomial.fromBinary(buf, N, q);
                for (int i=0; i<fPrime.coeffs.length; i++)
                    fPrime.coeffs[i] -= q/2;
            }
            else
                fPrime = IntegerPolynomial.fromBinary3Arith(buf, N);
            if (include_h)
                h = IntegerPolynomial.fromBinary(buf, N, q);
        }
        
        void encode(OutputStream os, boolean include_h) throws IOException {
            int q = params.q;
            
            os.write(f.toBinary3Arith());
            if (params.basisType == BasisType.STANDARD) {
                for (int i=0; i<fPrime.coeffs.length; i++)
                    fPrime.coeffs[i] += q/2;
                os.write(fPrime.toBinary(q));
            }
            else
                os.write(fPrime.toBinary3Arith());
            if (include_h)
                os.write(h.toBinary(q));
        }
    }
}
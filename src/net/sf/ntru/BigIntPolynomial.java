package net.sf.ntru;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class BigIntPolynomial {
    BigInteger[] coeffs;
    
    BigIntPolynomial(int N) {
        coeffs = new BigInteger[N];
        for (int i=0; i<N; i++)
            coeffs[i] = ZERO;
    }
    
    BigIntPolynomial(IntegerPolynomial p) {
        coeffs = new BigInteger[p.coeffs.length];
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] = BigInteger.valueOf(p.coeffs[i]);
    }
    
    BigIntPolynomial(BigInteger[] coeffs) {
        this.coeffs = coeffs;
    }
    
    // Generates a random polynomial with numOnes coefficients equal to 1,
    // numNegOnes coefficients equal to -1, and the rest equal to 0.
    static BigIntPolynomial generateRandomSmall(int N, int numOnes, int numNegOnes) {
        List<BigInteger> coeffs = new ArrayList<BigInteger>();
        for (int i=0; i<numOnes; i++)
            coeffs.add(ONE);
        for (int i=0; i<numNegOnes; i++)
            coeffs.add(BigInteger.valueOf(-1));
        while (coeffs.size() < N)
            coeffs.add(ZERO);
        Collections.shuffle(coeffs, new SecureRandom());
        
        BigIntPolynomial poly = new BigIntPolynomial(N);
        for (int i=0; i<coeffs.size(); i++)
            poly.coeffs[i] = coeffs.get(i);
        return poly;
    }
    
    BigIntPolynomial mult(BigIntPolynomial poly2, BigInteger modulus) {
        BigIntPolynomial c = mult(poly2);
        c.mod(modulus);
        return c;
    }
    
    BigIntPolynomial mult(IntegerPolynomial poly2) {
        return mult(new BigIntPolynomial(poly2));
    }
    
    /** Multiplies the polynomial with another, taking the indices mod N */
    BigIntPolynomial mult(BigIntPolynomial poly2) {
        int N = coeffs.length;
        if (poly2.coeffs.length != N)
            throw new RuntimeException("Number of coefficients must be the same");
        
        BigIntPolynomial c = multRecursive(poly2);
        
        if (c.coeffs.length > N) {
            for (int k=N; k<c.coeffs.length; k++)
                c.coeffs[k-N] = c.coeffs[k-N].add(c.coeffs[k]);
            c.coeffs = Arrays.copyOf(c.coeffs, N);
        }
        return c;
    }
    
    /** Karazuba multiplication */
    private BigIntPolynomial multRecursive(BigIntPolynomial poly2) {
        BigInteger[] a = coeffs;
        BigInteger[] b = poly2.coeffs;
        
        int n = poly2.coeffs.length;
        if (n <= 1) {
            BigInteger[] c = coeffs.clone();
            for (int i=0; i<coeffs.length; i++)
                c[i] = c[i].multiply(poly2.coeffs[0]);
            return new BigIntPolynomial(c);
        }
        else {
            int n1 = n / 2;
            
            BigIntPolynomial a1 = new BigIntPolynomial(Arrays.copyOf(a, n1));
            BigIntPolynomial a2 = new BigIntPolynomial(Arrays.copyOfRange(a, n1, n));
            BigIntPolynomial b1 = new BigIntPolynomial(Arrays.copyOf(b, n1));
            BigIntPolynomial b2 = new BigIntPolynomial(Arrays.copyOfRange(b, n1, n));
            
            BigIntPolynomial A = a1.clone();
            A.add(a2);
            BigIntPolynomial B = b1.clone();
            B.add(b2);
            
            BigIntPolynomial c1 = a1.multRecursive(b1);
            BigIntPolynomial c2 = a2.multRecursive(b2);
            BigIntPolynomial c3 = A.multRecursive(B);
            c3.sub(c1);
            c3.sub(c2);
            
            BigIntPolynomial c = new BigIntPolynomial(2*n-1);
            for (int i=0; i<c1.coeffs.length; i++)
                c.coeffs[i] = c1.coeffs[i];
            for (int i=0; i<c3.coeffs.length; i++)
                c.coeffs[n1+i] = c.coeffs[n1+i].add(c3.coeffs[i]);
            for (int i=0; i<c2.coeffs.length; i++)
                c.coeffs[2*n1+i] = c.coeffs[2*n1+i].add(c2.coeffs[i]);
            return c;
        }
    }
    
    /** O(n²) multiplication */
    BigIntPolynomial multSimple(BigIntPolynomial poly2) {
        BigInteger[] a = coeffs;
        BigInteger[] b = poly2.coeffs;
        if (b.length != a.length)
            throw new RuntimeException("Number of coefficients must be the same");
        int N = a.length;
        BigInteger[] c = new BigInteger[N];
        
        for(int k=N-1; k>=0; k--)
        {
            c[k] = BigInteger.ZERO;
            int j = k + 1;
            for(int i=N-1; i>=0; i--)
            {
                if(j == N)
                    j = 0;
                if(!a[i].equals(BigInteger.ZERO) && !b[j].equals(BigInteger.ZERO))
                    c[k] = c[k].add(a[i].multiply(b[j]));
                j++;
            }
        }
        return new BigIntPolynomial(c);
    }
    
    void add(BigIntPolynomial b, BigInteger modulus) {
        add(b);
        mod(modulus);
    }
    
    /** Adds another polynomial which can have a different number of coefficients */
    void add(BigIntPolynomial b) {
      if (b.coeffs.length > coeffs.length) {
          int N = coeffs.length;
          coeffs = Arrays.copyOf(coeffs, b.coeffs.length);
          for (int i=N; i<coeffs.length; i++)
              coeffs[i] = ZERO;
      }
      for (int i=0; i<b.coeffs.length; i++)
          coeffs[i] = coeffs[i].add(b.coeffs[i]);
    }
    
    /** Subtracts another polynomial which can have a different number of coefficients */
    void sub(BigIntPolynomial b) {
        if (b.coeffs.length > coeffs.length) {
            int N = coeffs.length;
            coeffs = Arrays.copyOf(coeffs, b.coeffs.length);
            for (int i=N; i<coeffs.length; i++)
                coeffs[i] = ZERO;
        }
        for (int i=0; i<b.coeffs.length; i++)
            coeffs[i] = coeffs[i].subtract(b.coeffs[i]);
    }
    
    void mult(BigInteger factor) {
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] = coeffs[i].multiply(factor);
    }
    
    void mult(int factor) {
        mult(BigInteger.valueOf(factor));
    }
    
    void div(BigInteger divisor) {
        BigInteger d = divisor.add(ONE).divide(BigInteger.valueOf(2));
        for (int i=0; i<coeffs.length; i++) {
            coeffs[i] = coeffs[i].compareTo(ZERO)>0 ? coeffs[i].add(d) : coeffs[i].add(d.negate());
            coeffs[i] = coeffs[i].divide(divisor);
        }
    }
    
    void mod(BigInteger modulus) {
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] = coeffs[i].mod(modulus);
    }
    
    BigInteger sumCoeffs() {
        BigInteger sum = ZERO;
        for (int i=0; i<coeffs.length; i++)
            sum = sum.add(coeffs[i]);
        return sum;
    }
    
    void clear() {
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] = ZERO;
    }
    
    @Override
    public BigIntPolynomial clone() {
        return new BigIntPolynomial(coeffs.clone());
    }
}
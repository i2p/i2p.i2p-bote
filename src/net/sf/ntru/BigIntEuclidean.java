package net.sf.ntru;

import java.math.BigInteger;

/** Extended Euclidean Algorithm in BigIntegers */
public class BigIntEuclidean {
    BigInteger x, y, gcd;
    
    private BigIntEuclidean() {
    }
    
    // from pseudocode at http://en.wikipedia.org/wiki/Extended_Euclidean_algorithm
    static BigIntEuclidean calculate(BigInteger a, BigInteger b) {
        BigInteger x = BigInteger.ZERO;
        BigInteger lastx = BigInteger.ONE;
        BigInteger y = BigInteger.ONE;
        BigInteger lasty = BigInteger.ZERO;
        while (!b.equals(BigInteger.ZERO)) {
            BigInteger quotient = a.divide(b);
            
            BigInteger temp = a;
            a = b;
            b = temp.mod(b);
            
            temp = x;
            x = lastx.subtract(quotient.multiply(x));
            lastx = temp;
            
            temp = y;
            y = lasty.subtract(quotient.multiply(y));
            lasty = temp;
        }
        
        BigIntEuclidean result = new BigIntEuclidean();
        result.x = lastx;
        result.y = lasty;
        result.gcd = a;
        return result;
    }
}
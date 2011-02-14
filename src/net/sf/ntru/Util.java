package net.sf.ntru;

public class Util {

    /** Calculates the inverse of n mod modulus */
    static int invert(int n, int modulus) {
        n %= modulus;
        if (n < 0)
            n += modulus;
        return IntEuclidean.calculate(n, modulus).x;
    }
    
    /** Calculates a^b mod modulus */
    static int pow(int a, int b, int modulus) {
        int p = 1;
        for (int i=0; i<b; i++)
            p = (p*a) % modulus;
        return p;
    }
}
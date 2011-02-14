package net.sf.ntru;

import java.math.BigInteger;

class Resultant {
    BigIntPolynomial rho;
    BigInteger res;
    
    Resultant(BigIntPolynomial rho, BigInteger res) {
        this.rho = rho;
        this.res = res;
    }
}
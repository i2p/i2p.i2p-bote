package net.sf.ntru;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class IntegerPolynomial {
    /**
     * http://stackoverflow.com/questions/1562548/how-to-make-a-message-into-a-polynomial
     * 
     * Convert each three-bit quantity to two ternary coefficients as follows, and concatenate the resulting
     * ternary quantities to obtain [the output].
     * 
     * {0, 0, 0} -> {0, 0}
     * {0, 0, 1} -> {0, 1}
     * {0, 1, 0} -> {0, -1}
     * {0, 1, 1} -> {1, 0}
     * {1, 0, 0} -> {1, 1}
     * {1, 0, 1} -> {1, -1}
     * {1, 1, 0} -> {-1, 0}
     * {1, 1, 1} -> {-1, 1}
     */
    static final int[] COEFF1_TABLE = {0, 0, 0, 1, 1, 1, -1, -1};
    static final int[] COEFF2_TABLE = {0, 1, -1, 0, 1, -1, 0, 1};
    /**
     * http://stackoverflow.com/questions/1562548/how-to-make-a-message-into-a-polynomial
     * 
     * Convert each set of two ternary coefficients to three bits as follows, and concatenate the resulting bit
     * quantities to obtain [the output]:
     * 
     * {-1, -1} -> set "fail" to 1 and set bit string to {1, 1, 1}
     * {-1, 0} -> {1, 1, 0}
     * {-1, 1} -> {1, 1, 1}
     * {0, -1} -> {0, 1, 0}
     * {0, 0} -> {0, 0, 0}
     * {0, 1} -> {0, 0, 1}
     * {1, -1} -> {1, 0, 1}
     * {1, 0} -> {0, 1, 1}
     * {1, 1} -> {1, 0, 0}
     */
    static final int[] BIT1_TABLE = {1, 1, 1, 0, 0, 0, 1, 0, 1};
    static final int[] BIT2_TABLE = {1, 1, 1, 1, 0, 0, 0, 1, 0};
    static final int[] BIT3_TABLE = {1, 0, 1, 0, 0, 1, 1, 1, 0};
    // prime numbers > 10^4
    private static final List<BigInteger> PRIMES = Arrays.asList(new BigInteger[] {
            new BigInteger("10007"), new BigInteger("10009"), new BigInteger("10037"), new BigInteger("10039"), new BigInteger("10061"),
            new BigInteger("10067"), new BigInteger("10069"), new BigInteger("10079"), new BigInteger("10091"), new BigInteger("10093"),
            new BigInteger("10099"), new BigInteger("10103"), new BigInteger("10111"), new BigInteger("10133"), new BigInteger("10139"),
            new BigInteger("10141"), new BigInteger("10151"), new BigInteger("10159"), new BigInteger("10163"), new BigInteger("10169"),
            new BigInteger("10177"), new BigInteger("10181"), new BigInteger("10193"), new BigInteger("10211"), new BigInteger("10223"),
            new BigInteger("10243"), new BigInteger("10247"), new BigInteger("10253"), new BigInteger("10259"), new BigInteger("10267"),
            new BigInteger("10271"), new BigInteger("10273"), new BigInteger("10289"), new BigInteger("10301"), new BigInteger("10303"),
            new BigInteger("10313"), new BigInteger("10321"), new BigInteger("10331"), new BigInteger("10333"), new BigInteger("10337"),
            new BigInteger("10343"), new BigInteger("10357"), new BigInteger("10369"), new BigInteger("10391"), new BigInteger("10399"),
            new BigInteger("10427"), new BigInteger("10429"), new BigInteger("10433"), new BigInteger("10453"), new BigInteger("10457"),
            new BigInteger("10459"), new BigInteger("10463"), new BigInteger("10477"), new BigInteger("10487"), new BigInteger("10499"),
            new BigInteger("10501"), new BigInteger("10513"), new BigInteger("10529"), new BigInteger("10531"), new BigInteger("10559"),
            new BigInteger("10567"), new BigInteger("10589"), new BigInteger("10597"), new BigInteger("10601"), new BigInteger("10607"),
            new BigInteger("10613"), new BigInteger("10627"), new BigInteger("10631"), new BigInteger("10639"), new BigInteger("10651"),
            new BigInteger("10657"), new BigInteger("10663"), new BigInteger("10667"), new BigInteger("10687"), new BigInteger("10691"),
            new BigInteger("10709"), new BigInteger("10711"), new BigInteger("10723"), new BigInteger("10729"), new BigInteger("10733"),
            new BigInteger("10739"), new BigInteger("10753"), new BigInteger("10771"), new BigInteger("10781"), new BigInteger("10789"),
            new BigInteger("10799"), new BigInteger("10831"), new BigInteger("10837"), new BigInteger("10847"), new BigInteger("10853"),
            new BigInteger("10859"), new BigInteger("10861"), new BigInteger("10867"), new BigInteger("10883"), new BigInteger("10889"),
            new BigInteger("10891"), new BigInteger("10903"), new BigInteger("10909"), new BigInteger("10937"), new BigInteger("10939"),
            new BigInteger("10949"), new BigInteger("10957"), new BigInteger("10973"), new BigInteger("10979"), new BigInteger("10987"),
            new BigInteger("10993"), new BigInteger("11003"), new BigInteger("11027"), new BigInteger("11047"), new BigInteger("11057"),
            new BigInteger("11059"), new BigInteger("11069"), new BigInteger("11071"), new BigInteger("11083"), new BigInteger("11087"),
            new BigInteger("11093"), new BigInteger("11113"), new BigInteger("11117"), new BigInteger("11119"), new BigInteger("11131"),
            new BigInteger("11149"), new BigInteger("11159"), new BigInteger("11161"), new BigInteger("11171"), new BigInteger("11173"),
            new BigInteger("11177"), new BigInteger("11197"), new BigInteger("11213"), new BigInteger("11239"), new BigInteger("11243"),
            new BigInteger("11251"), new BigInteger("11257"), new BigInteger("11261"), new BigInteger("11273"), new BigInteger("11279"),
            new BigInteger("11287"), new BigInteger("11299"), new BigInteger("11311"), new BigInteger("11317"), new BigInteger("11321"),
            new BigInteger("11329"), new BigInteger("11351"), new BigInteger("11353"), new BigInteger("11369"), new BigInteger("11383"),
            new BigInteger("11393"), new BigInteger("11399"), new BigInteger("11411"), new BigInteger("11423"), new BigInteger("11437"),
            new BigInteger("11443"), new BigInteger("11447"), new BigInteger("11467"), new BigInteger("11471"), new BigInteger("11483"),
            new BigInteger("11489"), new BigInteger("11491"), new BigInteger("11497"), new BigInteger("11503"), new BigInteger("11519"),
            new BigInteger("11527"), new BigInteger("11549"), new BigInteger("11551"), new BigInteger("11579"), new BigInteger("11587"),
            new BigInteger("11593"), new BigInteger("11597"), new BigInteger("11617"), new BigInteger("11621"), new BigInteger("11633"),
            new BigInteger("11657"), new BigInteger("11677"), new BigInteger("11681"), new BigInteger("11689"), new BigInteger("11699"),
            new BigInteger("11701"), new BigInteger("11717"), new BigInteger("11719"), new BigInteger("11731"), new BigInteger("11743"),
            new BigInteger("11777"), new BigInteger("11779"), new BigInteger("11783"), new BigInteger("11789"), new BigInteger("11801"),
            new BigInteger("11807"), new BigInteger("11813"), new BigInteger("11821"), new BigInteger("11827"), new BigInteger("11831"),
            new BigInteger("11833"), new BigInteger("11839"), new BigInteger("11863"), new BigInteger("11867"), new BigInteger("11887"),
            new BigInteger("11897"), new BigInteger("11903"), new BigInteger("11909"), new BigInteger("11923"), new BigInteger("11927"),
            new BigInteger("11933"), new BigInteger("11939"), new BigInteger("11941"), new BigInteger("11953"), new BigInteger("11959"),
            new BigInteger("11969"), new BigInteger("11971"), new BigInteger("11981"), new BigInteger("11987"), new BigInteger("12007"),
            new BigInteger("12011"), new BigInteger("12037"), new BigInteger("12041"), new BigInteger("12043"), new BigInteger("12049"),
            new BigInteger("12071"), new BigInteger("12073"), new BigInteger("12097"), new BigInteger("12101"), new BigInteger("12107"),
            new BigInteger("12109"), new BigInteger("12113"), new BigInteger("12119"), new BigInteger("12143"), new BigInteger("12149"),
            new BigInteger("12157"), new BigInteger("12161"), new BigInteger("12163"), new BigInteger("12197"), new BigInteger("12203"),
            new BigInteger("12211"), new BigInteger("12227"), new BigInteger("12239"), new BigInteger("12241"), new BigInteger("12251"),
            new BigInteger("12253"), new BigInteger("12263"), new BigInteger("12269"), new BigInteger("12277"), new BigInteger("12281"),
            new BigInteger("12289"), new BigInteger("12301"), new BigInteger("12323"), new BigInteger("12329"), new BigInteger("12343"),
            new BigInteger("12347"), new BigInteger("12373"), new BigInteger("12377"), new BigInteger("12379"), new BigInteger("12391"),
            new BigInteger("12401"), new BigInteger("12409"), new BigInteger("12413"), new BigInteger("12421"), new BigInteger("12433"),
            new BigInteger("12437"), new BigInteger("12451"), new BigInteger("12457"), new BigInteger("12473"), new BigInteger("12479"),
            new BigInteger("12487"), new BigInteger("12491"), new BigInteger("12497"), new BigInteger("12503"), new BigInteger("12511"),
            new BigInteger("12517"), new BigInteger("12527"), new BigInteger("12539"), new BigInteger("12541"), new BigInteger("12547"),
            new BigInteger("12553"), new BigInteger("12569"), new BigInteger("12577"), new BigInteger("12583"), new BigInteger("12589"),
            new BigInteger("12601"), new BigInteger("12611"), new BigInteger("12613"), new BigInteger("12619"), new BigInteger("12637"),
            new BigInteger("12641"), new BigInteger("12647"), new BigInteger("12653"), new BigInteger("12659"), new BigInteger("12671"),
            new BigInteger("12689"), new BigInteger("12697"), new BigInteger("12703"), new BigInteger("12713"), new BigInteger("12721"),
            new BigInteger("12739"), new BigInteger("12743"), new BigInteger("12757"), new BigInteger("12763"), new BigInteger("12781"),
            new BigInteger("12791"), new BigInteger("12799"), new BigInteger("12809"), new BigInteger("12821"), new BigInteger("12823"),
            new BigInteger("12829"), new BigInteger("12841"), new BigInteger("12853"), new BigInteger("12889"), new BigInteger("12893"),
            new BigInteger("12899"), new BigInteger("12907"), new BigInteger("12911"), new BigInteger("12917"), new BigInteger("12919"),
            new BigInteger("12923"), new BigInteger("12941"), new BigInteger("12953"), new BigInteger("12959"), new BigInteger("12967"),
            new BigInteger("12973"), new BigInteger("12979"), new BigInteger("12983"), new BigInteger("13001"), new BigInteger("13003"),
            new BigInteger("13007"), new BigInteger("13009"), new BigInteger("13033"), new BigInteger("13037"), new BigInteger("13043"),
            new BigInteger("13049"), new BigInteger("13063"), new BigInteger("13093"), new BigInteger("13099"), new BigInteger("13103"),
            new BigInteger("13109"), new BigInteger("13121"), new BigInteger("13127"), new BigInteger("13147"), new BigInteger("13151"),
            new BigInteger("13159"), new BigInteger("13163"), new BigInteger("13171"), new BigInteger("13177"), new BigInteger("13183"),
            new BigInteger("13187"), new BigInteger("13217"), new BigInteger("13219"), new BigInteger("13229"), new BigInteger("13241"),
            new BigInteger("13249"), new BigInteger("13259"), new BigInteger("13267"), new BigInteger("13291"), new BigInteger("13297"),
            new BigInteger("13309"), new BigInteger("13313"), new BigInteger("13327"), new BigInteger("13331"), new BigInteger("13337"),
            new BigInteger("13339"), new BigInteger("13367"), new BigInteger("13381"), new BigInteger("13397"), new BigInteger("13399"),
    });
    
    int[] coeffs;
    
    IntegerPolynomial(int N) {
        coeffs = new int[N];
    }
    
    IntegerPolynomial(int[] coeffs) {
        this.coeffs = coeffs;
    }
    
    IntegerPolynomial(BigIntPolynomial p) {
        coeffs = new int[p.coeffs.length];
        for (int i=0; i<p.coeffs.length; i++)
            coeffs[i] = p.coeffs[i].intValue();
    }
    
    // Returns a polynomial with N coefficients between -1 and 1.
    static IntegerPolynomial fromBinary3(byte[] data, int N) {
        IntegerPolynomial poly = new IntegerPolynomial(N);
        int coeffIndex = 0;
        for (int bitIndex=0; bitIndex<data.length*8; ) {
            int bit1 = getBit(data, bitIndex++);
            int bit2 = getBit(data, bitIndex++);
            int bit3 = getBit(data, bitIndex++);
            int coeffTableIndex = bit1*4 + bit2*2 + bit3;
            poly.coeffs[coeffIndex++] = COEFF1_TABLE[coeffTableIndex];
            poly.coeffs[coeffIndex++] = COEFF2_TABLE[coeffTableIndex];
            // ignore bytes that can't fit
            if (coeffIndex > N-2)
                break;
        }
        return poly;
    }
    
    // Returns a polynomial with N coefficients between 0 and q-1. q must be a power of 2.
    static IntegerPolynomial fromBinary(byte[] data, int N, int q) {
        IntegerPolynomial poly = new IntegerPolynomial(N);
        int bitsPerCoeff = 31 - Integer.numberOfLeadingZeros(q);
        int numBits = N * bitsPerCoeff;
        int coeffIndex = 0;
        for (int bitIndex=0; bitIndex<numBits; bitIndex++) {
            if (bitIndex>0 && bitIndex%bitsPerCoeff==0)
                coeffIndex++;
            int bit = getBit(data, bitIndex);
            poly.coeffs[coeffIndex] += bit << (bitIndex%bitsPerCoeff);
        }
        return poly;
    }
    
    // Returns a polynomial with N coefficients between 0 and q-1. q must be a power of 2.
    static IntegerPolynomial fromBinary(ByteBuffer buf, int N, int q) {
        int qBits = 31 - Integer.numberOfLeadingZeros(q);
        int size = (N*qBits+7) / 8;
        byte[] arr = new byte[size];
        buf.get(arr);
        return fromBinary(arr, N, q);
    }
    
    private static int getBit(byte[] arr, int bitIndex) {
        int byteIndex = bitIndex / 8;
        int arrElem = arr[byteIndex] & 0xFF;
        return (arrElem >> (bitIndex%8)) & 1;
    }
    
    // Converts a polynomial whose coefficients are between -1 and 1, to binary.
    // coeffs[2*i] and coeffs[2*i+1] must not both equal -1 for any integer i,
    // so this method is only safe to use with polynomials produced by fromBinary3().
    byte[] toBinary3() {
        int numBits = (coeffs.length*3+2) / 2;
        int numBytes = (numBits+7) / 8;
        byte[] data = new byte[numBytes];
        int bitIndex = 0;
        int byteIndex = 0;
        for (int i=0; i<coeffs.length/2*2; ) {   // coeffs.length is an odd number, throw away the highest coeff
            int coeff1 = coeffs[i++] + 1;
            int coeff2 = coeffs[i++] + 1;
            if (coeff1==0 && coeff2==0)
                throw new RuntimeException("Illegal encoding!");
            int bitTableIndex = coeff1*3 + coeff2;
            int[] bits = new int[] {BIT1_TABLE[bitTableIndex], BIT2_TABLE[bitTableIndex], BIT3_TABLE[bitTableIndex]};
            for (int j=0; j<3; j++) {
                data[byteIndex] |= bits[j] << bitIndex;
                if (bitIndex == 7) {
                    bitIndex = 0;
                    byteIndex++;
                }
                else
                    bitIndex++;
            }
        }
        return data;
    }
    
    // Converts a polynomial whose coefficients are between -1 and 1, to binary.
    byte[] toBinary3Arith() {
        BigInteger sum = ZERO;
        for (int i=coeffs.length-1; i>=0; i--) {
            sum = sum.multiply(BigInteger.valueOf(3));
            sum = sum.add(BigInteger.valueOf(coeffs[i]+1));
        }
        
        int size = (BigInteger.valueOf(3).pow(coeffs.length).bitLength()+7) / 8;
        byte[] arr = sum.toByteArray();
        
        if (arr.length < size) {
            // pad with leading zeros so arr.length==size
            byte[] arr2 = new byte[size];
            System.arraycopy(arr, 0, arr2, size-arr.length, arr.length);
            return arr2;
        }
        
        if (arr.length > size)
            // drop sign bit
            arr = Arrays.copyOfRange(arr, 1, arr.length);
        return arr;
    }
    
    // Converts a byte array produced by toBinary3Arith() to a polynomial.
    static IntegerPolynomial fromBinary3Arith(byte[] b, int N) {
        BigInteger sum = new BigInteger(1, b);
        IntegerPolynomial p = new IntegerPolynomial(N);
        for (int i=0; i<N; i++) {
            p.coeffs[i] = sum.mod(BigInteger.valueOf(3)).intValue() - 1;
            if (p.coeffs[i] > 1)
                p.coeffs[i] -= 3;
            sum = sum.divide(BigInteger.valueOf(3));
        }
        return p;
    }
    
    static IntegerPolynomial fromBinary3Arith(ByteBuffer buf, int N) {
        int size = (int)Math.ceil(N * Math.log(3) / Math.log(2) / 8);
        byte[] arr = new byte[size];
        buf.get(arr);
        return fromBinary3Arith(arr, N);
    }
    
    // Converts a polynomial whose coefficients are between 0 and q, to binary. q must be a power of 2.
    byte[] toBinary(int q) {
        int bitsPerCoeff = 31 - Integer.numberOfLeadingZeros(q);
        int numBits = coeffs.length * bitsPerCoeff;
        int numBytes = (numBits+7) / 8;
        byte[] data = new byte[numBytes];
        int bitIndex = 0;
        int byteIndex = 0;
        for (int i=0; i<coeffs.length; i++) {
            for (int j=0; j<bitsPerCoeff; j++) {
                int currentBit = (coeffs[i] >> j) & 1;
                data[byteIndex] |= currentBit << bitIndex;
                if (bitIndex == 7) {
                    bitIndex = 0;
                    byteIndex++;
                }
                else
                    bitIndex++;
            }
        }
        return data;
    }
    
    // Generates a random polynomial with numOnes coefficients equal to 1,
    // numNegOnes coefficients equal to -1, and the rest equal to 0.
    static IntegerPolynomial generateRandomSmall(int N, int numOnes, int numNegOnes) {
        List<Integer> coeffs = new ArrayList<Integer>();
        for (int i=0; i<numOnes; i++)
            coeffs.add(1);
        for (int i=0; i<numNegOnes; i++)
            coeffs.add(-1);
        while (coeffs.size() < N)
            coeffs.add(0);
        Collections.shuffle(coeffs, new SecureRandom());
        
        IntegerPolynomial poly = new IntegerPolynomial(N);
        for (int i=0; i<coeffs.size(); i++)
            poly.coeffs[i] = coeffs.get(i);
        return poly;
    }
    
    static IntegerPolynomial generateRandom(int N) {
        SecureRandom rng = new SecureRandom();
        IntegerPolynomial poly = new IntegerPolynomial(N);
        for (int i=0; i<N; i++)
            poly.coeffs[i] = rng.nextInt(3) - 1;
        return poly;
    }
    
    /** Multiplies the polynomial with another, taking the values mod modulus and the indices mod N */
    // XXX Not the most efficient alg
    IntegerPolynomial mult(IntegerPolynomial poly2, int modulus) {
        int[] a = coeffs;
        int[] b = poly2.coeffs;
        if (b.length != a.length)
            throw new RuntimeException("Number of coefficients must be the same");
        int N = a.length;
        int[] c = new int[N];
        
        for(int k=N-1; k>=0; k--)
        {
            c[k] = 0;
            int j = k + 1;
            for(int i=N-1; i>=0; i--)
            {
                if(j == N)
                    j = 0;
                if(a[i]!=0 && b[j]!=0) {
                    c[k] += a[i] * b[j];
                    c[k] %= modulus;
                }
                j++;
            }
        }
        return new IntegerPolynomial(c);
    }
    
    /** Multiplies the polynomial with another, taking the indices mod N */
    // XXX Not the most efficient alg
    IntegerPolynomial mult(IntegerPolynomial poly2) {
        int[] a = coeffs;
        int[] b = poly2.coeffs;
        if (b.length != a.length)
            throw new RuntimeException("Number of coefficients must be the same");
        int N = a.length;
        int[] c = new int[N];
        
        for(int k=N-1; k>=0; k--)
        {
            c[k] = 0;
            int j = k + 1;
            for(int i=N-1; i>=0; i--)
            {
                if(j == N)
                    j = 0;
                if(a[i]!=0 && b[j]!=0)
                    c[k] += a[i] * b[j];
                j++;
            }
        }
        return new IntegerPolynomial(c);
    }
    
    // Computes the inverse mod q; q must be a power of 2.
    // Returns null if the polynomial is not invertible.
    IntegerPolynomial invertFq(int q) {
        int N = coeffs.length;
        int k = 0;
        IntegerPolynomial b = new IntegerPolynomial(N+1);
        b.coeffs[0] = 1;
        IntegerPolynomial c = new IntegerPolynomial(N+1);
        IntegerPolynomial f = new IntegerPolynomial(N+1);
        f.coeffs = Arrays.copyOf(coeffs, N+1);
        // set g(x) = x^N − 1
        IntegerPolynomial g = new IntegerPolynomial(N+1);
        g.coeffs[0] = -1;
        g.coeffs[N] = 1;
        while (true) {
            while (f.coeffs[0] == 0) {
                for (int i=1; i<=N; i++) {
                    f.coeffs[i-1] = f.coeffs[i];   // f(x) = f(x) / x
                    c.coeffs[N+1-i] = c.coeffs[N-i];   // c(x) = c(x) * x
                }
                f.coeffs[N] = 0;
                c.coeffs[0] = 0;
                k++;
                if (f.equalsZero())
                    return null;   // not invertible
            }
            if (f.equalsOne())
                break;
            if (f.degree() < g.degree()) {
                // exchange f and g
                IntegerPolynomial temp = f;
                f = g;
                g = temp;
                // exchange b and c
                temp = b;
                b = c;
                c = temp;
            }
            f.add(g, 2);
            b.add(c, 2);
        }
        
        if (b.coeffs[N] != 0)
            return null;
        // Fq(x) = x^(N-k) * b(x)
        IntegerPolynomial Fq = new IntegerPolynomial(N);
        int j = 0;
        k %= N;
        for (int i=N-1; i>=0; i--) {
            j = i - k;
            if (j < 0)
                j += N;
            Fq.coeffs[j] = b.coeffs[i];
        }
        
        // inverse mod 2 --> inverse mod q
        int v = 2;
        while (v < q) {
            v *= 2;
            IntegerPolynomial temp = new IntegerPolynomial(Arrays.copyOf(Fq.coeffs, Fq.coeffs.length));
            temp.mult2(v);
            Fq = mult(Fq, v).mult(Fq, v);
            temp.sub(Fq, v);
            Fq = temp;
        }
        
        Fq.ensurePositive(q);
        return Fq;
    }
    
    // Computes the inverse mod 3.
    // Returns null if the polynomial is not invertible.
    IntegerPolynomial invertF3() {
        int N = coeffs.length;
        int k = 0;
        IntegerPolynomial b = new IntegerPolynomial(N+1);
        b.coeffs[0] = 1;
        IntegerPolynomial c = new IntegerPolynomial(N+1);
        IntegerPolynomial f = new IntegerPolynomial(N+1);
        f.coeffs = Arrays.copyOf(coeffs, N+1);
        // set g(x) = x^N − 1
        IntegerPolynomial g = new IntegerPolynomial(N+1);
        g.coeffs[0] = -1;
        g.coeffs[N] = 1;
        while (true) {
            while (f.coeffs[0] == 0) {
                for (int i=1; i<=N; i++) {
                    f.coeffs[i-1] = f.coeffs[i];   // f(x) = f(x) / x
                    c.coeffs[N+1-i] = c.coeffs[N-i];   // c(x) = c(x) * x
                }
                f.coeffs[N] = 0;
                c.coeffs[0] = 0;
                k++;
                if (f.equalsZero())
                    return null;   // not invertible
            }
            if (f.equalsAbsOne())
                break;
            if (f.degree() < g.degree()) {
                // exchange f and g
                IntegerPolynomial temp = f;
                f = g;
                g = temp;
                // exchange b and c
                temp = b;
                b = c;
                c = temp;
            }
            if (f.coeffs[0] == g.coeffs[0]) {
                f.sub(g, 3);
                b.sub(c, 3);
            }
            else {
                f.add(g, 3);
                b.add(c, 3);
            }
        }
        
        if (b.coeffs[N] != 0)
            return null;
        // Fp(x) = [+-] x^(N-k) * b(x)
        IntegerPolynomial Fp = new IntegerPolynomial(N);
        int j = 0;
        k %= N;
        for (int i=N-1; i>=0; i--) {
            j = i - k;
            if (j < 0)
                j += N;
            Fp.coeffs[j] = f.coeffs[0] * b.coeffs[i];
        }
        
        Fp.ensurePositive(3);
        return Fp;
    }
    
    /**
     * Resultant of this polynomial with x^n-1.
     * Returns (rho, res) satisfying res = rho*this + t*(x^n-1) for some integer t
     */
    Resultant resultant() {
        int N = coeffs.length;
        
        // upper bound for resultant(f, g) = ||f, 2||^deg(f) * ||g, 2||^deg(g) = squaresum(f)^(deg(f)/2) * 2^(N/2) because g(x)=x^N-1
        BigInteger max = squareSum().pow((degree()+1)/2);
        max = max.multiply(BigInteger.valueOf(2).pow((N+1)/2));
        BigInteger max2 = max.multiply(BigInteger.valueOf(2));
        BigInteger pProd = ONE;
        Iterator<BigInteger> primes = PRIMES.iterator();
        
        BigInteger res = ONE;
        BigIntPolynomial rhoP = new BigIntPolynomial(N);
        rhoP.coeffs[0] = ONE;
        BigInteger prime = BigInteger.valueOf(10000);
        while (pProd.compareTo(max2) < 0) {
            if (primes.hasNext())
                prime = primes.next();
            else
                prime = prime.nextProbablePrime();
            Resultant crr = resultant(prime.intValue());
            
            BigInteger temp = pProd.multiply(prime);
            BigIntEuclidean er = BigIntEuclidean.calculate(prime, pProd);
            
            res = res.multiply(er.x).multiply(prime);
            BigInteger res2 = crr.res.multiply(er.y.multiply(pProd));
            res = res.add(res2).mod(temp);
            
            rhoP.mult(er.x.multiply(prime));
            BigIntPolynomial rho = crr.rho;
            rho.mult(er.y.multiply(pProd));
            rhoP.add(rho);
            rhoP.mod(temp);
            pProd = temp;
        }
        
        res = res.mod(pProd);
        if (res.compareTo(pProd.divide(BigInteger.valueOf(2))) > 0)
            res = res.subtract(pProd);
        if (res.compareTo(pProd.divide(BigInteger.valueOf(-2))) < 0)
            res = res.add(pProd);
        
        rhoP.mod(pProd);
        for (int i=0; i<N; i++) {
            if (rhoP.coeffs[i].compareTo(pProd.divide(BigInteger.valueOf(2))) > 0)
                rhoP.coeffs[i] = rhoP.coeffs[i].subtract(pProd);
            if (rhoP.coeffs[i].compareTo(pProd.divide(BigInteger.valueOf(-2))) < 0)
                rhoP.coeffs[i] = rhoP.coeffs[i].add(pProd);
        }
        
        return new Resultant(rhoP, res);
    }
        
    /**
     * Resultant of this polynomial with x^n-1 mod p.
     * Returns (rho, res) satisfying res = rho*this + t*(x^n-1) mod p for some integer t.
     */
    Resultant resultant(int p) {
        // Add a coefficient as the following operations involve polynomials of degree deg(f)+1
        int[] fcoeffs = Arrays.copyOf(coeffs, coeffs.length+1);
        IntegerPolynomial f = new IntegerPolynomial(fcoeffs);
        int N = fcoeffs.length;
        
        IntegerPolynomial a = new IntegerPolynomial(N);
        a.coeffs[0] = -1;
        a.coeffs[N-1] = 1;
        IntegerPolynomial b = new IntegerPolynomial(f.coeffs);
        IntegerPolynomial v1 = new IntegerPolynomial(N);
        IntegerPolynomial v2 = new IntegerPolynomial(N);
        v2.coeffs[0] = 1;
        int da = a.degree();
        int db = b.degree();
        int ta = da;
        int c = 0;
        int r = 1;
        while (db > 0) {
            c = Util.invert(b.coeffs[db], p);
            c = (c * a.coeffs[da]) % p;
            
            IntegerPolynomial cb = b.clone();
            cb.mult(c);
            cb.shift(da - db);
            a.sub(cb, p);
            
            IntegerPolynomial v2c = v2.clone();
            v2c.mult(c);
            v2c.shift(da - db);
            v1.sub(v2c, p);
            
            da = a.degree();
            if (da < db) {
                r *= Util.pow(b.coeffs[db], ta-da, p);
                r %= p;
                if (ta%2==1 && db%2==1)
                    r = (-r) % p;
                IntegerPolynomial temp = a;
                a = b;
                b = temp;
                da = db;
                temp = v1;
                v1 = v2;
                v2 = temp;
                ta = db;
            }
            db = b.degree();
        }
        r *= Util.pow(b.coeffs[0], da, p);
        r %= p;
        c = Util.invert(b.coeffs[0], p);
        v2.mult(c);
        v2.mod(p);
        v2.mult(r);
        v2.mod(p);
        
        // drop the highest coefficient so #coeffs matches the original input
        v2.coeffs = Arrays.copyOf(v2.coeffs, v2.coeffs.length-1);
        return new Resultant(new BigIntPolynomial(v2), BigInteger.valueOf(r));
    }
    
    private BigInteger squareSum() {
        BigInteger sum = ZERO;
        for (int i=0; i<coeffs.length; i++)
            sum = sum.add(BigInteger.valueOf(coeffs[i]*coeffs[i]));
        return sum;
    }
    
    int degree() {
        int degree = coeffs.length - 1;
        while (degree>0 && coeffs[degree]==0)
            degree--;
        return degree;
    }
    
    void add(IntegerPolynomial b, int modulus) {
        add(b);
        mod(modulus);
    }
    
    /** Adds another polynomial which can have a different number of coefficients */
    void add(IntegerPolynomial b) {
        if (b.coeffs.length > coeffs.length)
            coeffs = Arrays.copyOf(coeffs, b.coeffs.length);
        for (int i=0; i<b.coeffs.length; i++)
            coeffs[i] += b.coeffs[i];
    }
    
    void sub(IntegerPolynomial b, int modulus) {
        sub(b);
        mod(modulus);
    }
    
    /** Subtracts another polynomial which can have a different number of coefficients */
    void sub(IntegerPolynomial b) {
        if (b.coeffs.length > coeffs.length)
            coeffs = Arrays.copyOf(coeffs, b.coeffs.length);
        for (int i=0; i<b.coeffs.length; i++)
            coeffs[i] -= b.coeffs[i];
    }
    
    void sub(int b) {
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] -= b;
    }
    
    void mult(int factor) {
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] *= factor;
    }
    
    private void mult2(int modulus) {
        for (int i=0; i<coeffs.length; i++) {
            coeffs[i] *= 2;
            coeffs[i] %= modulus;
        }
    }
    
    void mult3(int modulus) {
        for (int i=0; i<coeffs.length; i++) {
            coeffs[i] *= 3;
            coeffs[i] %= modulus;
        }
    }
    
    void mult3Add1(int modulus) {
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] = (coeffs[i]*3+1) % modulus;
    }
    
    /** divides by k and rounds to the nearest integer */
    void div(int k) {
        int k2 = (k+1) / 2;
        for (int i=0; i<coeffs.length; i++) {
            coeffs[i] += coeffs[i]>0 ? k2 : -k2;
            coeffs[i] /= k;
        }
    }
    
    // returns a polynomial all of whose coefficients are between -1 and 1
    void mod3() {
        for (int i=0; i<coeffs.length; i++) {
            coeffs[i] %= 3;
            if (coeffs[i] > 1)
                coeffs[i] -= 3;
            if (coeffs[i] < -1)
                coeffs[i] += 3;
        }
    }
    
    // ensures all coefficients are between 0 and modulus-1
    void modPositive(int modulus) {
        mod(modulus);
        ensurePositive(modulus);
    }
    
    /** Reduces all coefficients to the interval [-modulus/2, modulus/2) */
    void modCenter(int modulus) {
        mod(modulus);
        for (int j=0;j<coeffs.length;j++){
            while (coeffs[j] < modulus/2)
                coeffs[j] += modulus;
            while (coeffs[j] >= modulus/2)
                coeffs[j]-=modulus;
        }
    }
    
    void mod(int modulus) {
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] %= modulus;
    }
    
    void ensurePositive(int modulus) {
        for (int i=0; i<coeffs.length; i++)
            while (coeffs[i] < 0)
                coeffs[i] += modulus;
    }
    
    /* moves all coefficients up k places, which is equivalent to a multiplication with x^k. The shift is not cyclic. */
    void shift(int k) {
        int N = coeffs.length;
        if (k > N-1-degree()) {
            N = degree() + 1 + k;
            coeffs = Arrays.copyOf(coeffs, N);
        }
        for (int i=N-k-1; i>=0; i--)
            coeffs[i+k] = coeffs[i];
        for (int i=0; i<k; i++)
            coeffs[i] = 0;
    }
    
    // shifts the values of all coefficients to the interval [-q/2, q/2]
    void center0(int q) {
        for (int i=0; i<coeffs.length; i++) {
            while (coeffs[i] < -q/2)
                coeffs[i] += q;
            while (coeffs[i] > q/2)
                coeffs[i] -= q;
        }
    }
    
    // shifts all coefficients to the interval [A, A+q-1]
    void centerN(int A, int q) {
        for (int i=0; i<coeffs.length; i++) {
            while (coeffs[i] < A)
                coeffs[i] += q;
            while (coeffs[i] >= A+q)
                coeffs[i] -= q;
        }
    }
    
    int sumCoeffs() {
        int sum = 0;
        for (int i=0; i<coeffs.length; i++)
            sum += coeffs[i];
        return sum;
    }
    
    // tests if p(x) = 0
    private boolean equalsZero() {
        for (int i=0; i<coeffs.length; i++)
            if (coeffs[i] != 0)
                return false;
        return true;
    }
    
    // tests if p(x) = 1
    boolean equalsOne() {
        for (int i=1; i<coeffs.length; i++)
            if (coeffs[i] != 0)
                return false;
        return coeffs[0] == 1;
    }
    
    // tests if |p(x)| = 1
    private boolean equalsAbsOne() {
        for (int i=1; i<coeffs.length; i++)
            if (coeffs[i] != 0)
                return false;
        return Math.abs(coeffs[0]) == 1;
    }
    
    // counts the number of coefficients equal to an integer
    int count(int value) {
        int count = 0;
        for (int coeff: coeffs)
            if (coeff == value)
                count++;
        return count;
    }
    
    /** Centered norm (inefficient algorithm) */
    int centerNormSq(int modulus) {
        int N = coeffs.length;
        int minNormSq=Integer.MAX_VALUE;

//int sum = 0;
//int ssum = 0;
//for (int i=0; i<N; i++) {
//    int ci = coeffs[i];
//    sum += ci;
//    ssum += ci * ci;
//}
//int normSq = ssum - sum*sum/N;
//return normSq;
        
        for (int s=0; s<modulus; s++) {
            int sum = 0;
            int ssum = 0;
            for (int i=0; i<N; i++) {
                int ci = (coeffs[i]+s) % modulus;
                // reduce to [-modulus/2, modulus/2)
                if (2*ci >= modulus)
                    ci -= modulus;
                if (2*ci < -modulus)
                    ci += modulus;
                
                sum += ci;
                ssum += ci * ci;
            }
            int normSq = ssum - sum*sum/N;
            minNormSq = Math.min(minNormSq, normSq);
        }
        return minNormSq;
    }
    
    // mult w/ X in Z[X]/Z[X^n-1]
    void rotate1() {
        int clast = coeffs[coeffs.length-1];
        for (int i=coeffs.length-1; i>0; i--)
            coeffs[i] = coeffs[i-1];
        coeffs[0] = clast;
    }
    
   void clear() {
        for (int i=0; i<coeffs.length; i++)
            coeffs[i] = 0;
    }
    
    @Override
    public IntegerPolynomial clone() {
        return new IntegerPolynomial(coeffs.clone());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntegerPolynomial)
            return Arrays.equals(coeffs, ((IntegerPolynomial)obj).coeffs);
        else
            return false;
    }
}
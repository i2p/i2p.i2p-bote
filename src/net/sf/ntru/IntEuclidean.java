package net.sf.ntru;

/** Extended Euclidean Algorithm in ints */
public class IntEuclidean {
    public int x, y, gcd;
    
    private IntEuclidean() { }
    
    // from pseudocode at http://en.wikipedia.org/wiki/Extended_Euclidean_algorithm
    public static IntEuclidean calculate(int a, int b) {
        int x = 0;
        int lastx = 1;
        int y = 1;
        int lasty = 0;
        while (b != 0) {
            int quotient = a / b;
            
            int temp = a;
            a = b;
            b = temp % b;
            
            temp = x;
            x = lastx - quotient*x;
            lastx = temp;
            
            temp = y;
            y = lasty - quotient*y;
            lasty = temp;
        }
        
        IntEuclidean result = new IntEuclidean();
        result.x = lastx;
        result.y = lasty;
        result.gcd = a;
        return result;
    }
}
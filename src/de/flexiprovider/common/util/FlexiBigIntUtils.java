package de.flexiprovider.common.util;

import de.flexiprovider.common.math.FlexiBigInt;

public final class FlexiBigIntUtils {

    /**
     * Default constructor (private).
     */
    private FlexiBigIntUtils() {
	// empty
    }

    /**
     * Checks if two FlexiBigInt arrays contain the same entries
     * 
     * @param a
     *                first FlexiBigInt array
     * @param b
     *                second FlexiBigInt array
     * @return true or false
     */
    public static boolean equals(FlexiBigInt[] a, FlexiBigInt[] b) {
	int flag = 0;

	if (a.length != b.length) {
	    return false;
	}
	for (int i = 0; i < a.length; i++) {
	    // avoid branches here!
	    // problem: compareTo on FlexiBigInts is not
	    // guaranteed constant-time!
	    flag |= a[i].compareTo(b[i]);
	}
	return flag == 0;
    }

    /**
     * Fill the given FlexiBigInt array with the given value.
     * 
     * @param array
     *                the array
     * @param value
     *                the value
     */
    public static void fill(FlexiBigInt[] array, FlexiBigInt value) {
	for (int i = array.length - 1; i >= 0; i--) {
	    array[i] = value;
	}
    }

    /**
     * Generates a subarray of a given FlexiBigInt array.
     * 
     * @param input -
     *                the input FlexiBigInt array
     * @param start -
     *                the start index
     * @param end -
     *                the end index
     * @return a subarray of <tt>input</tt>, ranging from <tt>start</tt> to
     *         <tt>end</tt>
     */
    public static FlexiBigInt[] subArray(FlexiBigInt[] input, int start, int end) {
	FlexiBigInt[] result = new FlexiBigInt[end - start];
	System.arraycopy(input, start, result, 0, end - start);
	return result;
    }

    /**
     * Converts a FlexiBigInt array into an integer array
     * 
     * @param input -
     *                the FlexiBigInt array
     * @return the integer array
     */
    public static int[] toIntArray(FlexiBigInt[] input) {
	int[] result = new int[input.length];
	for (int i = 0; i < input.length; i++) {
	    result[i] = input[i].intValue();
	}
	return result;
    }

    /**
     * Converts a FlexiBigInt array into an integer array, reducing all
     * FlexiBigInts mod q.
     * 
     * @param q -
     *                the modulus
     * @param input -
     *                the FlexiBigInt array
     * @return the integer array
     */
    public static int[] toIntArrayModQ(int q, FlexiBigInt[] input) {
	FlexiBigInt bq = FlexiBigInt.valueOf(q);
	int[] result = new int[input.length];
	for (int i = 0; i < input.length; i++) {
	    result[i] = input[i].mod(bq).intValue();
	}
	return result;
    }

    /**
     * Return the value of <tt>big</tt> as a byte array. Although FlexiBigInt
     * has such a method, it uses an extra bit to indicate the sign of the
     * number. For elliptic curve cryptography, the numbers usually are
     * positive. Thus, this helper method returns a byte array of minimal
     * length, ignoring the sign of the number.
     * 
     * @param value
     *                the <tt>FlexiBigInt</tt> value to be converted to a byte
     *                array
     * @return the value <tt>big</tt> as byte array
     */
    public static byte[] toMinimalByteArray(FlexiBigInt value) {
	byte[] valBytes = value.toByteArray();
	if ((valBytes.length == 1) || (value.bitLength() & 0x07) != 0) {
	    return valBytes;
	}
	byte[] result = new byte[value.bitLength() >> 3];
	System.arraycopy(valBytes, 1, result, 0, result.length);
	return result;
    }

}

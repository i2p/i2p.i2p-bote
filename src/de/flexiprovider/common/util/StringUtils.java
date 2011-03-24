package de.flexiprovider.common.util;

/**
 * This class is a utility class for manipulating strings.
 */
public final class StringUtils {

    /**
     * Default constructor (private)
     */
    private StringUtils() {
	// empty
    }

    /**
     * Strip whitespace off a string, returning a new string.
     * 
     * @param str
     *                the string
     * @return the filtered string
     */
    public static String filterSpaces(String str) {
	StringBuffer buf = new StringBuffer(str);

	for (int i = 0; i < buf.length(); i++) {
	    if (buf.charAt(i) == ' ') {
		buf = buf.deleteCharAt(i);
		i--;
	    }
	}
	return buf.toString();
    }

    // ****************************************************
    // Array utilities
    // ****************************************************

    /**
     * Compare two String arrays. No null checks are performed.
     * 
     * @param left
     *                the first String array
     * @param right
     *                the second String array
     * @return the result of the comparison
     */
    public static boolean equals(String[] left, String[] right) {
	if (left.length != right.length) {
	    return false;
	}
	boolean result = true;
	for (int i = left.length - 1; i >= 0; i--) {
	    result &= left[i].equals(right[i]);
	}
	return result;
    }

}

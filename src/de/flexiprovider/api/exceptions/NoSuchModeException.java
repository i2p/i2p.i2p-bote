package de.flexiprovider.api.exceptions;

/**
 * Exception used to indicate that a mode of operation cannot be found.
 * 
 * @author Martin Döring
 */
public class NoSuchModeException extends NoSuchAlgorithmException {

    /**
     * Constructor.
     * 
     * @param msg
     *                the error message
     */
    public NoSuchModeException(String msg) {
	super(msg);
    }

}

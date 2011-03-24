package de.flexiprovider.common.mode;

/**
 * This class is the parameter specification of the Cipher Feedback Mode
 * 
 * @author Johannes Müller
 */
public class CFBParameterSpec extends ModeParameterSpec {

    // the block size
    private int blockSize;

    /**
     * Constructor. Set the passed initialization vector and block size.
     * 
     * @param iv
     *                the initialization vector
     * @param blockSize
     *                the block size
     */
    public CFBParameterSpec(byte[] iv, int blockSize) {
	super(iv);
	this.blockSize = blockSize;
    }

    /**
     * @return the block size
     */
    public final int getBlockSize() {
	return blockSize;
    }

}

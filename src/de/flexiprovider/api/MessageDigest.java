package de.flexiprovider.api;

import de.flexiprovider.api.exceptions.DigestException;

public abstract class MessageDigest extends java.security.MessageDigestSpi {

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    protected final int engineGetDigestLength() {
	return getDigestLength();
    }

    protected final void engineUpdate(byte input) {
	update(input);
    }

    protected final void engineUpdate(byte[] input, int offset, int len) {
	update(input, offset, len);
    }

    protected final byte[] engineDigest() {
	return digest();
    }

    protected final void engineReset() {
	reset();
    }

    // ****************************************************
    // FlexiAPI methods
    // ****************************************************

    /**
     * @return the digest length in bytes
     */
    public abstract int getDigestLength();

    /**
     * Update the digest using the specified byte.
     * 
     * @param input
     *                the byte to use for the update
     */
    public abstract void update(byte input);

    /**
     * Update the digest using the specified array of bytes, starting at the
     * specified offset.
     * 
     * @param input
     *                the array of bytes to use for the update
     */
    public final void update(byte[] input) {
	if (input == null) {
	    return;
	}
	update(input, 0, input.length);
    }

    /**
     * Update the digest using the specified array of bytes, starting at the
     * specified offset.
     * 
     * @param input
     *                the array of bytes to use for the update
     * @param offset
     *                the offset to start from in the array of bytes
     * @param len
     *                the number of bytes to use, starting at <tt>offset</tt>
     */
    public abstract void update(byte[] input, int offset, int len);

    /**
     * Complete the hash computation by performing final operations such as
     * padding. Once {@link #digest()} has been called, the engine should be
     * reset (see {@link #reset()}). Resetting is the responsibility of the
     * engine implementor.
     * 
     * @return the array of bytes for the resulting hash value.
     */
    public abstract byte[] digest();

    /**
     * Update the digest and complete the hash computation by performing final
     * operations such as padding. Once {@link #digest(byte[])} has been called,
     * the engine should be reset (see {@link #reset()}). Resetting is the
     * responsibility of the engine implementor.
     * 
     * @param input
     *                the array of bytes to use for the update
     * @return the array of bytes for the resulting hash value
     */
    public final byte[] digest(byte[] input) {
	update(input);
	return digest();
    }

    /**
     * Complete the hash computation by performing final operations such as
     * padding. Once {@link #digest(byte[], int, int)} has been called, the
     * engine should be reset (see {@link #reset()}). Resetting is the
     * responsibility of the engine implementor.
     * 
     * @param buf
     *                the output buffer in which to store the digest
     * @param offset
     *                offset to start from in the output buffer
     * @param len
     *                number of bytes within buf allotted for the digest. Both
     *                this default implementation and the SUN provider do not
     *                return partial digests. The presence of this parameter is
     *                solely for consistency in our API's. If the value of this
     *                parameter is less than the actual digest length, the
     *                method will throw a DigestException. This parameter is
     *                ignored if its value is greater than or equal to the
     *                actual digest length.
     * @return the length of the digest stored in the output buffer.
     * @throws DigestException
     *                 if an error occurs.
     */
    public final int digest(byte[] buf, int offset, int len)
	    throws DigestException {

	byte[] digest = digest();
	if (len < digest.length) {
	    throw new DigestException("partial digests not returned");
	}
	if (buf.length - offset < digest.length) {
	    throw new DigestException("insufficient space in the output "
		    + "buffer to store the digest");
	}
	System.arraycopy(digest, 0, buf, offset, digest.length);
	return digest.length;
    }

    /**
     * Reset the digest for further use.
     */
    public abstract void reset();

}

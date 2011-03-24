/*
 * Copyright (c) 1998-2003 by The FlexiProvider Group,
 *                            Technische Universitaet Darmstadt 
 *
 * For conditions of usage and distribution please refer to the
 * file COPYING in the root directory of this package.
 *
 */

package de.flexiprovider.api;

import de.flexiprovider.api.exceptions.InvalidAlgorithmParameterException;
import de.flexiprovider.api.exceptions.InvalidKeyException;
import de.flexiprovider.api.parameters.AlgorithmParameterSpec;

/**
 * This class defines a <b>key derivation function</b>. All the abstract
 * methods in this class must be implemented by each cryptographic service
 * provider who wishes to supply the implementation of a particular Key
 * Derivation algorithm. A key derivation function is used to generate a longer
 * or shorter secret key, with a second secret shared by both parties. The
 * derived secret key may be used by other schemes which use different key
 * length, to the normal secret keys.
 * 
 * @author Jochen Hechler
 * @author Marcus Stögbauer
 * @author Martin Döring
 */
public abstract class KeyDerivation {

    /**
     * Initialize this KDF with a secret and parameters.
     * 
     * @param secret
     *                the secret from which to derive the key
     * @param params
     *                the parameters
     * @throws InvalidKeyException
     *                 if the secret is invalid.
     * @throws InvalidAlgorithmParameterException
     *                 if the parameters are invalid.
     */
    public abstract void init(byte[] secret, AlgorithmParameterSpec params)
	    throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Start the derivation process and return the derived key. If supported by
     * the concrete implementation, the derived key will be of the specified
     * length.
     * 
     * @param keySize
     *                the desired length of the derived key
     * @return the derived key with the specified length, or <tt>null</tt> if
     *         the key size is <tt>&lt; 0</tt>.
     */
    public abstract byte[] deriveKey(int keySize);

}

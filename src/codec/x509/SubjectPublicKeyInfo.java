/* ========================================================================
 *
 *  This file is part of CODEC, which is a Java package for encoding
 *  and decoding ASN.1 data structures.
 *
 *  Author: Fraunhofer Institute for Computer Graphics Research IGD
 *          Department A8: Security Technology
 *          Fraunhoferstr. 5, 64283 Darmstadt, Germany
 *
 *  Rights: Copyright (c) 2004 by Fraunhofer-Gesellschaft 
 *          zur Foerderung der angewandten Forschung e.V.
 *          Hansastr. 27c, 80686 Munich, Germany.
 *
 * ------------------------------------------------------------------------
 *
 *  The software package is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 2.1 of the 
 *  License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public 
 *  License along with this software package; if not, write to the Free 
 *  Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 *  MA 02110-1301, USA or obtain a copy of the license at 
 *  http://www.fsf.org/licensing/licenses/lgpl.txt.
 *
 * ------------------------------------------------------------------------
 *
 *  The CODEC library can solely be used and distributed according to 
 *  the terms and conditions of the GNU Lesser General Public License for 
 *  non-commercial research purposes and shall not be embedded in any 
 *  products or services of any user or of any third party and shall not 
 *  be linked with any products or services of any user or of any third 
 *  party that will be commercially exploited.
 *
 *  The CODEC library has not been tested for the use or application 
 *  for a determined purpose. It is a developing version that can 
 *  possibly contain errors. Therefore, Fraunhofer-Gesellschaft zur 
 *  Foerderung der angewandten Forschung e.V. does not warrant that the 
 *  operation of the CODEC library will be uninterrupted or error-free. 
 *  Neither does Fraunhofer-Gesellschaft zur Foerderung der angewandten 
 *  Forschung e.V. warrant that the CODEC library will operate and 
 *  interact in an uninterrupted or error-free way together with the 
 *  computer program libraries of third parties which the CODEC library 
 *  accesses and which are distributed together with the CODEC library.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not warrant that the operation of the third parties's computer 
 *  program libraries themselves which the CODEC library accesses will 
 *  be uninterrupted or error-free.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  shall not be liable for any errors or direct, indirect, special, 
 *  incidental or consequential damages, including lost profits resulting 
 *  from the combination of the CODEC library with software of any user 
 *  or of any third party or resulting from the implementation of the 
 *  CODEC library in any products, systems or services of any user or 
 *  of any third party.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not provide any warranty nor any liability that utilization of 
 *  the CODEC library will not interfere with third party intellectual 
 *  property rights or with any other protected third party rights or will 
 *  cause damage to third parties. Fraunhofer Gesellschaft zur Foerderung 
 *  der angewandten Forschung e.V. is currently not aware of any such 
 *  rights.
 *
 *  The CODEC library is supplied without any accompanying services.
 *
 * ========================================================================
 */
package codec.x509;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import codec.CorruptedCodeException;
import codec.InconsistentStateException;
import codec.asn1.ASN1BitString;
import codec.asn1.ASN1Exception;
import codec.asn1.ASN1Sequence;
import codec.asn1.ASN1Type;
import codec.asn1.DERDecoder;
import codec.asn1.DEREncoder;

/**
 * Subject Public Key Info according to RFC2459. Consists of an AlgorithmID and
 * the corresponding public key <blockquote>
 * 
 * <pre>
 * SubjectPublicKeyInfo  ::=  SEQUENCE  {
 *   algorithm            AlgorithmIdentifier,
 *   subjectPublicKey     BIT STRING
 * }
 * </pre>
 * 
 * <blockquote>
 * 
 * Raw public key material embedded in this structure need not be ASN.1/DER
 * encoded. Some crypto systems such as elliptic curve systems use specific
 * encodings of keys. For this reason, keys can be retrieved by means of two
 * alternative ways:
 * <ul>
 * <li> Raw encoded keys, returned as byte arrays, and
 * <li> Raw decoded keys, returned as ASN.1 types.
 * </ul>
 * Only ASN.1/DER is supported as decoding method. Keys encoded in another code
 * must be decoded externally.
 * 
 * Creation date: (18.08.99 15:23:09)
 * 
 * @author Markus Tak
 * @version "$Id: SubjectPublicKeyInfo.java,v 1.3 2004/08/16 08:53:58 pebinger
 *          Exp $"
 */
public class SubjectPublicKeyInfo extends ASN1Sequence {

    private AlgorithmIdentifier algorithm_;

    private ASN1BitString encodedKey_;

    /**
     * Creates an instance that is ready for decoding.
     */
    public SubjectPublicKeyInfo() {
	super(2);

	algorithm_ = new AlgorithmIdentifier();
	add(algorithm_);

	encodedKey_ = new ASN1BitString();
	add(encodedKey_);
    }

    /**
     * Creates an instance with the given {@link AlgorithmIdentifier
     * AlgorithmIdentifier} and encoded public key. The public key is wrapped
     * into an {@link ASN1BitString ASN1BitString} as required by the
     * specification of this structure. Hence, the encoded key is taken &quot;as
     * is&quot;.
     * <p>
     * 
     * The given arguments are put into this instance literally. The arguments
     * are not copied or cloned. Therefor side effects can occur when the given
     * arguments are modified after this constructor was called.
     * 
     * @param aid
     *                AlgorithmIdentifier of the public key algorithm
     * @param key
     *                The encoded key material.
     * @throws NullPointerException
     *                 if either argument is <code>null</code>.
     */
    public SubjectPublicKeyInfo(AlgorithmIdentifier aid, byte[] key) {
	super(2);

	if (aid == null || key == null)
	    throw new NullPointerException("Some arg is null!");

	algorithm_ = aid;
	add(algorithm_);
	encodedKey_ = new ASN1BitString(key, 0);
	add(encodedKey_);
    }

    /**
     * Creates an instance with the given {@link AlgorithmIdentifier
     * AlgorithmIdentifier} and key. <b>The given key must be a raw key
     * structure represented by means of an ASN.1 structure. The key structure
     * is encoded into a bit string which is put into this instance.</b>
     * 
     * @param aid
     *                The AlgorithmIdentifier of the public key algorithm.
     * @param key
     *                The public key as a ASN.1 data structure.
     */
    public SubjectPublicKeyInfo(AlgorithmIdentifier aid, ASN1Type key) {
	super(2);

	algorithm_ = aid;
	add(algorithm_);
	add(null);
	setRawKey(key);
    }

    /**
     * Creates an instance that is initialized from the given public key.
     * 
     * @param key
     *                The public key.
     * @throws InvalidKeyException
     *                 if the key cannot be decoded properly.
     * @throws NullPointerException
     *                 if the given key is <code>null</code>.
     */
    public SubjectPublicKeyInfo(PublicKey key) throws InvalidKeyException {
	super(2);
	setPublicKey(key);
    }

    /**
     * Returns the public key embedded in this structure.
     * <p>
     * 
     * This method creates an X509EncodedKeySpec of this instance and feeds it
     * into a key factory. In order to locate a suitable key factory, the
     * installed providers must define appropriate OID mappings.
     * 
     * @return The public key.
     * @throws InconsistentStateException
     *                 if the key spec generated by this method is rejected by
     *                 the key factory that is used to generate the key.
     * @throws NoSuchAlgorithmException
     *                 if there is no key factory registered for the algorithm
     *                 of the embedded key or no appropriate OID mapping is
     *                 defined by the installed providers.
     */
    public PublicKey getPublicKey() throws NoSuchAlgorithmException {
	ByteArrayOutputStream bos;
	X509EncodedKeySpec spec;
	DEREncoder enc;
	KeyFactory kf;
	String alg;

	try {
	    bos = new ByteArrayOutputStream();
	    enc = new DEREncoder(bos);
	    encode(enc);
	    spec = new X509EncodedKeySpec(bos.toByteArray());
	    enc.close();

	    alg = algorithm_.getAlgorithmOID().toString();
	    kf = KeyFactory.getInstance(alg);

	    return kf.generatePublic(spec);
	} catch (ASN1Exception e) {
	    throw new InconsistentStateException("Internal, encoding error!");
	} catch (IOException e) {
	    throw new InconsistentStateException(
		    "Internal, I/O exception caught!");
	} catch (InvalidKeySpecException e) {
	    throw new InconsistentStateException(
		    "Encoded key spec rejected by key factory!");
	}
    }

    /**
     * Initializes this instance with the given public key.
     * 
     * @param key
     *                The public key from which this instance is initialized.
     * @throws InvalidKeyException
     *                 if the given key cannot be decoded properly.
     * @throws NullPointerException
     *                 if the given key is <code>null</code>.
     */
    public void setPublicKey(PublicKey key) throws InvalidKeyException {
	if (key == null)
	    throw new NullPointerException("Key is null!");

	DERDecoder dec;

	clear();

	algorithm_ = new AlgorithmIdentifier();
	add(algorithm_);
	encodedKey_ = new ASN1BitString();
	add(encodedKey_);

	try {
	    dec = new DERDecoder(new ByteArrayInputStream(key.getEncoded()));

	    decode(dec);
	    dec.close();
	} catch (IOException e) {
	    throw new InvalidKeyException("Caught IOException!");
	} catch (ASN1Exception e) {
	    throw new InvalidKeyException("Bad encoding!");
	}
    }

    /**
     * Encodes and sets the given ASN.1 key structure as the raw key.
     * 
     * @throws InconsistentStateException
     *                 if an internal error occurs while the key is encoded.
     *                 This should never happen.
     */
    protected void setRawKey(ASN1Type key) {
	ByteArrayOutputStream bos;
	DEREncoder enc;

	try {
	    bos = new ByteArrayOutputStream();
	    enc = new DEREncoder(bos);
	    key.encode(enc);
	    encodedKey_ = new ASN1BitString(bos.toByteArray(), 0);
	    enc.close();
	    set(1, encodedKey_);
	} catch (ASN1Exception e) {
	    throw new InconsistentStateException("Internal, encoding error!");
	} catch (IOException e) {
	    throw new InconsistentStateException(
		    "Internal, I/O exception caught!");
	}
    }

    /**
     * Returns the raw key material. <b>The key material consists of an encoded
     * key structure.</b> ASN.1/DER is often used as the encoding. However,
     * this need not always be the case. Elliptic curve cryptosystems use
     * specific encodings.
     * <p>
     * 
     * If the key encoding is ASN.1/DER then the raw key can be retrieved as an
     * ASN.1 type by means of the {@link #getDecodedRawKey getDecodedRawKey()}
     * method.
     * 
     * @return The raw key bytes as a byte array.
     */
    public byte[] getRawKey() {
	return encodedKey_.getBytes();
    }

    /**
     * Returns an ASN.1 type that represents the decoded raw key. Decoding is
     * done by means of ASN.1/DER. <b>Be careful, not all public keys are
     * encoded according to DER.</b> Elliptic curve cryptosystems use specific
     * encodings.
     * 
     * @return The raw key decoded according to DER.
     */
    public ASN1Type getDecodedRawKey() throws CorruptedCodeException {
	DERDecoder dec;
	ASN1Type raw;

	try {
	    dec = new DERDecoder(new ByteArrayInputStream(encodedKey_
		    .getBytes()));

	    raw = dec.readType();
	    dec.close();

	    return raw;
	} catch (ASN1Exception e) {
	    throw new CorruptedCodeException("Cannot decode raw key!");
	} catch (IOException e) {
	    throw new InconsistentStateException(
		    "Internal, I/O exception caught!");
	}
    }

    /**
     * Returns the {@link AlgorithmIdentifier AlgorithmIdentifier} of the public
     * key.
     * 
     * @return The key algorithm's AlgorithmIdentifier.
     */
    public AlgorithmIdentifier getAlgorithmIdentifier() {
	return algorithm_;
    }

    /**
     * sets the AlgorithmIdentifier for this public key
     * 
     * @param aid
     *                AlgorithmIdentifier of this public key
     */
    public void setAlgorithmIdentifier(AlgorithmIdentifier aid) {
	if (aid == null)
	    throw new NullPointerException("Need an algorithm identifier!");

	set(0, aid);
	algorithm_ = aid;
    }

    /**
     * @deprecated This method is no longer used.
     */
    public ASN1Type getKeyStruct() throws CorruptedCodeException {
	return getKeyStruct(true);
    }

    /**
     * @param derDecode
     *                <code>true</code> if the raw key shall be decoded.
     * @deprecated This method is no longer used.
     */
    public ASN1Type getKeyStruct(boolean derDecode)
	    throws CorruptedCodeException {
	if (derDecode)
	    return getDecodedRawKey();

	return encodedKey_;
    }

    /**
     * @param key
     *                The key structure.
     * @deprecated This method is no longer used.
     */
    public void setKeyStruct(ASN1Type key) {
	setRawKey(key);
    }

}

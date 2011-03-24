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
package codec.pkcs8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import codec.CorruptedCodeException;
import codec.InconsistentStateException;
import codec.asn1.ASN1Exception;
import codec.asn1.ASN1Integer;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1OctetString;
import codec.asn1.ASN1Sequence;
import codec.asn1.ASN1Set;
import codec.asn1.ASN1SetOf;
import codec.asn1.ASN1TaggedType;
import codec.asn1.ASN1Type;
import codec.asn1.DERDecoder;
import codec.asn1.DEREncoder;
import codec.x501.Attribute;
import codec.x509.AlgorithmIdentifier;

/**
 * This class represents a <code>PrivateKeyInfo</code> as defined in <a
 * href="http://www.rsa.com/rsalabs/pubs/PKCS/html/pkcs-8.html"> PKCS#8</a>.
 * The ASN.1 definition of this structure is
 * <p>
 * <blockquote>
 * 
 * <pre>
 * PrivateKeyInfo ::= SEQUENCE (
 *   version Version,  -- 0 for version 1.2 Nov 93
 *   privateKeyAlgorithm PrivateKeyAlgorithmIdentifier,
 *   privateKey PrivateKey,
 *   attributes [0] IMPLICIT Attributes OPTIONAL
 * }
 * Version ::= INTEGER
 * PrivateKeyAlgorithmIdentifier ::= AlgorithmIdentifier
 * PrivateKey ::= OCTET STRING
 * Attributes ::= SET OF Attribute
 * </pre>
 * 
 * The following definitions are taken from the X501 standard:
 * 
 * <pre>
 * Attribute ::= SEQUENCE {
 *   type AttributeType
 *   values SET OF AttributeValue
 *   -- at least one value is required --
 * }
 * AttributeType ::= OBJECT IDENTIFIER
 * AttributeValue ::= ANY
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Markus Tak
 * @author Volker Roth
 * @version "$Id: PrivateKeyInfo.java,v 1.3 2004/08/24 10:01:21 pebinger Exp $"
 */
public class PrivateKeyInfo extends ASN1Sequence {
    /**
     * The default version.
     */
    public static final int VERSION = 0;

    /**
     * Version 1.2 November 1993 identifier.
     */
    public static final int VERSION_1_2 = 0;

    /**
     * Version is the syntax version number, for compatibility with future
     * revisions of the <a
     * href="http://www.rsa.com/rsalabs/pubs/PKCS/html/pkcs-8.html" PKCS#8
     * Standard</a>. It shall be 0 for that version.
     */
    protected ASN1Integer version_;

    /**
     * The {@link ASN1ObjectIdentifier OID} of the private key algorithm used in
     * this structure.
     */
    protected AlgorithmIdentifier algorithm_;

    /**
     * Stores the <b>encoded</b> {@link PrivateKey private Key}. The
     * interpretation of the contents is defined in the registration of the
     * private-key algorithm. For an RSA private key, for example, the contents
     * are a DER encoding of a value of type RSAPrivateKey.
     */
    transient private ASN1OctetString encodedKey_;

    /**
     * Attributes are the extended information that is encrypted along with the
     * private-key information.
     */
    protected ASN1Set attributes_;

    /**
     * This constructor builds the data structure.
     */
    public PrivateKeyInfo() {
	version_ = new ASN1Integer(VERSION);
	add(version_);

	algorithm_ = new AlgorithmIdentifier();
	add(algorithm_);

	encodedKey_ = new ASN1OctetString();
	add(encodedKey_);

	attributes_ = new ASN1SetOf(Attribute.class);
	add(new ASN1TaggedType(0, attributes_, false, true));
    }

    /**
     * Creates an instance with the given <b>pre encoded raw key</b>. The
     * encoded is embedded &quot;as is&quot;, the key encoding can be either a
     * DER compliant one or a special encoding.
     * 
     * Please note that the byte array returned by the <code>
     * getEncoded()</code>
     * method of the <code>Key</code> interface must not be passed to this
     * constructor because the bytes returned by this method do not contain a
     * raw key but a complete PrivateKeyInfo structure (as this one).
     * 
     * @param aid
     *                The AlgorithmIdentifier with the OID and parameters for
     *                the raw algorithm that belongs to the given key.
     * @param key
     *                The raw key that shall be wrapped in this instance.
     */
    public PrivateKeyInfo(AlgorithmIdentifier aid, byte[] key) {
	version_ = new ASN1Integer(VERSION);
	add(version_);

	algorithm_ = aid;
	add(algorithm_);

	encodedKey_ = new ASN1OctetString(key);
	add(encodedKey_);

	attributes_ = new ASN1Set();
	add(new ASN1TaggedType(0, attributes_, false, true));
    }

    /**
     * Creates an instance with the given <b>ASN.1 raw key</b>. The given raw
     * key is <b>encoded using DER</b> before it is set up in this instance.
     * <p>
     * 
     * @param aid
     *                The AlgorithmIdentifier with the OID and parameters for
     *                the raw algorithm that belongs to the given key.
     * @param key
     *                The raw key that shall be wrapped in this instance.
     * @throws InconsistentStateException
     *                 if an exception is thrown while encoding the given key.
     *                 No such exception should ever happen.
     */
    public PrivateKeyInfo(AlgorithmIdentifier aid, ASN1Type key) {
	ByteArrayOutputStream bos;
	DEREncoder enc;
	byte[] code;

	version_ = new ASN1Integer(VERSION);
	add(version_);

	algorithm_ = aid;
	add(algorithm_);

	try {
	    bos = new ByteArrayOutputStream();
	    enc = new DEREncoder(bos);
	    key.encode(enc);
	    code = bos.toByteArray();
	    enc.close();
	} catch (IOException e) {
	    throw new InconsistentStateException("Caught IOException!");
	} catch (ASN1Exception e) {
	    throw new InconsistentStateException("Caught ASN1Exception!");
	}
	encodedKey_ = new ASN1OctetString(code);
	add(encodedKey_);

	attributes_ = new ASN1Set();
	add(new ASN1TaggedType(0, attributes_, false, true));
    }

    /**
     * Creates an instance with the given private key.
     * 
     * @param key
     *                the actual private key as a java object
     * @throws NullPointerException
     *                 if the given key is <code>null</code>.
     */
    public PrivateKeyInfo(PrivateKey key) throws InvalidKeyException {
	super(2);
	setPrivateKey(key);
    }

    /**
     * Returns the {@link AlgorithmIdentifier AlgorithmIdentifier} of the
     * embedded key.
     * 
     * @return The AlgorithmIdentifier.
     */
    public AlgorithmIdentifier getAlgorithmIdentifier() {
	return algorithm_;
    }

    /**
     * Returns the private key embedded in this structure.
     * <p>
     * 
     * This method creates an PKCS8EncodedKeySpec of this instance and feeds it
     * into a key factory. In order to locate a suitable key factory, the
     * installed providers must define appropriate OID mappings.
     * 
     * @return The private key.
     * @throws InconsistentStateException
     *                 if the key spec generated by this method is rejected by
     *                 the key factory that is used to generate the key.
     * @throws NoSuchAlgorithmException
     *                 if there is no key factory registered for the algorithm
     *                 of the embedded key or no appropriate OID mapping is
     *                 defined by the installed providers.
     */
    public PrivateKey getPrivateKey() throws NoSuchAlgorithmException {
	ByteArrayOutputStream bos;
	PKCS8EncodedKeySpec spec;
	DEREncoder enc;
	KeyFactory kf;
	String alg;

	try {
	    bos = new ByteArrayOutputStream();
	    enc = new DEREncoder(bos);
	    encode(enc);
	    spec = new PKCS8EncodedKeySpec(bos.toByteArray());
	    enc.close();

	    alg = algorithm_.getAlgorithmOID().toString();
	    kf = KeyFactory.getInstance(alg);

	    return kf.generatePrivate(spec);
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
     * Initializes this instance with the given private key.
     * 
     * @param key
     *                The private key from which this instance is initialized.
     * @throws InvalidKeyException
     *                 if the given key cannot be decoded properly.
     * @throws NullPointerException
     *                 if the given key is <code>null</code>.
     */
    public void setPrivateKey(PrivateKey key) throws InvalidKeyException {
	if (key == null)
	    throw new NullPointerException("Key is null!");

	DERDecoder dec;

	clear();

	version_ = new ASN1Integer(VERSION);
	add(version_);

	algorithm_ = new AlgorithmIdentifier();
	add(algorithm_);

	encodedKey_ = new ASN1OctetString();
	add(encodedKey_);

	attributes_ = new ASN1SetOf(Attribute.class);
	add(new ASN1TaggedType(0, attributes_, false, true));

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
     * Returns the version number of this instance.
     * 
     * @return The version number.
     */
    public int getVersion() {
	return version_.getBigInteger().intValue();
    }

    /**
     * Sets the version number of this instance.
     * 
     * @param version
     *                The version number.
     */
    public void setVersion(int version) {
	version_ = new ASN1Integer(version);
	set(0, version_);
    }

    /**
     * Sets the {@link AlgorithmIdentifier AlgorithmIdentifier} of this
     * instance. This algorithm identifier must match the raw key of this
     * instance.
     * <p>
     * 
     * The given instance is set up in this structure. Side effects will occur
     * if it is modified subsequently.
     * 
     * @param aid
     *                The AlgorithmIdentifier.
     */
    public void setAlgorithm(AlgorithmIdentifier aid) {
	if (aid == null)
	    throw new NullPointerException("Algorithm identifier is null!");

	set(1, aid);
	algorithm_ = aid;
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
	    encodedKey_ = new ASN1OctetString(bos.toByteArray());
	    enc.close();
	    set(2, encodedKey_);
	} catch (ASN1Exception e) {
	    throw new InconsistentStateException("Internal, encoding error!");
	} catch (IOException e) {
	    throw new InconsistentStateException(
		    "Internal, I/O exception caught!");
	}
    }

    /**
     * Returns an unmodifiable list view on the attributes.
     * 
     * @return The attributes.
     */
    public List getAttributes() {
	if (attributes_.isOptional()) {
	    return null;
	}
	return Collections.unmodifiableList(attributes_);
    }

    /**
     * Sets the given attributes.
     * 
     * @param attributes
     *                The attributes.
     */
    public void setAttributes(Collection attributes) {
	if (attributes == null) {
	    throw new NullPointerException("Attributes instance is null!");
	}
	attributes_.clear();
	attributes_.addAll(attributes);
	attributes_.setOptional(false);
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
     * <p>
     * 
     * The returned value is a copy. No side effects are caused by modifying it.
     * 
     * @return The raw key bits as a byte array.
     */
    public byte[] getRawKey() {
	return (byte[]) encodedKey_.getByteArray().clone();
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
		    .getByteArray()));

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

}

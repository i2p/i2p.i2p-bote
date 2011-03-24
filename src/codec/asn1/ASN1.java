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
package codec.asn1;

/**
 * Defines various constants used with ASN.1 such as the tag and type class
 * identifiers. The classes in this package are modelled along the lines of
 * ITU-T Recommendations X.680, X.681, X.682, X.690, and X.691. From now on we
 * assume the reader is familiar with ASN.1, BER, and DER.
 * <p>
 * 
 * This package defines a number of primitive types as specified by the basic
 * syntax in X.680. Based on these primitive types more complex types can be
 * created. We refer to these types as <i>compound types</i> or <i> structures</i>.
 * Below, we discuss how such types are constructed, encoded and decoded using
 * the classes in this package.
 * <p>
 * 
 * For instance the type <tt>PrivateKeyInfo</tt> is defined in PKCS#8 as
 * follows using ASN.1: <blockquote>
 * 
 * <pre>
 * PrivateKeyInfo ::= SEQUENCE (
 *   version Version,
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
 * </blockquote>
 * 
 * This type can be created as follows based on the classes in this package:
 * <blockquote>
 * 
 * <pre>
 * public class PrivateKeyInfo extends ASN1Sequence
 *  {
 *    public PrivateKeyInfo()
 *     {
 *       add(new ASN1Integer());
 *       add(new AlgorithmIdentifier()); // Detailed below
 *       add(new ASN1OctetString());
 *       add(new ASN1TaggedType(
 *         0, new ASN1SetOf(Attribute.class), false, true);
 *     }
 *    ...
 *  }
 * </pre>
 * 
 * </blockquote> The {@link ASN1TaggedType tagged type} allows to define types
 * of the ASN.1 tag class UNIVERSAL, CONTEXT SPECIFIC, APPLICATION, and PRIVATE.
 * The constructor shown above is a convenience constructor that assumes the
 * class is CONTEXT SPECIFIC. The third parameter specifies that the tagging is
 * not EXPLICIT (hence IMPLICIT as required by the definition above), and the
 * fourth parameter specifies that <tt>attributes</tt> is OPTIONAL.
 * <p>
 * 
 * The interface {@link ASN1Type ASN1Type} specifies a number of methods to
 * declare types as OPTIONAL, IMPLICIT or EXPLICIT. In principle, ASN.1
 * structures can be modelled almost one to one using the classes in this
 * package.
 * <p>
 * 
 * Individual types also offer setter methods and constructors that allow to
 * preset certain values as the values of the ASN.1 types from native Java types
 * such as String, int, byte[] etc.
 * <p>
 * 
 * Once, a primitive type or a compound type <tt>x</tt> has been defined and
 * initialized it can be encoded in a number of ways. The first step is to
 * choose an {@link Encoder encoder}. One example encoder is the
 * {@link DEREncoder DEREncoder}. This encoder encodes types according to the
 * Distinguished Encoding Rules, a subset of the Basic Encoding Rules defined in
 * the ITU-T Recommendations. Encoding to a file is simple, the following code
 * snippet shows how to do it: <blockquote>
 * 
 * <pre>
 * DEREncoder enc;
 * enc = new DEREncoder(new FileOutputStream(&quot;code.der&quot;));
 * enc.writeType(x);
 * enc.close();
 * </pre>
 * 
 * </blockquote> There is nothing more to it. However, only types not declared
 * as OPTIONAL are written to the stream. Hence, the type <tt>
 * attributes</tt>
 * mentioned above is not written.
 * <p>
 * 
 * Why then is it declared as OPTIONAL? Answer, by convention in this package
 * (and by reason) the default constructor is meant to initialize a type for
 * decoding. In a code the <tt>
 * attributes</tt> type can be encountered. The
 * decoder clears the OPTIONAL flag of types it encounters during decoding so it
 * is clear which types were present and which were not. If a type is only used
 * for encoding and not for decoding then the OPTIONAL types not required can be
 * omitted.
 * 
 * <pre>
 * AlgorithmIdentifier  ::= SEQUENCE{
 *   algorithm  OBJECT IDENTIFIER,
 *   parameters ANY DEFINED BY algorithm OPTIONAL
 * }
 * </pre>
 * 
 * @author Volker Roth
 * @version "$Id: ASN1.java,v 1.4 2005/03/22 16:04:37 flautens Exp $"
 */
public final class ASN1 extends Object {
    /**
     * DOCUMENT ME!
     */
    public static final int TAG_EOC = 0;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_BOOLEAN = 1;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_INTEGER = 2;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_BITSTRING = 3;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_OCTETSTRING = 4;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_NULL = 5;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_OID = 6;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_REAL = 9;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_ENUMERATED = 10;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_UTF8STRING = 12;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_SEQUENCE = 16;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_SET = 17;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_NUMERICSTRING = 18;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_PRINTABLESTRING = 19;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_T61STRING = 20;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_VIDEOTEXTSTRING = 21;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_IA5STRING = 22;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_UTCTIME = 23;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_GENERALIZEDTIME = 24;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_GRAPHICSTRING = 25;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_VISIBLESTRING = 26;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_GENERALSTRING = 27;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_UNIVERSALSTRING = 28;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_BMPSTRING = 30;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_MASK = 0x1f;

    /**
     * DOCUMENT ME!
     */
    public static final int TAG_LONGFORM = 0x1f;

    /**
     * DOCUMENT ME!
     */
    public static final int CLASS_UNIVERSAL = 0x00;

    /**
     * DOCUMENT ME!
     */
    public static final int CLASS_APPLICATION = 0x40;

    /**
     * DOCUMENT ME!
     */
    public static final int CLASS_CONTEXT = 0x80;

    /**
     * DOCUMENT ME!
     */
    public static final int CLASS_PRIVATE = 0xc0;

    /**
     * DOCUMENT ME!
     */
    public static final int CLASS_MASK = 0xc0;

    /**
     * DOCUMENT ME!
     */
    public static final int PRIMITIVE = 0x00;

    /**
     * DOCUMENT ME!
     */
    public static final int CONSTRUCTED = 0x20;

    /**
     * DOCUMENT ME!
     */
    public static final int LENGTH_LONGFORM = 0x80;

    /**
     * DOCUMENT ME!
     */
    public static final int LENGTH_MASK = 0x7f;

    /**
     * No-one can instantiate this class.
     */
    private ASN1() {
    }
}

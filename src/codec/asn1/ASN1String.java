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
 * The common interface of all ASN.1 string types. This interface specifies
 * setter and getter methods for string values and methods for string to octet
 * and octet to string conversion. See {@link ASN1AbstractString
 * ASN1AbstractString} for details on strings.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1String.java,v 1.2 2000/12/06 17:47:26 vroth Exp $"
 * @see ASN1AbstractString
 */
public interface ASN1String extends ASN1Type {
    /**
     * Returns the represented string value.
     * 
     * @return The string value of this type.
     */
    public String getString();

    /**
     * Sets the string value.
     * 
     * @param s
     *                The string value.
     */
    public void setString(String s) throws ConstraintException;

    /**
     * Converts the given byte array to a string.
     * 
     * @param b
     *                The byte array to convert.
     */
    public String convert(byte[] b) throws ASN1Exception;

    /**
     * Converts the given string to a byte array.
     * 
     * @param s
     *                The string to convert.
     */
    public byte[] convert(String s) throws ASN1Exception;

    /**
     * Returns the number of octets required to encode the given string
     * according to the basic encoding scheme of this type. For restricted
     * string types this likely equals the number of characters in the string
     * unless special characters or escape sequences are allowed. For
     * {@link ASN1BMPString BMPStrings} this is twice the number of characters
     * and for {@link ASN1UniversalString UniversalStrings} it is four times the
     * number of characters in the string.
     * <p>
     * 
     * The number returned must equal the number returned by the method call
     * {@link #convert(java.lang.String) convert(s)}. This method is required
     * for DER encoding of string types in order to determine the number of
     * octets required for encoding the given string. For BER encoding this
     * method is not and the encoding of the string may be broken up into
     * consecutive OCTET STRINGS.
     * 
     * @param s
     *                The string whose encoding length is determined.
     */
    public int convertedLength(String s) throws ASN1Exception;

}

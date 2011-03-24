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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * @author Volker Roth
 * @version "$Id: AbstractEncoder.java,v 1.3 2005/03/22 15:55:36 flautens Exp $"
 */
public abstract class AbstractEncoder extends FilterOutputStream implements
	Encoder {
    /**
     * Creates an encoder that writes its output to the given output stream.
     * 
     * @param out
     *                The output stream to which the encoded ASN.1 objects are
     *                written.
     */
    public AbstractEncoder(OutputStream out) {
	super(out);
    }

    /**
     * 
     */
    public void writeType(ASN1Type t) throws ASN1Exception, IOException {
	t.encode(this);
    }

    /**
     * This method encodes identifier and length octets. The given length can be
     * negative in which case 0x80 is written to indicate INDEFINITE LENGTH
     * encoding. Please note that this is appropriate only for a BER encoding or
     * CER encoding (ITU-T Recommenation X.690). Encoders are responsible for
     * writing the end of code octets <code>0x00 0x00</code> after encoding
     * the content octets.
     * 
     * @param tag
     *                The ASN.1 tag
     * @param cls
     *                The ASN.1 tag class.
     * @param prim
     *                <code>true</code> if the encoding is PRIMITIVE and
     *                <code>false</code> if it is CONSTRUCTED.
     * @param len
     *                The number of content octets or -1 to indicate INDEFINITE
     *                LENGTH encoding.
     */
    protected void writeHeader(int tag, int cls, boolean prim, int len)
	    throws IOException {
	int b;
	int i;

	b = cls & ASN1.CLASS_MASK;

	if (!prim) {
	    b = b | ASN1.CONSTRUCTED;
	}

	if (tag > 30) {
	    b = b | ASN1.TAG_MASK;
	    out.write(b);
	    writeBase128(tag);
	} else {
	    b = b | tag;
	    out.write(b);
	}
	if (len == -1) {
	    out.write(0x80);
	} else {
	    if (len > 127) {
		i = (significantBits(len) + 7) / 8;
		out.write(i | 0x80);
		writeBase256(len);
	    } else {
		out.write(len);
	    }
	}
    }

    /**
     * This method computes the number of octets needed to encode the identifier
     * and length octets of the {@link ASN1Type ASN.1 type} with the given tag
     * and contents length. The given length can be negative in which case
     * INDEFINITE LENGTH encoding is assumed.
     * 
     * @return The number of octets required for encoding the identifier and
     *         length octets.
     * @param tag
     *                The ASN.1 tag.
     * @param len
     *                The number of contents octets of the ASN.1 type with the
     *                given tag and length.
     */
    protected int getHeaderLength(int tag, int len) {
	int n;

	n = 2;
	if (tag > 30) {
	    n = n + ((significantBits(tag) + 6) / 7);
	}

	if (len > 127) {
	    n = n + ((significantBits(len) + 7) / 8);
	}

	return n;
    }

    /**
     * Writes the given integer to the output in base 128 representation with
     * bit 7 of all octets except the last one being set to &quot;1&quot;. The
     * minimum number of octets necessary is used.
     * 
     * @param n
     *                The integer to be written to the output.
     * @throws IOException
     *                 Thrown by the underlying output stream.
     */
    protected void writeBase128(int n) throws IOException {
	int i;
	int j;

	i = (significantBits(n) + 6) / 7;
	j = (i - 1) * 7;

	while (i > 1) {
	    out.write(((n >>> j) & 0x7f) | 0x80);
	    j = j - 7;
	    i--;
	}
	out.write(n & 0x7f);
    }

    /**
     * Writes the given integer to the output in base 256 with the minimal
     * number of octets.
     * 
     * @param n
     *                The integer to be written to the output.
     * @throws IOException
     *                 Thrown by the underlying output stream.
     */
    protected void writeBase256(int n) throws IOException {
	int i;
	int j;

	i = (significantBits(n) + 7) / 8;
	j = (i - 1) * 8;

	while (i > 0) {
	    out.write((n >>> j) & 0xff);
	    j = j - 8;
	    i--;
	}
    }

    /**
     * Counts the number of significant bits in the given integer. There is
     * always at least one significant bit.
     * 
     * @param n
     *                The integer.
     * @return The number of significant bits in the given integer.
     */
    protected int significantBits(int n) {
	int i;

	if (n == 0) {
	    return 1;
	}

	i = 0;
	while (n > 255) {
	    n = n >>> 8;
	    i += 8;
	}
	while (n > 0) {
	    n = n >>> 1;
	    i++;
	}
	return i;
    }
}

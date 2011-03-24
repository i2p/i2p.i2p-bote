package de.flexiprovider.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import codec.asn1.ASN1Exception;
import codec.asn1.ASN1Integer;
import codec.asn1.ASN1Type;
import codec.asn1.DERDecoder;
import codec.asn1.DEREncoder;
import de.flexiprovider.common.math.FlexiBigInt;

/**
 * ASN.1 utility class. Used to translate between {@link FlexiBigInt} and
 * {@link java.math.BigInteger} types. Provides DER encoding and decoding
 * methods.
 * 
 * @author Martin Döring
 */
public final class ASN1Tools {

    /**
     * Default constructor (private).
     */
    private ASN1Tools() {
	// empty
    }

    /**
     * Create a new {@link ASN1Integer} from the given {@link FlexiBigInt}
     * value.
     * 
     * @param value
     *                the {@link FlexiBigInt} value
     * @return a new {@link ASN1Integer} holding the {@link FlexiBigInt} value
     */
    public static ASN1Integer createInteger(FlexiBigInt value) {
	return new ASN1Integer(value.bigInt);
    }

    /**
     * Get the {@link FlexiBigInt} value from the given {@link ASN1Integer}.
     * 
     * @param value
     *                the {@link ASN1Integer}
     * @return the {@link FlexiBigInt} value stored in the {@link ASN1Integer}
     */
    public static FlexiBigInt getFlexiBigInt(ASN1Integer value) {
	return new FlexiBigInt(value.getBigInteger());
    }

    /**
     * DER encode the given ASN.1 structure.
     * 
     * @param type
     *                the ASN.1 structure
     * @return the DER encoded ASN.1 structure
     */
    public static byte[] derEncode(ASN1Type type) {
	try {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    DEREncoder encoder = new DEREncoder(baos);
	    type.encode(encoder);
	    byte[] result = baos.toByteArray();
	    encoder.close();
	    return result;
	} catch (ASN1Exception e) {
	    throw new RuntimeException("ASN1Exception: " + e.getMessage());
	} catch (IOException e) {
	    throw new RuntimeException("IOException: " + e.getMessage());
	}
    }

    /**
     * Decode the given DER encoded ASN.1 structure.
     * 
     * @param encoding
     *                the encoded ASN.1 structure
     * @param type
     *                the type holding the decoding
     * @throws IOException
     *                 on decoding errors.
     * @throws ASN1Exception
     *                 on decoding errors.
     */
    public static void derDecode(byte[] encoding, ASN1Type type)
	    throws ASN1Exception, IOException {
	ByteArrayInputStream bais = new ByteArrayInputStream(encoding);
	DERDecoder decoder = new DERDecoder(bais);
	type.decode(decoder);
	decoder.close();
    }

}

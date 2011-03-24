package de.flexiprovider.common.math.polynomials;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

import codec.asn1.ASN1Exception;
import codec.asn1.ASN1Integer;
import codec.asn1.ASN1Sequence;
import codec.asn1.ASN1SequenceOf;
import codec.asn1.DERDecoder;
import codec.asn1.DEREncoder;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.SecureRandom;
import de.flexiprovider.common.util.ASN1Tools;
import de.flexiprovider.common.util.IntUtils;

/**
 * An Element of this class represents a Polynomial within a GFP Ring Structure.
 * <p>
 * The Structure is defined by the modulo Function and the large "prime" p. This
 * Class has methods for multiplying, adding and reducing Polynomials.
 * <p>
 * This Class has been developed mainly for the LMOTS Signature scheme, so the
 * available methods may not be a complete Implementation of the methods that
 * may be required or expected on Ring Arithmetic and some of the implemented
 * Methods are intended only for use of the LMOTS Signature scheme. Such as the
 * ability to create random Polynomials within this Ring Structure with a custom
 * modulo limit or Multiplication of a Polynomial with a Vector of Polynomials.
 */
public class GFP32Polynomial {

	private int[] f;
	private int[] poly;
	private int degree;
	private int p;

	private SecureRandom generator;

	/**
	 * Constructor for decoding a previously encoded {@link GFP32Polynomial}
	 * using the getEncoded() method
	 * 
	 * @param encoded
	 *            the byte array containing the encoded {@link GFP32Polynomial}
	 * @throws IOException
	 * @throws ASN1Exception
	 */
	public GFP32Polynomial(byte[] encoded) throws ASN1Exception, IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(encoded);
		DERDecoder decoder = new DERDecoder(in);
		ASN1Sequence gfpSequence = new ASN1Sequence(3);
		gfpSequence.add(new ASN1SequenceOf(ASN1Integer.class));
		gfpSequence.add(new ASN1Integer());
		gfpSequence.add(new ASN1SequenceOf(ASN1Integer.class));
		gfpSequence.decode(decoder);
		in.close();

		ASN1SequenceOf asn1F = (ASN1SequenceOf) gfpSequence.get(0);
		ASN1Integer asn1P = (ASN1Integer) gfpSequence.get(1);
		ASN1SequenceOf asn1Poly = (ASN1SequenceOf) gfpSequence.get(2);

		int[] poly = new int[asn1Poly.size()];
		for (int i = poly.length - 1; i >= 0; i--) {
			poly[i] = ASN1Tools.getFlexiBigInt((ASN1Integer) asn1Poly.get(i))
					.intValue();
		}
		this.poly = poly;
		int[] f = new int[asn1F.size()];
		degree = f.length - 1;
		for (int i = degree; i >= 0; i--) {
			f[i] = ASN1Tools.getFlexiBigInt((ASN1Integer) asn1F.get(i))
					.intValue();
		}
		this.f = f;
		p = ASN1Tools.getFlexiBigInt(asn1P).intValue();

		generator = Registry.getSecureRandom();
	}

	/**
	 * Standard Constructor for generating a new GFPPolynomial
	 * 
	 * @param f
	 *            the modulo Polynomial of the Ring
	 * @param p
	 *            the modulo "prime" of the Ring
	 * @param poly
	 *            the Polynomial an int array, most significant entry is right
	 */
	public GFP32Polynomial(int[] f, int p, int[] poly) {
		this.f = f;
		degree = f.length - 1;
		this.p = p;
		this.poly = reduce(poly);
		generator = Registry.getSecureRandom();
	}

	/**
	 * Special Constructor without a Polynomial parameter but with a Secure
	 * Random generator. This Constructor can only be used for methods which do
	 * not require a Polynomial to be present, such as generatePoly()
	 * 
	 * @param f
	 *            the modulo Polynomial of the Ring
	 * @param p
	 *            the modulo "prime" of the Ring
	 * @param gen
	 *            a predefined secure Random Number Generator
	 */
	public GFP32Polynomial(int[] f, int p, SecureRandom gen) {
		this.f = f;
		degree = f.length - 1;
		this.p = p;
		generator = Registry.getSecureRandom();
	}

	/**
	 * adds the given Polynomial to this Polynomial and returns the result
	 * 
	 * @param gfp
	 *            the Polynomial to be added
	 * @return the Addition of the two Polynomials
	 */
	public GFP32Polynomial add(GFP32Polynomial gfp) {
		if (!paramEqual(gfp)) {
			return null;
		}
		int[] b = gfp.getPoly();
		int[] a = poly;
		if (a.length < b.length) {
			a = b;
			b = poly;
		}

		int[] result = new int[a.length];

		for (int i = a.length - 1; i >= 0; i--) {
			result[i] = a[i];
			if (i < b.length) {
				result[i] = (result[i] + b[i]) % p;
			}
		}

		return new GFP32Polynomial(f, p, reduce(result));
	}

	/**
	 * Adds the supplied Polynomial to this Polynomial and sets this Polynomial
	 * as the result
	 * 
	 * @param gfp
	 *            the Polynomial to be added
	 */
	public void addToThis(GFP32Polynomial gfp) {
		poly = add(gfp).getPoly();
	}

	private int[] compress(int[] a) {
		return compress(a, p);
	}

	private int[] compress(int[] a, int p) {
		for (int i = a.length; i > 0; i--) {
			if (a[i - 1] > (p / 2)) {
				a[i - 1] -= p;
			}
		}
		return a;
	}

	/**
	 * Subtracts p from every entry in this polynomial with a value greater p/2.
	 * <p>
	 * This Function is used for calculating the correct Norm of a Polynomial,
	 * since the Norm uses the absolute Value for determining the Maximum
	 * 
	 * @return the compressed polynomial
	 */
	public int[] compressThis() {
		return compress(poly);
	}

	public boolean equals(Object other) {
		if ((other == null) || !(other instanceof GFP32Polynomial)) {
			return false;
		}

		GFP32Polynomial otherPol = (GFP32Polynomial) other;
		return ((p == otherPol.p) && IntUtils.equals(f, otherPol.f) && IntUtils
				.equals(compressThis(), otherPol.compressThis()));
	}

	/**
	 * also compares SecureRandom, but since that class has no default equals
	 * operator, this method is useless (for now)
	 * 
	 * @param gfp
	 *            the {@link GFP32Polynomial} to compare to.
	 * @return
	 */
	private boolean fullEquals(GFP32Polynomial gfp) {
		return ((p == gfp.getP()) && IntUtils.equals(f, gfp.getF())
				&& IntUtils.equals(poly, gfp.getPoly()) && generator.equals(gfp
				.getRandomizer()));
	}

	/**
	 * Generates a random Polynomial, complying to the specification of this
	 * Ring
	 * 
	 * @return the randomly generated Polynomial
	 */
	public GFP32Polynomial generatePoly() {
		return generatePoly(0);
	}

	/**
	 * Generates a random Polynomial with the specified limit, denoting the
	 * maximum Value of entries in this Polynomial.
	 * 
	 * @param limit
	 *            the limit to be used for modulo in the generated Polynomial.
	 *            Only used if the supplied number is lower than p, otherwise p
	 *            is used.
	 * @return the randomly generated Polynomial
	 */
	public GFP32Polynomial generatePoly(int limit) {
		return generatePoly(limit, false);
	}

	public GFP32Polynomial generatePoly(int limit, boolean negative) {
		if ((limit == 0) || (limit > p)) {
			limit = p;
		}
		int[] resPoly = new int[degree];
		for (int i = degree; i > 0; i--) {
			if (negative) {
				resPoly[i - 1] = generator.nextInt((limit * 2) - 1);
			} else {
				resPoly[i - 1] = generator.nextInt(limit);
			}
		}
		if (negative) {
			return new GFP32Polynomial(f, p, compress(resPoly, (limit * 2) - 1));
		}
		return new GFP32Polynomial(f, p, reduceZeros(resPoly));
	}

	public byte[] getEncoded() throws ASN1Exception, IOException {
		// TODO use ASN.1
		ASN1Sequence gfpSequence = new ASN1Sequence(3);
		ASN1Integer asn1P = new ASN1Integer(p);
		ASN1SequenceOf asn1Poly = new ASN1SequenceOf(ASN1Integer.class);
		ASN1SequenceOf asn1F = new ASN1SequenceOf(ASN1Integer.class);
		for (int i = 0; i < poly.length; i++) {
			asn1Poly.add(new ASN1Integer(poly[i]));
		}
		for (int i = 0; i < f.length; i++) {
			asn1F.add(new ASN1Integer(f[i]));
		}
		gfpSequence.add(asn1F);
		gfpSequence.add(asn1P);
		gfpSequence.add(asn1Poly);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		gfpSequence.encode(new DEREncoder(baos));
		byte[] res = baos.toByteArray();
		baos.flush();
		baos.close();

		return res;
	}

	public int[] getF() {
		return f;
	}

	public int getP() {
		return p;
	}

	/**
	 * 
	 * @return The (reduced) Polynomial as an int array
	 */
	public int[] getPoly() {
		return poly;
	}

	public SecureRandom getRandomizer() {
		return generator;
	}

	private int[] mod(int[] poly) {
		for (int i = poly.length; i > 0; i--) {
			if (poly[i - 1] < 0) {
				poly[i - 1] = (poly[i - 1] % p) + p;
			} else {
				poly[i - 1] = poly[i - 1] % p;
			}
		}
		return poly;
	}

	/**
	 * multiplies the given Polynomial to this Polynomial and returns the Result
	 * 
	 * @param gfp
	 *            the Polynomial to be multiplied
	 * @return the Product of the two Polynomials
	 */
	public GFP32Polynomial multiply(GFP32Polynomial gfp) {
		int[] a = poly;
		int[] z = gfp.getPoly();
		int degree = a.length + z.length - 1;
		int[] result = new int[degree];
		// Arrays.fill(result, 0);

		a = compress(a);
		z = compress(z);

		for (int i = a.length - 1; i >= 0; i--) {
			for (int j = z.length - 1; j >= 0; j--) {
				long l = a[i];
				long k = z[j];
				k = (l * k) % p;
				result[i + j] = ((int) k + result[i + j]) % p;
			}
		}

		return new GFP32Polynomial(f, p, reduce(result));
	}

	/**
	 * multiplies this Polynomial with a Vector of Polynomials and returns the
	 * Result
	 * <p>
	 * Multiplication with a Vector is designed as follows:
	 * <p>
	 * Vector � = (a1, a2, ... , am), Polynomial p � * p = (a1*p, a2*p, ...
	 * , am * p)
	 * 
	 * @param k
	 *            the Vector of Polynomials to be multiplied
	 * @return the Vector of the Product
	 */
	public Vector multiply(Vector k) {
		Vector result = new Vector();
		result.setSize(k.size());

		for (int i = k.size() - 1; i >= 0; i--) {
			GFP32Polynomial next = (GFP32Polynomial) k.elementAt(i);
			result.setElementAt(multiply(next), i);

		}

		return result;
	}

	/**
	 * Multiplies the given Polynomial to this Polynomial and sets this
	 * Polynomial to the Result
	 * 
	 * @param gfp
	 *            the Polynomial to be multiplied
	 */
	public void multiplyToThis(GFP32Polynomial gfp) {
		poly = multiply(gfp).getPoly();
	}

	public boolean paramEqual(GFP32Polynomial gfp) {
		return ((p == gfp.p) && IntUtils.equals(f, gfp.f));
	}

	public void print() {
		System.out.println("printing GFP");
		System.out.println("p: " + getP());
		System.out.println("f: " + printPoly(getF()));
		System.out.println("poly: " + printPoly(mod(getPoly())));
	}

	private String printPoly(int[] arr) {
		String result = "{";
		for (int i = arr.length - 1; i > 0; i--) {
			result += arr[i] + ", ";
		}
		result += arr[0] + "}";
		return result;
	}

	/**
	 * This Methods reduces a Polynomial in int array representation, with the
	 * most significant value rightmost. This means that the supplied Polynomial
	 * will be calculated modulo the Ring Polynomial f and the remainder is
	 * returned.
	 * 
	 * @param z
	 *            The supplied Polynomial to be reduced
	 * @return the remainder of the reduced Polynomial
	 */
	private int[] reduce(int[] z) {
		z = reduceZeros(mod(z));
		if (z.length < f.length) {
			return z;
		}
		int exp = z.length - f.length;
		long v = ((-z[z.length - 1]) + p) % p;
		int zSize = z.length - 1;
		int[] newZ = new int[zSize];
		for (int i = zSize; i > 0; i--) {
			if (i <= exp) {
				newZ[i - 1] = z[i - 1];
			} else {
				long l = f[i - exp - 1];
				long k = (l * v) % p;
				newZ[i - 1] = ((int) k + z[i - 1]) % p;
			}
		}
		return reduce(newZ);
	}

	private int[] reduceZeros(int[] z) {
		int zSize = z.length;
		for (int i = z.length; i > 0; i--) {
			if (z[i - 1] == 0) {
				zSize--;
			} else {
				break;
			}
		}

		if (zSize == z.length) {
			return z;
		}
		int[] newZ = new int[zSize];
		System.arraycopy(z, 0, newZ, 0, zSize);
		return newZ;
	}

	public GFP32Polynomial subtract(GFP32Polynomial gfp) {
		if (!paramEqual(gfp)) {
			return null;
		}
		int[] b = gfp.getPoly();
		int[] a = poly;

		int[] result = new int[Math.max(a.length, b.length)];

		for (int i = result.length - 1; i >= 0; i--) {
			if (i < b.length && i < a.length) {
				result[i] = (a[i] - b[i]) % p;
			} else if (i < b.length) {
				result[i] = -b[i];
			} else {
				result[i] = a[i];
			}
		}

		return new GFP32Polynomial(f, p, reduce(result));
	}

	public void subtractFromThis(GFP32Polynomial gfp) {
		poly = subtract(gfp).getPoly();
	}

	// public static void main(String[] args) throws ASN1Exception, IOException
	// {
	// GFPPolynomial gfp = new GFPPolynomial(new int[]{5, 0, 0, 0, 0, 0, 0, 0,
	// 1}, 19, new int[]{2, 5, 7, 12, 239, 124, 12312, 123, 23, 5325, 6322,
	// 12322});
	// byte[] testArr = gfp.getEncoded();
	//
	// System.out.println(new FlexiBigInt(testArr));
	// GFPPolynomial gfp2 = new GFPPolynomial(testArr);
	// gfp.print();
	// gfp2.print();
	// if (gfp.equals(gfp2)) {
	// System.out.println("YAHOOOOO");
	// }
	// }
}

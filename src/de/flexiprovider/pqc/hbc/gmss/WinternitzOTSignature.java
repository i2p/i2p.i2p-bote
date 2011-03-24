package de.flexiprovider.pqc.hbc.gmss;

import de.flexiprovider.api.MessageDigest;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;
import de.flexiprovider.api.exceptions.SignatureException;
import de.flexiprovider.core.CoreRegistry;

/**
 * This class implements key pair generation and signature generation of the
 * Winternitz one-time signature scheme (OTSS), described in C.Dods, N.P. Smart,
 * and M. Stam, "Hash Based Digital Signature Schemes", LNCS 3796, pages
 * 96&#8211;115, 2005. The class is used by the GMSS classes.
 * 
 * @author Elena Klintsevich
 * 
 */

public class WinternitzOTSignature {

	/**
	 * The hash function used by the OTS
	 */
	private MessageDigest messDigestOTS;

	/**
	 * The length of the message digest and private key
	 */
	private int mdsize, keysize;

	/**
	 * An array of strings, containing the name of the used hash function, the
	 * name of the PRGN and the names of the corresponding providers
	 */
	// private String[] name = new String[2];
	/**
	 * The private key
	 */
	private byte[][] privateKeyOTS;

	/**
	 * The Winternitz parameter
	 */
	private int w;

	/**
	 * The source of randomness for OTS private key generation
	 */
	private GMSSRandom gmssRandom;

	/**
	 * Sizes of the message and the checksum, both
	 */
	private int messagesize, checksumsize;

	/**
	 * The constructor generates an OTS key pair, using <code>seed0</code> and
	 * the PRNG
	 * <p>
	 * 
	 * @param seed0
	 *            the seed for the PRGN
	 * @param name
	 *            an array of strings, containing the name of the used hash
	 *            function, the name of the PRGN and the names of the
	 *            corresponding providers
	 * @param w
	 *            the Winternitz parameter
	 */
	public WinternitzOTSignature(byte[] seed0, String[] name, int w) {
		// this.name = name;
		this.w = w;

		CoreRegistry.registerAlgorithms();
		try {
			messDigestOTS = Registry.getMessageDigest(name[0]);
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println(": message digest " + name[0] + " not found in "
					+ name[1]);
		}

		gmssRandom = new GMSSRandom(messDigestOTS);

		// calulate keysize for private and public key and also the help
		// array

		mdsize = messDigestOTS.getDigestLength();
		int mdsizeBit = mdsize << 3;
		messagesize = (int) Math.ceil((double) (mdsizeBit) / (double) w);

		checksumsize = getLog((messagesize << w) + 1);

		keysize = messagesize
				+ (int) Math.ceil((double) checksumsize / (double) w);

		/*
		 * mdsize = messDigestOTS.getDigestLength(); messagesize =
		 * ((mdsize<<3)+(w-1))/w;
		 * 
		 * checksumsize = getlog((messagesize<<w)+1);
		 * 
		 * keysize = messagesize + (checksumsize+w-1)/w;
		 */
		// define the private key messagesize
		privateKeyOTS = new byte[keysize][mdsize];

		// gmssRandom.setSeed(seed0);
		byte[] dummy = new byte[mdsize];
		System.arraycopy(seed0, 0, dummy, 0, dummy.length);

		// generate random bytes and
		// assign them to the private key
		for (int i = 0; i < keysize; i++)
			privateKeyOTS[i] = gmssRandom.nextSeed(dummy);
	}

	/**
	 * @return The private OTS key
	 */
	public byte[][] getPrivateKey() {
		return privateKeyOTS;
	}

	/**
	 * @return The public OTS key
	 */
	public byte[] getPublicKey() {
		byte[] helppubKey = new byte[keysize * mdsize];

		byte[] help = new byte[mdsize];
		int two_power_t = 1 << w;

		for (int i = 0; i < keysize; i++) {
			// hash w-1 time the private key and assign it to the public key
			messDigestOTS.update(privateKeyOTS[i]);
			help = messDigestOTS.digest();
			for (int j = 2; j < two_power_t; j++) {
				messDigestOTS.update(help);
				help = messDigestOTS.digest();
			}
			System.arraycopy(help, 0, helppubKey, mdsize * i, mdsize);
		}

		messDigestOTS.update(helppubKey);
		return messDigestOTS.digest();
	}

	/**
	 * @return The one-time signature of the message, generated with the private
	 *         key
	 */
	public byte[] getSignature(byte[] message) throws SignatureException {
		byte[] sign = new byte[keysize * mdsize];
		// byte [] message; // message m as input
		byte[] hash = new byte[mdsize]; // hash of message m
		int counter = 0;
		int c = 0;
		int test = 0;
		// create hash of message m
		messDigestOTS.update(message);
		hash = messDigestOTS.digest();

		if (8 % w == 0) {
			int d = 8 / w;
			int k = (1 << w) - 1;
			byte[] hlp = new byte[mdsize];

			// create signature
			for (int i = 0; i < hash.length; i++)
				for (int j = 0; j < d; j++) {
					test = hash[i] & k;
					c += test;

					System.arraycopy(privateKeyOTS[counter], 0, hlp, 0, mdsize);

					while (test > 0) {
						messDigestOTS.update(hlp);
						hlp = messDigestOTS.digest();
						test--;
					}
					System.arraycopy(hlp, 0, sign, counter * mdsize, mdsize);
					hash[i] = (byte) (hash[i] >>> w);
					counter++;
				}

			c = (messagesize << w) - c;
			for (int i = 0; i < checksumsize; i += w) {
				test = c & k;

				System.arraycopy(privateKeyOTS[counter], 0, hlp, 0, mdsize);

				while (test > 0) {
					messDigestOTS.update(hlp);
					hlp = messDigestOTS.digest();
					test--;
				}
				System.arraycopy(hlp, 0, sign, counter * mdsize, mdsize);
				c >>>= w;
				counter++;
			}
		} else if (w < 8) {
			int d = mdsize / w;
			int k = (1 << w) - 1;
			byte[] hlp = new byte[mdsize];
			long big8;
			int ii = 0;
			// create signature
			// first d*w bytes of hash
			for (int i = 0; i < d; i++) {
				big8 = 0;
				for (int j = 0; j < w; j++) {
					big8 ^= (hash[ii] & 0xff) << (j << 3);
					ii++;
				}
				for (int j = 0; j < 8; j++) {
					test = (int) (big8 & k);
					c += test;

					System.arraycopy(privateKeyOTS[counter], 0, hlp, 0, mdsize);

					while (test > 0) {
						messDigestOTS.update(hlp);
						hlp = messDigestOTS.digest();
						test--;
					}
					System.arraycopy(hlp, 0, sign, counter * mdsize, mdsize);
					big8 >>>= w;
					counter++;
				}
			}
			// rest of hash
			d = mdsize % w;
			big8 = 0;
			for (int j = 0; j < d; j++) {
				big8 ^= (hash[ii] & 0xff) << (j << 3);
				ii++;
			}
			d <<= 3;
			for (int j = 0; j < d; j += w) {
				test = (int) (big8 & k);
				c += test;

				System.arraycopy(privateKeyOTS[counter], 0, hlp, 0, mdsize);

				while (test > 0) {
					messDigestOTS.update(hlp);
					hlp = messDigestOTS.digest();
					test--;
				}
				System.arraycopy(hlp, 0, sign, counter * mdsize, mdsize);
				big8 >>>= w;
				counter++;
			}

			// check bytes
			c = (messagesize << w) - c;
			for (int i = 0; i < checksumsize; i += w) {
				test = c & k;

				System.arraycopy(privateKeyOTS[counter], 0, hlp, 0, mdsize);

				while (test > 0) {
					messDigestOTS.update(hlp);
					hlp = messDigestOTS.digest();
					test--;
				}
				System.arraycopy(hlp, 0, sign, counter * mdsize, mdsize);
				c >>>= w;
				counter++;
			}
		}// end if(w<8)
		else if (w < 57) {
			int d = (mdsize << 3) - w;
			int k = (1 << w) - 1;
			byte[] hlp = new byte[mdsize];
			long big8, test8;
			int r = 0;
			int s, f, rest, ii;
			// create signature
			// first a*w bits of hash where a*w <= 8*mdsize < (a+1)*w
			while (r <= d) {
				s = r >>> 3;
				rest = r % 8;
				r += w;
				f = (r + 7) >>> 3;
				big8 = 0;
				ii = 0;
				for (int j = s; j < f; j++) {
					big8 ^= (hash[j] & 0xff) << (ii << 3);
					ii++;
				}

				big8 >>>= rest;
				test8 = (big8 & k);
				c += test8;

				System.arraycopy(privateKeyOTS[counter], 0, hlp, 0, mdsize);
				while (test8 > 0) {
					messDigestOTS.update(hlp);
					hlp = messDigestOTS.digest();
					test8--;
				}
				System.arraycopy(hlp, 0, sign, counter * mdsize, mdsize);
				counter++;

			}
			// rest of hash
			s = r >>> 3;
			if (s < mdsize) {
				rest = r % 8;
				big8 = 0;
				ii = 0;
				for (int j = s; j < mdsize; j++) {
					big8 ^= (hash[j] & 0xff) << (ii << 3);
					ii++;
				}

				big8 >>>= rest;
				test8 = (big8 & k);
				c += test8;

				System.arraycopy(privateKeyOTS[counter], 0, hlp, 0, mdsize);
				while (test8 > 0) {
					messDigestOTS.update(hlp);
					hlp = messDigestOTS.digest();
					test8--;
				}
				System.arraycopy(hlp, 0, sign, counter * mdsize, mdsize);
				counter++;
			}
			// check bytes
			c = (messagesize << w) - c;
			for (int i = 0; i < checksumsize; i += w) {
				test8 = (c & k);

				System.arraycopy(privateKeyOTS[counter], 0, hlp, 0, mdsize);

				while (test8 > 0) {
					messDigestOTS.update(hlp);
					hlp = messDigestOTS.digest();
					test8--;
				}
				System.arraycopy(hlp, 0, sign, counter * mdsize, mdsize);
				c >>>= w;
				counter++;
			}
		}// end if(w<57)

		return sign;
	}

	/**
	 * This method returns the least integer that is greater or equal to the
	 * logarithm to the base 2 of an integer <code>intValue</code>.
	 * 
	 * @param intValue
	 *            an integer
	 * @return The least integer greater or equal to the logarithm to the base 2
	 *         of <code>intValue</code>
	 */
	public int getLog(int intValue) {
		int log = 1;
		int i = 2;
		while (i < intValue) {
			i <<= 1;
			log++;
		}
		return log;
	}

}

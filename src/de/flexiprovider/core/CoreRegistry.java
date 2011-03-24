package de.flexiprovider.core;

import de.flexiprovider.api.Registry;
import de.flexiprovider.core.md.SHA1;
import de.flexiprovider.core.md.SHA224;
import de.flexiprovider.core.md.SHA256;
import de.flexiprovider.core.md.SHA384;
import de.flexiprovider.core.md.SHA512;

/**
 * Register all algorithms of the <a href="package.html">core package</a>.
 */
public abstract class CoreRegistry extends Registry {

	// flag indicating if algorithms already have been registered
	private static boolean registered = false;

	/**
	 * Register all algorithms of the <a href="package.html">core package</a>.
	 */
	public static void registerAlgorithms() {
		if (!registered) {
			registerSHAfamily();
			registered = true;
		}
	}

	private static void registerSHAfamily() {
		add(MESSAGE_DIGEST, SHA1.class, new String[] { SHA1.ALG_NAME,
				SHA1.ALG_NAME2, SHA1.OID });
		add(MESSAGE_DIGEST, SHA224.class, new String[] { SHA224.ALG_NAME,
				SHA224.OID });
		add(MESSAGE_DIGEST, SHA256.class, new String[] { SHA256.ALG_NAME,
				SHA256.OID });
		add(MESSAGE_DIGEST, SHA384.class, new String[] { SHA384.ALG_NAME,
				SHA384.OID });
		add(MESSAGE_DIGEST, SHA512.class, new String[] { SHA512.ALG_NAME,
				SHA512.OID });
	}

}

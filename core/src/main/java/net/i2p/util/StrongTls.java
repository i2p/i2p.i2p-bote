/*
 * free (adj.): unencumbered; not under the control of others
 * Written by str4d in 2015 and released into the public domain 
 * with no warranty of any kind, either expressed or implied.  
 * It probably won't make your computer catch on fire, or eat 
 * your children, but it might.  Use at your own risk.
 *
 */

package net.i2p.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constants to use strong protocols and cipher suites with the TLS transport layer.
 * Selected protocols and cipher suites have been chosen based on the categories in
 * https://wiki.mozilla.org/Security/Server_Side_TLS
 * <p>
 * See https://www.owasp.org/index.php/Transport_Layer_Protection_Cheat_Sheet for
 * general advice.
 * <p>
 * See https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html
 * for the standard names.
 * <p>
 * Inspired by the 2009 version provided by
 * <a href="http://blog.trifork.com/2009/11/10/securing-connections-with-tls/">
 * Erik van Oosten</a>.
 *
 * @author str4d
 */
public class StrongTls {
    private static final Double javaVersion = Double.parseDouble(System.getProperty("java.specification.version"));

    /**
     * Protocols from the "Modern" category.
     */
    public static final String[] MODERN_PROTOCOLS = new String[] {
        "TLSv1.2",
        "TLSv1.1",
    };

    /**
     * Protocols from the "Intermediate" category.
     */
    public static final String[] INTERMEDIATE_PROTOCOLS = new String[] {
        "TLSv1.2",
        "TLSv1.1",
        "TLSv1",
    };

    /**
     * Protocols from the "Old" category.
     */
    public static final String[] OLD_PROTOCOLS = new String[] {
        "TLSv1.2",
        "TLSv1.1",
        "TLSv1",
        "SSLv3",
    };

    /**
     * Protocols that should never be used.
     */
    public static final String[] WEAK_PROTOCOLS = new String[] {
        "SSLv2Hello",
        "SSLv2",
    };

    /**
     * Cipher suites from the "Modern" category.
     */
    public static final String[] MODERN_CIPHER_SUITES = new String[] {

        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",

    };

    /**
     * Cipher suites from the "Intermediate" category.
     */
    public static final String[] INTERMEDIATE_CIPHER_SUITES = new String[] {

        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        // AES128-GCM-SHA256
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        // AES256-GCM-SHA384
        "TLS_RSA_WITH_AES_256_GCM_SHA384",
        // AES128-SHA256
        "TLS_RSA_WITH_AES_128_CBC_SHA256",
        // AES256-SHA256
        "TLS_RSA_WITH_AES_256_CBC_SHA256",
        // AES128-SHA
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        // AES256-SHA
        "TLS_RSA_WITH_AES_256_CBC_SHA",
        // AES
        "TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA",
        "TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA",
        "TLS_SRP_SHA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
        "TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA",
        "TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA",
        "TLS_SRP_SHA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        // CAMELLIA
        "TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_RSA_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_RSA_WITH_CAMELLIA_128_CBC_SHA",
        // DES-CBC3-SHA
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",

    };

    /**
     * Cipher suites from the "Old" category.
     */
    public static final String[] OLD_CIPHER_SUITES = new String[] {

        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
        // AES128-GCM-SHA256
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        // AES256-GCM-SHA384
        "TLS_RSA_WITH_AES_256_GCM_SHA384",
        // AES128-SHA256
        "TLS_RSA_WITH_AES_128_CBC_SHA256",
        // AES256-SHA256
        "TLS_RSA_WITH_AES_256_CBC_SHA256",
        // AES128-SHA
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        // AES256-SHA
        "TLS_RSA_WITH_AES_256_CBC_SHA",
        // AES
        "TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA",
        "TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA",
        "TLS_SRP_SHA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
        "TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA",
        "TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA",
        "TLS_SRP_SHA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        // DES-CBC3-SHA
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
        // HIGH
        "TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_RSA_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA",
        "TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA",
        "TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_RSA_WITH_CAMELLIA_128_CBC_SHA",

    };

    /**
     * Gets the recommended protocols based on the JVM version, per the
     * MozillaWiki.
     *
     * @return the recommended protocols.
     */
    public static String[] getBestProtocolsForJVM() {
        if (javaVersion >= 1.8)
            return MODERN_PROTOCOLS;
        else if (javaVersion == 1.7)
            return INTERMEDIATE_PROTOCOLS;
        else if (javaVersion == 1.6)
            return OLD_PROTOCOLS;
        else
            return new String[0];
    }

    /**
     * Gets the recommended cipher suites based on the JVM version, per the
     * MozillaWiki.
     *
     * @return the recommended cipher suites.
     */
    public static String[] getBestCipherSuitesForJVM() {
        if (javaVersion >= 1.8)
            return MODERN_CIPHER_SUITES;
        else if (javaVersion == 1.7)
            return INTERMEDIATE_CIPHER_SUITES;
        else if (javaVersion == 1.6)
            return OLD_CIPHER_SUITES;
        else
            return new String[0];
    }

    /**
     * Filter one array based on whether its elements appear in another array.
     * The ordering of the array is preserved. Duplicates in the array are not
     * removed.
     *
     * @param sourceArray the array of Strings to filter
     * @param testArray the array of Strings to test against
     * @return an array of Strings that exist in both sourceArray and testArray
     */
    public static String[] filter(String[] sourceArray, String[] testArray) {
        List<String> test = Arrays.asList(testArray);
        List<String> ret = new ArrayList<String>();
        for (String s : sourceArray) {
            if (test.contains(s))
                ret.add(s);
        }
        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Filters the provided array of supported protocols based on the
     * recommended set for this JVM. The returned array is ordered, with the
     * most recommended protocol at the start.
     *
     * @param supportedProtocols a string array of supported protocols
     * @return the recommended subset of supportedProtocols
     */
    public static String[] getRecommendedProtocols(String[] supportedProtocols) {
        return filter(getBestProtocolsForJVM(), supportedProtocols);
    }

    /**
     * Filters the provided array of supported cipher suites based on the
     * recommended set for this JVM. The returned array is ordered, with the
     * most recommended cipher suite at the start.
     *
     * @param supportedCipherSuites a string array of supported cipher suites
     * @return the recommended subset of supportedCipherSuites
     */
    public static String[] getRecommendedCipherSuites(String[] supportedCipherSuites) {
        return filter(getBestCipherSuitesForJVM(), supportedCipherSuites);
    }
}


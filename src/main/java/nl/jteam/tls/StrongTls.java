/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.jteam.tls;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
     * Gets the recommended protocols based on the JVM version, per the
     * MozillaWiki.
     *
     * @return the recommended protocols.
     */
    public static String[] getRecommendedProtocols() {
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
    public static String[] getRecommendedCipherSuites() {
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
     * Gives the intersection of 2 string arrays.
     *
     * @param stringSetA a set of strings (not null)
     * @param stringSetB another set of strings (not null)
     * @return the intersection of strings in stringSetA and stringSetB
     */
    public static String[] intersection(String[] stringSetA, String[] stringSetB) {
        Set<String> intersection = new HashSet<String>(Arrays.asList(stringSetA));
        intersection.retainAll(new HashSet<String>(Arrays.asList(stringSetB)));
        return intersection.toArray(new String[intersection.size()]);
    }
}


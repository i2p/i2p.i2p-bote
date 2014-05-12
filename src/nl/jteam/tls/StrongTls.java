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
 * <p>
 * See http://java.sun.com/javase/6/docs/technotes/guides/security/StandardNames.html
 * for the standard names.
 *
 * @author Erik van Oosten
 */
public class StrongTls {

    /**
     * The protocols that are enabled.
     */
    public static final String[] ENABLED_PROTOCOLS = new String[] {

            // Strong protocols

            "SSLv3",
            "TLSv1",
            "TLSv1.1",
            "SSLv2Hello",
            
            // Weak protocols

//            "SSLv2"

    };

    /**
     * The SSL cipher suites that are enabled.
     */
    public static final String[] ENABLED_CIPHER_SUITES = new String[] {

            // Cipher suites that are not listed at
            // http://java.sun.com/javase/6/docs/technotes/guides/security/StandardNames.html
            // but are known to be strong.

            "TLS_RSA_WITH_DES_CBC_SHA",
            "TLS_DHE_DSS_WITH_DES_CBC_SHA",
            "TLS_DHE_RSA_WITH_DES_CBC_SHA",
            "TLS_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA",
            "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_RSA_WITH_RC4_128_SHA",
            "TLS_RSA_WITH_RC4_128_MD5",
            "TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "TLS_DHE_DSS_WITH_RC4_128_SHA",
            "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",

            // Strong cipher suites that are listed at
            // http://java.sun.com/javase/6/docs/technotes/guides/security/StandardNames.html

            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
            "SSL_RSA_WITH_RC4_128_SHA",

            // Cipher suites that are listed at
            // http://java.sun.com/javase/6/docs/technotes/guides/security/StandardNames.html
            // that are know to be weak, or are of unknown strength.

//            "SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA",
//            "SSL_DH_DSS_WITH_DES_CBC_SHA",
//            "SSL_DH_DSS_EXPORT_WITH_DES40_CBC_SHA",
//            "SSL_DH_RSA_WITH_DES_CBC_SHA",
//            "SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA",
//            "SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA",
//            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
//            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
//            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
//            "SSL_DHE_DSS_WITH_RC4_128_SHA",
//            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
//            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
//            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
//            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
//            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
//            "SSL_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA",
//            "SSL_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA",
//            "TLS_DH_anon_WITH_AES_128_CBC_SHA",
//            "TLS_DH_anon_WITH_AES_256_CBC_SHA",
//            "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
//            "SSL_DH_anon_WITH_DES_CBC_SHA",
//            "SSL_DH_anon_WITH_RC4_128_MD5",
//            "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
//            "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
//            "SSL_FORTEZZA_DMS_WITH_NULL_SHA",
//            "SSL_FORTEZZA_DMS_WITH_FORTEZZA_CBC_SHA",
//            "SSL_RSA_WITH_DES_CBC_SHA",
//            "SSL_RSA_WITH_IDEA_CBC_SHA",
//            "SSL_RSA_WITH_NULL_MD5",
//            "SSL_RSA_WITH_NULL_SHA",
//            "SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5",
//            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
//            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
//            "SSL_RSA_EXPORT1024_WITH_RC4_56_SHA",
//            "SSL_RSA_EXPORT1024_WITH_DES_CBC_SHA",
//            "SSL_RSA_FIPS_WITH_DES_CBC_SHA",
//            "SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA",
//            "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
//            "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
//            "TLS_KRB5_WITH_DES_CBC_MD5",
//            "TLS_KRB5_WITH_DES_CBC_SHA",
//            "TLS_KRB5_WITH_IDEA_CBC_SHA",
//            "TLS_KRB5_WITH_IDEA_CBC_MD5",
//            "TLS_KRB5_WITH_RC4_128_MD5",
//            "TLS_KRB5_WITH_RC4_128_SHA",
//            "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",
//            "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
//            "TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA",
//            "TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5",
//            "TLS_KRB5_EXPORT_WITH_RC4_40_MD5",
//            "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
//            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
//            "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
//            "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
//            "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
//            "TLS_ECDH_ECDSA_WITH_NULL_SHA",
//            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
//            "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
//            "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
//            "TLS_ECDH_RSA_WITH_RC4_128_SHA",
//            "TLS_ECDH_RSA_WITH_NULL_SHA",
//            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
//            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
//            "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
//            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
//            "TLS_ECDHE_ECDSA_WITH_NULL_SHA",
//            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
//            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
//            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
//            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
//            "TLS_ECDHE_RSA_WITH_NULL_SHA",
//            "TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
//            "TLS_ECDH_anon_WITH_AES_256_CBC_SHA",
//            "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
//            "TLS_ECDH_anon_WITH_RC4_128_SHA",
//            "TLS_ECDH_anon_WITH_NULL_SHA",
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
        intersection.retainAll(Arrays.asList(stringSetB));
        return intersection.toArray(new String[intersection.size()]);
    }

}


/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 * 
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 * 
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

/** Tests all <code>CryptoImplementation</code>s */
public class CryptoImplementationTest {
    private List<TestConfiguration> testConfigurationSet;
    private List<byte[]> testMessages;

    @Before
    public void setUp() throws Exception {
        testConfigurationSet = new ArrayList<TestConfiguration>();
        
        TestConfiguration elGamal2048TestData = new TestConfiguration();
        elGamal2048TestData.cryptoImpl = new ElGamal2048_DSA1024();
        elGamal2048TestData.base64PublicKeyPair = "-GygBJmy3XXPaDCD6uG0a7c23udye7H9jVFQ2WCeCnmls353ewLyITt7D3oneFYBsM1dHm~ciORrLtgZUCRqeJwIJIjzzKMVL93FSuMD8PQB9IX~F2l-Jn~5oBJCJWK~rnkNX7yBl-uUrylzPidfZ-NpW0U6wJREOQTx4oGvcGNv2oDkHBL44Oqencuw9NXxHJ9SjapuSgo2vg8YN6BP67oHR5-SlaIN6bHaF9T5tjJMkf32frT-qmWTcyB~0OgXXL3Z9cTERqVihYIBmk4EaTPa5oB~sOUIhUv5DqedBD~BDY5P4d7TroNWoW4FOhnfGqtTD-cS-qEn0ww3tHn7JEppbWGcgKrbdb4F4Qt8VYBd-ogATOXFbbo-PG~PgmUOa2QWGIi4RSXK1L3NoYO4ha7SkQMJpKj8ySi-ixk3ivofk6lRgoZ4WhbReaB352pF1iMXqp4p7-mnLMPZUX41ibHPeWrq7TyNqb-ouyn9ZfqORlko3bi04eXkfzkeDVuf";
        elGamal2048TestData.base64PrivateKeyPair = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADClvCJzneZ4R4yUJbm6zOP3wOh~CTfnWx3MSR7QDZS22-njK3KHuZHBbCK7HbyQLr";
        testConfigurationSet.add(elGamal2048TestData);
        
        TestConfiguration ecdh256TestData = new TestConfiguration();
        ecdh256TestData.cryptoImpl = new ECDH256_ECDSA256();
        ecdh256TestData.base64PublicKeyPair = "xE1fQK3nPfcmABNpYrHEDVHLj1sq01mmtrDrrIKAZcMnK9vmbiBZ4ygsksNpe5rV-TILTQUUTIUry5qt8q5ybB";
        ecdh256TestData.base64PrivateKeyPair = "M3yBbveBPFwfd59UY06RtJnfZtHU8yC7RZMYCITBwdRGkyPsftKS7M2~OSMsmHWUfejRolqztJ4lf4keS~KSge";
        testConfigurationSet.add(ecdh256TestData);
        
        TestConfiguration ecdh521TestData = new TestConfiguration();
        ecdh521TestData.cryptoImpl = new ECDH521_ECDSA521();
        ecdh521TestData.base64PublicKeyPair = "m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY";
        ecdh521TestData.base64PrivateKeyPair = "YujtjOOwCqXPH9PIbcZeFRkegbOxw5G6I7M4-TZBFbxYDtaew6HX9hnQEGWHkaapq2kTTB3Hmv0Uyo64jvcfMmSRcPng3J1Ho5mHgnzsH0qxQemnBcw7Lfc9fU8xRz858uyiQ8J8XH3T8S7k2~8L7awSgaT7uHQgpV~Rs0p1ofJ70g";
        testConfigurationSet.add(ecdh521TestData);
        
        TestConfiguration ntru1087TestData = new TestConfiguration();
        ntru1087TestData.cryptoImpl = new NTRUEncrypt1087_NTRUSign349();
        ntru1087TestData.base64PublicKeyPair = "573-aZRkI9gAGisC9X7Kh0UfYhb0OoS4atMN27aCcJfOnUehQSnFonPPKcPXFGck0EtRnUPOwjABU0kiQdcnfq05LT7XaLv2bNlUAlpD4RM3dSoepmx5~lytrn4KkhEjTm7G45fHBbZzS-wfQHB6UDc4ABZYPz-g3aMQ9etuS8mMfgH~UprGYxwHd8Lfx46LJkU-YQ7OVE~aqSJ3UaVTwkjgPKH5EbGuVHM72zZxNnEw3mvCFGNix-jewg4esp5lEbuhWxmcnPc8FNiXSYGnGnfERJphuteYHUF2KzbN21kmbGm19L4SbKYiMZaABWxWgA~R-zRpjIAlrr4yVditnBfBVSVyK-M6AUeXFImqT6hshQwYf0nLa5EW1A8PmTW8ZjuCIsFQQVjF9I5ppRD1gq3jEv0JEgaF8F7TERmA~1mc5ylMm6hUXYcwB3vtdXKvwBBD8MG4JlNb2lbiQP6UxOau8NfslCT~YkAPrZZB-cEk0r2jsh6wi6eTbsa0mjS1i0ssO~8ThkrGrbBXP2BNoRoHbKb7RBHsfWpjn5qum7Co4i7Sp2wyXdO3JI3z2CEBgbC-wpLPpkWEG1ODv1LJr~NXcl0zrlqhxExgoJ~NuZ-GMZn3XNPQcEiHHddtdA4PADfZbfqzZeJKtIjZl~UAiGBcnPKglmKRrPF3zRLAiMwlWkHcAuV2NWjkj~WyGUFMMw3oUV9JR6Y84ZiaNkVPkfQtislNt7~fGmYz7LzwYfvhZC1zm3VNLs12~Xp0LUmFu~BsIgX-WPW8mQgWEiuiHj9sFg6KUbRfpzDAGp-mU4XrD4OoNg75CVgMna2W8~6VDh1bB7Xjg5QgyyNHbUUrB-uaGe8LTwhc7XPBh8PgvKGb1~v7NYIwUX10kt3Ohhtmk7tyteJcOEP-8X6htCKlayfTgL8uEkr5685okK1JSfeI1uDGRBpL0zkuBkNTz6ukHJj613dg5zrVjK0mME7y7hV29gWEPedqU01mHB6wOGssn4Z2~X3y8cr3FF2xfTu~W-hjnC50LUokleJPKOTcleHwuylMoVk9gPRE9BV5cbpK-1tiBWC0yeZgJVAEkJF-lEeGyu0U4YgIpyH1JvSJRWXqJRdLOI4FSy4r0fJwmbFuf-uItc3dzwIRE5dMc4AeDSZ31OYq5TLov6vYoDpyu0tWgfrXAMLwY7jwjPunTOxYjwqKicF2j0ha3FxsRq17KD~TZ3ouu8HQkUuuOwCRnUtzKc-Y8IEavRWiFxM3K8Cg2wBoXoXttuaZwvRNuUW24trbFFiH2jBUnPsb0kRril65tQaHWBEZCMpEmQ8mDEENAQwzoyvP~sl0pKS4cyAb-v2jFcWRLRXozx2fR8KIa3IQT9pMGgKbiOjh6pWtz3vL2QWIoWMs-XFQaUP5Dzr1LbVUx1SFX3iJN3wldffnLTRZS9OmL9IoBPOCiQEw-C5KWbkrew~4lOXp4Pz065-6~s0PbTkbzqchKl1Z9W0cZjSesotkyPFlUq5QXWbjSZ-3Y9VxdmFgXALJaR9MmmFoeSiwxURAtihMHVvJ2LC0nARPvLMJoYhiHkzZRFg62bnRR2XsI4RMWn~fTwgbPCBy-N9ia9~RmkgWnPMpo8pRcKcYsslEgdsnt8uLy3Rn1oZRU2uKzi5WFTa-wBO83tl-ZD17NN~HbdiWeO4KphSGYYXscBRsDB1ebbp~ykgHzKK9W7rpbqTYe0oRVPD24LqOwk4UJtFZ7vbwb6O58FUvdAu4ZHTo79eNPAuP44RCcvc7lEeaxSUe5pUxIp6WH1ywdR36neh2FCUSnz9YCJetw8-~EkOWGWB9ESixov-m0husxDrti~~u0pnhgw5~NNQ-w37CKd9NUOsHzpMGYzzx8ed8tyzmvsTzUnzXr6CNsc49sibTzroUHkcJWOkqmwpq-48uYELgqsxUiceTnkj0NdpR96XjbpkHJVMxfo7giOMJKxmESQjObk4ItukpR06wQyQKRbKKB0N2BofVoPUZV3IP4srT05yuQ6j-1ZKrmErcf2fB2-dvNb5jz9r9VNwBQtY57cuJ3uQCVjIu-5~E92uKhqHAfyVlHlSGmecyWJ66TRQgyT2W-uXhfDDsg8yQPJia~FTNtAQvtrrUVEBQiB5fAg9gej1I3nvLjX1O~SGpg-K5rWNWDFzSyjd83fFY96rLpBRKW0P~OHk2lByk9imEvbNEirSNCvFkrn2G8KT7u~ScdF7piBr8Z~ZudbknH-tAwxxuFZN9lZRDoQiJER1dhpYc-f2NFCJXg-agduSxna0drNI7ALTKyRB8ovlqP1BYf49Og01e2810IxvqqYPIX-Fkl2JZHIgw1iwM2GB2SfpjtTvnczeGN-bxDGX39PMb91rVi8H-FB6WDEpY5CjOpGRYzjhtau6lYQXQRwXW9w1LysOxx2HTvAgVewyfq6Qiyx5a2qzVWdxTuchdDGZd2Wep9syNc8fyjMj7pUvix1F76cllr-FHMApdrA~uSNKlC6jJjR-haC5DCr17Gw";
        ntru1087TestData.base64PrivateKeyPair = "ATd65ryBzz46hRpJRx8Noyr5F201qsW11dHbp8yx7a0x5BCtGRG9Vd-nFbDBuOqUlzV0mTUEhUNPNhl8umBLvyDfscg~ezIIfABWiDWt2V8PUvf5Lr~1~M3rx6mwbCYZEoqrGuVpkD4scAj9qFl6yydgIBz2HR9-yCxZ~6lnBbjoOjUcvg-nngFFU7Y0rKzJYPOZoZszzhxIdEcta99Ai3z5~iFjTfLUivdodp1PXU3~jOp6VHfjKjPuugkUytUoWdHMNdQp0B~YTrWYIx8WkBv8425xbTicAdMJEY6E~HDol5fQ2E4qaCjCoO2o33ffYut4oBm5r1D3Q07brn6tzFxzeJOcJ2cKcGSgTAxa8mv6fWFuwsCfq7GGVYM5MABd8N05v9TumlxIoco-HkzzUXJI928H93dpsVPBqdy26lqQSwPEqtPNdPeS~ZpWfGak9U0XUD95tJ5ggNa4pUegz8a8ZX4BxPu9f62Clf~C95tni3q73P~CQDdhHXhBdQFWbe4Tvy4BKJ48FtxJKJ56FKalxvAcKF61qr7oMxoXE4PKGl9vuurQ5T5cAZD1ViZ74oO9oc~19N9088Glmql26Ox0mHzkU94Pt1rrrpOpCLg5ZOvlL~WaL7xXaXgeL731qrRokRdswg-VGans8Ya6~kN2BofVoPUZV3IP4srT05yuQ6j-1ZKrmErcf2fB2-dvNb5jz9r9VNwBQtY57cuJ3uQCVjIu-5~E92uKhqHAfyVlHlSGmecyWJ66TRQgyT2W-uXhfDDsg8yQPJia~FTNtAQvtrrUVEBQiB5fAg9gej1I3nvLjX1O~SGpg-K5rWNWDFzSyjd83fFY96rLpBRKW0P~OHk2lByk9imEvbNEirSNCvFkrn2G8KT7u~ScdF7piBr8Z~ZudbknH-tAwxxuFZN9lZRDoQiJER1dhpYc-f2NFCJXg-agduSxna0drNI7ALTKyRB8ovlqP1BYf49Og01e2810IxvqqYPIX-Fkl2JZHIgw1iwM2GB2SfpjtTvnczeGN-bxDGX39PMb91rVi8H-FB6WDEpY5CjOpGRYzjhtau6lYQXQRwXW9w1LysOxx2HTvAgVewyfq6Qiyx5a2qzVWdxTuchdDGZd2Wep9syNc8fyjMj7pUvix1F76cllr-FHMApdrA~uSNKlC6jJjR-haC5DCr17Gw";
        testConfigurationSet.add(ntru1087TestData);
        
        for (TestConfiguration testData: testConfigurationSet) {
            testData.publicKeys = testData.cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            testData.privateKeys = testData.cryptoImpl.createPrivateKeyPair(testData.base64PrivateKeyPair);
            testData.signingKeys = new KeyPair(testData.publicKeys.signingKey, testData.privateKeys.signingKey);
            testData.encryptionKeys = new KeyPair(testData.publicKeys.encryptionKey, testData.privateKeys.encryptionKey);
        }
        
        testMessages = new ArrayList<byte[]>();
        testMessages.add("Test test 1234567890 %&$%/&§,--.:_ abcdef äöüß".getBytes());
        Random rng = new Random(0);
        // include messages of different lengths that cover all padding sizes
        for (int n=15000; n<15016; n++) {
            byte[] message = new byte[n];
            rng.nextBytes(message);
            testMessages.add(message);
        }
    }

    @Test
    public void testToByteArray() throws GeneralSecurityException {
        for (TestConfiguration testData: testConfigurationSet) {
            CryptoImplementation cryptoImpl = testData.cryptoImpl;
            
            // test public key pair
            PublicKeyPair originalPublicKeyPair = cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            byte[] originalPublic = cryptoImpl.toByteArray(originalPublicKeyPair);
            PublicKeyPair keyPairPublic = cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            byte[] reencodedPublic = cryptoImpl.toByteArray(keyPairPublic);
            assertTrue(Arrays.equals(originalPublic, reencodedPublic));
            assertEquals(cryptoImpl.getByteArrayPublicKeyPairLength(), reencodedPublic.length);
            
            // test private key pair
            PrivateKeyPair originalPrivateKeyPair = cryptoImpl.createPrivateKeyPair(testData.base64PrivateKeyPair);
            byte[] originalPrivate = cryptoImpl.toByteArray(originalPrivateKeyPair);
            PrivateKeyPair keyPairPrivate = cryptoImpl.createPrivateKeyPair(testData.base64PrivateKeyPair);
            byte[] reencodedPrivate = cryptoImpl.toByteArray(keyPairPrivate);
            assertTrue(Arrays.equals(originalPrivate, reencodedPrivate));
        }
    }

    @Test
    public void encodeDecodeBase64() throws GeneralSecurityException {
        for (TestConfiguration testData: testConfigurationSet) {
            CryptoImplementation cryptoImpl = testData.cryptoImpl;
            
            // test a public key pair
            PublicKeyPair publicKeyPair = cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            String reencodedPublic = cryptoImpl.toBase64(publicKeyPair);
            assertEquals(testData.base64PublicKeyPair, reencodedPublic);
            assertEquals("Wrong public key length!", cryptoImpl.getBase64PublicKeyPairLength(), testData.base64PublicKeyPair.length());
            
            // test a private key pair
            PrivateKeyPair privateKeyPair = cryptoImpl.createPrivateKeyPair(testData.base64PrivateKeyPair);
            String reencodedPrivate = cryptoImpl.toBase64(privateKeyPair);
            assertEquals(testData.base64PrivateKeyPair, reencodedPrivate);
            assertEquals("Wrong private key length!", cryptoImpl.getBase64CompleteKeySetLength()-cryptoImpl.getBase64PublicKeyPairLength(), testData.base64PrivateKeyPair.length());
        }
    }

    @Test
    public void encryptAndDecrypt() throws GeneralSecurityException, InvalidCipherTextException {
        for (TestConfiguration testData: testConfigurationSet)
            for (byte[] original: testMessages) {
                CryptoImplementation cryptoImpl = testData.cryptoImpl;
                KeyPair encryptionKeys = testData.encryptionKeys;
                byte[] encrypted = cryptoImpl.encrypt(original, encryptionKeys.getPublic());
                byte[] decrypted = cryptoImpl.decrypt(encrypted, encryptionKeys.getPublic(), encryptionKeys.getPrivate());
                assertTrue("encrypted data != decrypted data for crypto implementation <" + cryptoImpl.getName() + ">", Arrays.equals(original, decrypted));
            }
    }

    @Test
    public void signAndVerify() throws GeneralSecurityException {
        for (TestConfiguration testData: testConfigurationSet)
            for (byte[] message: testMessages) {
                CryptoImplementation cryptoImpl = testData.cryptoImpl;
                KeyPair signingKeys = testData.signingKeys;
                byte[] signature = cryptoImpl.sign(message, signingKeys.getPublic(), signingKeys.getPrivate());
                assertTrue("Invalid signature for crypto implementation <" + cryptoImpl.getName() + ">", cryptoImpl.verify(message, signature, signingKeys.getPublic()));
            }
    }
 
    /** Contains a CryptoImplementation and a set of keys */
    private class TestConfiguration {
        CryptoImplementation cryptoImpl;
        String base64PublicKeyPair;
        String base64PrivateKeyPair;
        PublicKeyPair publicKeys;
        PrivateKeyPair privateKeys;
        KeyPair encryptionKeys;
        KeyPair signingKeys;
    }
}
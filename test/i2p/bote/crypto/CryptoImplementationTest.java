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

import org.junit.Before;
import org.junit.Test;

/** Tests both <code>CryptoImplementation</code>s */
public class CryptoImplementationTest {
    private List<TestData> testDataSet;

    @Before
    public void setUp() throws Exception {
        testDataSet = new ArrayList<TestData>();
        
        TestData elGamal2048TestData;
        elGamal2048TestData = new TestData();
        elGamal2048TestData.cryptoImpl = new ElGamal2048_DSA1024();
        elGamal2048TestData.base64PublicKeyPair = "-GygBJmy3XXPaDCD6uG0a7c23udye7H9jVFQ2WCeCnmls353ewLyITt7D3oneFYBsM1dHm~ciORrLtgZUCRqeJwIJIjzzKMVL93FSuMD8PQB9IX~F2l-Jn~5oBJCJWK~rnkNX7yBl-uUrylzPidfZ-NpW0U6wJREOQTx4oGvcGNv2oDkHBL44Oqencuw9NXxHJ9SjapuSgo2vg8YN6BP67oHR5-SlaIN6bHaF9T5tjJMkf32frT-qmWTcyB~0OgXXL3Z9cTERqVihYIBmk4EaTPa5oB~sOUIhUv5DqedBD~BDY5P4d7TroNWoW4FOhnfGqtTD-cS-qEn0ww3tHn7JEppbWGcgKrbdb4F4Qt8VYBd-ogATOXFbbo-PG~PgmUOa2QWGIi4RSXK1L3NoYO4ha7SkQMJpKj8ySi-ixk3ivofk6lRgoZ4WhbReaB352pF1iMXqp4p7-mnLMPZUX41ibHPeWrq7TyNqb-ouyn9ZfqORlko3bi04eXkfzkeDVuf";
        elGamal2048TestData.base64PrivateKeyPair = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADClvCJzneZ4R4yUJbm6zOP3wOh~CTfnWx3MSR7QDZS22-njK3KHuZHBbCK7HbyQLr";
        testDataSet.add(elGamal2048TestData);
        
        TestData ecdh521TestData;
        ecdh521TestData = new TestData();
        ecdh521TestData.cryptoImpl = new ECDH521_ECDSA521();
        ecdh521TestData.base64PublicKeyPair = "m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY";
        ecdh521TestData.base64PrivateKeyPair = "YujtjOOwCqXPH9PIbcZeFRkegbOxw5G6I7M4-TZBFbxYDtaew6HX9hnQEGWHkaapq2kTTB3Hmv0Uyo64jvcfMmSRcPng3J1Ho5mHgnzsH0qxQemnBcw7Lfc9fU8xRz858uyiQ8J8XH3T8S7k2~8L7awSgaT7uHQgpV~Rs0p1ofJ70g";
        testDataSet.add(ecdh521TestData);
        
        for (TestData testData: testDataSet) {
            testData.publicKeys = testData.cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            testData.privateKeys = testData.cryptoImpl.createPrivateKeyPair(testData.base64PrivateKeyPair);
            testData.signingKeys = new KeyPair(testData.publicKeys.signingKey, testData.privateKeys.signingKey);
            testData.encryptionKeys = new KeyPair(testData.publicKeys.encryptionKey, testData.privateKeys.encryptionKey);
        }
    }

    @Test
    public void testToByteArray() throws GeneralSecurityException {
        for (TestData testData: testDataSet) {
            CryptoImplementation cryptoImpl = testData.cryptoImpl;
            
            // test public key pair
            PublicKeyPair originalPublicKeyPair = cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            byte[] originalPublic = cryptoImpl.toByteArray(originalPublicKeyPair);
            PublicKeyPair keyPairPublic = cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            byte[] reencodedPublic = cryptoImpl.toByteArray(keyPairPublic);
            assertTrue(Arrays.equals(originalPublic, reencodedPublic));
            
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
        for (TestData testData: testDataSet) {
            CryptoImplementation cryptoImpl = testData.cryptoImpl;
            
            // test a public key pair
            PublicKeyPair publicKeyPair = cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            String reencodedPublic = cryptoImpl.toBase64(publicKeyPair );
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
    public void encryptAndDecrypt() throws GeneralSecurityException {
        for (TestData testData: testDataSet) {
            CryptoImplementation cryptoImpl = testData.cryptoImpl;
            KeyPair encryptionKeys = testData.encryptionKeys;
            byte[] original = testData.message;
            byte[] encrypted = cryptoImpl.encrypt(original, encryptionKeys.getPublic());
            byte[] decrypted = cryptoImpl.decrypt(encrypted, encryptionKeys.getPrivate());
            assertTrue("encrypted data != decrypted data for crypto implementation <" + cryptoImpl.getName() + ">", Arrays.equals(original, decrypted));
        }
    }

    @Test
    public void signAndVerify() throws GeneralSecurityException {
        for (TestData testData: testDataSet) {
            CryptoImplementation cryptoImpl = testData.cryptoImpl;
            KeyPair signingKeys = testData.signingKeys;
            byte[] message = testData.message;
            byte[] signature = cryptoImpl.sign(message, signingKeys.getPrivate());
            assertTrue("Invalid signature for crypto implementation <" + cryptoImpl.getName() + ">", cryptoImpl.verify(message, signature, signingKeys.getPublic()));
        }
    }
 
    /** Contains all the data needed for testing one CryptoImplementation */
    private class TestData {
        byte[] message = "Test test 1234567890 %&$%/&§,--.:_ abcdef äöüß".getBytes();
        CryptoImplementation cryptoImpl;
        String base64PublicKeyPair;
        String base64PrivateKeyPair;
        PublicKeyPair publicKeys;
        PrivateKeyPair privateKeys;
        KeyPair encryptionKeys;
        KeyPair signingKeys;
   }
}
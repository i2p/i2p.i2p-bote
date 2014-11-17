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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import i2p.bote.TestUtil;
import i2p.bote.TestUtil.TestIdentity;
import i2p.bote.fileencryption.PasswordException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

/** Tests all <code>CryptoImplementation</code>s */
public class CryptoImplementationTest {
    private List<byte[]> testMessages;
    private List<TestIdentity> testIdentities;

    @Before
    public void setUp() throws Exception {
        testMessages = new ArrayList<byte[]>();
        testMessages.add("Test test 1234567890 %&$%/&§,--.:_ abcdef äöüß".getBytes());
        Random rng = new Random(0);
        // include messages of different lengths that cover all padding sizes
        for (int n=15000; n<15016; n++) {
            byte[] message = new byte[n];
            rng.nextBytes(message);
            testMessages.add(message);
        }
        
        testIdentities = TestUtil.createTestIdentities();
    }

    @Test
    public void testToByteArray() throws GeneralSecurityException {
        for (TestIdentity testData: testIdentities) {
            CryptoImplementation cryptoImpl = testData.cryptoImpl;
            
            // test public key pair
            PublicKeyPair originalPublicKeyPair = cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            byte[] originalPublic = cryptoImpl.toByteArray(originalPublicKeyPair);
            PublicKeyPair keyPairPublic = cryptoImpl.createPublicKeyPair(testData.base64PublicKeyPair);
            byte[] reencodedPublic = cryptoImpl.toByteArray(keyPairPublic);
            assertArrayEquals(originalPublic, reencodedPublic);
            assertEquals(cryptoImpl.getByteArrayPublicKeyPairLength(), reencodedPublic.length);
            
            // test private key pair
            PrivateKeyPair originalPrivateKeyPair = cryptoImpl.createPrivateKeyPair(testData.base64PrivateKeyPair);
            byte[] originalPrivate = cryptoImpl.toByteArray(originalPrivateKeyPair);
            PrivateKeyPair keyPairPrivate = cryptoImpl.createPrivateKeyPair(testData.base64PrivateKeyPair);
            byte[] reencodedPrivate = cryptoImpl.toByteArray(keyPairPrivate);
            assertArrayEquals(originalPrivate, reencodedPrivate);
        }
    }

    @Test
    public void encodeDecodeBase64() throws GeneralSecurityException {
        for (TestIdentity testData: testIdentities) {
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
        for (TestIdentity testData: testIdentities)
            for (byte[] original: testMessages) {
                CryptoImplementation cryptoImpl = testData.cryptoImpl;
                KeyPair encryptionKeys = testData.encryptionKeys;
                byte[] encrypted = cryptoImpl.encrypt(original, encryptionKeys.getPublic());
                byte[] decrypted = cryptoImpl.decrypt(encrypted, encryptionKeys.getPublic(), encryptionKeys.getPrivate());
                assertArrayEquals("encrypted data != decrypted data for crypto implementation <" + cryptoImpl.getName() + ">", original, decrypted);
            }
    }

    @Test
    public void signAndVerify() throws GeneralSecurityException, IOException, PasswordException {
        for (TestIdentity testIdentity: testIdentities) {
            KeyUpdateHandler keyUpdateHandler;
            if (testIdentity.cryptoImpl instanceof NTRUEncrypt1087_GMSS512)
                keyUpdateHandler = TestUtil.createVerifyingKeyUpdateHandler(testMessages.size());   // verify that KeyUpdateHandler is called once for each signed message
            else
                keyUpdateHandler = TestUtil.createDummyKeyUpdateHandler();
           
            for (byte[] message: testMessages) {
                CryptoImplementation cryptoImpl = testIdentity.cryptoImpl;
                KeyPair signingKeys = testIdentity.signingKeys;
                byte[] signature = cryptoImpl.sign(message, signingKeys.getPrivate(), keyUpdateHandler);
                assertTrue("Invalid signature for crypto implementation <" + cryptoImpl.getName() + ">", cryptoImpl.verify(message, signature, signingKeys.getPublic()));
            }
        }
    }
}
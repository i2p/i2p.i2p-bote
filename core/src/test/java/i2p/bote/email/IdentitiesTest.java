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

package i2p.bote.email;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import i2p.bote.TestUtil;
import i2p.bote.TestUtil.TestIdentity;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.crypto.NTRUEncrypt1087_GMSS512;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordHolder;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IdentitiesTest {
    private File testDir;
    private File identitiesFile;
    private PasswordHolder passwordHolder;
    private Identities identities;
    
    @Before
    public void setUp() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tmpDir, "IdentitiesTest-" + System.currentTimeMillis());
        identitiesFile = new File(testDir, "identities");
        assertTrue("Can't create directory: " + testDir.getAbsolutePath(), testDir.mkdir());
        passwordHolder = TestUtil.createPasswordCache(testDir);
        
        identities = new Identities(identitiesFile, passwordHolder);
        for (TestIdentity identity: TestUtil.createTestIdentities())
            identities.add(identity.identity);
    }
    
    @After
    public void tearDown() throws Exception {
        assertTrue("Can't delete file: " + identitiesFile.getAbsolutePath(), identitiesFile.delete());
        File derivParamsFile = TestUtil.createConfiguration(testDir).getKeyDerivationParametersFile();
        assertTrue("Can't delete file: " + derivParamsFile, derivParamsFile.delete());
        assertTrue("Can't delete directory: " + testDir.getAbsolutePath(), testDir.delete());
    }
    
    /** Checks that the private signing key is updated on disk if the <code>CryptoImplementation</code> requires it */
    @Test
    public void testUpdateKey() throws GeneralSecurityException, PasswordException, IOException {
        byte[] message = "Hopfen und Malz, Gott erhalt's!".getBytes();
        
        Iterator<EmailIdentity> iterator = identities.iterator();
        while (iterator.hasNext()) {
            EmailIdentity identity = iterator.next();
            PublicKey publicKey = identity.getPublicSigningKey();
            PrivateKey privateKey = identity.getPrivateSigningKey();
            
            // make a copy of the old signing keys
            byte[] encodedPublicKey = publicKey.getEncoded().clone();
            byte[] encodedPrivateKey = privateKey.getEncoded().clone();
            
            CryptoImplementation cryptoImpl = identity.getCryptoImpl();
            cryptoImpl.sign(message, privateKey, identities);
            
            // read identities from file and compare keys before / after
            boolean publicKeyChanged;
            boolean privateKeyChanged;
            if (!identitiesFile.exists())
                publicKeyChanged = privateKeyChanged = false;
            else {
                Identities newIdentities = new Identities(identitiesFile, passwordHolder);
                PublicKey newPublicKey = newIdentities.get(identity).getPublicSigningKey();
                PrivateKey newPrivateKey = newIdentities.get(identity).getPrivateSigningKey();
                publicKeyChanged = !Arrays.equals(encodedPublicKey, newPublicKey.getEncoded());
                privateKeyChanged = !Arrays.equals(encodedPrivateKey, newPrivateKey.getEncoded());
            }
            assertFalse(publicKeyChanged);
            assertTrue(privateKeyChanged == (cryptoImpl instanceof NTRUEncrypt1087_GMSS512));
        }
    }
}
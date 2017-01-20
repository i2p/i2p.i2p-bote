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

package i2p.bote.fileencryption;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import i2p.bote.Configuration;
import i2p.bote.TestUtil;
import i2p.bote.fileencryption.DerivedKey;
import i2p.bote.fileencryption.FileEncryptionConstants;
import i2p.bote.fileencryption.FileEncryptionUtil;
import i2p.bote.fileencryption.PasswordCache;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PasswordCacheTest {
    private static final byte[] PASSWORD = "MySecretPassword12345".getBytes();
    
    private File testDir;
    private PasswordCache passwordCache;
    
    @Before
    public void setUp() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "PasswordCacheTest-" + System.currentTimeMillis());
        testDir.mkdir();
        passwordCache = TestUtil.createPasswordCache(testDir);
    }
    
    @After
    public void tearDown() throws Exception {
        TestUtil.createConfiguration(testDir).getKeyDerivationParametersFile().delete();
        assertTrue("Directory not empty: <" + testDir.getAbsolutePath() + ">", testDir.delete());
    }
    
    @Test
    public void testGetKey() throws IOException, GeneralSecurityException {
        passwordCache.setPassword(PASSWORD);
        DerivedKey derivedKey = passwordCache.getKey();
        assertEquals(derivedKey.scryptParams, FileEncryptionConstants.KDF_PARAMETERS);
        byte[] expectedKey = FileEncryptionUtil.getEncryptionKey(PASSWORD, derivedKey.salt, derivedKey.scryptParams);
        assertArrayEquals(expectedKey, derivedKey.key);
        
        // verify that the salt was cached in a file and is reused
        PasswordCache newPasswordCache = TestUtil.createPasswordCache(testDir);
        newPasswordCache = TestUtil.createPasswordCache(testDir);
        newPasswordCache.setPassword(PASSWORD);
        byte[] oldSalt = derivedKey.salt;
        byte[] newSalt = passwordCache.getKey().salt;
        assertArrayEquals(oldSalt, newSalt);
        
        // delete the cache file, clear the derived key, and verify that a new salt is generated
        Configuration configuration = TestUtil.createConfiguration(testDir);
        File derivParamsFile = configuration.getKeyDerivationParametersFile();
        boolean deleted = derivParamsFile.delete();
        assertTrue("Can't delete derivation parameters cache file: <" + derivParamsFile.getAbsolutePath() + ">", deleted);
        passwordCache.setPassword(PASSWORD);   // clear the key
        newSalt = passwordCache.getKey().salt;
        assertFalse(Arrays.equals(oldSalt, newSalt));
    }
    
    @Test
    public void testExpiration() throws InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        passwordCache.setPassword(PASSWORD);
        TimeUnit.SECONDS.sleep(1);   // delay the PasswordCache thread so the password expires a little less than a minute after the thread is started (it loops every minute)
        passwordCache.start();
        
        try {
            // Verify that the password expires 60 seconds after it is set
            // (the timeout is set in TestUtil.createConfiguration(File))
            TimeUnit.SECONDS.sleep(58);
            assertArrayEquals(PASSWORD, getPassword(passwordCache));
            TimeUnit.SECONDS.sleep(3);
            assertNull("Password was not cleared!", getPassword(passwordCache));
            
            // lock the password and verify that is is not cleared until after unlocking
            passwordCache.setPassword(PASSWORD);
            synchronized(passwordCache) {
                TimeUnit.SECONDS.sleep(61);
                assertArrayEquals(PASSWORD, getPassword(passwordCache));
            }
            TimeUnit.SECONDS.sleep(61);
            assertNull("Password was not cleared!", getPassword(passwordCache));
        }
        finally {
            passwordCache.interrupt();
            passwordCache.join(2000);
            assertFalse("Password cache thread is still running!", passwordCache.isAlive());
        }
    }
    
    /** Verifies that <code>setPassword</code> blocks when the password is locked */
    @Test
    public void testLockPassword() throws InterruptedException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        // set password while not locked
        Thread setPasswordThread = createSetPasswordThread();
        assertFalse(Arrays.equals(passwordCache.getPassword(), PASSWORD));
        setPasswordThread.start();
        Thread.sleep(100);
        assertFalse(setPasswordThread.isAlive());
        assertArrayEquals(passwordCache.getPassword(), PASSWORD);
        
        // call setPassword while locked, should block
        passwordCache.setPassword(null);
        setPasswordThread = createSetPasswordThread();
        synchronized(passwordCache) {
            setPasswordThread.start();
            Thread.sleep(100);
            assertTrue(setPasswordThread.isAlive());
            assertNull(getPassword(passwordCache));
        }
        Thread.sleep(100);
        assertFalse(setPasswordThread.isAlive());
        assertArrayEquals(passwordCache.getPassword(), PASSWORD);
    }
    
    /**
     * Returns the value of the private field <code>password</code>. Does not reset
     * the expiration time, unlike {@link PasswordCache#getPassword()}.
     * @param passwordCache
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private byte[] getPassword(PasswordCache passwordCache) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field passwordField = PasswordCache.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        return (byte[])passwordField.get(passwordCache);
    }
    
    private Thread createSetPasswordThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                passwordCache.setPassword(PASSWORD);
            }
        });
    }
}
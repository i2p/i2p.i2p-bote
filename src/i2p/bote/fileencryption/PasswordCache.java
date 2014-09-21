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

import static i2p.bote.fileencryption.FileEncryptionConstants.KDF_PARAMETERS;
import static i2p.bote.fileencryption.FileEncryptionConstants.SALT_LENGTH;
import i2p.bote.Configuration;
import i2p.bote.Util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import net.i2p.I2PAppContext;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;
import net.i2p.util.SecureFileOutputStream;

/**
 * Stores a password in memory so the user doesn't have to re-enter it.
 * Also caches key derivation parameters (salt and <code>scrypt</code> parameters)
 * so the key derivation function only needs to run once.
 */
public class PasswordCache extends I2PAppThread implements PasswordHolder {
    private Log log = new Log(PasswordCache.class);
    private byte[] password;
    private DerivedKey derivedKey;
    private long lastReset;
    private Configuration configuration;
    private Collection<PasswordCacheListener> cacheListeners;
    
    /**
     * Creates a new <code>PasswordCache</code>.
     * @param configuration
     */
    public PasswordCache(Configuration configuration) {
        super("PasswordCache");
        this.configuration = configuration;
        cacheListeners = new ArrayList<PasswordCacheListener>();
    }
    
    /**
     * Sets the password and calls <code>passwordProvided</code>
     * on all {@link PasswordCacheListener}s.
     * @param password
     */
    public synchronized void setPassword(byte[] password) {
        synchronized(this) {
            resetExpiration();
            this.password = password;
            // clear the old key
            if (derivedKey != null) {
                derivedKey.clear();
                derivedKey = null;
            }
        }
        
        for (PasswordCacheListener listener: cacheListeners)
            listener.passwordProvided();
    }
    
    /**
     * Reads salt and <code>scrypt</code> parameters from the cache file, or chooses
     * a new salt array if the file doesn't exist. The encryption key is then computed
     * and the variable <code>derivedKey</code> is populated.
     * @throws IOException 
     * @throws GeneralSecurityException 
     */
    private void createDerivedKey() throws IOException, GeneralSecurityException {
        byte[] salt = null;
        derivedKey = null;
        
        // read salt + scrypt parameters from file if available
        File derivParamFile = configuration.getKeyDerivationParametersFile();
        if (derivParamFile.exists())
            derivedKey = FileEncryptionUtil.getEncryptionKey(password, derivParamFile);
        
        // if necessary, create a new salt and key and write the derivation parameters to the cache file
        if (derivedKey==null || !derivedKey.scryptParams.equals(KDF_PARAMETERS)) {
            I2PAppContext appContext = I2PAppContext.getGlobalContext();
            salt = new byte[SALT_LENGTH];
            appContext.random().nextBytes(salt);
            
            DataOutputStream outputStream = null;
            try {
                byte[] key = FileEncryptionUtil.getEncryptionKey(password, salt, KDF_PARAMETERS);
                derivedKey = new DerivedKey(salt, KDF_PARAMETERS, key);
                outputStream = new DataOutputStream(new SecureFileOutputStream(derivParamFile));
                KDF_PARAMETERS.writeTo(outputStream);
                outputStream.write(salt);
            }
            finally {
                if (outputStream != null)
                    outputStream.close();
            }
        }
    }
    
    /**
     * Returns the cached password. If the password is not in the cache, the default password (if no
     * password is set) or <code>null</code> (if a password is set) is returned.
     * @return The cached password or <code>null</code> if the password is not in the cache
     */
    public synchronized byte[] getPassword() {
        resetExpiration();
        if ((password==null || password.length<=0) && !configuration.getPasswordFile().exists())
            return FileEncryptionConstants.DEFAULT_PASSWORD;
        else
            return password;
    }
    
    @Override
    public synchronized DerivedKey getKey() throws IOException, GeneralSecurityException {
        if (derivedKey == null)
            createDerivedKey();
        return derivedKey;
    }

    private void resetExpiration() {
        lastReset = System.currentTimeMillis();
    }
    
    /** Returns <code>true</code> if the password is currently cached. */
    public boolean isPasswordInCache() {
        return password != null && password.length>0;
    }
    
    /**
     * Clears the password if it is in the cache,
     * and fires {@link PasswordCacheListener}s.
     */
    public void clear() {
        synchronized(this) {
            if (password == null)
                return;
            Util.zeroOut(password);
            password = null;
            if (derivedKey != null) {
                derivedKey.clear();
                derivedKey = null;
            }
        }
        
        for (PasswordCacheListener listener: cacheListeners)
            listener.passwordCleared();
    }
    
    public void addPasswordCacheListener(PasswordCacheListener listener) {
        cacheListeners.add(listener);
    }

    public void removePasswordCacheListener(PasswordCacheListener listener) {
        cacheListeners.remove(listener);
    }
    
    /**
     * Clears the password after a certain time if {@link #getPassword()} hasn't been called.
     * @see Configuration#getPasswordCacheDuration()
     */
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
            
            try {
                long durationMilliseconds = TimeUnit.MILLISECONDS.convert(configuration.getPasswordCacheDuration(), TimeUnit.MINUTES);
                boolean isEmpty = password==null || password.length==0;
                if (System.currentTimeMillis()>lastReset+durationMilliseconds && !isEmpty)   // cache empty passwords forever
                    clear();
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in PasswordCache loop", e);
            }
        }
        
        log.debug("PasswordCache thread exiting.");
    }
}
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

package i2p.bote.io;

import i2p.bote.Configuration;
import i2p.bote.Util;
import i2p.bote.service.I2PBoteThread;

import java.util.concurrent.TimeUnit;

/**
 * Stores a password in memory so the user doesn't have to re-enter it.
 */
public class PasswordCache extends I2PBoteThread implements PasswordHolder {
    private char[] password;
    private long lastReset;
    private Configuration configuration;
    
    /**
     * Creates a new <code>PasswordCache</code>.
     * @param configuration
     */
    public PasswordCache(Configuration configuration) {
        super("PasswordCache");
        this.configuration = configuration;
    }
    
    /**
     * Sets the password.
     * @param password
     */
    public synchronized void setPassword(char[] password) {
        this.password = password;
    }
    
    /**
     * Returns the cached password. If the password is not in the cache, the default password (if no
     * password is set) or <code>null</code> (if a password is set) is returned.
     * @return The cached password or <code>null</code> if the password is not in the cache
     */
    public synchronized char[] getPassword() {
        resetExpiration();
        if (password==null && !configuration.getPasswordFile().exists())
            return FileEncryptionConstants.DEFAULT_PASSWORD;
        else
            return password;
    }
    
    private void resetExpiration() {
        lastReset = System.currentTimeMillis();
    }
    
    /**
     * Clears the password after a certain time if {@link #getPassword()} hasn't been called.
     * @see Configuration#getPasswordCacheDuration()
     */
    @Override
    protected void doStep() throws InterruptedException {
        awaitShutdownRequest(1, TimeUnit.MINUTES);
        long durationMilliseconds = TimeUnit.MILLISECONDS.convert(configuration.getPasswordCacheDuration(), TimeUnit.MINUTES);
        synchronized(this) {
            boolean isEmpty = password==null || password.length==0;
            if (System.currentTimeMillis()>lastReset+durationMilliseconds && !isEmpty) {   // cache empty passwords forever
                Util.zeroOut(password);
                password = null;
            }
        }
    }
}
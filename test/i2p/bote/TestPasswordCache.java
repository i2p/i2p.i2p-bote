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

package i2p.bote;

import i2p.bote.io.PasswordCache;

/**
 * A {@link PasswordCache} for unit tests. It doesn't need a {@link Configuration},
 * but it can't be started as a thread which means it never expires the password.
 */
public class TestPasswordCache {

    private TestPasswordCache() {
    }
    
    public static PasswordCache createPasswordCache() {
        // We can get away with passing a null Configuration because we set a non-null password and don't start
        // the PasswordCache thread, which means the password is never removed from the cache.
        PasswordCache passwordCache = new PasswordCache(null);
        passwordCache.setPassword("test password 12345".toCharArray());
        return passwordCache;
    }
}
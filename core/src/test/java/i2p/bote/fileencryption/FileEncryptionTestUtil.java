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

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class FileEncryptionTestUtil {
    
    public static DerivedKey deriveKey(byte[] password) throws GeneralSecurityException {
        byte[] salt = new byte[FileEncryptionConstants.SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] keyBytes = FileEncryptionUtil.getEncryptionKey(password, salt, FileEncryptionConstants.KDF_PARAMETERS);
        return new DerivedKey(salt, FileEncryptionConstants.KDF_PARAMETERS, keyBytes);
    }
}
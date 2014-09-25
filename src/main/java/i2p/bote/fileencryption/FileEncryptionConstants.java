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

import java.util.Random;

public class FileEncryptionConstants {
    public static final byte[] START_OF_FILE = "IBef".getBytes();   // "I2P-Bote encrypted file"
    static final int FORMAT_VERSION = 1;   // file format identifier
    static final int KEY_LENGTH = 32;   // encryption key length
    static final int SALT_LENGTH = 32;
    static final SCryptParameters KDF_PARAMETERS = new SCryptParameters(1<<14, 8, 1);
    static final int BLOCK_SIZE = 16;   // length of the AES initialization vector; also the AES block size for padding. Not to be confused with the AES key size.
    static final byte[] PASSWORD_FILE_PLAIN_TEXT = "If this is the decrypted text, the password was correct.".getBytes();
    static final byte[] DEFAULT_PASSWORD;   // this is substituted for empty passwords to add some security through obscurity,
                                            // and because empty passwords don't work with scrypt (see FileEncryptionUtil.getEncryptionKey())
    static {
        Random rng = new Random(999);
        DEFAULT_PASSWORD = new byte[10];
        rng.nextBytes(DEFAULT_PASSWORD);
    }
}
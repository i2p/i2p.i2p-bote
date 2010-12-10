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

import static org.junit.Assert.assertEquals;

import i2p.bote.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.junit.Test;

public class EncryptedStreamTest {

    @Test
    public void testEncryptionDecryption() throws Exception {
        String plainText = "Kräht der Hahn hoch auf dem Mist, ändert sich das Wetter, oder es bleibt wie's ist.";
        
        char[] password = "xyz12345".toCharArray();
        
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        OutputStream encryptedOutputStream = new EncryptedOutputStream(byteOutputStream, password);
        encryptedOutputStream.write(plainText.getBytes());
        encryptedOutputStream.close();
        
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
        EncryptedInputStream encryptedInputStream = new EncryptedInputStream(byteInputStream, password);
        String decryptedText = new String(Util.readBytes(encryptedInputStream));
        
        assertEquals(plainText, decryptedText);
    }
}
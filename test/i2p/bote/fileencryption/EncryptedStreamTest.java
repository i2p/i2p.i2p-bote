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
        
        byte[] password = "xyz12345".getBytes();
        DerivedKey derivedKey = FileEncryptionTestUtil.deriveKey(password);
        
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        OutputStream encryptedOutputStream = new EncryptedOutputStream(byteOutputStream, derivedKey);
        encryptedOutputStream.write(plainText.getBytes());
        encryptedOutputStream.close();
        
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
        EncryptedInputStream encryptedInputStream = new EncryptedInputStream(byteInputStream, password);
        String decryptedText = new String(Util.readBytes(encryptedInputStream));
        
        assertEquals(plainText, decryptedText);
    }
    
    /** Tests <code>mark()</code> and <code>reset()</code>. */
    @Test
    public void testReset() throws Exception {
        String plainText = "Ich bin Salomo. Ich bin der arme König Salomo." +
        		"Einst war ich unermeßlich reich, weise und gottesfürchtig. Ob" +
        		"meiner Macht erzitterten die Gewaltigen. Ich war ein Fürst des" +
        		"Friedens und der Gerechtigkeit. Aber meine Weisheit zerstörte" +
        		"meine Gottesfurcht und als ich Gott nicht mehr fürchtete," +
        		"zerstörte meine Weisheit meinen Reichtum. Nun sind die" +
        		"Städte tot, über die ich regierte, mein Reich leer, das mir" +
        		"anvertraut worden war, eine blauschimmernde Wüste, und" +
        		"irgendwo um einen kleinen, gelben, namenlosen Stern kreist," +
        		"sinnlos, immerzu die radioaktive Erde. Ich bin Salomo, ich bin" +
        		"Salomo, ich bin der arme König Salomo.";
        
        byte[] password = "s3krit p@ssw3rd".getBytes();
        DerivedKey derivedKey = FileEncryptionTestUtil.deriveKey(password);
        
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        OutputStream encryptedOutputStream = new EncryptedOutputStream(byteOutputStream, derivedKey);
        encryptedOutputStream.write(plainText.getBytes());
        encryptedOutputStream.close();
        
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
        EncryptedInputStream encryptedInputStream = new EncryptedInputStream(byteInputStream, password);
        encryptedInputStream.mark(1024 * 1024);
        String decryptedText = new String(Util.readBytes(encryptedInputStream));
        assertEquals(plainText, decryptedText);
        
        // reset 
        encryptedInputStream.reset();
        decryptedText = new String(Util.readBytes(encryptedInputStream));
        assertEquals(plainText, decryptedText);
   }
}
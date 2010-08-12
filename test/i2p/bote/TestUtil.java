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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import i2p.bote.email.Email;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.mail.MessagingException;

public class TestUtil {
    
    public static void assertEquals(String message, Email email1, Email email2) throws IOException, MessagingException {
        assertTrue(message, equals(email1, email2));
    }
    
    public static void assertUnequal(String message, Email email1, Email email2) throws IOException, MessagingException {
        assertFalse(message, equals(email1, email2));
    }
    
    private static boolean equals(Email email1, Email email2) throws IOException, MessagingException {
        if (email1==null || email2==null)
            return false;
        
        ByteArrayOutputStream byteStream1 = new ByteArrayOutputStream();
        email1.writeTo(byteStream1);
        
        ByteArrayOutputStream byteStream2 = new ByteArrayOutputStream();
        email2.writeTo(byteStream2);
        
        return Arrays.equals(byteStream1.toByteArray(), byteStream2.toByteArray());
    }
}

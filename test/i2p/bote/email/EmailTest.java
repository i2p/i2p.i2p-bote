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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Test;

public class EmailTest {
    private EmailIdentity[] identities;
    private Email[] emails;

    @Before
    public void setUp() throws Exception {
        identities = new EmailIdentity[2];
        emails = new Email[2];
        
        identities[0] = new EmailIdentity("uDl~luJ5RMhTJHIpYIiZUoxyE6xQHcOELDq9yaXELLYnODd88DBClohCfoIKDKOJL4fMBb34mLN42K8ptisCkLiVWliAZl4jiFtaXUGbXtNnZtkCYheelbL5mFyvcuGmmri-smOMZ-ROcio3V18VwQZfBeV-4-LHWpLaa8tqc1B0KVwCr0PVnwihcibut10VdfELhbhLYfI32fHQFTG6hCCZzhhe1jt8Ixl-aTAj2vXaPyJrfWn3M~Md1XsBAuFAQ5EHh0niJgF~CHn~gsRROpVvVZRDL9OAfAGAomnZMFEixnFW6B3Dce-uCTKFP5Jck3n1gP7cgRfVcXsRd4WCvWmzDFlmNMA~fIfscbTseSSke0AzA05sGskqQxNlnLyPFaXSX5OE4szfYz55onKRbgQFIJ-Pru9C8Ejvd7WGocmF6Lz6mtxhnzEl8-~wutAga94XQuRTlGr1kDSsve1hdGMyQ8UrUS~wP3Ke9JjLI7feg~uUI-bB6YvaVsOuVEHCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACQ2~7HQlbJ7oT-fpSot9DUclsw0LGra2Fk86phRsyGriq5uCv1GLGawFTkFvtBkOP");
        identities[0].setPublicName("Max Mustermann");
        emails[0] = new Email();
        emails[0].setSender(new InternetAddress(identities[0].getPublicName() + " <" + identities[0].getKey() + ">"));
        emails[0].addRecipient(RecipientType.TO, new InternetAddress("Erika Mustermann <uDl~luJ5RMhTJHIpYIiZUoxyE6xQHcOELDq9yaXELLYnODd88DBClohCfoIKDKOJL4fMBb34mLN42K8ptisCkLiVWliAZl4jiFtaXUGbXtNnZtkCYheelbL5mFyvcuGmmri-smOMZ-ROcio3V18VwQZfBeV-4-LHWpLaa8tqc1B0KVwCr0PVnwihcibut10VdfELhbhLYfI32fHQFTG6hCCZzhhe1jt8Ixl-aTAj2vXaPyJrfWn3M~Md1XsBAuFAQ5EHh0niJgF~CHn~gsRROpVvVZRDL9OAfAGAomnZMFEixnFW6B3Dce-uCTKFP5Jck3n1gP7cgRfVcXsRd4WCvWmzDFlmNMA~fIfscbTseSSke0AzA05sGskqQxNlnLyPFaXSX5OE4szfYz55onKRbgQFIJ-Pru9C8Ejvd7WGocmF6Lz6mtxhnzEl8-~wutAga94XQuRTlGr1kDSsve1hdGMyQ8UrUS~wP3Ke9JjLI7feg~uUI-bB6YvaVsOuVEHC>"));
        emails[0].setSubject("Test", "UTF-8");
        
        identities[1] = new EmailIdentity("m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UYYujtjOOwCqXPH9PIbcZeFRkegbOxw5G6I7M4-TZBFbxYDtaew6HX9hnQEGWHkaapq2kTTB3Hmv0Uyo64jvcfMmSRcPng3J1Ho5mHgnzsH0qxQemnBcw7Lfc9fU8xRz858uyiQ8J8XH3T8S7k2~8L7awSgaT7uHQgpV~Rs0p1ofJ70g");
        identities[1].setPublicName("Max Mustermann");
        emails[1] = new Email();
        emails[1].setSender(new InternetAddress(identities[1].getPublicName() + " <" + identities[1].getKey() + ">"));
        emails[1].addRecipient(RecipientType.TO, new InternetAddress("Erika Mustermann <m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY>"));
        emails[1].setSubject("Test", "UTF-8");
        
        for (Email email: emails) {
            // Make a large email so it is a good test case for testCreateEmailPackets()
            StringBuilder emailText = new StringBuilder();
            for (int i=0; i<50*1000; i++)
                emailText.append("1234567890");
            email.setText(emailText.toString(), "UTF-8");
        }
    }

    @Test
    public void testSign() throws MessagingException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
        assertEquals(emails.length, identities.length);
        
        for (int i=0; i<emails.length; i++) {
            Email email = emails[i];
            EmailIdentity identity = identities[i];
            
            // sign and verify signature
            sign(email, identity);
            assertTrue(email.isSignatureValid());
            
            // write the email to a byte array, make a new email from the byte array, and verify the signature
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            email.writeTo(outputStream);
            email = new Email(outputStream.toByteArray());
            assertTrue(email.isSignatureValid());
        }
    }

    /**
     * Calls the private method {link Email.sign(EmailIdentity)}.
     * @param email
     * @param identity
     * @throws NoSuchMethodException 
     * @throws SecurityException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    private void sign(Email email, EmailIdentity identity) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method signMethod = Email.class.getDeclaredMethod("sign", EmailIdentity.class);
        signMethod.setAccessible(true);
        signMethod.invoke(email, identity);
    }
    
    @Test
    public void testCreateEmailPackets() throws MessagingException, IOException, GeneralSecurityException {
        for (int i=0; i<emails.length; i++) {
            Email email = emails[i];
            EmailIdentity identity = identities[i];
            
            Collection<UnencryptedEmailPacket> packets = email.createEmailPackets(identity, null);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (UnencryptedEmailPacket packet: packets)
                outputStream.write(packet.getContent());
            Email newEmail = new Email(outputStream.toByteArray());
            assertEquals(email.getContent(), newEmail.getContent());
        }
    }
}
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

package i2p.bote.packet.dht;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import i2p.bote.TestUtil;
import i2p.bote.Util;
import i2p.bote.crypto.KeyUpdateHandler;
import i2p.bote.crypto.wordlist.WordListAnchor;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Fingerprint;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import org.junit.Before;
import org.junit.Test;

public class ContactTest {
    private Contact contact;
    private String contactName = "Test 123";

    @Before
    public void setUp() throws Exception {
        EmailIdentity identity = new EmailIdentity("0FXoqTc2bakNPiNZWD7rwT4Q465bFnF66yV7p5emCl6s9shuU3pdTBExBgLf7Pn6KswQ2hn8amqJKepFW7RgUYFncf-UXH~IWrD0E3VAR94WVuSzpqK33LA1aS7By4juHOSDDmDTL0sMBESQADS0NLLp7y7nrNUD93loexkO63DF");
        identity.setPublicName(contactName);
        String text = "Der Friederich, der Friederich,\n" +
                "Das war ein arger Wüterich!\n" +
                "Er fing die Fliegen in dem Haus\n" +
                "Und riß ihnen die Flügel aus.\n" +
                "Er schlug die Stühl’ und Vögel tot,\n" +
                "Die Katzen litten große Not.\n" +
                "Und höre nur, wie bös er war:\n" +
                "Er peitschte, ach, sein Gretchen gar!\n" +
                "\n" +
                "Am Brunnen stand ein großer Hund,\n" +
                "Trank Wasser dort mit seinem Mund.\n" +
                "Da mit der Peitsch’ herzu sich schlich\n" +
                "Der bitterböse Friederich;\n" +
                "Und schlug den Hund, der heulte sehr,\n" +
                "Und trat und schlug ihn immer mehr.\n" +
                "Da biß der Hund ihn in das Bein,\n" +
                "Recht tief bis in das Blut hinein.\n" +
                "Der bitterböse Friederich,\n" +
                "Der schrie und weinte bitterlich.\n" +
                "Jedoch nach Hause lief der Hund\n" +
                "Und trug die Peitsche in dem Mund.\n" +
                "\n" +
                "Ins Bett muß Friedrich nun hinein,\n" +
                "Litt vielen Schmerz an seinem Bein;\n" +
                "Und der Herr Doktor sitzt dabei\n" +
                "Und gibt ihm bitt’re Arzenei.\n" +
                "Der Hund an Friedrichs Tischchen saß,\n" +
                "Wo er den großen Kuchen aß;\n" +
                "Aß auch die gute Leberwurst\n" +
                "Und trank den Wein für seinen Durst.\n" +
                "Die Peitsche hat er mitgebracht\n" +
                "Und nimmt sie sorglich sehr in acht.\n";
        InputStream inputStream = getClass().getResourceAsStream("Struwwelpeter.jpg");
        byte[] picture = Util.readBytes(inputStream);
        KeyUpdateHandler keyUpdateHandler = TestUtil.createVerifyingKeyUpdateHandler(1);
        identity.generateFingerprint();
        contact = new Contact(identity, keyUpdateHandler, picture, text, identity.getFingerprint());
    }

    @Test
    public void toByteArrayAndBack() throws IOException, GeneralSecurityException {
        WordListAnchor wordLists = new WordListAnchor();
        String[] wordListEN = wordLists.getWordList("en");
        String[] wordListDE = wordLists.getWordList("de");
        
        byte[] arrayA = contact.toByteArray();
        Contact contact2 = new Contact(arrayA);
        assertEquals(contact.getName(), contact2.getName());
        assertEquals(contact.getDestination().toBase64(), contact2.getDestination().toBase64());
        assertEquals(contact.getText(), contact2.getText());
        assertEquals(contact.getFingerprint().getWords(wordListDE), contact2.getFingerprint().getWords(wordListDE));
        assertEquals(contact.getFingerprint().getWords(wordListEN), contact2.getFingerprint().getWords(wordListEN));
        assertFalse(contact.getFingerprint().getWords(wordListEN).equals(contact2.getFingerprint().getWords(wordListDE)));
        assertEquals(contact.getPictureBase64(), contact2.getPictureBase64());
        byte[] arrayB = contact2.toByteArray();
        assertArrayEquals("The two arrays differ!", arrayA, arrayB);
    }
    
    @Test
    public void testSignatureAndFingerprint() throws GeneralSecurityException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        assertTrue(contact.verify());
        
        // change the name
        char[] chars = contactName.toCharArray();
        chars[0]++;
        String alteredName = new String(chars);
        contact.setName(alteredName);
        assertFalse(contact.verify());
        
        // restore the original name and make the salt invalid
        contact.setName(contactName);
        Fingerprint fingerprint = contact.getFingerprint();
        Field saltField = Fingerprint.class.getDeclaredField("salt");
        saltField.setAccessible(true);
        byte[] salt = (byte[])saltField.get(fingerprint);
        salt[2]++;
        assertFalse(contact.verify());
        
        // restore salt
        salt[2]--;
        assertTrue(contact.verify());
    }
}
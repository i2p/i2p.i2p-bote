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
import i2p.bote.UniqueId;
import i2p.bote.crypto.ECDH521_ECDSA521;
import i2p.bote.crypto.ElGamal2048_DSA1024;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.packet.I2PBotePacket;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Hash;

import org.junit.Before;
import org.junit.Test;

public class EncryptedEmailPacketTest {
    EncryptedEmailPacket[] encryptedPackets;
    EncryptedEmailPacket ecdhEncryptedPacket;
    UnencryptedEmailPacket plaintextPacket;
    EmailIdentity[] identities;
    String message = "This is a test message. Test 1 2 3 Test";

    @Before
    public void setUp() throws Exception {
        // make an UnencryptedEmailPacket
        byte[] content = message.getBytes();
        byte[] messageIdBytes = new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        int fragmentIndex = 0;
        plaintextPacket = new UnencryptedEmailPacket(new ByteArrayInputStream(content), messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        plaintextPacket.setNumFragments(1);
        
        encryptedPackets = new EncryptedEmailPacket[2];
        identities = new EmailIdentity[2];
        
        // make a ElGamal/DSA identity
        String elGamalBase64 = "piYT1uJ3O8~bBPZmTvehMbp3-Zksg5enhvIlp2X8txqL25l0WdQMWwyt30UAOVQqxGdnMPTqqjh~-zoa~rCQORo~J1gRxLwCX9LlHQqaIimJilrbN-rhKy4Xlft054wbgQjLSC-WICE4W64KDfitwRzdr7lV6lz~0KFiZ8erZ-~WPMG1CgWEku9lILQUdUHyFBguPcK9oPDq7oGBuFGy8w0CvAq7ex3nmbL7zQVA~VqILtOGeGK2fidCuuofj4AQsTcXmH9O0nxZGCIJBhf~4EWmazvxu8XVB8pabNQvRDbmFu6q85JTwmxC45lCjqNw30hp8q2zoqP-zchjWOrxFUhSumpBdD0xXJR~qmhejh4WnuRnnam9j3fcxH5i~T7xWgmvIbpZEI4kyc9VEbXbLI7k-bU2A6sdP-AGt5~TjGLcxpdsPnOLRXO-Dsi7E9-3Kc84s4TmdpEJdtHn1dxYyeeT-ysVOqXjv5w5Cuk0XJpUIJG8n7aXHpNb-QLxPD3yAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADWF3qnAX-p41Po~VNmOUzS-Yt~noD8-e~L3P5rZXBWf-XtB4hkloo6m1jwqphEdf1";
        identities[0] = new EmailIdentity(elGamalBase64);
        EmailDestination elGamalDestination = new EmailDestination(identities[0].getKey());
        // make an ElGamal encrypted packet
        assertTrue(identities[0].getCryptoImpl() instanceof ElGamal2048_DSA1024);
        encryptedPackets[0] = new EncryptedEmailPacket(plaintextPacket, elGamalDestination);
        
        // make a ECDH/ECDSA identity
        String ecdhBase64 = "m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UYYujtjOOwCqXPH9PIbcZeFRkegbOxw5G6I7M4-TZBFbxYDtaew6HX9hnQEGWHkaapq2kTTB3Hmv0Uyo64jvcfMmSRcPng3J1Ho5mHgnzsH0qxQemnBcw7Lfc9fU8xRz858uyiQ8J8XH3T8S7k2~8L7awSgaT7uHQgpV~Rs0p1ofJ70g";
        identities[1] = new EmailIdentity(ecdhBase64);
        EmailDestination ecdhDestination = new EmailDestination(identities[1].getKey());
        // make an ECDH encrypted packet
        assertTrue(identities[1].getCryptoImpl() instanceof ECDH521_ECDSA521);
        encryptedPackets[1] = new EncryptedEmailPacket(plaintextPacket, ecdhDestination);
    }

    @Test
    public void toByteArrayAndBack() throws Exception {
        for (EncryptedEmailPacket packet: encryptedPackets) {
            byte[] arrayA = packet.toByteArray();
            byte[] arrayB = new EncryptedEmailPacket(arrayA).toByteArray();
            assertArrayEquals("The two arrays differ! CryptoImplementation = " + packet.getCryptoImpl().getName(), arrayA, arrayB);
        }
    }
    
    @Test
    public void testEncryptionDecryption() throws Exception {
        for (int i=0; i<encryptedPackets.length; i++) {
            EncryptedEmailPacket packet = encryptedPackets[i];
            UnencryptedEmailPacket decryptedPacket = packet.decrypt(identities[i]);
            byte[] arrayA = decryptedPacket.getContent();
            byte[] arrayB = message.getBytes();
            assertArrayEquals("Email message differs after decryption! CryptoImplementation = " + packet.getCryptoImpl().getName(), arrayA, arrayB);
        }
    }
    
    @Test
    public void testDeleteVerificationHash() {
        for (int i=0; i<encryptedPackets.length; i++) {
            EncryptedEmailPacket packet = encryptedPackets[i];
            Hash expectedHash = SHA256Generator.getInstance().calculateHash(plaintextPacket.getDeleteAuthorization().toByteArray());
            assertEquals("The delete authorization key does not hash to the delete verification hash!", expectedHash, packet.getDeleteVerificationHash());
        }
    }
    
    @Test
    public void testHash() throws Exception {
        for (EncryptedEmailPacket packet: encryptedPackets) {
            assertTrue("Hash not valid! CryptoImplementation = " + packet.getCryptoImpl().getName(), packet.verifyPacketHash());
        
            alterEncryptedData(packet);
            assertFalse("Hash is valid, but should be invalid! CryptoImplementation = " + packet.getCryptoImpl().getName(), packet.verifyPacketHash());
        }
    }
    
    private void alterEncryptedData(EncryptedEmailPacket packet) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field encryptedDataField = EncryptedEmailPacket.class.getDeclaredField("encryptedData");
        encryptedDataField.setAccessible(true);
        Object encryptedDataObject = encryptedDataField.get(packet);
        byte[] encryptedData = (byte[])encryptedDataObject;
        
        // flip one bit
        encryptedData[0] ^= 1;
    }
}
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import i2p.bote.UniqueId;
import i2p.bote.email.EmailIdentity;
import i2p.bote.packet.I2PBotePacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import net.i2p.client.I2PSessionException;
import net.i2p.data.Hash;

import org.junit.Before;
import org.junit.Test;

public class IndexPacketTest {
    EmailIdentity identity;
    UniqueId deleteAuthorization1, deleteAuthorization2, deleteAuthorization3, deleteAuthorization4, deleteAuthorization5;
    EncryptedEmailPacket emailPacket1, emailPacket2, emailPacket3, emailPacket4, emailPacket5;
    IndexPacket indexPacket1, indexPacket2;

    @Before
    public void setUp() throws Exception {
        identity = new EmailIdentity("5LqFf~U3aLbJfbfTVtp7kXLPFoeIFo4l8WTg1Wi52bWoxAYaevVVBtR9AvKqy1YmZHbnOIcu59~2X6wMmi6SveljmvAeTc5YEHvfIRrJhnxqjaC4IczYKXfUdrXfaeVEKMQ~PKuvhINh~EhlJUQne0NZQ~S6QAGfUAu83mMoBTVaz0eoUnAzySxbSf~NpxUoK-H6iULsFekmYfaz-yq8cxPFy62LyylTRMGFFwb9is7E~mFnV6Fa0iGSDJvpFfYV29efVUjxiW9JT5T0HwgdaDB4ssSNr0-hthigJmB7zLXOJ8F1gxi3qCfTX9SiGMrZ9KZsOLc7Qs7Iix3ECqesfGTIs9n5G1qnfZriyc1FZdylCMQcnq5QvTITV-Cil0XrU1csV5CEFYEUGfGdLP1xP2SCZr8KJwOI0xfUnkkVnNPc2y~ZGhqxpHeIcnZCpScW-p81vFvTe5fwvEVWixgk6MlFKYyQku28brQ19Tz5tsIH3tUvl4cqGzLVQUbi3cODAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAmogzaNWx7p~GTVQxl1JzqcOTyRfELoxqEGS5BowqPe4FNa3Bo-diEcU3k90Cx5MX");
        
        // Make an IndexPacket with 3 entries
        emailPacket1 = makeEmailPacket("abc");
        emailPacket2 = makeEmailPacket("abcd");
        emailPacket3 = makeEmailPacket("abcde");
        indexPacket1 = new IndexPacket(identity);
        indexPacket1.put(emailPacket1);
        indexPacket1.put(emailPacket2);
        indexPacket1.put(emailPacket3);
        
        // Make an IndexPacket with 2 entries
        emailPacket4 = makeEmailPacket("abcdef");
        emailPacket5 = makeEmailPacket("abcdefg");
        indexPacket2 = new IndexPacket(identity);
        indexPacket2.put(emailPacket4);
        indexPacket2.put(emailPacket5);
    }

    private EncryptedEmailPacket makeEmailPacket(String message) throws GeneralSecurityException, I2PSessionException, IOException {
        byte[] content = message.getBytes();
        
        byte[] messageIdBytes = new byte[] {6, -32, -23, 17, 55, 15, -45, -19, 91, 100, -76, -76, 118, -118, -53, -109, -108, 113, -112, 81, 117, 9, -126, 20, 0, -83, -89, 7, 48, 76, -58, 83};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        int fragmentIndex = 0;
        
        UnencryptedEmailPacket plaintextPacket = new UnencryptedEmailPacket(new ByteArrayInputStream(content), messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        plaintextPacket.setNumFragments(1);
        return new EncryptedEmailPacket(plaintextPacket, identity);
    }
    
    /**
     * Verifies that the arrays are the right length 
     * @throws IllegalAccessException 
     * @throws NoSuchFieldException 
     * @throws IllegalArgumentException 
     * @throws SecurityException
     */
    @Test
    public void testToByteArray() throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        // test the first index packet
        int arrayLength1 = indexPacket1.toByteArray().length;
        int expectedLength1 = 2 + Hash.HASH_LENGTH + 4 + indexPacket1.getNumEntries() * (2*Hash.HASH_LENGTH+4);
        assertEquals(expectedLength1, arrayLength1);
        
        // test the second index packet
        int arrayLength2 = indexPacket2.toByteArray().length;
        int expectedLength2 = 2 + Hash.HASH_LENGTH + 4 + indexPacket2.getNumEntries() * (2*Hash.HASH_LENGTH+4);
        assertEquals(expectedLength2, arrayLength2);
    }
    
    @Test
    public void testToByteArrayAndBack() throws GeneralSecurityException {
        IndexPacket newPacket1 = new IndexPacket(indexPacket1.toByteArray());
        assertTrue("The two packets differ!", equal(indexPacket1, newPacket1));
        
        IndexPacket newPacket2 = new IndexPacket(indexPacket2.toByteArray());
        assertTrue("The two packets differ!", equal(indexPacket2, newPacket2));
    }

    /**
     * Tests if two <code>IndexPacket</code>s are equal. They are considered equal if
     * they contain the same number of entries, and there is a matching entry in
     * packet B for every entry in packet A.
     * @param packetA
     * @param packetB
     * @return
     */
    private boolean equal(IndexPacket packetA, IndexPacket packetB) {
        // test if #entries is the same
        if (packetA.getNumEntries() != packetB.getNumEntries())
            return false;
        
        // compare entries
        for (IndexPacketEntry entryA: packetA) {
            boolean found = false;
            for (IndexPacketEntry entryB: packetB)
                // compare entryA and entryB
                if (entryA.emailPacketKey.equals(entryB.emailPacketKey)) {
                    boolean delVerifEqual = (entryA.delVerificationHash==null && entryB.delVerificationHash==null) || entryA.delVerificationHash.equals(entryB.delVerificationHash);
                    boolean timeEqual = entryA.storeTime == entryB.storeTime;
                    if (delVerifEqual && timeEqual) {
                        found = true;
                        break;
                    }
                }
            if (!found)
                return false;
        }
        return true;
    }
    
    @Test
    public void testMergePackets() {
        for (int i=0; i<8; i+=2) {
            IndexPacket mergedPacket = new IndexPacket(indexPacket1, indexPacket2);
        
            // Verify that the merged packet and the two original packets all have the same email destination key
            assertEquals(mergedPacket.getDhtKey(), indexPacket1.getDhtKey());
            assertEquals(mergedPacket.getDhtKey(), indexPacket2.getDhtKey());
    
            // Verify that the merged packet contains the email packet keys from the original two packets
            assertEquals(5, mergedPacket.getNumEntries());
            for (IndexPacketEntry entry: indexPacket1)
                assertTrue("Merged packet does not contain key: " + entry.emailPacketKey, mergedPacket.contains(entry.emailPacketKey));
            for (IndexPacketEntry entry: indexPacket2)
                assertTrue("Merged packet does not contain key: " + entry.emailPacketKey, mergedPacket.contains(entry.emailPacketKey));
        }
    }
    
    @Test
    public void testRemoveKey() {
        assertEquals(3, indexPacket1.getNumEntries());
        
        // Try to remove a key that doesn't exist in the packet
        indexPacket1.remove(emailPacket4.getDhtKey());
        assertEquals(3, indexPacket1.getNumEntries());
        
        // Remove a key that does exist in the packet
        indexPacket1.remove(emailPacket1.getDhtKey());
        assertEquals(2, indexPacket1.getNumEntries());
    }
}
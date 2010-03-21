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

package i2p.bote.packet;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import i2p.bote.UniqueId;
import i2p.bote.email.EmailDestination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import net.i2p.I2PAppContext;
import net.i2p.client.I2PSessionException;
import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;

import org.junit.Before;
import org.junit.Test;

public class IndexPacketTest {
    EmailDestination destination;
    UniqueId deletionKey1, deletionKey2, deletionKey3, deletionKey4, deletionKey5;
    EncryptedEmailPacket emailPacket1, emailPacket2, emailPacket3, emailPacket4, emailPacket5;
    IndexPacket indexPacket1, indexPacket2;

    @Before
    public void setUp() throws Exception {
        Collection<EncryptedEmailPacket> emailPackets;
        
        I2PAppContext appContext = new I2PAppContext();
        destination = new EmailDestination("RKm-q0laRq2iZpsCoG6DMcuUERjA5cQ2DpAtQUxPuNbkKFukLEpU8c50stkDiO2SVKCeciOPou64CGFgW8yJ~vhvENBZWJUSQIohzVkupBdpeX8osCpy8t51lu9pvmx2RReWLLuZ1vQbn5zEa6dBbUhJnmls~gT1bHojM6I3fHJM7DDztOMCKjehaqFdW6DZwvxfNg2Dvro0D8GkpW4ID8Hw1pmoT5Ux6tzRo9JJdE5Fu17joEOVqNdJ4LUgjw6hPnuXCB4BBT07U-yNImusEyVCdPYk10Vc24iPBRCkKUFga9chshf3AoufEi~QkNk5F8ZZXo991ZNTZT2H5wx6JX0sGzg2HJxeT1t2f6Z9oGhFi5LtcJEztLjJhV52WcvEQjEmusi4oS1-aYvn81peoRR8nN045ZdYAnWyzT-xtx16qvKj~sqE9N0~pgKqx3rFDqA235zFfEpIXTWNssebOtCKrK2roFAMRFo2gkNMuEmUVfH2pOqTUrC4MMySoZNh");
        
        // Make the first IndexPacket
        deletionKey1 = new UniqueId(new byte[] {-62, -112, 99, -65, 13, 44, -117, -111, 96, 45, -6, 64, 78, 57, 117, 103, -24, 101, 106, -116, -18, 62, 99, -49, 60, -81, 8, 64, 27, -41, -104, 58}, 0);
        emailPacket1 = makeEmailPacket("abc", deletionKey1, appContext);
        deletionKey2 = new UniqueId(new byte[] {120, 120, -8, -88, 21, 126, 46, -61, 18, -101, 15, 53, 20, -44, -112, 42, 86, -117, 30, -96, -66, 33, 71, -55, -102, -78, 78, -82, -105, 66, -116, 43}, 0);
        emailPacket2 = makeEmailPacket("abcd", deletionKey2, appContext);
        deletionKey3 = new UniqueId(new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123}, 0);
        emailPacket3 = makeEmailPacket("abcde", deletionKey3, appContext);
        emailPackets = new ArrayList<EncryptedEmailPacket>();
        emailPackets.add(emailPacket1);
        emailPackets.add(emailPacket2);
        emailPackets.add(emailPacket3);
        indexPacket1 = new IndexPacket(emailPackets, destination);
        
        // Make the second IndexPacket
        deletionKey4 = new UniqueId(new byte[] {-16, 67, 107, 80, 27, 65, 81, 71, 61, 70, -72, 126, 64, -10, 57, -128, 111, -107, -42, 24, -90, -4, -46, 63, 7, -6, 43, 76, -9, -81, 8, -68}, 0);
        emailPacket4 = makeEmailPacket("abcdef", deletionKey4, appContext);
        deletionKey5 = new UniqueId(new byte[] {-37, -8, 37, 82, -40, -34, 68, -51, -16, 74, 27, 89, 113, -15, 112, 69, 92, 102, 62, 111, 99, -27, -42, -71, 6, 38, 106, 121, 21, -72, -83, 3}, 0);
        emailPacket5 = makeEmailPacket("abcdefg", deletionKey5, appContext);
        emailPackets = new ArrayList<EncryptedEmailPacket>();
        emailPackets.add(emailPacket4);
        emailPackets.add(emailPacket5);
        indexPacket2 = new IndexPacket(emailPackets, destination);
    }

    private EncryptedEmailPacket makeEmailPacket(String message, UniqueId deletionKey, I2PAppContext appContext) throws DataFormatException, I2PSessionException {
        byte[] content = message.getBytes();
        
        byte[] messageIdBytes = new byte[] {6, -32, -23, 17, 55, 15, -45, -19, 91, 100, -76, -76, 118, -118, -53, -109, -108, 113, -112, 81, 117, 9, -126, 20, 0, -83, -89, 7, 48, 76, -58, 83};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        int fragmentIndex = 0;
        int numFragments = 1;
        
        UnencryptedEmailPacket plaintextPacket = new UnencryptedEmailPacket(messageId, fragmentIndex, numFragments, content, deletionKey);
        return new EncryptedEmailPacket(plaintextPacket, destination, appContext);
    }
    
    @Test
    public void testToByteArray() {
        // Verify that the array is the right length
        int arrayLength = indexPacket1.toByteArray().length;
        int expectedLength = Hash.HASH_LENGTH + 3 + indexPacket1.getDhtKeys().size() * (Hash.HASH_LENGTH+UniqueId.LENGTH);
        assertEquals(expectedLength, arrayLength);
    }
    
    @Test
    public void testToByteArrayAndBack() {
        IndexPacket packetA = new IndexPacket(indexPacket1.toByteArray());
        IndexPacket packetB = new IndexPacket(indexPacket2.toByteArray());
        assertTrue("The two packets differ!", equal(packetA, packetB));
    }

    /**
     * Tests if two <code>IndexPacket</code>s are equal. They are considered equal if
     * they contain the same DHT keys, and the DHT keys map to the same deletion keys.
     * @param packetA
     * @param packetB
     * @return
     */
    private boolean equal(IndexPacket packetA, IndexPacket packetB) {
        for (Map.Entry<Hash, UniqueId> entry: packetA.getEntries().entrySet()) {
            Hash dhtKey = entry.getKey();
            UniqueId delKey = packetB.getDeletionKey(dhtKey);
            if (delKey!=null && !delKey.equals(entry.getValue()))
                return false;
        }
        for (Map.Entry<Hash, UniqueId> entry: packetB.getEntries().entrySet()) {
            Hash dhtKey = entry.getKey();
            UniqueId delKey = packetA.getDeletionKey(dhtKey);
            if (delKey!=null && !delKey.equals(entry.getValue()))
                return false;
        }
        return true;
    }
    
    @Test
    public void testMergePackets() {
        IndexPacket indexPacket3 = new IndexPacket(indexPacket1, indexPacket2);
        
        // Verify that all three index packets have the same email destination key
        assertEquals(indexPacket3.getDhtKey(), indexPacket1.getDhtKey());
        assertEquals(indexPacket3.getDhtKey(), indexPacket2.getDhtKey());

        // Verify that the merged index packet contains the email packet keys from the original two index packets
        assertEquals(5, indexPacket3.getDhtKeys().size());
        for (Hash dhtKey: indexPacket1.getDhtKeys())
            assertTrue("Merged packet does not contain key: " + dhtKey, indexPacket3.contains(dhtKey));
        for (Hash dhtKey: indexPacket2.getDhtKeys())
            assertTrue("Merged packet does not contain key: " + dhtKey, indexPacket3.contains(dhtKey));
    }
    
    @Test
    public void testRemoveKey() {
        assertEquals(3, indexPacket1.getDhtKeys().size());
        
        // Try to remove a key that doesn't exist in the packet
        indexPacket1.remove(emailPacket4.getDhtKey());
        assertEquals(3, indexPacket1.getDhtKeys().size());
        
        // Remove a key that does exist in the packet
        indexPacket1.remove(emailPacket1.getDhtKey());
        assertEquals(2, indexPacket1.getDhtKeys().size());
    }
    
    @Test
    public void testGetDeletionKey() {
        // Verify that the deletion keys were assigned to the right email packets
        assertEquals(deletionKey1, indexPacket1.getDeletionKey(emailPacket1.getDhtKey()));
        assertEquals(deletionKey2, indexPacket1.getDeletionKey(emailPacket2.getDhtKey()));
        assertEquals(deletionKey3, indexPacket1.getDeletionKey(emailPacket3.getDhtKey()));
        assertEquals(deletionKey4, indexPacket2.getDeletionKey(emailPacket4.getDhtKey()));
        assertEquals(deletionKey5, indexPacket2.getDeletionKey(emailPacket5.getDhtKey()));
    }
}
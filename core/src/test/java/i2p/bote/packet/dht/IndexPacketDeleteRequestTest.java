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

import static org.junit.Assert.assertEquals;
import i2p.bote.UniqueId;

import java.util.Arrays;
import java.util.Comparator;

import net.i2p.data.Hash;

import org.junit.Before;
import org.junit.Test;

public class IndexPacketDeleteRequestTest {
    IndexPacketDeleteRequest delRequest;
    Hash[] dhtKeys;
    UniqueId[] delKeys;

    @Before
    public void setUp() throws Exception {
        Hash emailDestHash = new Hash(new byte[] {-48, 78, 66, 58, -79, 87, 38, -103, -60, -27, 108, 55, 117, 37, -99, 93, -23, -102, -83, 20, 44, -80, 65, 89, -68, -73, 69, 51, 115, 79, 24, 127});
        delRequest = new IndexPacketDeleteRequest(emailDestHash);

        dhtKeys = new Hash[3];
        delKeys = new UniqueId[3];
        
        dhtKeys[0] = new Hash(new byte[] {120, 120, -8, -88, 21, 126, 46, -61, 18, -101, 15, 53, 20, -44, -112, 42, 86, -117, 30, -96, -66, 33, 71, -55, -102, -78, 78, -82, -105, 66, -116, 43});
        delKeys[0] = new UniqueId(new byte[] {-62, -112, 99, -65, 13, 44, -117, -111, 96, 45, -6, 64, 78, 57, 117, 103, -24, 101, 106, -116, -18, 62, 99, -49, 60, -81, 8, 64, 27, -41, -104, 58}, 0);
        delRequest.put(dhtKeys[0], delKeys[0]);

        dhtKeys[1] = new Hash(new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123});
        delKeys[1] = new UniqueId(new byte[] {-16, 67, 107, 80, 27, 65, 81, 71, 61, 70, -72, 126, 64, -10, 57, -128, 111, -107, -42, 24, -90, -4, -46, 63, 7, -6, 43, 76, -9, -81, 8, -68}, 0);
        delRequest.put(dhtKeys[1], delKeys[1]);

        dhtKeys[2] = new Hash(new byte[] {-37, -8, 37, 82, -40, -34, 68, -51, -16, 74, 27, 89, 113, -15, 112, 69, 92, 102, 62, 111, 99, -27, -42, -71, 6, 38, 106, 121, 21, -72, -83, 3});
        delKeys[2] = new UniqueId(new byte[] {6, -32, -23, 17, 55, 15, -45, -19, 91, 100, -76, -76, 118, -118, -53, -109, -108, 113, -112, 81, 117, 9, -126, 20, 0, -83, -89, 7, 48, 76, -58, 83}, 0);
        delRequest.put(dhtKeys[2], delKeys[2]);
    }

    @Test
    public void toByteArrayAndBack() {
        assertEquals(3, delRequest.getDhtKeys().size());
        
        Comparator<Hash> hashComparator = new Comparator<Hash>() {
            @Override
            public int compare(Hash key1, Hash key2) {
                return key1.toString().compareTo(key2.toString());
            }
        };
 
        byte[] bytes = delRequest.toByteArray();
        IndexPacketDeleteRequest delRequest2 = new IndexPacketDeleteRequest(bytes);
        // Make an array of DHT keys from delRequest2.
        // Sort the array elements first because they are not guaranteed to be in any order.
        Arrays.sort(dhtKeys, hashComparator);
        Hash[] dhtKeys2 = delRequest2.getDhtKeys().toArray(new Hash[0]);
        Arrays.sort(dhtKeys2, hashComparator);
        assertEquals("The two arrays are different lengths", dhtKeys.length, dhtKeys2.length);
        for (int i=0; i<dhtKeys.length; i++)
            assertEquals("DHT keys differ: " + dhtKeys[i] + " vs " + dhtKeys2[i], dhtKeys[i], dhtKeys2[i]);
    }
}
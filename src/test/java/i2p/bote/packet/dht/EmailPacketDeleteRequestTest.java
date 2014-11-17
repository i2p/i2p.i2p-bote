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
import i2p.bote.UniqueId;

import net.i2p.data.Hash;

import org.junit.Before;
import org.junit.Test;

public class EmailPacketDeleteRequestTest {
    private EmailPacketDeleteRequest delRequest;

    @Before
    public void setUp() throws Exception {
        Hash dhtKey = new Hash(new byte[] {-48, 78, 66, 58, -79, 87, 38, -103, -60, -27, 108, 55, 117, 37, -99, 93, -23, -102, -83, 20, 44, -80, 65, 89, -68, -73, 69, 51, 115, 79, 24, 127});
        byte[] packetIdBytes = new byte[] {120, 120, -8, -88, 21, 126, 46, -61, 18, -101, 15, 53, 20, -44, -112, 42, 86, -117, 30, -96, -66, 33, 71, -55, -102, -78, 78, -82, -105, 66, -116, 43};
        UniqueId deleteAuthorization = new UniqueId(packetIdBytes, 0);
        delRequest = new EmailPacketDeleteRequest(dhtKey, deleteAuthorization);
    }

    @Test
    public void toByteArrayAndBack() {
        byte[] arrayA = delRequest.toByteArray();
        byte[] arrayB;
        arrayB = new EmailPacketDeleteRequest(arrayA).toByteArray();
        assertArrayEquals("The two arrays differ!", arrayA, arrayB);
    }
}
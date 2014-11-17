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
import i2p.bote.packet.dht.FindClosePeersPacket;

import net.i2p.data.Hash;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FindClosePeersPacketTest {
    FindClosePeersPacket findCloseNodesPacket;

    @Before
    public void setUp() throws Exception {
        Hash key = new Hash(new byte[] {-48, 78, 66, 58, -79, 87, 38, -103, -60, -27, 108, 55, 117, 37, -99, 93, -23, -102, -83, 20, 44, -80, 65, 89, -68, -73, 69, 51, 115, 79, 24, 127});
        
        findCloseNodesPacket = new FindClosePeersPacket(key);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void toByteArrayAndBack() throws Exception {
        byte[] arrayA = findCloseNodesPacket.toByteArray();
        byte[] arrayB = new FindClosePeersPacket(arrayA).toByteArray();
        assertArrayEquals("The two arrays differ!", arrayA, arrayB);
    }
}
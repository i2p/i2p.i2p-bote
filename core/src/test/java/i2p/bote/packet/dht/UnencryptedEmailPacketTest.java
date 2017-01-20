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
import i2p.bote.packet.I2PBotePacket;

import java.io.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Test;

public class UnencryptedEmailPacketTest {
    private UnencryptedEmailPacket packet;

    @Before
    public void setUp() throws Exception {
        String message = "This is a test message. Test 1 2 3 Test";
        byte[] content = message.getBytes();
        
        byte[] messageIdBytes = new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        
        int fragmentIndex = 0;

        packet = new UnencryptedEmailPacket(new ByteArrayInputStream(content), messageId, fragmentIndex, I2PBotePacket.MAX_DATAGRAM_SIZE);
        packet.setNumFragments(1);
    }
    
    @Test
    public void toByteArrayAndBack() {
        byte[] arrayA = packet.toByteArray();
        byte[] arrayB = new UnencryptedEmailPacket(arrayA).toByteArray();
        assertArrayEquals("The two arrays differ!", arrayA, arrayB);
    }
}
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

import i2p.bote.UniqueId;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class UnencryptedEmailPacketTest {
	private UnencryptedEmailPacket packet;

    @Before
    public void setUp() throws Exception {
        String message = "This is a test message. Test 1 2 3 Test";
        byte[] content = message.getBytes();
        UniqueId deletionKeyPlain = new UniqueId(new byte[] {72, -18, -72, -39, 122, 40, -104, -66, -54, -61, -108, 72, 54, 30, 37, 76, 44, 86, 104, -124, -31, 32, -82, -27, 26, 76, 7, 106, -76, 72, 49, -44}, 0);
        UniqueId deletionKeyVerify = new UniqueId(new byte[] {-62, -112, 99, -65, 13, 44, -117, -111, 96, 45, -6, 64, 78, 57, 117, 103, -24, 101, 106, -116, -18, 62, 99, -49, 60, -81, 8, 64, 27, -41, -104, 58}, 0);
        
        byte[] messageIdBytes = new byte[] {2, -69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123};
        UniqueId messageId = new UniqueId(messageIdBytes, 0);
        
        int fragmentIndex = 0;
        int numFragments = 1;

        packet = new UnencryptedEmailPacket(deletionKeyPlain, deletionKeyVerify, messageId, fragmentIndex, numFragments, content);
    }
    
	@Test
	public void toByteArrayAndBack() {
        byte[] arrayA = packet.toByteArray();
        byte[] arrayB = new UnencryptedEmailPacket(arrayA).toByteArray();
        assertTrue("The two arrays differ!", Arrays.equals(arrayA, arrayB));
	}
}
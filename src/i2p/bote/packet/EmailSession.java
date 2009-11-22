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

import java.nio.ByteBuffer;

import net.i2p.util.RandomSource;

public class EmailSession {
    private static final byte SESSION_ID_LENGTH = 16;
    
    private byte[] sessionId;
    
    public EmailSession() {
        sessionId = generateSessionId();
    }

    /**
     * Construct a <CODE>EmailSession</CODE> using data read from a <CODE>ByteBuffer</CODE>.
     * @param buffer
     */
    public EmailSession(ByteBuffer buffer) {
        sessionId = new byte[SESSION_ID_LENGTH];
        buffer.get(sessionId);
    }

    public byte[] getSessionId() {
        return sessionId;
    }
    
    private byte[] generateSessionId() {
        RandomSource randomSource = RandomSource.getInstance();
        byte[] sessionId = new byte[SESSION_ID_LENGTH];
        for (int i=0; i<sessionId.length; i++)
            sessionId[i] = (byte)randomSource.nextInt(256);
        return sessionId;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("[");
        for (int i=0; i<sessionId.length; i++) {
            if (i > 0)
                buffer = buffer.append(" ");
            String hexByte = Integer.toHexString(sessionId[i] & 0xFF);
            if (hexByte.length() < 2)
                buffer = buffer.append("0");
            buffer = buffer.append(hexByte);
        }
        return buffer.append("]").toString();
    }
}
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

package i2p.bote;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import net.i2p.data.Base64;
import net.i2p.util.RandomSource;

public class UniqueId implements Comparable<UniqueId> {
    public static final byte LENGTH = 32;

    private byte[] bytes;

    /**
     * Create a random <code>UniqueId</code>.
     */
    public UniqueId() {
        bytes = new byte[LENGTH];
        for (int i=0; i<LENGTH; i++)
            bytes[i] = (byte)RandomSource.getInstance().nextInt(256);
    }

    /**
     * Create a packet id from 32 bytes of an array, starting at <code>offset</code>.
     * @param bytes
     */
    public UniqueId(byte[] bytes, int offset) {
        this.bytes = new byte[LENGTH];
        System.arraycopy(bytes, offset, this.bytes, 0, LENGTH);
    }
    
    /**
     * Creates a <code>UniqueId</code> using data read from a {@link ByteBuffer}.
     * @param buffer
     */
    public UniqueId(ByteBuffer buffer) {
        bytes = new byte[LENGTH];
        buffer.get(bytes);
    }
    
    /**
     * @param base64 A 44-character base64-encoded string
     */
    public UniqueId(String base64) {
        bytes = Base64.decode(base64);
    }
    
    public byte[] toByteArray() {
        return bytes;
    }
    
    public String toBase64() {
        return Base64.encode(bytes);
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(bytes);
    }
    
    @Override
    public int compareTo(UniqueId otherPacketId) {
        return new BigInteger(1, bytes).compareTo(new BigInteger(1, otherPacketId.bytes));
    }
    
    @Override
    public String toString() {
        return Base64.encode(bytes);
    }
    
    @Override
    public boolean equals(Object anotherObject) {
        if (anotherObject == null)
            return false;
        if (!(anotherObject.getClass() == getClass()))
            return false;
        UniqueId otherPacketId = (UniqueId)anotherObject;
        
        return Arrays.equals(bytes, otherPacketId.bytes);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
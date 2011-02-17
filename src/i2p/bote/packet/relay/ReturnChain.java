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

package i2p.bote.packet.relay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import net.i2p.util.Log;

/**
 * All this class can do at the moment is read bytes from a <code>ByteBuffer</code>
 * and write them back out.
 */
public class ReturnChain {
    private Log log = new Log(ReturnChain.class);
    private byte[] buffer;
    
    /** Creates an empty return chain. */
    public ReturnChain() {
        buffer = new byte[0];
    }
    
    public ReturnChain(ByteBuffer input) {
        int length = input.getShort() & 0xFFFF;
        if (length != 0)
            log.error("Length of return chain must be 0 for this protocol version!");
        buffer = new byte[length];
        input.get(buffer);
    }
    
    public void writeTo(OutputStream output) throws IOException {
        output.write(buffer.length >> 8);
        output.write(buffer.length & 0xFF);
        output.write(buffer);
    }
    
    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            writeTo(byteStream);
        } catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
}
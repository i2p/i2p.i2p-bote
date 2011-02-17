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

import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.TypeCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.i2p.util.Log;

@TypeCode('A')
public class PeerListRequest extends CommunicationPacket {
    private Log log = new Log(PeerListRequest.class);

    public PeerListRequest() {
    }

    public PeerListRequest(byte[] data) {
        super(data);
        
        int remaining = data.length - CommunicationPacket.HEADER_LENGTH;
        if (remaining > 0)
            log.debug("Peer List Request packet has " + remaining + " extra bytes.");
    }

    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            writeHeader(outputStream);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        
        return outputStream.toByteArray();
    }
}
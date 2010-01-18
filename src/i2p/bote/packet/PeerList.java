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

import i2p.bote.network.kademlia.KademliaPeer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.Log;

@TypeCode('L')
public class PeerList extends DataPacket {
    private Log log = new Log(PeerList.class);
    // TODO should be a Collection<Destination> because this class will also be used for relay peer lists
    private Collection<KademliaPeer> peers;

    public PeerList(Collection<KademliaPeer> peers) {
        this.peers = peers;
    }

    public PeerList(byte[] data) throws DataFormatException {
        super(data);
        
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        int numPeers = buffer.getShort();
        peers = new ArrayList<KademliaPeer>();
        for (int i=0; i<numPeers; i++) {
            Destination destination = new Destination();
            byte[] peerData = new byte[388];
            // read 384 bytes, leave the last 3 bytes zero
            buffer.get(peerData, 0, 384);
            
            destination.readBytes(peerData, 0);
            KademliaPeer peer = new KademliaPeer(destination, 0);
            peers.add(peer);
        }
        
        if (buffer.hasRemaining())
            log.debug("Peer List has " + buffer.remaining() + " extra bytes.");
    }
    
    public Collection<KademliaPeer> getPeers() {
        return peers;
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(arrayOutputStream);
        
        try {
            writeHeader(dataStream);
            dataStream.writeShort(peers.size());
            for (KademliaPeer peer: peers)
                // write the first 384 bytes (the two public keys)
                dataStream.write(peer.getDestination().toByteArray(), 0, 384);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        
        return arrayOutputStream.toByteArray();
    }
}
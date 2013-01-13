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

import i2p.bote.UniqueId;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.TypeCode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * Contains information about deleted DHT items, which
 * can be Email Packets or Index Packet entries.<br/>
 * Objects of this class are used locally to keep track
 * of deleted packets, and they are sent to peers in
 * response to <code>DeletionQueries</code>.
 * <p/>
 * This class is not thread-safe.
 */
@TypeCode('T')
public class DeletionInfoPacket extends DataPacket implements Iterable<DeletionRecord> {
    private Collection<DeletionRecord> entries;
    private Log log = new Log(DeletionInfoPacket.class);

    public DeletionInfoPacket() {
        entries = new ArrayList<DeletionRecord>();
    }
    
    public DeletionInfoPacket(byte[] data) {
        super(data);
        entries = new ArrayList<DeletionRecord>();
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        try {
            int numEntries = buffer.getInt();
            for (int i=0; i<numEntries; i++) {
                Hash dhtKey = readHash(buffer);
                UniqueId delAuthentication = new UniqueId(buffer);
                long storeTime = buffer.getInt() * 1000L;
                DeletionRecord entry = new DeletionRecord(dhtKey, delAuthentication, storeTime);
                entries.add(entry);
            }
        }
        catch (BufferUnderflowException e) {
            log.error("Not enough bytes in packet.", e);
        }
        
        if (buffer.hasRemaining())
            log.debug("Extra bytes in Index Packet data.");
    }
    
    /**
     * Adds an entry to the <code>DeletionInfoPacket</code>. If an entry with the same DHT key
     * exists in the packet already, nothing happens.
     * @param dhtKey
     * @param delAuthorization
     */
    public void put(Hash dhtKey, UniqueId delAuthorization) {
        if (contains(dhtKey))
            return;
        DeletionRecord entry = new DeletionRecord(dhtKey, delAuthorization);
        entries.add(entry);
    }
    
    /**
     * Tests if the <code>DeletionInfoPacket</code> contains a given DHT key.
     * @param dhtKey
     * @return <code>true</code> if the packet containes the DHT key, <code>false</code> otherwise.
     */
    public boolean contains(Hash dhtKey) {
        return getEntry(dhtKey) != null;
    }
    
    public DeletionRecord getEntry(Hash dhtKey) {
        for (DeletionRecord entry: entries)
            if (entry.dhtKey.equals(dhtKey))
                return entry;
        return null;
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        try {
            writeHeader(dataStream);
            
            dataStream.writeInt(entries.size());
            for (DeletionRecord entry: entries) {
                dataStream.write(entry.dhtKey.toByteArray());
                dataStream.write(entry.delAuthorization.toByteArray());
                dataStream.writeInt((int)(entry.storeTime/1000L));   // store as seconds
            }
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream/DataOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
    
    public boolean isEmpty() {
        return entries.isEmpty();
    }
    
    public Iterator<DeletionRecord> iterator() {
        return entries.iterator();
    }
}
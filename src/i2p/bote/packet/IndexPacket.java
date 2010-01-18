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

import i2p.bote.UniqueId;
import i2p.bote.email.EmailDestination;
import i2p.bote.packet.dht.DhtStorablePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * Stores DHT keys of Email Packets and their deletion keys.
 * 
 * This class is not thread-safe.
 */
@TypeCode('I')
public class IndexPacket extends DhtStorablePacket {
    private Log log = new Log(IndexPacket.class);
//    private Collection<Entry> entries; should probably use a Map and get rid of the Entry class
    private Map<Hash, UniqueId> entries;
    private Hash destinationHash;   // The DHT key of this packet

    /**
     * 
     * @param emailPackets One or more email packets
     * @param emailDestination Determines the DHT key of this Index Packet
     */
    public IndexPacket(Collection<EncryptedEmailPacket> emailPackets, EmailDestination emailDestination) {
//        entries = new ArrayList<Entry>();
        entries = new ConcurrentHashMap<Hash, UniqueId>();
        for (EncryptedEmailPacket emailPacket: emailPackets)
//            Entry entry = new Entry(emailPacket.getDhtKey(), emailPacket.getPlaintextDeletionKey());
            entries.put(emailPacket.getDhtKey(), emailPacket.getPlaintextDeletionKey());
        
        destinationHash = emailDestination.getHash();
    }
    
    /**
     * Merges the DHT keys of multiple index packets into one big index packet.
     * The DHT key of this packet is set to the DHT key of the first input packet.
     * @param indexPackets
     * @throws IllegalArgumentException If an empty <code>Collection</code> or <code>null</code> was passed in
     */
    public IndexPacket(Collection<IndexPacket> indexPackets) {
        if (indexPackets==null || indexPackets.isEmpty())
            throw new IllegalArgumentException("This method must be invoked with at least one index packet.");
        
        destinationHash = indexPackets.iterator().next().getDhtKey();
        entries = new ConcurrentHashMap<Hash, UniqueId>();
        for (IndexPacket packet: indexPackets)
            entries.putAll(packet.getEntries());
    }
    
    /**
     * A varargs version of {@link IndexPacket(Collection<IndexPacket>)}.
     * @param indexPackets
     */
    public IndexPacket(IndexPacket... indexPackets) {
        this(Arrays.asList(indexPackets));
    }
    
    public IndexPacket(byte[] data) {
        super(data);
        
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        destinationHash = readHash(buffer);

        int numKeys = buffer.get();
        
        entries = new ConcurrentHashMap<Hash, UniqueId>();
        for (int i=0; i<numKeys; i++) {
            Hash dhtKey = readHash(buffer);
            UniqueId delKey = new UniqueId(buffer);
            entries.put(dhtKey, delKey);
        }
        
        // TODO catch BufferUnderflowException; warn if extra bytes in the array
    }

    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            writeHeader(outputStream);
            destinationHash.writeBytes(outputStream);
            outputStream.write((byte)entries.size());
            for (Entry<Hash, UniqueId> entry: entries.entrySet()) {
                entry.getKey().writeBytes(outputStream);
                entry.getValue().writeTo(outputStream);
            }
            // TODO in the unit test, verify that toByteArray().length = Hash.NUM_BYTES + 1 + dhtKeys.size()*Hash.NUM_BYTES
        } catch (DataFormatException e) {
            log.error("Invalid format for email destination.", e);
        } catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return outputStream.toByteArray();
    }

    /**
     * Returns the deletion key for a given DHT key of an Email Packet, or <code>null</code> if
     * the <code>IndexPacket</code> doesn't contain the DHT key.
     * @param dhtKey
     * @return
     */
    public UniqueId getDeletionKey(Hash dhtKey) {
        return entries.get(dhtKey);
    }
    
    /**
     * Returns all DHT keys in this <code>IndexPacket</code>.
     * @return
     */
    public Collection<Hash> getDhtKeys() {
        return entries.keySet();
    }
    
    /**
     * Adds an entry to the <code>IndexPacket</code>. If the DHT key exists in the packet already,
     * nothing happens.
     * @param dhtKey
     * @param deletionKey
     */
    public void put(Hash dhtKey, UniqueId deletionKey) {
        entries.put(dhtKey, deletionKey);
    }
    
    /**
     * Removes an entry from the <code>IndexPacket</code>.
     * @param dhtKey
     */
    public void remove(Hash dhtKey) {
        entries.remove(dhtKey);
    }
    
    /**
     * Tests if the <code>IndexPacket</code> contains a given DHT key.
     * @param dhtKey
     * @return <code>true</code> if the packet containes the DHT key, <code>false</code> otherwise.
     */
    public boolean contains(Hash dhtKey) {
        return entries.containsKey(dhtKey);
    }
    
    /**
     * Returns the DHT key / deletion key pairs for the {@link EncryptedEmailPacket}s referenced
     * by this <code>IndexPacket</code>.
     * @return
     */
//    public Collection<Entry> getEntries() {
    public Map<Hash, UniqueId> getEntries() {
        return entries;
    }
    
    /**
     * Returns the DHT key of this packet. The DHT key of an <code>IndexPacket</code> is the
     * hash of the Email Destination whose entries this packet stores.
     */
    @Override
    public Hash getDhtKey() {
        return destinationHash;
    }

    /**
     * This class holds a DHT key and a deletion key for an email packet.
     */
/*    public class Entry {
        public Hash dhtKey;   // The DHT key of an email packet
        public UniqueId deletionKey;   // The deletion key of the email packet
        
        public Entry(Hash dhtKey, UniqueId deletionKey) {
            this.dhtKey = dhtKey;
            this.deletionKey = deletionKey;
        }
    }*/
}
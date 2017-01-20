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

import i2p.bote.email.EmailDestination;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.Splittable;
import i2p.bote.packet.TypeCode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * Contains {@link IndexPacketEntry} objects for an Email Destination.<br/>
 * Index Packets can be sent between two peers, or stored in a file. They are used when:
 * <ol>
 *   <li/>A peer sends an Index Packet to another peer in a Store Request, or
 *        responds to a Retrieve Request.<br/>
 *        Each entry contains an Email Packet key and a Delete Verification Hash.<br/>
 *        The time stamp is zero.
 *   <li/>A peer stores entries in an Index Packet file.<br/>
 *        Each entry contains a an Email Packet key, a Delete Verification Hash,
 *        and a time stamp.
 * </ol>
 * This class is not thread-safe.
 */
@TypeCode('I')
public class IndexPacket extends DhtStorablePacket implements Iterable<IndexPacketEntry>, Splittable {
    private Log log = new Log(IndexPacket.class);
    private Hash destinationHash;   // The DHT key of this packet, which is the hash of the Email Destination for which this Index Packet stores Email Packet keys
    private List<IndexPacketEntry> entries;

    /**
     * @param emailDestination Determines the DHT key of this Index Packet
     */
    public IndexPacket(EmailDestination emailDestination) {
        this(emailDestination.getHash());
    }
    
    /**
     * @param destinationHash The DHT key of this Index Packet
     */
    public IndexPacket(Hash destinationHash) {
        this.destinationHash = destinationHash;
        entries = new ArrayList<IndexPacketEntry>();
    }
    
    /**
     * Merges the DHT keys of multiple index packets into one big index packet.<br/>
     * The Email Destination of this packet is set to that of the first input packet.
     * @param indexPackets
     * @throws IllegalArgumentException If an empty <code>Collection</code> or <code>null</code> was passed in
     */
    public IndexPacket(Collection<IndexPacket> indexPackets) {
        if (indexPackets==null || indexPackets.isEmpty())
            throw new IllegalArgumentException("This method must be invoked with at least one index packet.");

        IndexPacket firstPacket = indexPackets.iterator().next();
        destinationHash = firstPacket.getDhtKey();
        
        entries = new ArrayList<IndexPacketEntry>();
        for (IndexPacket packet: indexPackets) {
            for (IndexPacketEntry entry: packet)
                put(entry);
        }
    }
    
    /**
     * A varargs version of {@link #IndexPacket(Collection)}.
     * @param indexPackets
     */
    public IndexPacket(IndexPacket... indexPackets) {
        this(Arrays.asList(indexPackets));
    }
    
    public IndexPacket(byte[] data) {
        super(data);
        entries = new ArrayList<IndexPacketEntry>();
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        try {
            destinationHash = readHash(buffer);
            int numEntries = buffer.getInt();
            for (int i=0; i<numEntries; i++) {
                Hash emailPacketKey = readHash(buffer);
                Hash delVerificationHash = readHash(buffer);
                long storeTime = buffer.getInt() * 1000L;
                IndexPacketEntry entry = new IndexPacketEntry(emailPacketKey, delVerificationHash, storeTime);
                entries.add(entry);
            }
        }
        catch (BufferUnderflowException e) {
            log.error("Not enough bytes in packet.", e);
        }
        
        if (buffer.hasRemaining())
            log.debug("Extra bytes in Index Packet data.");
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        try {
            writeHeader(dataStream);
            
            destinationHash.writeBytes(dataStream);
            dataStream.writeInt(entries.size());
            for (IndexPacketEntry entry: entries) {
                dataStream.write(entry.emailPacketKey.toByteArray());
                dataStream.write(entry.delVerificationHash.toByteArray());
                dataStream.writeInt((int)(entry.storeTime/1000L));   // store as seconds
            }
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream/DataOutputStream.", e);
        } catch (DataFormatException e) {
            log.error("Can't write destination hash to output stream: " + destinationHash, e);
        }
        return byteStream.toByteArray();
    }
    
    /**
     * Adds a new entry containing the DHT key and Delete Verification Hash of an Email Packet.<br/>
     * If an entry with the same DHT key exists already, nothing happens.
     * @param emailPacket
     */
    public void put(EncryptedEmailPacket emailPacket) {
        Hash emailPacketKey = emailPacket.getDhtKey();
        Hash delVerificationHash = emailPacket.getDeleteVerificationHash();
        IndexPacketEntry newEntry = new IndexPacketEntry(emailPacketKey, delVerificationHash);
        put(newEntry);
    }
    
    public void put(Collection<EncryptedEmailPacket> emailPackets) {
        for (EncryptedEmailPacket emailPacket: emailPackets) {
            Hash emailPacketKey = emailPacket.getDhtKey();
            Hash delVerificationHash = emailPacket.getDeleteVerificationHash();
            IndexPacketEntry entry = new IndexPacketEntry(emailPacketKey, delVerificationHash);
            put(entry);
        }
    }
    
    /**
     * Adds an entry to the <code>IndexPacket</code>. If an entry with the same DHT key
     * exists in the packet already, nothing happens.
     * @param entry
     */
    public void put(IndexPacketEntry entry) {
        if (contains(entry.emailPacketKey))
            return;
        entries.add(entry);
    }
    
    /**
     * Returns the delete verification hash for an email packet DHT key,
     * or <code>null</code> if the index packet doesn't contain the DHT key.
     * @param emailPacketKey
     */
    public Hash getDeleteVerificationHash(Hash emailPacketKey) {
        IndexPacketEntry entry = getEntry(emailPacketKey);
        if (entry == null)
            return null;
        else
            return entry.delVerificationHash;
    }
    
    /**
     * Removes an entry from the <code>IndexPacket</code> if it exists.
     * @param emailPacketKey
     */
    public void remove(Hash emailPacketKey) {
        IndexPacketEntry entry = getEntry(emailPacketKey);
        if (entry != null)
            entries.remove(entry);
    }
    
    /**
     * Tests if the <code>IndexPacket</code> contains a given DHT key.
     * @param emailPacketKey
     * @return <code>true</code> if the packet containes the DHT key, <code>false</code> otherwise.
     */
    public boolean contains(Hash emailPacketKey) {
        return getEntry(emailPacketKey) != null;
    }
    
    private IndexPacketEntry getEntry(Hash emailPacketKey) {
        for (IndexPacketEntry entry: entries)
            if (entry.emailPacketKey.equals(emailPacketKey))
                return entry;
        return null;
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
     * Returns the number of entries in this Index Packet.
     */
    public int getNumEntries() {
        return entries.size();
    }
    
    @Override
    public Iterator<IndexPacketEntry> iterator() {
        return entries.iterator();
    }

    @Override
    public Collection<? extends DataPacket> split() {
        if (isTooBig()) {
            int bytesPerEntry = entries.get(0).emailPacketKey.toByteArray().length + entries.get(0).delVerificationHash.toByteArray().length + 4;   // see toByteArray()
            List<IndexPacket> subpackets = new ArrayList<IndexPacket>();
            IndexPacket currentSubpacket = new IndexPacket(destinationHash);
            for (IndexPacketEntry entry: entries) {
                if (currentSubpacket.getSize()+bytesPerEntry > MAX_DATAGRAM_SIZE) {
                    subpackets.add(currentSubpacket);
                    currentSubpacket = new IndexPacket(destinationHash);
                }
                currentSubpacket.put(entry);
            }
            subpackets.add(currentSubpacket);
            return subpackets;
        }
        else
            return Collections.singleton(this);
    }
}
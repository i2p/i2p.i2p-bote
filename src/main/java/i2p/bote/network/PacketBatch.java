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

package i2p.bote.network;

import i2p.bote.UniqueId;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.DataPacket;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Destination;
import net.i2p.util.Log;

/**
 * This class is used for sending a number of packets to other nodes,
 * and collecting the response packets.
 */
// TODO use I2PSendQueue.sendAndWait(), get rid of PacketBatch.sentSignal, etc?
public class PacketBatch implements Iterable<PacketBatchItem> {
    private final Log log = new Log(PacketBatch.class);
    private volatile Map<UniqueId, PacketBatchItem> outgoingPackets;
    private volatile Map<Destination, DataPacket> incomingPackets;
    private CountDownLatch sentSignal;   // this field is initialized by I2PSendQueue when the batch is submitted for sending
    private CountDownLatch firstReplyReceivedSignal;

    public PacketBatch() {
        outgoingPackets = new ConcurrentHashMap<UniqueId, PacketBatchItem>();
        incomingPackets = new ConcurrentHashMap<Destination, DataPacket>();
        sentSignal = new CountDownLatch(0);
        firstReplyReceivedSignal = new CountDownLatch(1);
    }
    
    // TODO throw an exception if this method is called after the batch has been submitted to the queue
    public synchronized void putPacket(CommunicationPacket packet, Destination destination) {
        outgoingPackets.put(packet.getPacketId(), new PacketBatchItem(packet, destination));
        sentSignal = new CountDownLatch((int)sentSignal.getCount() + 1);
    }

    /**
     * Return <code>true</code> if the batch contains a packet with a given {@link UniqueId}.
     * @param packetId
     */
    public boolean contains(final UniqueId packetId) {
        return outgoingPackets.containsKey(packetId);
    }
    
    public int getPacketCount() {
        return outgoingPackets.keySet().size();
    }

    public Iterator<PacketBatchItem> iterator() {
        return outgoingPackets.values().iterator();
    }
    
    void addResponse(Destination peer, DataPacket packet) {
        incomingPackets.put(peer, packet);
        firstReplyReceivedSignal.countDown();
    }
    
    /**
     * Returns all responses received so far. If there are no responses,
     * an empty <code>Map</code> is returned.
     * @return An immutable {@link Map}
     */
    public Map<Destination, DataPacket> getResponses() {
        return Collections.unmodifiableMap(incomingPackets);
    }
    
    synchronized void initializeSentSignal() {
        sentSignal = new CountDownLatch(getPacketCount());
    }
    
    void decrementSentLatch() {
        sentSignal.countDown();
    }
    
    /**
     * Only to be called after batch has been submitted to {@link I2PSendQueue}.
     */
    public void awaitSendCompletion() throws InterruptedException {
      boolean timedOut = !sentSignal.await(5, TimeUnit.MINUTES);
        if (timedOut)
            log.warn("Batch not sent within 5 minutes!");
    }
    
    public void awaitFirstReply(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
        firstReplyReceivedSignal.await(timeout, timeoutUnit);
    }
    
    public void awaitAllResponses(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutUnit.toMillis(timeout);
        long endTime = startTime + timeoutMillis;
        
        log.debug("Waiting for responses to batch packets. Start time=" + startTime + ", end time=" + endTime);
        
        while (System.currentTimeMillis()<=endTime && incomingPackets.size()<outgoingPackets.size())
            TimeUnit.SECONDS.sleep(1);
        log.debug("Finished waiting. Time now: " + System.currentTimeMillis() + ", #incoming=" + incomingPackets.size() + ", #outgoing=" + outgoingPackets.size());
    }
}
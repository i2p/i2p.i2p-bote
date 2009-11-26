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

import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.UniqueId;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Destination;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

/**
 * This class is used for sending a number of packets to other nodes,
 * and collecting the response packets.
 */
// TODO use I2PSendQueue.sendAndWait(), get rid of PacketBatch.sentSignal, etc?
public class PacketBatch implements Iterable<PacketBatchItem> {
    private final Log log = new Log(PacketBatch.class);
    private Map<UniqueId, PacketBatchItem> outgoingPackets;
    private Set<I2PBotePacket> incomingPackets;
    private CountDownLatch sentSignal;   // this field is initialized by I2PSendQueue when the batch is submitted for sending
    private CountDownLatch firstReplyReceivedSignal;

    public PacketBatch() {
        outgoingPackets = new ConcurrentHashMap<UniqueId, PacketBatchItem>();
        incomingPackets = new ConcurrentHashSet<I2PBotePacket>();
        sentSignal = new CountDownLatch(0);
        firstReplyReceivedSignal = new CountDownLatch(1);
    }
    
    public synchronized void putPacket(CommunicationPacket packet, Destination destination) {
        outgoingPackets.put(packet.getPacketId(), new PacketBatchItem(packet, destination));
        sentSignal = new CountDownLatch((int)sentSignal.getCount() + 1);
    }

    /**
     * Return <code>true</code> if the batch contains a packet with a given {@link UniqueId}.
     * @param packetId
     * @return
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
    
/*    private void decrementConfirmedLatch() {
        initSignals();
        confirmedSignal.countDown();
    }

    boolean areAllPacketsConfirmed() {
        return confirmedSignal.getCount() == 0;
    }

    boolean isPacketConfirmed(UniqueId packetId) {
        decrementConfirmedLatch();
        return packetMap.get(packetId).isDeliveryConfirmed();
    }*/

    /**
     * Notify the <code>PacketBatch</code> that delivery confirmation has been received for
     * a packet.
     * @param packetId
     */
/*    void confirmDelivery(UniqueId packetId) {
        if (outgoingPackets.containsKey(packetId))
            outgoingPackets.get(packetId).confirmDelivery();
    }*/
    
    void addResponsePacket(I2PBotePacket packet) {
        incomingPackets.add(packet);
        firstReplyReceivedSignal.countDown();
    }
    
    public Set<I2PBotePacket> getResponsePackets() {
        return incomingPackets;
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
    
    public int getUnsentPacketCount() {
        return (int)sentSignal.getCount();
    }
    
    public void awaitFirstReply(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
        firstReplyReceivedSignal.await(timeout, timeoutUnit);
    }
}
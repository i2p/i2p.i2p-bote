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
import i2p.bote.Util;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.EmptyResponse;
import i2p.bote.packet.ResponsePacket;
import i2p.bote.packet.StatusCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.datagram.I2PDatagramMaker;
import net.i2p.data.Destination;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * All outgoing traffic to I2P-Bote peers goes through this class.<br/>
 * All packets are sent as I2P datagrams.
 * Packets are sent at a rate no greater than specified by the
 * <CODE>maxBandwidth</CODE> property.
 * <p/>
 * The packet queue is FIFO with the exception of delayed packets.<br/>
 * The packet with the highest index in the queue is always the next
 * to be sent.
 */
public class I2PSendQueue extends I2PAppThread implements PacketListener {
    private Log log = new Log(I2PSendQueue.class);
    private I2PSession i2pSession;
    private I2PDatagramMaker datagramMaker;
    private PacketQueue packetQueue;
    private Set<PacketBatch> runningBatches;
    private int maxBandwidth;

    /**
     * @param i2pSession
     * @param i2pReceiver
     */
    public I2PSendQueue(I2PSession i2pSession, I2PPacketDispatcher i2pReceiver) {
        super("I2PSendQueue");
        
        this.i2pSession = i2pSession;
        i2pReceiver.addPacketListener(this);
        packetQueue = new PacketQueue();
        runningBatches = new ConcurrentHashSet<PacketBatch>();
        datagramMaker = new I2PDatagramMaker(i2pSession);
    }

    /**
     * Queues a packet behind the last undelayed packet.
     * @param packet
     * @param destination
     * @return A <code>CountdownLatch</code> that switches to zero when the packet has been sent
     */
    public CountDownLatch send(CommunicationPacket packet, Destination destination) {
        return send(packet, destination, 0);
    }
    
    /**
     * Queues a <code>Collection</code> of packets behind the last undelayed packet.
     * @param packets
     * @param destination
     */
    public void send(Collection<? extends CommunicationPacket> packets, Destination destination) {
        for (CommunicationPacket packet: packets)
            send(packet, destination, 0);
    }
    
    /**
     * Queues a packet for sending at or after a certain time.
     * @param packet
     * @param destination
     * @param earliestSendTime
     * @return A <code>CountdownLatch</code> that switches to zero when the packet has been sent
     */
    public CountDownLatch send(CommunicationPacket packet, Destination destination, long earliestSendTime) {
        ScheduledPacket scheduledPacket = new ScheduledPacket(packet, destination, earliestSendTime);
        packetQueue.add(scheduledPacket);
        return scheduledPacket.getSentLatch();
    }

    /**
     * Sends an empty response to a {@link Destination}, with the status code "OK".
     * @param destination
     * @param requestPacketId The packet id of the packet we're responding to
     */
    public void sendResponse(Destination destination, UniqueId requestPacketId) {
        sendResponse(null, destination, StatusCode.OK, requestPacketId);
    }
    
    /**
     * Sends a Response Packet to a {@link Destination}, with the status code "OK".
     * @param packet Can be <code>null</code> for a response that only contains a status code.
     * @param destination
     * @param requestPacketId The packet id of the packet we're responding to
     */
    public void sendResponse(DataPacket packet, Destination destination, UniqueId requestPacketId) {
        sendResponse(packet, destination, StatusCode.OK, requestPacketId);
    }
    
    /**
     * Sends a Response Packet to a {@link Destination}.
     * @param packet Can be <code>null</code> for a response that only contains a status code.
     * @param destination
     * @param statusCode
     * @param requestPacketId The packet id of the packet we're responding to
     */
    public void sendResponse(DataPacket packet, Destination destination, StatusCode statusCode, UniqueId requestPacketId) {
        send(ResponsePacket.create(packet, statusCode, requestPacketId), destination);
    }
    
    /**
     * Sends a batch of packets, each to its own destination.
     * Replies to the batch's packets are received until {@link #remove(PacketBatch)} is called.
     * @param batch
     */
    public void send(PacketBatch batch) {
        log.debug("Adding a batch containing " + batch.getPacketCount() + " packets.");
        runningBatches.add(batch);
        batch.initializeSentSignal();
        for (PacketBatchItem batchItem: batch) {
            ScheduledPacket scheduledPacket = new ScheduledPacket(batchItem.getPacket(), batchItem.getDestination(), 0, batch);
            packetQueue.add(scheduledPacket);
        }
    }

    public void remove(PacketBatch batch) {
        runningBatches.remove(batch);
    }
    
    /**
     * Set the maximum outgoing bandwidth in kbits/s
     * @param maxBandwidth
     */
    public void setMaxBandwidth(int maxBandwidth) {
        this.maxBandwidth = maxBandwidth;
    }

    /**
     * Return the maximum outgoing bandwidth in kbits/s
     */
    public int getMaxBandwidth() {
        return maxBandwidth;
    }
    
    public Destination getLocalDestination() {
        return i2pSession.getMyDestination();
    }
    
    // Implementation of PacketListener
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof ResponsePacket) {
            UniqueId packetId = packet.getPacketId();
            
            for (PacketBatch batch: runningBatches)
                if (batch.contains(packetId)) {
                    DataPacket payload = ((ResponsePacket)packet).getPayload();
                    if (payload != null)
                        batch.addResponse(sender, payload);
                    else
                        batch.addResponse(sender, new EmptyResponse());
                }
        }
    }
    
    @Override
    public void run() {
        while (!Thread.interrupted())
            try {
                ScheduledPacket scheduledPacket = packetQueue.take();
                if (scheduledPacket != null) {
                    doPacketDelay(scheduledPacket);
                    send(scheduledPacket);
                }
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in I2PSendQueue loop", e);
            }
        
        log.debug("I2PSendQueue thread exiting.");
    }
    
    /**
     * Waits for an amount of time that is long enough to keep the sending rate below <code>maxBandwidth<code>,
     * and until the current time is past <code>earliestSendTime</code>.
     * @param scheduledPacket
     */
    private void doPacketDelay(ScheduledPacket scheduledPacket) throws InterruptedException {
        if (maxBandwidth > 0) {
            CommunicationPacket i2pBotePacket = scheduledPacket.data;
            int packetSizeBits = i2pBotePacket.getSize() * 8;
            int maxBWBitsPerSecond = maxBandwidth * 1024;
            long waitTimeMsecs = 1000L * packetSizeBits / maxBWBitsPerSecond;
            if (System.currentTimeMillis()+waitTimeMsecs < scheduledPacket.earliestSendTime)
                waitTimeMsecs = scheduledPacket.earliestSendTime;
            TimeUnit.MILLISECONDS.sleep(waitTimeMsecs);
        }
    }
    
    /** This method actually sends a packet via the router */
    private void send(ScheduledPacket scheduledPacket) throws InterruptedException {
        CommunicationPacket i2pBotePacket = scheduledPacket.data;
        byte[] bytes = i2pBotePacket.toByteArray();
        
        PacketBatch batch = scheduledPacket.batch;
        boolean isBatchPacket = batch != null;
        log.debug("Sending " + (isBatchPacket?"":"non-") + "batch packet: [" + i2pBotePacket + "] to " + Util.toShortenedBase32(scheduledPacket.destination));
        
        try {
            sendDatagram(bytes, scheduledPacket.destination);
            
            // set sentTime, update queue and sentLatch, fire packet listeners
            scheduledPacket.data.setSentTime(System.currentTimeMillis());
            if (isBatchPacket)
                batch.decrementSentLatch();
            scheduledPacket.decrementSentLatch();
        }
        catch (Exception exc) {
            log.error("Can't send packet.", exc);
            // pause to avoid CPU hogging if the error doesn't go away
            TimeUnit.SECONDS.sleep(1);
        }
    }
    
    private void sendDatagram(byte[] data, Destination destination) throws I2PSessionException {
        byte[] replyableDatagram = datagramMaker.makeI2PDatagram(data);
        i2pSession.sendMessage(destination, replyableDatagram, I2PSession.PROTO_DATAGRAM, I2PSession.PORT_UNSPECIFIED, I2PSession.PORT_UNSPECIFIED);
    }
    
    /**
     * Keeps packets sorted by <code>earliestSendTime</code>.
     */
    private static class PacketQueue {
        private Log log = new Log(PacketQueue.class);
        private List<ScheduledPacket> queue;
        private Comparator<ScheduledPacket> comparator;
        
        public PacketQueue() {
            queue = new ArrayList<ScheduledPacket>();
            comparator = new Comparator<ScheduledPacket>() {
                @Override
                public int compare(ScheduledPacket packet1, ScheduledPacket packet2) {
                    return new Long(packet1.earliestSendTime).compareTo(packet2.earliestSendTime);
                }
            };
        }
        
        public synchronized void add(ScheduledPacket packet) {
            if (packet.earliestSendTime > 0) {
                int index = getInsertionIndex(packet);
                queue.add(index, packet);
            }
            else
                queue.add(packet);
        }

        private int getInsertionIndex(ScheduledPacket packet) {
            int index = Collections.binarySearch(queue, packet, comparator);
            if (index < 0)
                return -(index+1);
            else {
                log.warn("Packet exists in queue already: " + packet);
                return index;
            }
        }

        public ScheduledPacket take() throws InterruptedException {
            while (queue.isEmpty() && !Thread.currentThread().isInterrupted())
                TimeUnit.SECONDS.sleep(1);
            synchronized(this) {
                if (queue.isEmpty())
                    return null;
                else
                    return queue.remove(queue.size()-1);
            }
        }
    }

    private static class ScheduledPacket {
        CommunicationPacket data;
        Destination destination;
        long earliestSendTime;
        PacketBatch batch;   // the batch this packet belongs to, or null if not part of a batch
        CountDownLatch sentSignal;
        
        public ScheduledPacket(CommunicationPacket packet, Destination destination, long earliestSendTime) {
            this(packet, destination, earliestSendTime, null);
        }

        public ScheduledPacket(CommunicationPacket packet, Destination destination, long earliestSendTime, PacketBatch batch) {
            this.data = packet;
            this.destination = destination;
            this.earliestSendTime = earliestSendTime;
            this.batch = batch;
            this.sentSignal = new CountDownLatch(1);
        }

        public void decrementSentLatch() {
            sentSignal.countDown();
        }
        
        public CountDownLatch getSentLatch() {
            return sentSignal;
        }
    }
}
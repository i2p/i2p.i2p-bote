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
import i2p.bote.packet.RelayPacket;
import i2p.bote.packet.RelayRequest;
import i2p.bote.packet.ResponsePacket;
import i2p.bote.packet.StatusCode;
import i2p.bote.service.I2PBoteThread;

import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.datagram.I2PDatagramMaker;
import net.i2p.data.Destination;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

/**
 * All outgoing I2P traffic goes through this class.
 * Packets are sent at a rate no greater than specified by the
 * <CODE>maxBandwidth</CODE> property.
 *
 * The packet queue is FIFO with the exception of delayed packets.
 * The packet with the highest index in the queue is always the next
 * to be sent.
 */
public class I2PSendQueue extends I2PBoteThread implements PacketListener {
    private Log log = new Log(I2PSendQueue.class);
    private I2PSession i2pSession;
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
    }

    /**
     * Queues a packet behind the last undelayed packet.
     * @param packet
     * @param destination
     * @return 
     */
    public CountDownLatch send(CommunicationPacket packet, Destination destination) {
        return send(packet, destination, 0);
    }
    
    /**
     * Queues a packet for sending at or after a certain time.
     * @param packet
     * @param destination
     * @param earliestSendTime
     * @return
     */
    public CountDownLatch send(CommunicationPacket packet, Destination destination, long earliestSendTime) {
        ScheduledPacket scheduledPacket = new ScheduledPacket(packet, destination, earliestSendTime);
        packetQueue.add(scheduledPacket);
        return scheduledPacket.getSentLatch();
    }

    public void sendRelayRequest(RelayPacket relayPacket, HashCash hashCash, long earliestSendTime) {
        RelayRequest relayRequest = new RelayRequest(hashCash, relayPacket);
        ScheduledPacket scheduledPacket = new ScheduledPacket(relayRequest, relayPacket.getNextDestination(), earliestSendTime);
        packetQueue.add(scheduledPacket);
    }
    
    /**
     * Sends a Response Packet to a {@link Destination}, with the status code "OK".
     * @param packet
     * @param destination
     * @param requestPacketId The packet id of the packet we're responding to
     */
    public void sendResponse(DataPacket packet, Destination destination, UniqueId requestPacketId) {
        sendResponse(packet, destination, StatusCode.OK, requestPacketId);
    }
    
    /**
     * Sends a Response Packet to a {@link Destination}.
     * @param packet
     * @param destination
     * @param statusCode
     * @param requestPacketId The packet id of the packet we're responding to
     */
    public void sendResponse(DataPacket packet, Destination destination, StatusCode statusCode, UniqueId requestPacketId) {
        send(new ResponsePacket(packet, statusCode, requestPacketId), destination);
    }
    
    /**
     * Sends a batch of packets, each to its own destination.
     * Replies to the batch's packets are received until {@link remove(PacketBatch)} is called.
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
    
    public int getQueueLength() {
        return packetQueue.size();
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
     * @return
     */
    public int getMaxBandwidth() {
        return maxBandwidth;
    }
    
    // Implementation of PacketListener
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof ResponsePacket) {
            log.debug("Response Packet received: Packet Id = " + packet.getPacketId() + " Sender = " + sender);
            
            UniqueId packetId = packet.getPacketId();
            
            for (PacketBatch batch: runningBatches)
                if (batch.contains(packetId))
                    batch.addResponse(((ResponsePacket)packet).getPayload());
        }
    }
    
    @Override
    public void run() {
        I2PDatagramMaker datagramMaker = new I2PDatagramMaker(i2pSession);
        
        while (true) {
            ScheduledPacket scheduledPacket;
            try {
                scheduledPacket = packetQueue.take();
            }
            catch (InterruptedException e) {
                log.warn("Interrupted while waiting for new packets.", e);
                break;
            }
            CommunicationPacket i2pBotePacket = scheduledPacket.data;
            
            // wait long enough so rate <= maxBandwidth;
            if (maxBandwidth > 0) {
                int packetSizeBits = i2pBotePacket.getSize() * 8;
                int maxBWBitsPerSecond = maxBandwidth * 1024;
                long waitTimeMsecs = 1000L * packetSizeBits / maxBWBitsPerSecond;
                if (System.currentTimeMillis()+waitTimeMsecs < scheduledPacket.earliestSendTime)
                    waitTimeMsecs = scheduledPacket.earliestSendTime;
                try {
                    TimeUnit.MILLISECONDS.sleep(waitTimeMsecs);
                }
                catch (InterruptedException e) {
                    log.warn("Interrupted while waiting to send packet.", e);
                }
            }
            
            PacketBatch batch = scheduledPacket.batch;
            boolean isBatchPacket = batch != null;
            log.debug("Sending " + (isBatchPacket?"":"non-") + "batch packet: [" + i2pBotePacket + "] to peer: " + scheduledPacket.destination.toBase64());
                
            byte[] replyableDatagram = datagramMaker.makeI2PDatagram(i2pBotePacket.toByteArray());
            try {
                i2pSession.sendMessage(scheduledPacket.destination, replyableDatagram);
                
                // set sentTime; update queue+cache, update countdown latch, fire packet listeners
                scheduledPacket.data.setSentTime(System.currentTimeMillis());
                packetQueue.remove(scheduledPacket);
                if (isBatchPacket)
                    batch.decrementSentLatch();
                scheduledPacket.decrementSentLatch();
                
                log.debug("Packet sent. Send queue length is now " + packetQueue.size());
                if (isBatchPacket)
                    log.debug("  Batch has " + batch.getPacketCount() + " packets total, " + batch.getUnsentPacketCount() + " waiting to be sent.");
            }
            catch (I2PSessionException sessExc) {
                log.error("Can't send packet.", sessExc);
                // pause to avoid CPU hogging if the error doesn't go away
                try {
                    TimeUnit.SECONDS.sleep(1);
                }
                catch (InterruptedException intrExc) {
                    log.error("Interrupted while sleeping after a send error.", intrExc);
                }
            }
        }
    }
    
    private class PacketQueue extends LinkedList<ScheduledPacket> {
        public void add(ScheduledPacket packet, long earliestSendTime) {
            int index = getInsertionIndex(packet, earliestSendTime);
            add(index, packet);
        }

        private int getInsertionIndex(ScheduledPacket packet, long earliestSendTime) {
            if (isEmpty())
                return 0;

            // do a linear search (binary search isn't a good fit for LinkedList)
            // TODO using foreach would be more efficient
            for (int i=0; i<size(); i++)
                if (get(i).earliestSendTime <= earliestSendTime)
                    return i;

            return size();
        }

        public synchronized ScheduledPacket take() throws InterruptedException {
            while (isEmpty())
                TimeUnit.SECONDS.sleep(1);
            return pollLast();
        }
    }

    private class ScheduledPacket implements Comparable<ScheduledPacket> {
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

        @Override
        public synchronized int compareTo(ScheduledPacket anotherPacket) {
            return new Long(earliestSendTime).compareTo(anotherPacket.earliestSendTime);
        }
        
        public void decrementSentLatch() {
            sentSignal.countDown();
        }
        
        public CountDownLatch getSentLatch() {
            return sentSignal;
        }
    }
}
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

package i2p.bote.service;

import i2p.bote.Configuration;
import i2p.bote.Util;
import i2p.bote.folder.ExpirationListener;
import i2p.bote.folder.PacketFolder;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.ResponsePacket;
import i2p.bote.packet.StatusCode;
import i2p.bote.packet.relay.RelayRequest;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Destination;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * A background thread that sends packets in the relay outbox to the I2P network.
 */
public class RelayPacketSender extends I2PAppThread implements ExpirationListener, PacketListener {
    private final Log log = new Log(RelayPacketSender.class);

    private I2PSendQueue sendQueue;
    private PacketFolder<RelayRequest> packetFolder;
    private int pause;   // the wait time, in minutes, before processing the folder again
    private RelayRequest lastSentPacket;   // last relay packet sent, or null
    private CountDownLatch confirmationReceived;   // zero if a "OK" response has been received for lastSentPacket
    
    public RelayPacketSender(I2PSendQueue sendQueue, PacketFolder<RelayRequest> packetFolder, Configuration configuration) {
        super("RelayPktSndr");
        setPriority(MIN_PRIORITY);
        this.sendQueue = sendQueue;
        this.packetFolder = packetFolder;
        pause = configuration.getRelaySendPause();
    }
    
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Iterator<RelayRequest> iterator = packetFolder.iterator();
                while (iterator.hasNext()) {
                    RelayRequest packet = iterator.next();
                    if (System.currentTimeMillis() >= packet.getSendTime()) {
                        log.debug("Sending relay packet to destination " + Util.toBase32(packet.getNextDestination()));
                        CountDownLatch sentSignal;
                        Destination nextDestination = packet.getNextDestination();
                        // synchronize access to lastSentPacket (which can be null, so synchronize on "this")
                        synchronized(this) {
                            lastSentPacket = packet;
                            confirmationReceived = new CountDownLatch(1);
                            sentSignal = sendQueue.send(lastSentPacket, nextDestination);
                        }
                        sentSignal.await();
                        
                        TimeUnit.MINUTES.sleep(2);
                        // if confirmation has been received, delete the packet
                        if (confirmationReceived.await(0, TimeUnit.SECONDS)) {
                            log.debug("Confirmation received from relay peer " + Util.toShortenedBase32(nextDestination) + ", deleting packet: " + packet);
                            iterator.remove();
                        }
                    }
                }
                
                TimeUnit.MINUTES.sleep(pause);
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in RelayPacketSender loop", e);
            }
        }
        
        log.debug("RelayPacketSender thread interrupted, exiting.");
    }
    
    /** Deletes relay packets that are still in the folder 100 days after the scheduled send time */
    @Override
    public void deleteExpired() {
        for (Iterator<RelayRequest> iterator=packetFolder.iterator(); iterator.hasNext();) {
            RelayRequest packet = iterator.next();
            if (System.currentTimeMillis() > packet.getSendTime() + EXPIRATION_TIME_MILLISECONDS)
                iterator.remove();
        }
    }
    
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        // synchronize access to lastSentPacket (which can be null, so synchronize on "this")
        synchronized(this) {
            if (lastSentPacket!=null && packet instanceof ResponsePacket) {
                ResponsePacket responsePacket = (ResponsePacket)packet;
                boolean packetIdMatches = lastSentPacket.getPacketId().equals(responsePacket.getPacketId());
                boolean statusOk = StatusCode.OK == responsePacket.getStatusCode();
                if (packetIdMatches && statusOk)
                    confirmationReceived.countDown();
            }
        }
    }
}
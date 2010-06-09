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

import i2p.bote.folder.ExpirationListener;
import i2p.bote.folder.PacketFolder;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.packet.RelayDataPacket;
import i2p.bote.packet.RelayRequest;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import net.i2p.util.Log;

/**
 * A background thread that sends packets in the relay outbox to the I2P network.
 */
public class RelayPacketSender extends I2PBoteThread implements ExpirationListener {
    private final Log log = new Log(RelayPacketSender.class);
    private static final long PAUSE = 60 * 1000;   // the wait time, in milliseconds,  before processing the folder again
    
    private I2PSendQueue sendQueue;
    private PacketFolder<RelayDataPacket> packetFolder;
    
    public RelayPacketSender(I2PSendQueue sendQueue, PacketFolder<RelayDataPacket> packetFolder) {
        super("RelayPktSndr");
        setPriority(MIN_PRIORITY);
        this.sendQueue = sendQueue;
        this.packetFolder = packetFolder;
    }
    
    @Override
    public void run() {
        while (!shutdownRequested()) {
            log.debug("Processing outgoing relay packets in directory '" + packetFolder.getStorageDirectory().getAbsolutePath() + "'");
            for (Iterator<RelayDataPacket> iterator=packetFolder.iterator(); iterator.hasNext();) {
                RelayDataPacket packet = iterator.next();
                if (System.currentTimeMillis() >= packet.getSendTime()) {
                    log.debug("Processing packet file for destination " + packet.getNextDestination().calculateHash());
                    try {
                        RelayRequest request = packet.getRequest();
                        CountDownLatch sentSignal = sendQueue.send(request, packet.getNextDestination());
                        sentSignal.await();
                        iterator.remove();   // delete the packet after it has been sent
                    } catch (InterruptedException e) {
                        log.error("Interrupting while waiting for packet to be sent.", e);
                    } catch (Exception e) {
                        log.error("Error sending packet.", e);
                    }
                }
            }
            log.debug("Done processing outgoing relay packets.");
            
            try {
                Thread.sleep(PAUSE);
            } catch (InterruptedException e) {
                log.error("RelayPacketSender received an InterruptedException.");
            }
        }
        log.info(getClass().getSimpleName() + " exiting.");
    }
    
    /** Deletes relay packets that are still in the folder 100 days after the scheduled send time */
    @Override
    public void deleteExpired() {
        for (Iterator<RelayDataPacket> iterator=packetFolder.iterator(); iterator.hasNext();) {
            RelayDataPacket packet = iterator.next();
            if (System.currentTimeMillis() > packet.getSendTime() + EXPIRATION_TIME_MILLISECONDS)
                iterator.remove();
        }
    }
}
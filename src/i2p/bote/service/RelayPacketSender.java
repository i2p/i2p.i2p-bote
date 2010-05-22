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
import i2p.bote.folder.PacketFolder;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.packet.RelayPacket;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import net.i2p.util.Log;
import net.i2p.util.RandomSource;

import com.nettgryppa.security.HashCash;

/**
 * A background thread that sends packets in the relay outbox to the I2P network.
 */
public class RelayPacketSender extends I2PBoteThread {
    private static final long PAUSE = 10 * 60 * 1000;   // the wait time, in milliseconds,  before processing the folder again
    private static final long EXPIRED_CHECK_INTERVAL = TimeUnit.DAYS.toMillis(1);   // the interval for checking expired packets, in milliseconds
    private static final int PADDED_SIZE = 16 * 1024;
    private final Log log = new Log(RelayPacketSender.class);
    
    private I2PSendQueue sendQueue;
    private PacketFolder<RelayPacket> packetStore;
    
    public RelayPacketSender(I2PSendQueue sendQueue, PacketFolder<RelayPacket> packetStore) {
        super("RelayPacketSender");
        this.sendQueue = sendQueue;
        this.packetStore = packetStore;
    }
    
    @Override
    public void run() {
        long lastExpiredCheck = 0;
        
        while (!shutdownRequested()) {
            if (System.currentTimeMillis() - lastExpiredCheck > EXPIRED_CHECK_INTERVAL) {
                lastExpiredCheck = System.currentTimeMillis();
                log.debug("Checking for expired relay packets...");
                try {
                    deleteExpiredPackets();
                } catch (Exception e) {
                    log.error("Error deleting expired packets", e);
                }
            }
            
            log.info("Processing outgoing packets in directory '" + packetStore.getStorageDirectory().getAbsolutePath() + "'");
            for (RelayPacket packet: packetStore) {
                log.info("Processing packet file for destination <" + packet.getNextDestination().calculateHash() + ">");
                try {
                    HashCash hashCash = null;   // TODO
                    long sendTime = getRandomSendTime(packet);
                    sendQueue.sendRelayRequest(packet, hashCash, sendTime);
                } catch (Exception e) {
                    log.error("Error sending packet. ", e);
                }
            }
            
            try {
                Thread.sleep(PAUSE);
            } catch (InterruptedException e) {
                log.error("RelayPacketSender received an InterruptedException.");
            }
        }
        log.info(getClass().getSimpleName() + " exiting.");
    }
    
    private long getRandomSendTime(RelayPacket packet) {
        long min = packet.getEarliestSendTime();
        long max = packet.getLatestSendTime();
        return min + RandomSource.getInstance().nextLong(max-min);
    }
    
    public void deleteExpiredPackets() throws ParseException {
        // TODO look at filename which = receive time, delete if too old
    }
}
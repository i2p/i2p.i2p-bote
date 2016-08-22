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
import i2p.bote.UniqueId;
import i2p.bote.email.Email;
import i2p.bote.email.EmailMetadata;
import i2p.bote.email.EmailMetadata.PacketInfo;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderIterator;
import i2p.bote.network.DHT;
import i2p.bote.network.NetworkStatusSource;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * Periodically sends <code>DeletionQueries</code> for sent email packets and
 * updates the email's delivery status.
 * @see EmailMetadata 
 */
public class DeliveryChecker extends I2PAppThread {
    private Log log = new Log(DeliveryChecker.class);
    private DHT dht;
    private EmailFolder sentFolder;
    private Configuration configuration;
    private NetworkStatusSource networkStatusSource;
    
    public DeliveryChecker(DHT dht, EmailFolder sentFolder, Configuration configuration, NetworkStatusSource networkStatusSource) {
        super("DeliveryChkr");
        this.dht = dht;
        this.sentFolder = sentFolder;
        this.configuration = configuration;
        this.networkStatusSource = networkStatusSource;
        setPriority(MIN_PRIORITY);
    }
    
    @Override
    public void run() {
        try {
            while (!networkStatusSource.isConnected())
                TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException e) {
            return;
        }
        
        while (!Thread.interrupted())
            try {
                try {
                    if (configuration.isDeliveryCheckEnabled()) {
                        log.debug("Processing sent emails in directory '" + sentFolder.getStorageDirectory() + "'.");
                        FolderIterator<Email> iterator = sentFolder.iterate();
                        while (iterator.hasNext()) {
                            Email email = iterator.next();
                            if (!email.getMetadata().isDelivered())
                                checkDelivery(email);
                        }
                    }
                } finally {
                    TimeUnit.MINUTES.sleep(configuration.getDeliveryCheckInterval());
                }
            } catch (InterruptedException e) {
                break;
            } catch (PasswordException e) {
                log.debug("Can't scan sent folder because password is not cached.");
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in DeliveryChecker loop", e);
            }
    }
    
    /**
     * Checks the DHT for all undelivered packets belonging to a given email.
     * @param email
     * @throws InterruptedException
     */
    private void checkDelivery(Email email) throws InterruptedException {
        EmailMetadata metadata = email.getMetadata();
        Collection<PacketInfo> packets = metadata.getUndeliveredPacketKeys();
        synchronized(sentFolder) {
            boolean updateMetadata = false;
            
            for (PacketInfo packet: packets) {
                UniqueId delAuth = dht.findDeleteAuthorizationKey(packet.dhtKey, packet.delVerificationHash);
                if (delAuth != null) {
                    metadata.setPacketDelivered(packet.dhtKey, true);
                    updateMetadata = true;
                    log.debug("Delivery of email packet with DHT key " + packet.dhtKey + " confirmed.");
                }
            }
            
            if (updateMetadata)
                try {
                    sentFolder.saveMetadata(email);
                } catch (Exception e) {
                    log.error("Can't save email metadata.", e);
                }
        }
    }
}
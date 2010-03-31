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
import i2p.bote.I2PBote;
import i2p.bote.email.Email;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.folder.Outbox;
import i2p.bote.network.DHT;
import i2p.bote.network.DhtException;
import i2p.bote.network.EmailAddressResolver;
import i2p.bote.network.PeerManager;
import i2p.bote.packet.EncryptedEmailPacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.MessagingException;

import net.i2p.I2PAppContext;
import net.i2p.client.I2PSessionException;
import net.i2p.data.DataFormatException;
import net.i2p.util.Log;

/**
 * A background thread that checks the outbox for emails and sends them to the I2P network.
 */
public class OutboxProcessor extends I2PBoteThread {
    private static final int PAUSE = 10;   // The wait time, in minutes, before processing the folder again. Can be interrupted from the outside.
    
    private Log log = new Log(OutboxProcessor.class);
    private DHT dht;
    private Outbox outbox;
    private Configuration configuration;
    private I2PAppContext appContext;
    private EmailAddressResolver emailAddressResolver;
    private Map<EmailDestination, String> statusMap;
    private CountDownLatch wakeupSignal;   // tells the thread to interrupt the current wait and resume the loop
    
    public OutboxProcessor(DHT dht, Outbox outbox, Configuration configuration, PeerManager peerManager, I2PAppContext appContext) {
        super("OutboxProcsr");
        this.dht = dht;
        this.outbox = outbox;
        this.configuration = configuration;
        this.appContext = appContext;
        statusMap = new ConcurrentHashMap<EmailDestination, String>();
        emailAddressResolver = new EmailAddressResolver();
    }
    
    @Override
    public void run() {
        while (!shutdownRequested()) {
            synchronized(this) {
                wakeupSignal = new CountDownLatch(1);
            }
            
            log.info("Processing outgoing emails in directory '" + outbox.getStorageDirectory() + "'.");
            for (Email email: outbox) {
                log.info("Processing email with message Id: '" + email.getMessageID() + "'.");
                try {
                    sendEmail(email);
                } catch (Exception e) {
                    log.error("Error sending email.", e);
                }
            }
            
            try {
                wakeupSignal.await(PAUSE, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.error("OutboxProcessor received an InterruptedException.", e);
            }
        }
        log.info(getClass().getSimpleName() + " exiting.");
    }
    
    /**
     * Tells the <code>OutboxProcessor</code> to check for new outgoing emails immediately.
     */
    public void checkForEmail() {
        wakeupSignal.countDown();
    }

    @Override
    public void requestShutdown() {
        super.requestShutdown();
        if (wakeupSignal != null)
            wakeupSignal.countDown();
    }
    
    /**
     * Sends an {@link Email} to all recipients specified in the header.
     * @param email
     * @throws IOException
     * @throws MessagingException 
     */
    private void sendEmail(Email email) throws IOException, MessagingException, I2PSessionException {
        for (Address recipient: email.getAllRecipients())
            sendToOne(recipient.toString(), email);
    }

    /**
     * Sends an {@link Email} to one recipient.
     * @param recipient
     * @param email
     * @throws I2PSessionException 
     */
    private void sendToOne(String recipient, Email email) throws I2PSessionException {
        String logSuffix = null;   // only used for logging
        try {
            logSuffix = "Recipient = '" + recipient + "' Message ID = '" + email.getMessageID() + "'";
//            EmailDestination emailDestination = emailAddressResolver.getDestination(address);
            EmailDestination emailDestination = new EmailDestination(recipient);
            String base64Dest = emailDestination.toBase64();
            EmailIdentity identity = I2PBote.getInstance().getIdentities().get(base64Dest);
            
            Collection<UnencryptedEmailPacket> emailPackets = email.createEmailPackets(identity.getPrivateSigningKey(), recipient);
            Collection<EncryptedEmailPacket> encryptedPackets = EncryptedEmailPacket.encrypt(emailPackets, emailDestination, appContext);
            for (EncryptedEmailPacket packet: encryptedPackets)
                dht.store(packet);
            dht.store(new IndexPacket(encryptedPackets, emailDestination));
            outbox.updateStatus(email, new int[] {1}, "Email sent to recipient: " + recipient);
        } catch (DataFormatException e) {
            log.error("Invalid recipient address. " + logSuffix);
            outbox.updateStatus(email, new int[] {1}, "Error trying to send email to recipient: " + recipient);
        } catch (MessagingException e) {
            log.error("Can't create email packets. " + logSuffix);
            outbox.updateStatus(email, new int[] {1}, "Error trying to send email to recipient: " + recipient);
        } catch (DhtException e) {
            log.error("Can't store email packet on the DHT. " + logSuffix);
            outbox.updateStatus(email, new int[] {1}, "Error trying to send email to recipient: " + recipient);
        }
    }

    public Map<EmailDestination, String> getStatus() {
        return statusMap;
    }
}
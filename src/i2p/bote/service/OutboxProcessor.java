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

import static i2p.bote.Util._;
import i2p.bote.I2PBote;
import i2p.bote.email.Email;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.folder.Outbox;
import i2p.bote.network.DHT;
import i2p.bote.network.DhtException;
import i2p.bote.network.NetworkStatus;
import i2p.bote.network.PeerManager;
import i2p.bote.packet.EncryptedEmailPacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.MessagingException;

import net.i2p.util.Log;

/**
 * A background thread that periodically checks the outbox for emails and sends them.
 */
public class OutboxProcessor extends I2PBoteThread {
    private static final int PAUSE = 10;   // The wait time, in minutes, before processing the folder again. Can be interrupted from the outside by calling checkForEmail().
    
    private Log log = new Log(OutboxProcessor.class);
    private DHT dht;
    private Outbox outbox;
    private CountDownLatch wakeupSignal;   // tells the thread to interrupt the current wait and resume the loop
    private List<OutboxListener> outboxListeners;
    
    public OutboxProcessor(DHT dht, Outbox outbox, PeerManager peerManager) {
        super("OutboxProcsr");
        this.dht = dht;
        this.outbox = outbox;
        outboxListeners = Collections.synchronizedList(new ArrayList<OutboxListener>());
    }
    
    @Override
    public void run() {
        // wait until DHT is ready
        CountDownLatch startSignal = dht.readySignal();
        while (!shutdownRequested()) {
            try {
                if (startSignal.await(1, TimeUnit.SECONDS))
                    break;
            } catch (InterruptedException e) {
                log.error("OutboxProcessor received an InterruptedException.", e);
            }
        }
        
        while (!shutdownRequested()) {
            synchronized(this) {
                wakeupSignal = new CountDownLatch(1);
            }
            
            if (I2PBote.getInstance().getNetworkStatus() == NetworkStatus.CONNECTED) {
                log.debug("Processing outgoing emails in directory '" + outbox.getStorageDirectory() + "'.");
                for (Email email: outbox)
                    // only send emails whose status has not been set and which have not been set to "old"
                    if (Outbox.DEFAULT_STATUS.equals(outbox.getStatus(email)) && email.isNew()) {
                        log.info("Processing email with message Id: '" + email.getMessageID() + "'.");
                        try {
                            sendEmail(email);
                            fireOutboxListeners(email);
                        }
                        catch (Exception e) {
                            log.error("Error sending email.", e);
                        }
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
     * @throws MessagingException 
     * @throws DhtException 
     * @throws GeneralSecurityException 
     */
    private void sendEmail(Email email) throws MessagingException, DhtException, GeneralSecurityException {
        Address sender = email.getSender();
        if (sender == null) {
            log.error("No sender/from field in email: " + email);
            outbox.setStatus(email, _("No sender found in email."));
            return;
        }
        String base64sender = EmailDestination.extractBase64Dest(sender.toString());
        EmailIdentity senderIdentity = I2PBote.getInstance().getIdentities().get(base64sender);
        if (senderIdentity == null) {
            log.error("No identity matches the sender/from field: " + base64sender + " in email: " + email);
            return;
        }
        
        outbox.setStatus(email, _("Sending"));
        Address[] recipients = email.getAllRecipients();
        for (int i=0; i<recipients.length; i++) {
            Address recipient = recipients[i];
            sendToOne(senderIdentity, recipient.toString(), email);
            outbox.setStatus(email, _("Sent to {0} out of {1} recipients", i+1, recipients.length));
        }
        outbox.setStatus(email, _("Sent"));
    }

    /**
     * Sends an {@link Email} to one recipient.
     * @param senderIdentity
     * @param recipient
     * @param email
     * @throws MessagingException 
     * @throws DhtException 
     * @throws GeneralSecurityException 
     */
    private void sendToOne(EmailIdentity senderIdentity, String recipient, Email email) throws MessagingException, DhtException, GeneralSecurityException {
        String logSuffix = null;   // only used for logging
        try {
            logSuffix = "Recipient = '" + recipient + "' Message ID = '" + email.getMessageID() + "'";
            log.info("Sending email: " + logSuffix);
            EmailDestination recipientDest = new EmailDestination(recipient);
            
            Collection<UnencryptedEmailPacket> emailPackets = email.createEmailPackets(senderIdentity, recipient);
            
            IndexPacket indexPacket = new IndexPacket(recipientDest);
            for (UnencryptedEmailPacket unencryptedPacket: emailPackets) {
                EncryptedEmailPacket emailPacket = new EncryptedEmailPacket(unencryptedPacket, recipientDest);
                dht.store(emailPacket);
                indexPacket.put(emailPacket);
            }
            dht.store(indexPacket);
        } catch (GeneralSecurityException e) {
            log.error("Invalid recipient address. " + logSuffix, e);
            outbox.setStatus(email, _("Invalid recipient address: {0}", recipient));
            throw e;
        } catch (MessagingException e) {
            log.error("Can't create email packets. " + logSuffix, e);
            outbox.setStatus(email, _("Error creating email packets: {0}", e.getLocalizedMessage()));
            throw e;
        } catch (DhtException e) {
            log.error("Can't store email packet on the DHT. " + logSuffix, e);
            outbox.setStatus(email, _("Error while sending email: {0}", e.getLocalizedMessage()));
            throw e;
        }
    }
    
    public void addOutboxListener(OutboxListener listener) {
        outboxListeners.add(listener);
    }
    
    /**
     * Notifies listeners that an email has been sent.
     * @param email
     */
    private void fireOutboxListeners(Email email) {
        for (OutboxListener listener: outboxListeners)
            listener.emailSent(email);
    }
}
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
import i2p.bote.email.Email;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.EmailMetadata;
import i2p.bote.email.Identities;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.FolderIterator;
import i2p.bote.folder.Outbox;
import i2p.bote.folder.Outbox.EmailStatus.Status;
import i2p.bote.folder.RelayPacketFolder;
import i2p.bote.folder.Outbox.EmailStatus;
import i2p.bote.network.DHT;
import i2p.bote.network.DhtException;
import i2p.bote.network.NetworkStatusSource;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.IndexPacket;
import i2p.bote.packet.dht.StoreRequest;
import i2p.bote.packet.dht.UnencryptedEmailPacket;
import i2p.bote.packet.relay.RelayRequest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * A background thread that periodically checks the outbox for emails and sends them.
 */
public class OutboxProcessor extends I2PAppThread {
    private Log log = new Log(OutboxProcessor.class);
    private DHT dht;
    private Outbox outbox;
    private RelayPeerManager peerManager;
    private RelayPacketFolder relayPacketFolder;
    private Identities identities;
    private Configuration configuration;
    private NetworkStatusSource networkStatusSource;
    private CountDownLatch wakeupSignal;   // tells the thread to interrupt the current wait and resume the loop
    private List<OutboxListener> outboxListeners;
    
    public OutboxProcessor(DHT dht, Outbox outbox, RelayPeerManager peerManager, RelayPacketFolder relayPacketFolder, Identities identities, Configuration configuration, NetworkStatusSource networkStatusSource) {
        super("OutboxProcsr");
        this.dht = dht;
        this.outbox = outbox;
        this.peerManager = peerManager;
        this.relayPacketFolder = relayPacketFolder;
        this.identities = identities;
        this.configuration = configuration;
        this.networkStatusSource = networkStatusSource;
        wakeupSignal = new CountDownLatch(1);
        outboxListeners = Collections.synchronizedList(new ArrayList<OutboxListener>());
    }
    
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                synchronized(this) {
                    wakeupSignal = new CountDownLatch(1);
                }
                
                if (networkStatusSource.isConnected()) {
                    log.debug("Processing outgoing emails in directory '" + outbox.getStorageDirectory() + "'.");
                    FolderIterator<Email> iterator = outbox.iterate();
                    try {
                        while (iterator.hasNext()) {
                            Email email = iterator.next();
                            log.info("Processing email with message Id: '" + email.getMessageID() + "'.");
                            email.removeSignatureFlag();   // signature flag only makes sense locally
                            try {
                                sendEmail(email);
                                fireOutboxListeners(email);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw e;
                            } catch (Exception e) {
                                log.error("Error sending email.", e);
                            }
                        }
                    }
                    catch (PasswordException e) {
                        log.debug("Can't scan outbox because a password is set and the application is locked.");
                    }
                }
                
                int pause = configuration.getOutboxCheckInterval();
                wakeupSignal.await(pause, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in OutboxProcessor loop", e);
            }
        }
        
        log.debug("OutboxProcessor thread exiting.");
    }
    
    /**
     * Tells the <code>OutboxProcessor</code> to check for new outgoing emails immediately.
     */
    public void checkForEmail() {
        wakeupSignal.countDown();
    }

    /**
     * Sends an {@link Email} to all recipients specified in the header.
     * @param email
     * @throws MessagingException 
     * @throws DhtException 
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     * @throws IOException 
     * @throws InterruptedException 
     */
    void sendEmail(Email email) throws MessagingException, DhtException, GeneralSecurityException, PasswordException, IOException, InterruptedException {
        EmailIdentity senderIdentity = null;
        if (!email.isAnonymous()) {
            String sender = email.getOneFromAddress();
            senderIdentity = identities.extractIdentity(sender);
            if (senderIdentity == null) {
                log.error("No identity matches the sender/from field: " + sender + " in email: " + email);
                outbox.setStatus(email, new EmailStatus(Status.NO_IDENTITY_MATCHES,
                        sender));
            }
        }
        
        // send to I2P-Bote recipients
        outbox.setStatus(email, new EmailStatus(Status.SENDING));
        Address[] recipients = email.getAllRecipients();
        boolean containsExternalRecipients = false;
        for (int i=0; i<recipients.length; i++) {
            Address recipient = recipients[i];
            if (isExternalAddress(recipient))
                containsExternalRecipients = true;
            else
                sendToOne(senderIdentity, recipient.toString(), email);
            outbox.setStatus(email, new EmailStatus(Status.SENT_TO,
                    i+1, recipients.length));
        }
        
        // send to external recipients if there are any
        if (containsExternalRecipients) {
            if (configuration.isGatewayEnabled()) {
                sendToOne(senderIdentity, configuration.getGatewayDestination(), email);
                outbox.setStatus(email, new EmailStatus(Status.EMAIL_SENT));
            }
            else {
                outbox.setStatus(email, new EmailStatus(Status.GATEWAY_DISABLED));
                throw new MessagingException("The email contains external addresses, but the gateway is disabled.");
            }
        }
    }

    /**
     * Sends an {@link Email} to one recipient.
     * @param senderIdentity The sender's Email Identity, or <code>null</code> for anonymous emails
     * @param recipient
     * @param email
     * @throws MessagingException 
     * @throws DhtException 
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     * @throws InterruptedException 
     */
    private void sendToOne(EmailIdentity senderIdentity, String recipient, Email email) throws MessagingException, DhtException, GeneralSecurityException, PasswordException, InterruptedException {
        String logSuffix = null;   // only used for logging
        try {
            logSuffix = "Recipient = '" + recipient + "' Message ID = '" + email.getMessageID() + "'";
            log.info("Sending email: " + logSuffix);
            EmailDestination recipientDest = new EmailDestination(recipient);

            EmailIdentity.IdentityConfig identityConfig;
            if (email.isAnonymous())
                identityConfig = configuration;
            else
                identityConfig = senderIdentity.getWrappedConfig(configuration);
            int hops = identityConfig.getNumStoreHops();
            long minDelay = identityConfig.getRelayMinDelay() * 60 * 1000;
            long maxDelay = identityConfig.getRelayMaxDelay() * 60 * 1000;
            int relayRedundancy = identityConfig.getRelayRedundancy();

            int maxPacketSize = getMaxEmailPacketSize(hops);
            Collection<UnencryptedEmailPacket> emailPackets = email.createEmailPackets(senderIdentity, identities, recipient, maxPacketSize);
            
            IndexPacket indexPacket = new IndexPacket(recipientDest);
            EmailMetadata metadata = email.getMetadata();
            for (UnencryptedEmailPacket unencryptedPacket: emailPackets) {
                EncryptedEmailPacket emailPacket = new EncryptedEmailPacket(unencryptedPacket, recipientDest);
                send(emailPacket, hops, minDelay, maxDelay, relayRedundancy);
                indexPacket.put(emailPacket);
                metadata.addPacketInfo(recipientDest, emailPacket.getDhtKey(), emailPacket.getDeleteVerificationHash());
            }
            send(indexPacket, hops, minDelay, maxDelay, relayRedundancy);
            outbox.saveMetadata(email);
        } catch (GeneralSecurityException e) {
            log.error("Invalid recipient address. " + logSuffix, e);
            outbox.setStatus(email, new EmailStatus(Status.INVALID_RECIPIENT,
                    recipient));
            throw e;
        } catch (MessagingException e) {
            log.error("Can't create email packets. " + logSuffix, e);
            outbox.setStatus(email, new EmailStatus(Status.ERROR_CREATING_PACKETS,
                    e.getLocalizedMessage()));
            throw e;
        } catch (DhtException e) {
            log.error("Can't store email packet on the DHT. " + logSuffix, e);
            outbox.setStatus(email, new EmailStatus(Status.ERROR_SENDING,
                    e.getLocalizedMessage()));
            throw e;
        } catch (IOException e) {
            log.error("Can't save metadata. " + logSuffix, e);
            outbox.setStatus(email, new EmailStatus(Status.ERROR_SAVING_METADATA,
                    e.getLocalizedMessage()));
        }
    }
    
    /**
     * Stores a packet in the DHT directly or via relay peers.
     * @param hops The number of hops, or zero to store it directly in the DHT
     * @throws DhtException 
     * @throws InterruptedException 
     */
    private void send(DhtStorablePacket dhtPacket, int hops, long minDelay, long maxDelay, int relayRedundancy) throws DhtException, InterruptedException {
        if (hops > 0) {
            StoreRequest storeRequest = new StoreRequest(dhtPacket);
            for (int i=0; i<relayRedundancy; i++) {
                // TODO don't use the same relay peer twice if there are enough peers
                RelayRequest packet = RelayRequest.create(storeRequest, peerManager, hops, minDelay, maxDelay);
                relayPacketFolder.add(packet);
            }
        }
        else
            dht.store(dhtPacket);
    }
    
    /**
     * Returns the maximum size an <code>UnencryptedEmailPacket</code> can be
     * and still fit into one I2P datagram after the packet is encrypted and
     * wrapped in relay packets.
     * @param hops The number of layers of relay packets that the email packet will be wrapped in
     * @return the maximum size in bytes
     */
    private int getMaxEmailPacketSize(int hops) {
        int maxSize = I2PBotePacket.MAX_DATAGRAM_SIZE - EncryptedEmailPacket.MAX_OVERHEAD;
        if (hops > 0)
            maxSize -= RelayRequest.getMaxOverhead(hops);
        return maxSize;
    }
    
    /** Returns <code>true</code> if a given address is a regular email address. */
    private boolean isExternalAddress(Address address) {
        try {
            String adrString = address.toString();
            new InternetAddress(adrString, true);
            // InternetAddress accepts addresses without a domain, so check that there is a '.' after the '@'
            return adrString.indexOf('@') < adrString.indexOf('.');
        } catch (AddressException e) {
            return false;
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
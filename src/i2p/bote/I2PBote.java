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

package i2p.bote;

import static i2p.bote.Util._;
import i2p.bote.addressbook.AddressBook;
import i2p.bote.email.Email;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Identities;
import i2p.bote.fileencryption.DerivedKey;
import i2p.bote.fileencryption.FileEncryptionUtil;
import i2p.bote.fileencryption.PasswordCache;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.EmailPacketFolder;
import i2p.bote.folder.IncompleteEmailFolder;
import i2p.bote.folder.IndexPacketFolder;
import i2p.bote.folder.MessageIdCache;
import i2p.bote.folder.Outbox;
import i2p.bote.folder.RelayPacketFolder;
import i2p.bote.folder.TrashFolder;
import i2p.bote.migration.Migrator;
import i2p.bote.network.BanList;
import i2p.bote.network.BannedPeer;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.NetworkStatus;
import i2p.bote.network.NetworkStatusSource;
import i2p.bote.network.RelayPacketHandler;
import i2p.bote.network.RelayPeer;
import i2p.bote.network.kademlia.KademliaDHT;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.IndexPacket;
import i2p.bote.service.EmailChecker;
import i2p.bote.service.ExpirationThread;
import i2p.bote.service.I2PBoteThread;
import i2p.bote.service.OutboxListener;
import i2p.bote.service.OutboxProcessor;
import i2p.bote.service.POP3Service;
import i2p.bote.service.RelayPacketSender;
import i2p.bote.service.RelayPeerManager;
import i2p.bote.service.SMTPService;
import i2p.bote.service.UpdateChecker;
import i2p.bote.service.seedless.SeedlessInitializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.State;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import net.i2p.I2PAppContext;
import net.i2p.I2PException;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.Log;

/**
 * This is the core class of the application. It is implemented as a singleton.
 */
public class I2PBote implements NetworkStatusSource {
    public static final int PROTOCOL_VERSION = 4;
    private static final String APP_VERSION = "0.2.5";
    private static final int STARTUP_DELAY = 3;   // the number of minutes to wait before connecting to I2P (this gives the router time to get ready)
    private static volatile I2PBote instance;
    
    private Log log = new Log(I2PBote.class);
    private I2PClient i2pClient;
    private I2PSession i2pSession;
    private I2PSocketManager socketManager;
    private Configuration configuration;
    private Identities identities;
    private AddressBook addressBook;
    private Outbox outbox;   // stores outgoing emails for all local users
    private EmailFolder inbox;   // stores incoming emails for all local users
    private EmailFolder sentFolder;
    private TrashFolder trashFolder;
    private RelayPacketFolder relayPacketFolder;   // stores email packets we're forwarding for other machines
    private IncompleteEmailFolder incompleteEmailFolder;   // stores email packets addressed to a local user
    private EmailPacketFolder emailDhtStorageFolder;   // stores email packets for other peers
    private IndexPacketFolder indexPacketDhtStorageFolder;   // stores index packets
//TODO    private PacketFolder<> addressDhtStorageFolder;   // stores email address-destination mappings
    private Collection<I2PBoteThread> backgroundThreads;
    private SMTPService smtpService;
    private POP3Service pop3Service;
    private OutboxProcessor outboxProcessor;   // reads emails stored in the outbox and sends them
    private EmailChecker emailChecker;
    private UpdateChecker updateChecker;
    private KademliaDHT dht;
    private RelayPeerManager peerManager;
    private PasswordCache passwordCache;
    private ConnectTask connectTask;

    /**
     * Constructs a new instance of <code>I2PBote</code> and initializes
     * folders and a few other things. No background threads are spawned,
     * and network connectitivy is not initialized.
     */
    private I2PBote() {
        Thread.currentThread().setName("I2PBoteMain");
        
        I2PAppContext appContext = new I2PAppContext();
        appContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                shutDown();
            }
        });
        i2pClient = I2PClientFactory.createClient();
        configuration = new Configuration();
        
        new Migrator(configuration).migrateIfNeeded();
        
        passwordCache = new PasswordCache(configuration);
        identities = new Identities(configuration.getIdentitiesFile(), passwordCache);
        addressBook = new AddressBook(configuration.getAddressBookFile(), passwordCache);
        initializeFolderAccess(passwordCache);
    }

    /**
     * Initializes objects for accessing emails and packet files on the filesystem.
     * @param passwordCache
     */
    private void initializeFolderAccess(PasswordCache passwordCache) {
        inbox = new EmailFolder(configuration.getInboxDir(), passwordCache);
        outbox = new Outbox(configuration.getOutboxDir(), passwordCache);
        sentFolder = new EmailFolder(configuration.getSentFolderDir(), passwordCache);
        trashFolder = new TrashFolder(configuration.getTrashFolderDir(), passwordCache);
        relayPacketFolder = new RelayPacketFolder(configuration.getRelayPacketDir());
        MessageIdCache messageIdCache = new MessageIdCache(configuration.getMessageIdCacheFile(), configuration.getMessageIdCacheSize());
        incompleteEmailFolder = new IncompleteEmailFolder(configuration.getIncompleteDir(), messageIdCache, inbox);
        emailDhtStorageFolder = new EmailPacketFolder(configuration.getEmailDhtStorageDir());
        indexPacketDhtStorageFolder = new IndexPacketFolder(configuration.getIndexPacketDhtStorageDir());
    }

    /**
     * Sets up a {@link I2PSession}, using the I2P destination stored on disk or creating a new I2P
     * destination if no key file exists.
     */
    private void initializeSession() {
        Properties sessionProperties = new Properties();
        sessionProperties.setProperty("inbound.nickname", "I2P-Bote");
        sessionProperties.setProperty("outbound.nickname", "I2P-Bote");
        // According to sponge, muxed depends on gzip, so leave gzip enabled
                
        // read the local destination key from the key file if it exists
        File destinationKeyFile = configuration.getDestinationKeyFile();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(destinationKeyFile);
            char[] destKeyBuffer = new char[(int)destinationKeyFile.length()];
            fileReader.read(destKeyBuffer);
            byte[] localDestinationKey = Base64.decode(new String(destKeyBuffer));
            ByteArrayInputStream inputStream = new ByteArrayInputStream(localDestinationKey);
            socketManager = I2PSocketManagerFactory.createManager(inputStream, sessionProperties);
        }
        catch (IOException e) {
            log.debug("Destination key file doesn't exist or isn't readable." + e);
        }
        finally {
            if (fileReader != null)
                try {
                    fileReader.close();
                }
                catch (IOException e) {
                    log.debug("Error closing file: <" + destinationKeyFile.getAbsolutePath() + ">" + e);
                }
        }
        
        // if the local destination key can't be read or is invalid, create a new one
        if (socketManager == null) {
            log.debug("Creating new local destination key");
            try {
                ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
                i2pClient.createDestination(arrayStream);
                byte[] localDestinationKey = arrayStream.toByteArray();
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(localDestinationKey);
                socketManager = I2PSocketManagerFactory.createManager(inputStream, sessionProperties);
                if (socketManager == null)   // null indicates an error
                    log.error("Error creating I2PSocketManagerFactory");
                    
                saveLocalDestinationKeys(destinationKeyFile, localDestinationKey);
            } catch (I2PException e) {
                log.error("Error creating local destination key.", e);
            } catch (IOException e) {
                log.error("Error writing local destination key to file.", e);
            }
        }
        
        i2pSession = socketManager.getSession();
        Destination localDestination = i2pSession.getMyDestination();
        log.info("Local destination key (base64): " + localDestination.toBase64());
        log.info("Local destination hash (base64): " + localDestination.calculateHash().toBase64());
        log.info("Local destination hash (base32): " + Util.toBase32(localDestination));
    }
    
    /**
     * Initializes daemon threads, doesn't start them yet.
     */
    private void initializeServices() {
        I2PPacketDispatcher dispatcher = new I2PPacketDispatcher(socketManager.getServerSocket());
        backgroundThreads.add(dispatcher);

        i2pSession.addMuxedSessionListener(dispatcher, I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY);
        
        backgroundThreads.add(passwordCache);
/*        smtpService = new SMTPService();
        backgroundThreads.add(smtpService);
        pop3Service = new POP3Service();
        backgroundThreads.add(pop3Service);*/
        I2PSendQueue sendQueue = new I2PSendQueue(i2pSession, socketManager, dispatcher);
        backgroundThreads.add(sendQueue);
        RelayPacketSender relayPacketSender = new RelayPacketSender(sendQueue, relayPacketFolder, configuration);   // reads packets stored in the relayPacketFolder and sends them
        backgroundThreads.add(relayPacketSender);
        
        SeedlessInitializer seedless = new SeedlessInitializer(socketManager);
        backgroundThreads.add(seedless);
        
        dht = new KademliaDHT(sendQueue, dispatcher, configuration.getDhtPeerFile(), seedless);
        backgroundThreads.add(dht);
        
        dht.setStorageHandler(EncryptedEmailPacket.class, emailDhtStorageFolder);
        dht.setStorageHandler(IndexPacket.class, indexPacketDhtStorageFolder);
//TODO        dht.setStorageHandler(AddressPacket.class, );
        
        peerManager = new RelayPeerManager(sendQueue, getLocalDestination(), configuration.getRelayPeerFile());
        backgroundThreads.add(peerManager);
        
        dispatcher.addPacketListener(emailDhtStorageFolder);
        dispatcher.addPacketListener(indexPacketDhtStorageFolder);
        dispatcher.addPacketListener(new RelayPacketHandler(relayPacketFolder, dht, sendQueue, i2pSession));
        dispatcher.addPacketListener(peerManager);
        dispatcher.addPacketListener(relayPacketSender);
        
        ExpirationThread expirationThread = new ExpirationThread();
        expirationThread.addExpirationListener(emailDhtStorageFolder);
        expirationThread.addExpirationListener(indexPacketDhtStorageFolder);
        expirationThread.addExpirationListener(relayPacketSender);
        backgroundThreads.add(expirationThread);
        
        outboxProcessor = new OutboxProcessor(dht, outbox, peerManager, relayPacketFolder, identities, configuration, this);
        outboxProcessor.addOutboxListener(new OutboxListener() {
            /** Moves sent emails to the "sent" folder */
            @Override
            public void emailSent(Email email) {
                try {
                    outbox.setNew(email, false);
                    log.debug("Moving email [" + email + "] to the \"sent\" folder.");
                    outbox.move(email, sentFolder);
                }
                catch (Exception e) {
                    log.error("Cannot move email from outbox to sent folder: " + email, e);
                }
            }
        });
        backgroundThreads.add(outboxProcessor);
        
        emailChecker = new EmailChecker(identities, configuration, incompleteEmailFolder, emailDhtStorageFolder, indexPacketDhtStorageFolder, this, sendQueue, dht, peerManager);
        backgroundThreads.add(emailChecker);
        
        updateChecker = new UpdateChecker(this, configuration);
        backgroundThreads.add(updateChecker);
    }

    /**
     * Writes private + public keys for the local destination out to a file.
     * @param keyFile
     * @param localDestinationArray
     * @throws DataFormatException
     * @throws IOException
     */
    private void saveLocalDestinationKeys(File keyFile, byte[] localDestinationArray) throws DataFormatException, IOException {
        if (keyFile.exists()) {
            File oldKeyFile = new File(keyFile.getPath() + "_backup");
            if (!keyFile.renameTo(oldKeyFile))
                log.error("Cannot rename destination key file <" + keyFile.getAbsolutePath() + "> to <" + oldKeyFile.getAbsolutePath() + ">");
        }
        else
            if (!keyFile.createNewFile())
                log.error("Cannot create destination key file: <" + keyFile.getAbsolutePath() + ">");
        
        FileWriter fileWriter = new FileWriter(keyFile);
        fileWriter.write(Base64.encode(localDestinationArray));
        fileWriter.close();
        Util.makePrivate(keyFile);
    }
    
    /**
     * Initializes network connectivity and starts background threads.<br/>
     * This is done in a separate thread so the webapp thread is not blocked
     * by this method.
     */
    public void startUp() {
        backgroundThreads = new ArrayList<I2PBoteThread>();
        connectTask = new ConnectTask();
        backgroundThreads.add(connectTask);
        connectTask.start();
    }
    
    public void shutDown() {
        stopAllServices();

        try {
            if (i2pSession != null)
                i2pSession.destroySession();
        } catch (I2PSessionException e) {
            log.error("Can't destroy I2P session.", e);
        }
        if (socketManager != null)
            socketManager.destroySocketManager();
    }

    public static I2PBote getInstance() {
        if (instance == null)
            instance = new I2PBote();
        return instance;
    }
    
    public Configuration getConfiguration() {
        return configuration;
    }
    
    public static String getAppVersion() {
        return APP_VERSION;
    }
    
    /**
     * Returns the current router console language.
     */
    public static String getLanguage() {
        String language = System.getProperty("routerconsole.lang");
        if (language != null)
            return language;
        else
            return Locale.getDefault().getLanguage();
    }
    
    public Identities getIdentities() {
        return identities;
    }
    
    public AddressBook getAddressBook() {
        return addressBook;
    }
    
    public Destination getLocalDestination() {
        if (i2pSession == null)
            return null;
        else
            return i2pSession.getMyDestination();
    }
    
    public void sendEmail(Email email) throws Exception {
        email.checkAddresses();
        
        // sign email unless sender is anonymous
        if (!email.isAnonymous()) {
            String sender = email.getSender().toString();
            EmailIdentity senderIdentity = identities.extractIdentity(sender);
            if (senderIdentity == null)
                throw new MessagingException(_("No identity matches the sender/from field: " + sender));
            email.sign(senderIdentity, identities);
        }
        
        email.setSignatureFlag();   // set the signature flag so the signature isn't reverified every time the email is loaded
        outbox.add(email);
        if (outboxProcessor != null)
            outboxProcessor.checkForEmail();
    }

    public synchronized void checkForMail() throws PasswordException, IOException, GeneralSecurityException {
        emailChecker.checkForMail();
    }

    /** Returns <code>true</code> if I2P-Bote can be updated to a newer version */
    public boolean isUpdateAvailable() {
        if (updateChecker == null)
            return false;
        else
            return updateChecker.isUpdateAvailable();
    }
    
    /**
     * @see EmailChecker#isCheckingForMail()
     */
    public synchronized boolean isCheckingForMail() {
        if (emailChecker == null)
            return false;
        else
            return emailChecker.isCheckingForMail();
    }

    /**
     * @see EmailChecker#newMailReceived()
     */
    public boolean newMailReceived() {
        if (emailChecker == null)
            return false;
        else
            return emailChecker.newMailReceived();
    }
    
    public EmailFolder getInbox() {
        return inbox;
    }
    
    public Outbox getOutbox() {
        return outbox;
    }
    
    public EmailFolder getSentFolder() {
        return sentFolder;
    }
    
    public EmailFolder getTrashFolder() {
        return trashFolder;
    }
    
    public boolean moveToTrash(EmailFolder sourceFolder, String messageId) {
        return sourceFolder.move(messageId, trashFolder);
    }
    
    /**
     * Reencrypts all encrypted files with a new password
     * @param oldPassword
     * @param newPassword
     * @param confirmNewPassword
     * @return An error message if the two new passwords don't match, <code>null</code> otherwise
     * @throws IOException 
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     */
    public String changePassword(byte[] oldPassword, byte[] newPassword, byte[] confirmNewPassword) throws IOException, GeneralSecurityException, PasswordException {
        File passwordFile = configuration.getPasswordFile();
        
        if (!FileEncryptionUtil.isPasswordCorrect(oldPassword, passwordFile))
            return _("The old password is not correct.");
        if (!Arrays.equals(newPassword, confirmNewPassword))
            return _("The new password and the confirmation password do not match.");
        
        // lock so no files are encrypted with the old password while the password is being changed
        passwordCache.lockPassword();
        try {
            passwordCache.setPassword(newPassword);
            DerivedKey newKey = passwordCache.getKey();
            identities.changePassword(oldPassword, newKey);
            addressBook.changePassword(oldPassword, newKey);
            for (EmailFolder folder: getEmailFolders())
                folder.changePassword(oldPassword, newKey);
            
            FileEncryptionUtil.writePasswordFile(passwordFile, passwordCache.getPassword(), newKey);
        }
        finally {
            passwordCache.unlockPassword();
        }
        
        return null;
    }
    
    /**
     * Tests if a password is correct and stores it in the cache if it is.
     * If the password is not correct, a <code>PasswordException</code> is thrown.
     * @param password
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws PasswordException
     */
    public void tryPassword(byte[] password) throws IOException, GeneralSecurityException, PasswordException  {
        File passwordFile = I2PBote.getInstance().getConfiguration().getPasswordFile();
        boolean correct = FileEncryptionUtil.isPasswordCorrect(password, passwordFile);
        if (correct)
            passwordCache.setPassword(password);
        else
            throw new PasswordException();
    }
    
    /** Returns <code>true</code> if the password is currently cached. */
    public boolean isPasswordInCache() {
        return passwordCache.isPasswordInCache();
    }
    
    /**
     * Returns <code>false</code> if a password is set but is not currently cached;
     * <code>true</code> otherwise.
     */
    public boolean isPasswordRequired() {
        return passwordCache.getPassword() == null;
    }
    
    /** Removes the password from the password cache. If there is no password in the cache, nothing happens. */
    public void clearPassword() {
        passwordCache.clear();
        identities.clearPasswordProtectedData();
        addressBook.clearPasswordProtectedData();
    }
    
    private Collection<EmailFolder> getEmailFolders() {
        ArrayList<EmailFolder> folders = new ArrayList<EmailFolder>();
        folders.add(outbox);
        folders.add(inbox);
        folders.add(sentFolder);
        folders.add(trashFolder);
        return folders;
    }
    
    public int getNumDhtPeers() {
        if (dht == null)
            return 0;
        else
            return dht.getNumPeers();
    }
    
    public DhtPeerStats getDhtStats() {
        if (dht == null)
            return null;
        else
            return dht.getPeerStats();
    }
    
    public Set<RelayPeer> getRelayPeers() {
        return peerManager.getAllPeers();
    }
    
    public Collection<BannedPeer> getBannedPeers() {
        return BanList.getInstance().getAll();
    }

    private void startAllServices() {
        for (I2PBoteThread thread: backgroundThreads)
            if (thread!=null && thread.getState()==State.NEW)   // the check for State.NEW is only there for ConnectTask
                thread.start();
    }

    private void stopAllServices() {
        // first ask threads nicely to shut down
        for (I2PBoteThread thread: backgroundThreads)
            if (thread != null)
                thread.requestShutdown();
        awaitShutdown(backgroundThreads, 60 * 1000);
        printRunningThreads("Threads still running after requestShutdown():");

        // interrupt all threads that are still running
        for (I2PBoteThread thread: backgroundThreads)
            if (thread!=null && thread.isAlive())
                thread.interrupt();
        awaitShutdown(backgroundThreads, 5 * 1000);
        printRunningThreads("Threads still running 5 seconds after interrupt():");
    }

    private void printRunningThreads(String caption) {
        log.debug(caption);
        for (Thread thread: backgroundThreads)
            if (thread.isAlive())
                log.debug("  " + thread.getName());
    }
    
    /**
     * Waits up to <code>timeout</code> milliseconds for a <code>Collection</code> of threads to end.
     * @param threads
     * @param timeout In milliseconds
     */
    private void awaitShutdown(Collection<I2PBoteThread> threads, long timeout) {
        long deadline = System.currentTimeMillis() + timeout;   // the time at which any background threads that are still running are interrupted

        for (I2PBoteThread thread: backgroundThreads)
            if (thread != null)
                try {
                    long remainingTime = System.currentTimeMillis() - deadline;   // the time until the original timeout
                    if (remainingTime < 0)
                        return;
                    thread.join(remainingTime);
                } catch (InterruptedException e) {
                    log.error("Interrupted while waiting for thread <" + thread.getName() + "> to exit", e);
                }
    }
    
    /**
     * Connects to the network, skipping the connect delay.<br/>
     * If the delay time has already passed, calling this method has no effect.
     */
    public void connectNow() {
        connectTask.startSignal.countDown();
    }

    @Override
    public NetworkStatus getNetworkStatus() {
        if (!connectTask.isDone())
            return connectTask.getNetworkStatus();
        else if (dht != null)
            return dht.isReady()?NetworkStatus.CONNECTED:NetworkStatus.CONNECTING;
        else
            return NetworkStatus.ERROR;
    }
    
    @Override
    public boolean isConnected() {
        return getNetworkStatus() == NetworkStatus.CONNECTED;
    }
    
    /**
     * Waits <code>STARTUP_DELAY</code> milliseconds or until <code>startSignal</code>
     * is triggered from outside this class, then sets up an I2P session and everything
     * that depends on it.
     */
    private class ConnectTask extends I2PBoteThread {
        volatile NetworkStatus status = NetworkStatus.NOT_STARTED;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(1);
        
        protected ConnectTask() {
            super("ConnectTask");
            setDaemon(true);
        }

        public NetworkStatus getNetworkStatus() {
            return status;
        }
        
        public boolean isDone() {
            try {
                return doneSignal.await(0, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }
        
        @Override
        public void requestShutdown() {
            super.requestShutdown();
            startSignal.countDown();
        }
        
        @Override
        public void doStep() {
            status = NetworkStatus.DELAY;
            try {
                startSignal.await(STARTUP_DELAY, TimeUnit.MINUTES);
                status = NetworkStatus.CONNECTING;
                initializeSession();
                initializeServices();
                startAllServices();
                doneSignal.countDown();
            } catch (Exception e) {
                status = NetworkStatus.ERROR;
                log.error("Can't initialize the application.", e);
            }
            requestShutdown();
        }
    }
}
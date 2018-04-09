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
import net.i2p.data.Hash;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;
import net.i2p.util.SecureFile;
import net.i2p.util.SecureFileOutputStream;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.State;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import i2p.bote.addressbook.AddressBook;
import i2p.bote.crypto.wordlist.WordListAnchor;
import i2p.bote.debug.DebugSupport;
import i2p.bote.email.Email;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Identities;
import i2p.bote.email.NoIdentityForSenderException;
import i2p.bote.fileencryption.DerivedKey;
import i2p.bote.fileencryption.FileEncryptionUtil;
import i2p.bote.fileencryption.PasswordCache;
import i2p.bote.fileencryption.PasswordCacheListener;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordIncorrectException;
import i2p.bote.fileencryption.PasswordMismatchException;
import i2p.bote.fileencryption.PasswordVerifier;
import i2p.bote.folder.DirectoryEntryFolder;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.EmailFolderManager;
import i2p.bote.folder.EmailPacketFolder;
import i2p.bote.folder.IncompleteEmailFolder;
import i2p.bote.folder.IndexPacketFolder;
import i2p.bote.folder.MessageIdCache;
import i2p.bote.folder.NewEmailListener;
import i2p.bote.folder.Outbox;
import i2p.bote.folder.RelayPacketFolder;
import i2p.bote.folder.TrashFolder;
import i2p.bote.migration.Migrator;
import i2p.bote.network.BanList;
import i2p.bote.network.BannedPeer;
import i2p.bote.network.DhtException;
import i2p.bote.network.DhtPeerSource;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.DhtPeerStatsRenderer;
import i2p.bote.network.DhtResults;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.NetworkStatus;
import i2p.bote.network.NetworkStatusListener;
import i2p.bote.network.NetworkStatusSource;
import i2p.bote.network.RelayPacketHandler;
import i2p.bote.network.RelayPeer;
import i2p.bote.network.kademlia.KademliaDHT;
import i2p.bote.packet.dht.Contact;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.IndexPacket;
import i2p.bote.service.ApiService;
import i2p.bote.service.DeliveryChecker;
import i2p.bote.service.EmailChecker;
import i2p.bote.service.ExpirationThread;
import i2p.bote.service.OutboxListener;
import i2p.bote.service.OutboxProcessor;
import i2p.bote.service.RelayPacketSender;
import i2p.bote.service.RelayPeerManager;
import i2p.bote.status.ChangePasswordStatus;
import i2p.bote.status.StatusListener;

/**
 * This is the core class of the application. It is implemented as a singleton.
 */
public class I2PBote implements NetworkStatusSource, EmailFolderManager, MailSender, PasswordVerifier {
    public static final int PROTOCOL_VERSION = 4;
    private static final String APP_VERSION = "0.4.7";
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
    private DirectoryEntryFolder directoryDhtFolder;   // stores entries for the distributed address directory
    private WordListAnchor wordLists;
    private Collection<I2PAppThread> backgroundThreads;
    private ApiService apiService;
    private OutboxProcessor outboxProcessor;   // reads emails stored in the outbox and sends them
    private EmailChecker emailChecker;
    private DeliveryChecker deliveryChecker;
    private KademliaDHT dht;
    private RelayPeerManager peerManager;
    private PasswordCache passwordCache;
    private Future<Void> passwordChangeResult;
    private ConnectTask connectTask;
    private DebugSupport debugSupport;
    private Collection<NetworkStatusListener> networkStatusListeners;

    /**
     * Constructs a new instance of <code>I2PBote</code> and initializes
     * folders and a few other things. No background threads are spawned,
     * and network connectitivy is not initialized.
     */
    private I2PBote() {
        Thread.currentThread().setName("I2PBoteMain");
        
        I2PAppContext appContext = I2PAppContext.getGlobalContext();
        appContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                shutDown();
            }
        });
        i2pClient = I2PClientFactory.createClient();
        configuration = new Configuration();
        
        final Migrator migrator = new Migrator(configuration, APP_VERSION);
        migrator.migrateNonPasswordedDataIfNeeded();
        
        passwordCache = new PasswordCache(configuration);
        // purge identities and addresses from memory when the password is cleared
        passwordCache.addPasswordCacheListener(new PasswordCacheListener() {
            @Override
            public void passwordProvided() {
                migrator.migratePasswordedDataIfNeeded(passwordCache);
            }
            
            @Override
            public void passwordCleared() {
                identities.clearPasswordProtectedData();
                addressBook.clearPasswordProtectedData();
            }
        });
        identities = new Identities(configuration.getIdentitiesFile(), passwordCache);
        addressBook = new AddressBook(configuration.getAddressBookFile(), passwordCache);
        initializeFolderAccess(passwordCache);
        initializeExternalThemeDir();

        try {
            Class<?> clazz = Class.forName("i2p.bote.service.ApiServiceImpl");
            Constructor<?> ctor =
                clazz.getDeclaredConstructor(Configuration.class,
                                             EmailFolderManager.class,
                                             MailSender.class,
                                             PasswordVerifier.class);
            apiService = (ApiService) ctor.newInstance(configuration, this, this, this);
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        
        debugSupport = new DebugSupport(configuration, passwordCache);
        
        wordLists = new WordListAnchor();

        networkStatusListeners = new ArrayList<NetworkStatusListener>();
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
        directoryDhtFolder = new DirectoryEntryFolder(configuration.getDirectoryEntryDhtStorageDir());
    }

    /** Creates the external themes directory if it doesn't exist */
    private void initializeExternalThemeDir() {
        File dir = configuration.getExternalThemeDir();
        if (!dir.exists() && !dir.mkdirs())
            log.error("Can't create directory: <" + dir.getAbsolutePath() + ">");
    }
    
    /**
     * Sets up a {@link I2PSession}, using the I2P destination stored on disk or creating a new I2P
     * destination if no key file exists.
     */
    private void initializeSession() throws I2PSessionException {
        Properties sessionProperties = new Properties();
        // set tunnel names
        sessionProperties.setProperty("inbound.nickname", "I2P-Bote");
        sessionProperties.setProperty("outbound.nickname", "I2P-Bote");
        sessionProperties.putAll(configuration.getI2CPOptions());
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
            socketManager = I2PSocketManagerFactory.createDisconnectedManager(inputStream, null, 0, sessionProperties);
        }
        catch (IOException e) {
            log.debug("Destination key file doesn't exist or isn't readable." + e);
        } catch (I2PSessionException e) {
            // Won't happen, inputStream != null
        } finally {
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
                socketManager = I2PSocketManagerFactory.createDisconnectedManager(inputStream, null, 0, sessionProperties);

                saveLocalDestinationKeys(destinationKeyFile, localDestinationKey);
            } catch (I2PException e) {
                log.error("Error creating local destination key.", e);
            } catch (IOException e) {
                log.error("Error writing local destination key to file.", e);
            }
        }

        i2pSession = socketManager.getSession();
        // Throws I2PSessionException if the connection fails
        i2pSession.connect();

        Destination localDestination = i2pSession.getMyDestination();
        log.info("Local destination key (base64): " + localDestination.toBase64());
        log.info("Local destination hash (base64): " + localDestination.calculateHash().toBase64());
        log.info("Local destination hash (base32): " + Util.toBase32(localDestination));
    }
    
    /**
     * Initializes daemon threads, doesn't start them yet.
     */
    private void initializeServices() {
        I2PPacketDispatcher dispatcher = new I2PPacketDispatcher();
        i2pSession.addMuxedSessionListener(dispatcher, I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY);
        
        backgroundThreads.add(passwordCache);
        I2PSendQueue sendQueue = new I2PSendQueue(i2pSession, dispatcher);
        backgroundThreads.add(sendQueue);
        RelayPacketSender relayPacketSender = new RelayPacketSender(sendQueue, relayPacketFolder, configuration);   // reads packets stored in the relayPacketFolder and sends them
        backgroundThreads.add(relayPacketSender);

        I2PAppThread seedless = null;
        try {
            Class<? extends I2PAppThread> clazz = Class.forName(
                    "i2p.bote.service.seedless.SeedlessInitializer"
                ).asSubclass(I2PAppThread.class);
            Constructor<? extends I2PAppThread> ctor =
                clazz.getDeclaredConstructor(I2PSocketManager.class);
            seedless = ctor.newInstance(socketManager);
            backgroundThreads.add(seedless);
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }

        dht = new KademliaDHT(sendQueue, dispatcher, configuration.getDhtPeerFile(), (DhtPeerSource) seedless);
        backgroundThreads.add(dht);
        
        dht.setStorageHandler(EncryptedEmailPacket.class, emailDhtStorageFolder);
        dht.setStorageHandler(IndexPacket.class, indexPacketDhtStorageFolder);
        dht.setStorageHandler(Contact.class, directoryDhtFolder);
        
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
        
        deliveryChecker = new DeliveryChecker(dht, sentFolder, configuration, this);
        backgroundThreads.add(deliveryChecker);
    }

    /**
     * Writes private + public keys for the local destination out to a file.
     * @param keyFile
     * @param localDestinationArray
     * @throws DataFormatException
     * @throws IOException
     */
    private void saveLocalDestinationKeys(File keyFile, byte[] localDestinationArray) throws DataFormatException, IOException {
        keyFile = new SecureFile(keyFile.getAbsolutePath());
        if (keyFile.exists()) {
            File oldKeyFile = new File(keyFile.getPath() + "_backup");
            if (!keyFile.renameTo(oldKeyFile))
                log.error("Cannot rename destination key file <" + keyFile.getAbsolutePath() + "> to <" + oldKeyFile.getAbsolutePath() + ">");
        }
        else
            if (!keyFile.createNewFile())
                log.error("Cannot create destination key file: <" + keyFile.getAbsolutePath() + ">");
        
        BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new SecureFileOutputStream(keyFile)));
        try {
            fileWriter.write(Base64.encode(localDestinationArray));
        }
        finally {
            fileWriter.close();
        }
    }
    
    /**
     * Initializes network connectivity and starts background threads.<br/>
     * This is done in a separate thread so the webapp thread is not blocked
     * by this method.
     */
    public void startUp() {
        backgroundThreads = new ArrayList<I2PAppThread>();
        connectTask = new ConnectTask();
        backgroundThreads.add(connectTask);
        connectTask.start();

        if (apiService != null) {
            if (configuration.isImapEnabled())
                apiService.start(ApiService.IMAP);
            if (configuration.isSmtpEnabled())
                apiService.start(ApiService.SMTP);
        }
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

        connectTask = null;
        networkStatusChanged();
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
    
    /** Publishes an email destination in the address directory. */
    public void publishDestination(String destination, byte[] picture, String text) throws PasswordException, IOException, GeneralSecurityException, DhtException, InterruptedException {
        EmailIdentity identity = identities.get(destination);
        if (identity != null) {
            identity.setPicture(picture);
            identity.setText(text);
            if (identity.getFingerprint() == null)
                identity.generateFingerprint();   // if no fingerprint exists, generate one and save it in the next step
            identities.save();
            Contact entry = new Contact(identity, identities, picture, text, identity.getFingerprint());
            dht.store(entry);
        }
    }
    
    public Contact lookupInDirectory(String name) throws InterruptedException {
        Hash key = EmailIdentity.calculateHash(name);
        if(null == dht){
            return null;
        }
        DhtResults results = dht.findOne(key, Contact.class);
        if (!results.isEmpty()) {
            DhtStorablePacket packet = results.getPackets().iterator().next();
            if (packet instanceof Contact) {
                Contact contact = (Contact)packet;
                try {
                    if (contact.verify())
                        return contact;
                } catch (GeneralSecurityException e) {
                    log.error("Can't verify Contact", e);
                }
            }
        }
        return null;
    }
    
    public String[] getWordList(String localeCode) {
        return wordLists.getWordList(localeCode);
    }
    
    /** Returns all locale codes for which a word list exists. */
    public List<String> getWordListLocales() throws UnsupportedEncodingException, IOException, URISyntaxException {
        return wordLists.getLocaleCodes();
    }
    
    public Destination getLocalDestination() {
        if (i2pSession == null)
            return null;
        else
            return i2pSession.getMyDestination();
    }
    
    public void sendEmail(Email email) throws MessagingException, PasswordException, IOException, GeneralSecurityException {
        email.checkAddresses();
        
        // sign email unless sender is anonymous
        if (!email.isAnonymous()) {
            String sender = email.getOneFromAddress();
            EmailIdentity senderIdentity = identities.extractIdentity(sender);
            if (senderIdentity == null)
                throw new NoIdentityForSenderException(sender);
            email.sign(senderIdentity, identities);
        }
        
        email.setSignatureFlag();   // set the signature flag so the signature isn't reverified every time the email is loaded
        outbox.add(email);
        if (outboxProcessor != null)
            outboxProcessor.checkForEmail();
    }

    public synchronized void checkForMail() throws PasswordException, IOException, GeneralSecurityException {
        if (emailChecker != null)
            emailChecker.checkForMail();
    }

    public synchronized void checkForMail(String key) throws PasswordException, IOException, GeneralSecurityException {
        if (emailChecker != null)
            emailChecker.checkForMail(key);
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
     * @see EmailChecker#isCheckingForMail(EmailIdentity)
     */
    public synchronized boolean isCheckingForMail(EmailIdentity identity) {
        if (emailChecker == null)
            return false;
        else
            return emailChecker.isCheckingForMail(identity);
    }

    /**
     * @see EmailChecker#getLastMailCheckTime()
     */
    public Date getLastMailCheckTime() {
        if (emailChecker == null)
            return null;
        else {
            long time = emailChecker.getLastMailCheckTime();
            return time==0 ? null : new Date(time);
        }
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
    
    public void setImapEnabled(boolean enabled) {
        configuration.setImapEnabled(enabled);
        if (apiService != null) {
            if (enabled)
                apiService.start(ApiService.IMAP);
            else
                apiService.stop(ApiService.IMAP);
        }
    }

    public void setSmtpEnabled(boolean enabled) {
        configuration.setSmtpEnabled(enabled);
        if (apiService != null) {
            if (enabled)
                apiService.start(ApiService.SMTP);
            else
                apiService.stop(ApiService.SMTP);
        }
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

    public int getNumIncompleteEmails() {
        return incompleteEmailFolder.getNumIncompleteEmails();
    }

    public void addNewEmailListener(NewEmailListener newEmailListener) {
        incompleteEmailFolder.addNewEmailListener(newEmailListener);
    }

    public void removeNewEmailListener(NewEmailListener newEmailListener) {
        incompleteEmailFolder.removeNewEmailListener(newEmailListener);
    }
    
    public boolean deleteEmail(EmailFolder folder, String messageId) {
        if (folder instanceof TrashFolder)
            return folder.delete(messageId);
        else
            return folder.move(messageId, trashFolder);
    }
    
    /**
     * Calls {@link #changePassword(byte[], byte[], byte[])} in a new thread and
     * returns a {@link Future} that throws the same exceptions the synchronous
     * variant would.
     * @param oldPassword
     * @param newPassword
     * @param confirmNewPassword
     */
    public void changePasswordAsync(final byte[] oldPassword, final byte[] newPassword, final byte[] confirmNewPassword) {
        passwordChangeResult = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException, GeneralSecurityException, PasswordException {
                changePassword(oldPassword, newPassword, confirmNewPassword);
                return null;
            }
        });
    }
    
    public void waitForPasswordChange() throws Throwable {
        if (passwordChangeResult == null)
            return;
        
        try {
            passwordChangeResult.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        } finally {
            passwordChangeResult = null;
        }
    }
    
    /**
     * Reencrypts all encrypted files with a new password
     * @param oldPassword
     * @param newPassword
     * @param confirmNewPassword
     * @throws IOException 
     * @throws GeneralSecurityException 
     * @throws PasswordException if the old password is incorrect or two new passwords don't match
     */
    public void changePassword(byte[] oldPassword, byte[] newPassword, byte[] confirmNewPassword) throws IOException, GeneralSecurityException, PasswordException {
        changePassword(oldPassword, newPassword, confirmNewPassword, new StatusListener<ChangePasswordStatus>() {
            public void updateStatus(ChangePasswordStatus status, String... args) {} // Do nothing
        });
    }

    /**
     * Reencrypts all encrypted files with a new password
     * @param oldPassword
     * @param newPassword
     * @param confirmNewPassword
     * @param lsnr A StatusListener to report progress to
     * @throws IOException 
     * @throws GeneralSecurityException 
     * @throws PasswordException if the old password is incorrect or two new passwords don't match
     */
    public void changePassword(byte[] oldPassword, byte[] newPassword, byte[] confirmNewPassword,
            StatusListener<ChangePasswordStatus> lsnr) throws IOException, GeneralSecurityException, PasswordException {
        File passwordFile = configuration.getPasswordFile();

        lsnr.updateStatus(ChangePasswordStatus.CHECKING_PASSWORD);

        if (!FileEncryptionUtil.isPasswordCorrect(oldPassword, passwordFile))
            throw new PasswordIncorrectException();
        if (!Arrays.equals(newPassword, confirmNewPassword))
            throw new PasswordMismatchException();

        // lock so no files are encrypted with the old password while the password is being changed
        synchronized(passwordCache) {
            passwordCache.setPassword(newPassword);
            DerivedKey newKey = passwordCache.getKey();

            lsnr.updateStatus(ChangePasswordStatus.RE_ENCRYPTING_IDENTITIES);
            identities.changePassword(oldPassword, newKey);

            lsnr.updateStatus(ChangePasswordStatus.RE_ENCRYPTING_ADDRESS_BOOK);
            addressBook.changePassword(oldPassword, newKey);
            for (EmailFolder folder: getEmailFolders()) {
                lsnr.updateStatus(ChangePasswordStatus.RE_ENCRYPTING_FOLDER, folder.getName());
                folder.changePassword(oldPassword, newKey);
            }

            lsnr.updateStatus(ChangePasswordStatus.UPDATING_PASSWORD_FILE);
            FileEncryptionUtil.writePasswordFile(passwordFile, passwordCache.getPassword(), newKey);
        }
    }
    
    /**
     * Tests if a password is correct and stores it in the cache if it is.
     * If the password is not correct, a <code>PasswordException</code> is thrown.
     * @param password
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws PasswordException
     */
    @Override
    public void tryPassword(byte[] password) throws IOException, GeneralSecurityException, PasswordException  {
        File passwordFile = configuration.getPasswordFile();
        boolean correct = FileEncryptionUtil.isPasswordCorrect(password, passwordFile);
        if (correct) {
            // Don't cache tried password if none is set. This check is needed
            // because IMAP doesn't support a blank password, so the user
            // inputs a random string.
            if (passwordFile.exists()) 
                passwordCache.setPassword(password);
        } else
            throw new PasswordException();
    }
    
    /** Returns <code>true</code> if the password is currently cached. */
    public boolean isPasswordInCache() {
        return passwordCache.isPasswordInCache();
    }
    
    /**
     * Returns <code>true</code> if a password is set but is not currently cached;
     * <code>false</code> otherwise.
     */
    public boolean isPasswordRequired() {
        return passwordCache.getPassword() == null;
    }
    
    /** Removes the password from the password cache. If there is no password in the cache, nothing happens. */
    public void clearPassword() {
        passwordCache.clear();
    }

    public void addPasswordCacheListener(PasswordCacheListener passwordCacheListener) {
        passwordCache.addPasswordCacheListener(passwordCacheListener);
    }

    public void removePasswordCacheListener(PasswordCacheListener passwordCacheListener) {
        passwordCache.removePasswordCacheListener(passwordCacheListener);
    }
    
    public List<File> getUndecryptableFiles() throws PasswordException, IOException, GeneralSecurityException {
        return debugSupport.getUndecryptableFiles();
    }
    
    public List<EmailFolder> getEmailFolders() {
        ArrayList<EmailFolder> folders = new ArrayList<EmailFolder>();
        folders.add(inbox);
        folders.add(outbox);
        folders.add(sentFolder);
        folders.add(trashFolder);
        return folders;
    }
    
    public DhtPeerStats getDhtStats(DhtPeerStatsRenderer renderer) {
        if (dht == null)
            return null;
        else
            return dht.getPeerStats(renderer);
    }
    
    public Set<RelayPeer> getRelayPeers() {
        if (peerManager == null)
            return new HashSet<RelayPeer>();
        return peerManager.getAllPeers();
    }
    
    public Collection<BannedPeer> getBannedPeers() {
        return BanList.getInstance().getAll();
    }

    private void startAllServices() {
        for (I2PAppThread thread: backgroundThreads)
            if (thread!=null && thread.getState()==State.NEW)   // the check for State.NEW is only there for ConnectTask
                thread.start();
    }

    private void stopAllServices() {
        if (backgroundThreads != null) {
            // interrupt all threads
            for (I2PAppThread thread: backgroundThreads)
                if (thread!=null && thread.isAlive())
                    thread.interrupt();
        }
        if (apiService != null)
            apiService.stopAll();
        if (backgroundThreads != null) {
            awaitShutdown(5 * 1000);
            printRunningThreads();
        }
    }

    private void printRunningThreads() {
        List<Thread> runningThreads = new ArrayList<Thread>();
        for (Thread thread: backgroundThreads)
            if (thread.isAlive())
                runningThreads.add(thread);
        log.debug(runningThreads.size() + " threads still running 5 seconds after interrupt()" + (runningThreads.isEmpty()?'.':':'));
        for (Thread thread: runningThreads)
            log.debug("  " + thread.getName());
        if (apiService != null)
            apiService.printRunningThreads();
    }
    
    /**
     * Waits up to <code>timeout</code> milliseconds for the background threads to end.
     * @param timeout In milliseconds
     */
    private void awaitShutdown(long timeout) {
        long deadline = System.currentTimeMillis() + timeout;   // the time at which any background threads that are still running are interrupted

        for (I2PAppThread thread: backgroundThreads)
            if (thread != null)
                try {
                    long remainingTime = deadline - System.currentTimeMillis();   // the time until the original timeout
                    if (remainingTime < 0)
                        return;
                    thread.join(remainingTime);
                } catch (InterruptedException e) {
                    log.error("Interrupted while waiting for thread <" + thread.getName() + "> to exit", e);
                    return;
                }
    }
    
    /**
     * Connects to the network, skipping the connect delay.<br/>
     * If the delay time has already passed, calling this method has no effect.
     */
    public void connectNow() {
        connectTask.startSignal.countDown();
    }

    public void networkStatusChanged() {
        synchronized (networkStatusListeners) {
            for (NetworkStatusListener nsl : networkStatusListeners)
                nsl.networkStatusChanged();
        }
    }

    @Override
    public void addNetworkStatusListener(NetworkStatusListener networkStatusListener) {
        synchronized (networkStatusListeners) {
            networkStatusListeners.add(networkStatusListener);
        }
    }

    @Override
    public void removeNetworkStatusListener(NetworkStatusListener networkStatusListener) {
        synchronized (networkStatusListeners) {
            networkStatusListeners.remove(networkStatusListener);
        }
    }

    @Override
    public NetworkStatus getNetworkStatus() {
        if (connectTask == null)
            return NetworkStatus.NOT_STARTED;
        if (!connectTask.isDone())
            return connectTask.getNetworkStatus();
        else if (dht != null)
            return dht.isReady()?NetworkStatus.CONNECTED:NetworkStatus.CONNECTING;
        else
            return NetworkStatus.ERROR;
    }
    
    @Override
    public Exception getConnectError() {
        return connectTask.getError();
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
    private class ConnectTask extends I2PAppThread {
        volatile NetworkStatus status = NetworkStatus.NOT_STARTED;
        volatile Exception error;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(1);
        
        protected ConnectTask() {
            super("ConnectTask");
            setDaemon(true);
        }

        public NetworkStatus getNetworkStatus() {
            return status;
        }
        
        public Exception getError() {
            return error;
        }
        
        public boolean isDone() {
            try {
                return doneSignal.await(0, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        @Override
        public void run() {
            status = NetworkStatus.DELAY;
            networkStatusChanged();
            try {
                startSignal.await(STARTUP_DELAY, TimeUnit.MINUTES);
                status = NetworkStatus.CONNECTING;
                networkStatusChanged();
                initializeSession();
                initializeServices();
                startAllServices();
                doneSignal.countDown();
                networkStatusChanged();
            } catch (InterruptedException e) {
                log.debug("ConnectTask interrupted, exiting");
            } catch (Exception e) {
                status = NetworkStatus.ERROR;
                networkStatusChanged();
                error = e;
                log.error("Can't initialize the application.", e);
            }
        }
    }
}

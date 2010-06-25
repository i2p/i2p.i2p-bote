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

import i2p.bote.addressbook.AddressBook;
import i2p.bote.email.Email;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Identities;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.EmailPacketFolder;
import i2p.bote.folder.IncompleteEmailFolder;
import i2p.bote.folder.IndexPacketFolder;
import i2p.bote.folder.MessageIdCache;
import i2p.bote.folder.Outbox;
import i2p.bote.folder.RelayPacketFolder;
import i2p.bote.folder.TrashFolder;
import i2p.bote.locale.Locales;
import i2p.bote.network.BanList;
import i2p.bote.network.BannedPeer;
import i2p.bote.network.CheckEmailTask;
import i2p.bote.network.DHT;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.NetworkStatus;
import i2p.bote.network.RelayPacketHandler;
import i2p.bote.network.RelayPeer;
import i2p.bote.network.kademlia.KademliaDHT;
import i2p.bote.packet.EncryptedEmailPacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.service.AutoMailCheckTask;
import i2p.bote.service.I2PBoteThread;
import i2p.bote.service.OutboxListener;
import i2p.bote.service.OutboxProcessor;
import i2p.bote.service.POP3Service;
import i2p.bote.service.ProxyRequest;
import i2p.bote.service.RelayPacketSender;
import i2p.bote.service.RelayPeerManager;
import i2p.bote.service.SMTPService;

import i2p.bote.service.SeedlessAnnounce;
import i2p.bote.service.SeedlessRequestPeers;
import i2p.bote.service.SeedlessScrapePeers;
import i2p.bote.service.SeedlessScrapeServers;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.i2p.I2PAppContext;
import net.i2p.I2PException;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.Log;

import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.Hash;
import net.i2p.router.RouterContext;

import net.i2p.router.startup.ClientAppConfig;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Random;
import net.i2p.i2ptunnel.I2PTunnel;


// Because it's not public, grrr so much for OOP!
/**
 * This is the core class of the application. It is implemented as a singleton.
 */
public class I2PBote {

    public static final int PROTOCOL_VERSION = 3;
    private static final String APP_VERSION = "0.2.4";
    private static final int STARTUP_DELAY = 3;   // the number of minutes to wait before connecting to I2P (this gives the router time to get ready)
    private static volatile I2PBote instance;
    private Log log = new Log(I2PBote.class);
    private I2PClient i2pClient;
    private I2PSession i2pSession;
    private Configuration configuration;
    private Identities identities;
    private AddressBook addressBook;
    private I2PSendQueue sendQueue;
    private Outbox outbox;   // stores outgoing emails for all local users
    private EmailFolder inbox;   // stores incoming emails for all local users
    private EmailFolder sentFolder;
    private TrashFolder trashFolder;
    private RelayPacketFolder relayPacketFolder;   // stores email packets we're forwarding for other machines
    private IncompleteEmailFolder incompleteEmailFolder;   // stores email packets addressed to a local user
    private EmailPacketFolder emailDhtStorageFolder;   // stores email packets for other peers
    private IndexPacketFolder indexPacketDhtStorageFolder;   // stores index packets
//TODO    private PacketFolder<> addressDhtStorageFolder;   // stores email address-destination mappings
    private SMTPService smtpService;
    private POP3Service pop3Service;
    private OutboxProcessor outboxProcessor;   // reads emails stored in the outbox and sends them
    private AutoMailCheckTask autoMailCheckTask;
    private ExpirationThread expirationThread;
    private RelayPacketSender relayPacketSender;   // reads packets stored in the relayPacketFolder and sends them
    private DHT dht;
    private RelayPeerManager peerManager;
    private ThreadFactory mailCheckThreadFactory;
    private ExecutorService mailCheckExecutor;
    private Collection<Future<Boolean>> pendingMailCheckTasks;
    private long lastMailCheckTime;
    private ConnectTask connectTask;
    private I2PSocketManager sockMgr;
    private long lastSeedlessAnnounce = 0;
    private long lastSeedlessRequestPeers = 0;
    private long lastSeedlessRequestServers = 0;
    private long lastSeedlessScrapePeers = 0;
    private long lastSeedlessScrapeServers = 0;
    private SeedlessAnnounce seedlessAnnounce = null;
    private SeedlessRequestPeers seedlessRequestPeers = null;
    private SeedlessScrapePeers seedlessScrapePeers = null;
    private SeedlessScrapeServers seedlessScrapeServers = null;
    private String phost = null;
    private int pport = 0;
    private String svcURL = null;
    private String cpass = null;
    private String peersReqHeader;
    private String serversLocHeader;
    private String peersLocHeader;
    private List<String> SeedlessServers = new ArrayList<String>();
    private List<String> BotePeers = new ArrayList<String>();
    private String announceString;

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

        mailCheckThreadFactory = Util.createThreadFactory("ChkMailTask", CheckEmailTask.THREAD_STACK_SIZE);

        identities = new Identities(configuration.getIdentitiesFile());
        addressBook = new AddressBook(configuration.getAddressBookFile());
        initializeFolderAccess();

        // The rest of the initialization happens in ConnectTask because it needs an I2PSession.
        // It is done in the background so we don't block the webapp thread.
        connectTask = new ConnectTask();
        connectTask.start();
    }

    /**
     * Initializes objects for accessing emails and packet files on the filesystem.
     */
    private void initializeFolderAccess() {
        inbox = new EmailFolder(configuration.getInboxDir());
        outbox = new Outbox(configuration.getOutboxDir());
        sentFolder = new EmailFolder(configuration.getSentFolderDir());
        trashFolder = new TrashFolder(configuration.getTrashFolderDir());
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
        // I think muxed depends on gzip, let's try it.
        //sessionProperties.setProperty("i2cp.gzip", String.valueOf(false));   // most of the data we send is encrypted and therefore uncompressible

        // read the local destination key from the key file if it exists
        File destinationKeyFile = configuration.getDestinationKeyFile();
        FileReader fileReader = null;
        byte[] localDestinationKey = null;
        try {
            fileReader = new FileReader(destinationKeyFile);
            char[] destKeyBuffer = new char[(int)destinationKeyFile.length()];
            fileReader.read(destKeyBuffer);
            localDestinationKey = Base64.decode(new String(destKeyBuffer));

        } catch(IOException e) {
            log.debug("Destination key file doesn't exist or isn't readable." + e);
            log.debug("Creating new local destination key");
            try {
                ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
                i2pClient.createDestination(arrayStream);
                localDestinationKey = arrayStream.toByteArray();
                saveLocalDestinationKeys(destinationKeyFile, localDestinationKey);
            } catch(I2PException ex) {
                log.error("Error creating local destination key or I2PSession.", ex);
            } catch(IOException ex) {
                log.error("Error writing local destination key to file.", ex);
            }
        } finally {
            if(fileReader != null) {
                try {
                    fileReader.close();
                } catch(IOException e) {
                    log.debug("Error closing file: <" + destinationKeyFile.getAbsolutePath() + ">" + e);
                }
            }
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(localDestinationKey);
        // i2pSession = i2pClient.createSession(inputStream, sessionProperties);
        sockMgr = I2PSocketManagerFactory.createManager(inputStream, sessionProperties);
        i2pSession = sockMgr.getSession();
        Destination localDestination = i2pSession.getMyDestination();
        log.info("Local destination key = " + localDestination.toBase64());
        log.info("Local destination hash = " + localDestination.calculateHash().toBase64());

    }

    /**
     * Initializes daemon threads, doesn't start them yet.
     */
    private void initializeServices() {
        I2PPacketDispatcher dispatcher = new I2PPacketDispatcher();
//        i2pSession.addSessionListener(dispatcher, I2PSession.PROTO_ANY, I2PSession.PORT_ANY);
        i2pSession.addMuxedSessionListener(dispatcher, I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY);

        smtpService = new SMTPService();
        pop3Service = new POP3Service();
        sendQueue = new I2PSendQueue(i2pSession, dispatcher);
        relayPacketSender = new RelayPacketSender(sendQueue, relayPacketFolder);

        dht = new KademliaDHT(sendQueue, dispatcher, configuration.getDhtPeerFile());

        dht.setStorageHandler(EncryptedEmailPacket.class, emailDhtStorageFolder);
        dht.setStorageHandler(IndexPacket.class, indexPacketDhtStorageFolder);
//TODO        dht.setStorageHandler(AddressPacket.class, );

        peerManager = new RelayPeerManager(sendQueue, getLocalDestination(), configuration.getRelayPeerFile());

        dispatcher.addPacketListener(emailDhtStorageFolder);
        dispatcher.addPacketListener(indexPacketDhtStorageFolder);
        dispatcher.addPacketListener(new RelayPacketHandler(relayPacketFolder, dht, i2pSession));
        dispatcher.addPacketListener(peerManager);

        expirationThread = new ExpirationThread();
        expirationThread.addExpirationListener(emailDhtStorageFolder);
        expirationThread.addExpirationListener(indexPacketDhtStorageFolder);
        expirationThread.addExpirationListener(relayPacketSender);

        outboxProcessor = new OutboxProcessor(dht, outbox, peerManager, relayPacketFolder, configuration);
        outboxProcessor.addOutboxListener(new OutboxListener() {

            /** Moves sent emails to the "sent" folder */
            @Override
            public void emailSent(Email email) {
                try {
                    outbox.setNew(email, false);   // this prevents OutboxProcessor from sending the email again if it can't be moved for some reason
                    log.debug("Moving email [" + email + "] to the \"sent\" folder.");
                    outbox.move(email, sentFolder);
                } catch(Exception e) {
                    log.error("Cannot move email from outbox to sent folder: " + email, e);
                }
            }
        });

        autoMailCheckTask = new AutoMailCheckTask(configuration.getMailCheckInterval());

        if(checkForSeedless()) {
            log.info("Seedless found.");

            seedlessAnnounce = new SeedlessAnnounce(180);
            seedlessRequestPeers = new SeedlessRequestPeers(60);
            seedlessScrapePeers = new SeedlessScrapePeers(10);
            seedlessScrapeServers = new SeedlessScrapeServers(10);
        } else {
            log.info("Seedless NOT found.");
        }
    }

    /**
     * Writes private + public keys for the local destination out to a file.
     * @param keyFile
     * @param localDestinationArray
     * @throws DataFormatException
     * @throws IOException
     */
    private void saveLocalDestinationKeys(File keyFile, byte[] localDestinationArray) throws DataFormatException, IOException {
        if(keyFile.exists()) {
            File oldKeyFile = new File(keyFile.getPath() + "_backup");
            if(!keyFile.renameTo(oldKeyFile)) {
                log.error("Cannot rename destination key file <" + keyFile.getAbsolutePath() + "> to <" + oldKeyFile.getAbsolutePath() + ">");
            }
        } else if(!keyFile.createNewFile()) {
            log.error("Cannot create destination key file: <" + keyFile.getAbsolutePath() + ">");
        }

        FileWriter fileWriter = new FileWriter(keyFile);
        fileWriter.write(Base64.encode(localDestinationArray));
        fileWriter.close();
    }

    public static void startUp() {
        getInstance();
    }

    public static void shutDown() {
        if(instance != null) {
            instance.stopAllServices();
        }
    }

    public static I2PBote getInstance() {
        if(instance == null) {
            instance = new I2PBote();
        }
        return instance;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public static String getAppVersion() {
        return APP_VERSION;
    }

    public Locale[] getAllLocales() {
        return Locales.ALL_LOCALES;
    }

    public Identities getIdentities() {
        return identities;
    }

    public AddressBook getAddressBook() {
        return addressBook;
    }

    public Destination getLocalDestination() {
        return i2pSession.getMyDestination();
    }

    public void sendEmail(Email email) throws Exception {
        email.checkAddresses();
        outbox.add(email);
        outboxProcessor.checkForEmail();
    }

    public synchronized void checkForMail() {
        if(!isCheckingForMail()) {
            if(identities.size() <= 0) {
                log.info("Not checking for mail because no identities are defined.");
            } else {
                log.info("Checking mail for " + identities.size() + " Email Identities...");
            }

            lastMailCheckTime = System.currentTimeMillis();
            pendingMailCheckTasks = Collections.synchronizedCollection(new ArrayList<Future<Boolean>>());
            mailCheckExecutor = Executors.newFixedThreadPool(configuration.getMaxConcurIdCheckMail(), mailCheckThreadFactory);
            for(EmailIdentity identity: identities) {
                Callable<Boolean> checkMailTask = new CheckEmailTask(identity, dht, peerManager, sendQueue, incompleteEmailFolder, emailDhtStorageFolder, indexPacketDhtStorageFolder);
                Future<Boolean> task = mailCheckExecutor.submit(checkMailTask);
                pendingMailCheckTasks.add(task);
            }
            mailCheckExecutor.shutdown();   // finish all tasks, then shut down
        } else {
            log.info("Not checking for mail because the last mail check hasn't finished.");
        }
    }

    public synchronized long getLastMailCheckTime() {
        return lastMailCheckTime;
    }

    public synchronized boolean isCheckingForMail() {
        if(mailCheckExecutor == null) {
            return false;
        }

        return !mailCheckExecutor.isTerminated();
    }

    /**
     * Returns <code>true</code> if the last call to {@link checkForMail} has completed
     * and added new mail to the inbox.
     * If this method returns <code>true</code>, subsequent calls will always return
     * <code>false</code> until {@link checkForMail} is executed again.
     * @return
     */
    public synchronized boolean newMailReceived() {
        if(pendingMailCheckTasks == null) {
            return false;
        }
        if(isCheckingForMail()) {
            return false;
        }

        try {
            for(Future<Boolean> result: pendingMailCheckTasks) {
                if(result.get(1, TimeUnit.MILLISECONDS)) {
                    pendingMailCheckTasks = null;
                    return true;
                }
            }
        } catch(Exception e) {
            log.error("Error while checking whether new mail has arrived.", e);
        }

        pendingMailCheckTasks = null;
        return false;
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

    public int getNumDhtPeers() {
        if(dht == null) {
            return 0;
        } else {
            return dht.getNumPeers();
        }
    }

    public DhtPeerStats getDhtStats() {
        if(dht == null) {
            return null;
        } else {
            return dht.getPeerStats();
        }
    }

    public Set<RelayPeer> getRelayPeers() {
        return peerManager.getAllPeers();
    }

    public Collection<BannedPeer> getBannedPeers() {
        return BanList.getInstance().getAll();
    }


    // This would be MUCH easier if it was a list of threads we could iterate! :-)
    // Also would be nice to control things in a ThreadGroup, but, meh.
    // --Sponge
    private void startAllServices() {
        peerManager.start();
        outboxProcessor.start();
        dht.start();
        relayPacketSender.start();
//        smtpService.start();
//        pop3Service.start();
        sendQueue.start();
        autoMailCheckTask.start();
        expirationThread.start();
        if(seedlessAnnounce != null) {
            seedlessAnnounce.start();
        }
        if(seedlessRequestPeers != null) {
            seedlessRequestPeers.start();
        }
        if(seedlessScrapePeers != null) {
            seedlessScrapePeers.start();
        }
        if(seedlessScrapeServers != null) {
            seedlessScrapeServers.start();
        }
    }

    // This would be MUCH easier if it was a list of threads we could iterate! :-)
    // Also would be nice to control things in a ThreadGroup, but, meh.
    // --Sponge
    private void stopAllServices() {
        if(connectTask != null) {
            connectTask.requestShutdown();
        }
        if(dht != null) {
            dht.requestShutdown();
        }
        if(outboxProcessor != null) {
            outboxProcessor.requestShutdown();
        }
        if(relayPacketSender != null) {
            relayPacketSender.requestShutdown();
        }
        if(smtpService != null) {
            smtpService.requestShutdown();
        }
        if(pop3Service != null) {
            pop3Service.requestShutdown();
        }
        if(sendQueue != null) {
            sendQueue.requestShutdown();
        }
        if(mailCheckExecutor != null) {
            mailCheckExecutor.shutdown();
        }
        if(pendingMailCheckTasks != null) {
            for(Future<Boolean> mailCheckTask: pendingMailCheckTasks) {
                mailCheckTask.cancel(false);
            }
        }
        if(autoMailCheckTask != null) {
            autoMailCheckTask.requestShutdown();
        }
        if(expirationThread != null) {
            expirationThread.requestShutdown();
        }
        if(peerManager != null) {
            peerManager.requestShutdown();
        }


        if(seedlessAnnounce != null) {
            seedlessAnnounce.requestShutdown();
        }
        if(seedlessRequestPeers != null) {
            seedlessRequestPeers.requestShutdown();
        }
        if(seedlessScrapePeers != null) {
            seedlessScrapePeers.requestShutdown();
        }
        if(seedlessScrapeServers != null) {
            seedlessScrapeServers.requestShutdown();
        }

        long deadline = System.currentTimeMillis() + 1000 * 60;   // the time at which any background threads that are still running are killed
        if(dht != null) {
            try {
                dht.awaitShutdown(deadline - System.currentTimeMillis());
            } catch(InterruptedException e) {
                log.error("Interrupted while waiting for DHT shutdown.", e);
            }
        }

        if(seedlessAnnounce != null) {
            join(seedlessAnnounce, deadline);
        }
        if(seedlessRequestPeers != null) {
            join(seedlessRequestPeers, deadline);
        }
        if(seedlessScrapePeers != null) {
            join(seedlessScrapePeers, deadline);
        }
        if(seedlessScrapeServers != null) {
            join(seedlessScrapeServers, deadline);
        }

        if(outboxProcessor != null) {
            join(outboxProcessor, deadline);
        }
        if(relayPacketSender != null) {
            join(relayPacketSender, deadline);
        }
        if(smtpService != null) {
            join(smtpService, deadline);
        }
        if(pop3Service != null) {
            join(pop3Service, deadline);
        }
        if(sendQueue != null) {
            join(sendQueue, deadline);
        }
        if(mailCheckExecutor != null) {
            mailCheckExecutor.shutdownNow();
        }
        if(autoMailCheckTask != null) {
            join(autoMailCheckTask, deadline);
        }
        long currentTime = System.currentTimeMillis();
        if(mailCheckExecutor != null && currentTime < deadline) {
            try {
                mailCheckExecutor.awaitTermination(deadline - currentTime, TimeUnit.MILLISECONDS);
            } catch(InterruptedException e) {
                log.error("Interrupted while waiting for mailCheckExecutor to exit", e);
            }
        }
        try {
            if(i2pSession != null) {
                i2pSession.destroySession();
            }
        } catch(I2PSessionException e) {
            log.error("Can't destroy I2P session.", e);
        }
        if(sockMgr != null) {
            sockMgr.destroySocketManager();
        }
    }

    private void join(Thread thread, long until) {
        if(thread == null) {
            return;
        }
        long timeout = System.currentTimeMillis() - until;
        if(timeout > 0) {
            try {
                thread.join(timeout);
            } catch(InterruptedException e) {
                log.error("Interrupted while waiting for thread <" + thread.getName() + "> to exit", e);
            }
        }
    }

    /**
     * Connects to the network, skipping the connect delay.
     * If the delay time has already passed, calling this method has no effect.
     */
    public void connectNow() {
        connectTask.startSignal.countDown();
    }

    public NetworkStatus getNetworkStatus() {
        if(!connectTask.isDone()) {
            return connectTask.getNetworkStatus();
        } else if(dht != null) {
            return dht.isReady() ? NetworkStatus.CONNECTED : NetworkStatus.CONNECTING;
        } else {
            return NetworkStatus.ERROR;
        }
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
            } catch(InterruptedException e) {
                return false;
            }
        }

        @Override
        public void requestShutdown() {
            super.requestShutdown();
            startSignal.countDown();
        }

        @Override
        public void run() {
            status = NetworkStatus.DELAY;
            try {
                startSignal.await(STARTUP_DELAY, TimeUnit.MINUTES);
                status = NetworkStatus.CONNECTING;
                initializeSession();
                initializeServices();
                startAllServices();
                doneSignal.countDown();
            } catch(Exception e) {
                status = NetworkStatus.ERROR;
                log.error("Can't initialize the application.", e);
            }
        }
    }

    // Tobad this isn't public... oh well, I steal it :-)
    static String getPassword() {
        List<RouterContext> contexts = RouterContext.listContexts();
        if(contexts != null) {
            for(int i = 0; i < contexts.size(); i++) {
                RouterContext ctx = contexts.get(i);
                String password = ctx.getProperty("consolePassword");
                if(password != null) {
                    password = password.trim();
                    if(password.length() > 0) {
                        return password;
                    }
                }
            }
            // no password in any context
            return null;
        } else {
            // no contexts?!
            return null;
        }
    }

    private Boolean checkForSeedless() {
        /*
         * Of course we can do reflection, but...
         * Reflection is powerful, but should not be used indiscriminately.
         * If it is possible to perform an operation without using reflection,
         * then it is preferable to avoid using it. The following concerns
         * should be kept in mind when accessing code via reflection.
         *
         * http://java.sun.com/docs/books/tutorial/reflect/index.html
         *
         */
        Boolean ready = false;
        String host = null;
        String port = null;
        RouterContext _context = ContextHelper.getContext(null);
        String apass = null;
        // 1: Get the console IP:port
        List<ClientAppConfig> clients = ClientAppConfig.getClientApps(_context);
        for(int cur = 0; cur < clients.size(); cur++) {
            ClientAppConfig ca = clients.get(cur);

            if("net.i2p.router.web.RouterConsoleRunner".equals(ca.className)) {
                port = ca.args.split(" ")[0];
                host = ca.args.split(" ")[1];
                if(host.contains(",")) {
                    String checks[] = host.split(",");
                    host = null;
                    for(int h = 0; h < checks.length; h++) {
                        if(!checks[h].contains(":")) {
                            host = checks[h];
                        }
                    }

                }
            }
        }
        if(port == null || host == null) {
            log.error("checkForSeedless, no router console found!");
            return false;
        }
        // 2: Get console password
        apass = getPassword();
        log.info("Testing Seedless API");
        // 3: Check for the console API, if it exists, wait 'till it's status is ready.
        // and set the needed settings. Repeat test 10 times with some delay between when it fails.
        String url = "http://" + host + ":" + port + "/SeedlessConsole/";
        String svcurl = url + "Service";
        int tries = 10;
        BufferedReader in;
        HttpURLConnection h;
        int i;
        while(tries > 0) {
            try {
                ProxyRequest proxy = new ProxyRequest();
                h = proxy.doURLRequest(url, null, null, -1, "admin", apass);
                if(h != null) {
                    i = h.getResponseCode();
                    if(i == 200) {
                        log.info("Seedless, API says OK");
                        break;
                    }
                }

            } catch(IOException ex) {
            }

            tries--;
        }
        if(tries > 0) {
            // Now wait for it to be ready.
            // but not forever!
            log.info("Waiting for Seedless to become ready...");
            tries = 60; // ~2 minutes.
            String foo;
            while(!ready && tries > 0) {
                tries--;
                try {
                    ProxyRequest proxy = new ProxyRequest();
                    h = proxy.doURLRequest(svcurl, "stat ping!", null, -1, "admin", apass);
                    if(h != null) {
                        i = h.getResponseCode();
                        if(i == 200) {
                            foo = h.getHeaderField("X-Seedless");
                            ready = Boolean.parseBoolean(foo);
                        }
                    }

                } catch(IOException ex) {
                }
                if(!ready) {
                    try {
                        Thread.sleep(2000); // sleep for 2 seconds
                    } catch(InterruptedException ex) {
                        return false;
                    }
                }

            }
        }
        if(ready) {
            svcURL = svcurl;
            cpass = apass;
            peersReqHeader = "scan " + Base64.encode("i2p-bote X" + PROTOCOL_VERSION + "X");
            peersLocHeader = "locate " + Base64.encode("i2p-bote X" + PROTOCOL_VERSION + "X");
            serversLocHeader = "locate " + Base64.encode("seedless i2p-bote");
            announceString = "GET /Seedless/seedless HTTP/1.0\r\nX-Seedless: announce " + Base64.encode("i2p-bote X" + PROTOCOL_VERSION + "X") + "\r\n\r\n";
        }
        return ready;
    }

    public synchronized long getlastSeedlessAnnounce() {
        return lastSeedlessAnnounce;
    }

    public synchronized void doSeedlessAnnounce() {
        if(SeedlessServers.isEmpty()) {
            // try again in a minute.
            log.error("SeedlessServers.isEmpty, will retry shortly.");
            lastSeedlessAnnounce = System.currentTimeMillis() - (seedlessAnnounce.getInterval() - TimeUnit.MINUTES.toMillis(1));
            return;
        }
        // Announce to 10 servers.
        // We do this over the i2pSocket.
        int successful = Math.min(10, SeedlessServers.size());
        log.debug("Try to announce to " + successful + " Seedless Servers");
        Collections.shuffle(SeedlessServers, new Random());
        Iterator it = SeedlessServers.iterator();
        String line;
        I2PSocket I2P;
        InputStream Iin;
        OutputStream Iout;
        BufferedReader data;
        Boolean didsomething = false;
        BufferedWriter output;
        while(successful > 0 && it.hasNext()) {
            lastSeedlessAnnounce = System.currentTimeMillis();
            String b32 = (String)it.next();
            Destination dest = null;
            I2P = null;
            try {
                lastSeedlessAnnounce = System.currentTimeMillis();
                dest = I2PTunnel.destFromName(b32);
                lastSeedlessAnnounce = System.currentTimeMillis();
                line = dest.toBase64();
                dest = new Destination();
                dest.fromBase64(line);
                I2P = sockMgr.connect(dest);
                // I2P.setReadTimeout(0); // temp bugfix, this *SHOULD* be the default
                // make readers/writers
                Iin = I2P.getInputStream();
                Iout = I2P.getOutputStream();
                output = new BufferedWriter(new OutputStreamWriter(Iout));
                output.write(announceString);
                output.flush();
                data = new BufferedReader(new InputStreamReader(Iin));
                // Check for success.
                line = data.readLine();
                if(line != null) {
                    if(line.contains(" 200 ")) {
                        log.debug("Announced to " + b32);
                        successful--;
                        didsomething = true;
                    } else {
                        log.debug("Announce to " + b32 + " Failed with Error " + line);
                        log.debug("We sent " + announceString);
                    }
                }
                while((line = data.readLine()) != null) {
                }

            } catch(DataFormatException ex) {
                // not base64!
                log.debug("DataFormatException");
            } catch(ConnectException ex) {
                log.debug("ConnectException");
            } catch(NoRouteToHostException ex) {
                log.debug("NoRouteToHostException");
            } catch(InterruptedIOException ex) {
                log.debug("InterruptedIOException");
            } catch(IOException ex) {
                log.debug("IOException" + ex.toString());
                ex.printStackTrace();
            } catch(I2PException ex) {
                log.debug("I2PException");
            } catch(NullPointerException npe) {
                // Could not find the destination!
                log.debug("NullPointerException");
            }
            if(I2P != null) {
                try {
                    I2P.close();
                } catch(IOException ex) {
                    // don't care.
                }
            }
        }
        if(!didsomething) {
            // try again in 1 minute.
            lastSeedlessAnnounce = System.currentTimeMillis() - (seedlessAnnounce.getInterval() - TimeUnit.MINUTES.toMillis(1));
            return;
        }

        lastSeedlessAnnounce = System.currentTimeMillis();
    }

    public synchronized long getlastSeedlessRequestPeers() {
        return lastSeedlessRequestPeers;
    }

    public synchronized void doSeedlessRequestPeers() {
        if(getNetworkStatus().equals(NetworkStatus.CONNECTED)) {
            // We are connected, let kad do it's thing.
            lastSeedlessRequestPeers = System.currentTimeMillis() - (seedlessRequestPeers.getInterval() - TimeUnit.MINUTES.toMillis(1));
            return;
        }
        HttpURLConnection h;
        int i;
        String foo;
        log.debug("doSeedlessRequestPeers");
        try {
            ProxyRequest proxy = new ProxyRequest();
            h = proxy.doURLRequest(svcURL, peersReqHeader, null, -1, "admin", cpass);
            if(h != null) {
                i = h.getResponseCode();
            }

        } catch(IOException ex) {
        }
        log.debug("doSeedlessRequestPeers Done.");
        lastSeedlessRequestPeers = System.currentTimeMillis();
    }

    public synchronized long getlastSeedlessScrapePeers() {
        return lastSeedlessScrapePeers;
    }

    public synchronized void doSeedlessScrapePeers() {
        if(getNetworkStatus().equals(NetworkStatus.CONNECTED)) {
            // We are connected, let kad do it's thing.
            lastSeedlessScrapePeers = System.currentTimeMillis() - (seedlessScrapePeers.getInterval() - TimeUnit.MINUTES.toMillis(1));
            return;
        }
        HttpURLConnection h;
        int i;
        String foo;
        List<String> metadatas = new ArrayList<String>();
        List<String> ip32s = new ArrayList<String>();
        InputStream in;
        BufferedReader data;
        String line;
        String ip32;
        log.debug("doSeedlessScrapePeers");

        try {
            ProxyRequest proxy = new ProxyRequest();
            h = proxy.doURLRequest(svcURL, peersLocHeader, null, -1, "admin", cpass);
            if(h != null) {
                i = h.getResponseCode();
                if(i == 200) {
                    in = h.getInputStream();
                    data = new BufferedReader(new InputStreamReader(in));
                    while((line = data.readLine()) != null) {
                        metadatas.add(line);
                    }
                    Iterator it = metadatas.iterator();
                    while(it.hasNext()) {
                        foo = (String)it.next();
                        ip32 = Base64.decodeToString(foo).split(" ")[0].trim();
                        if(!ip32s.contains(ip32)) {
                            ip32s.add(ip32);
                        }
                    }
                }
            }

        } catch(IOException ex) {
        }
        BotePeers = ip32s;
        log.debug("doSeedlessScrapePeers Done.");
        BotePeers = dht.injectPeers(BotePeers);
        peerManager.injectPeers(BotePeers);
        BotePeers = null; // garbage now.
        lastSeedlessScrapePeers = System.currentTimeMillis();
    }

    public synchronized long getlastSeedlessScrapeServers() {
        return lastSeedlessScrapeServers;
    }

    public synchronized void doSeedlessScrapeServers() {
        HttpURLConnection h;
        int i;
        String foo;
        List<String> metadatas = new ArrayList<String>();
        List<String> ip32s = new ArrayList<String>();
        InputStream in;
        BufferedReader data;
        String line;
        String ip32;

        log.debug("doSeedlessScrapeServers");
        try {
            ProxyRequest proxy = new ProxyRequest();
            h = proxy.doURLRequest(svcURL, serversLocHeader, null, -1, "admin", cpass);
            if(h != null) {
                i = h.getResponseCode();
                if(i == 200) {
                    in = h.getInputStream();
                    data = new BufferedReader(new InputStreamReader(in));
                    while((line = data.readLine()) != null) {
                        metadatas.add(line);
                    }
                    Iterator it = metadatas.iterator();
                    while(it.hasNext()) {
                        foo = (String)it.next();
                        ip32 = Base64.decodeToString(foo).split(" ")[0];
                        if(!ip32s.contains(ip32)) {
                            ip32s.add(ip32.trim());
                        }
                    }
                }
            }

        } catch(IOException ex) {
        }
        Collections.shuffle(ip32s, new Random());
        SeedlessServers = ip32s;
        log.debug("doSeedlessScrapeServers Done");
        lastSeedlessScrapeServers = System.currentTimeMillis();
    }
}

class ContextHelper {

    /** @throws IllegalStateException if no context available */
    public static RouterContext getContext(String contextId) {
        List contexts = RouterContext.listContexts();
        if((contexts == null) || (contexts.isEmpty())) {
            throw new IllegalStateException("No contexts. This is usually because the router is either starting up or shutting down.");
        }
        if((contextId == null) || (contextId.trim().length() <= 0)) {
            return (RouterContext)contexts.get(0);
        }
        for(int i = 0; i < contexts.size(); i++) {
            RouterContext context = (RouterContext)contexts.get(i);
            Hash hash = context.routerHash();
            if(hash == null) {
                continue;
            }
            if(hash.toBase64().startsWith(contextId)) {
                return context;
            }
        }
        // not found, so just give them the first we can find
        return (RouterContext)contexts.get(0);
    }
}

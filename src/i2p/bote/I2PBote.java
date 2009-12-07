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

import i2p.bote.email.Email;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Identities;
import i2p.bote.folder.DhtPacketFolder;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.IncompleteEmailFolder;
import i2p.bote.folder.IndexPacketFolder;
import i2p.bote.folder.Outbox;
import i2p.bote.folder.PacketFolder;
import i2p.bote.network.CheckEmailTask;
import i2p.bote.network.DHT;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.PeerManager;
import i2p.bote.network.kademlia.KademliaDHT;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.EncryptedEmailPacket;
import i2p.bote.packet.IndexPacket;
import i2p.bote.packet.RelayPacket;
import i2p.bote.packet.UnencryptedEmailPacket;
import i2p.bote.service.OutboxProcessor;
import i2p.bote.service.POP3Service;
import i2p.bote.service.RelayPacketSender;
import i2p.bote.service.SMTPService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Callable;
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
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.Log;

/**
 * This is the core class of the application. Is is implemented as a singleton.
 */
public class I2PBote {
    private static final String VERSION = "0.1.2";
	private static I2PBote instance;
	
    private Log log = new Log(I2PBote.class);
    private I2PAppContext appContext;
	private I2PClient i2pClient;
	private I2PSession i2pSession;
	private Configuration configuration;
	private Identities identities;
	private I2PSendQueue sendQueue;
	private Outbox outbox;   // stores outgoing emails for all local users
    private EmailFolder inbox;   // stores incoming emails for all local users
	private PacketFolder<RelayPacket> relayPacketFolder;   // stores email packets we're forwarding for other machines
	private IncompleteEmailFolder incompleteEmailFolder;   // stores email packets addressed to a local user
    private DhtPacketFolder<? extends DataPacket> emailDhtStorageFolder;   // stores email packets and index packets for other peers
    private IndexPacketFolder indexPacketDhtStorageFolder;   // stores index packets
//TODO    private PacketFolder<> addressDhtStorageFolder;   // stores email address-destination mappings
	private SMTPService smtpService;
	private POP3Service pop3Service;
	private OutboxProcessor outboxProcessor;   // reads emails stored in the outbox and sends them
	private RelayPacketSender relayPacketSender;   // reads packets stored in the relayPacketFolder and sends them
	private DHT dht;
	private PeerManager peerManager;
    private ThreadFactory mailCheckThreadFactory;
    private ExecutorService mailCheckExecutor;
    private Collection<Future<Boolean>> mailCheckResults;

	private I2PBote() {
	    Thread.currentThread().setName("I2PBoteMain");
	    
        initializeLogging();
        
        appContext = new I2PAppContext();
		i2pClient = I2PClientFactory.createClient();
		configuration = new Configuration();
		
        mailCheckThreadFactory = Util.createThreadFactory("ChkMailTask", CheckEmailTask.THREAD_STACK_SIZE);
        mailCheckExecutor = Executors.newFixedThreadPool(configuration.getMaxConcurIdCheckMail(), mailCheckThreadFactory);
    
		identities = new Identities(configuration.getIdentitiesFile());
		initializeSession();
		initializeFolderAccess();
		initializeServices();
		startAllServices();
	}

    private void initializeLogging() {
    }
    
	/**
	 * Initializes objects for accessing emails and packet files on the filesystem.
	 */
	private void initializeFolderAccess() {
	    inbox = new EmailFolder(configuration.getInboxDir());
		outbox = new Outbox(configuration.getLocalOutboxDir());
		relayPacketFolder = new PacketFolder<RelayPacket>(configuration.getRelayOutboxDir());
		incompleteEmailFolder = new IncompleteEmailFolder(configuration.getIncompleteDir(), inbox);
		emailDhtStorageFolder = new DhtPacketFolder<EncryptedEmailPacket>(configuration.getEmailDhtStorageDir());
        indexPacketDhtStorageFolder = new IndexPacketFolder(configuration.getIndexPacketDhtStorageDir());
	}

	/**
	 * Sets up a {@link I2PSession}, using the I2P destination stored on disk or creating a new I2P
	 * destination if no key file exists.
	 */
	private void initializeSession() {
	    Properties sessionProperties = new Properties();
	    sessionProperties.setProperty("i2cp.gzip", String.valueOf(false));   // most of the data we send is encrypted and therefore uncompressible
	    
        // read the local destination key from the key file if it exists
        File destinationKeyFile = configuration.getDestinationKeyFile();
        try {
            FileReader fileReader = new FileReader(destinationKeyFile);
            char[] destKeyBuffer = new char[(int)destinationKeyFile.length()];
            fileReader.read(destKeyBuffer);
            byte[] localDestinationKey = Base64.decode(new String(destKeyBuffer));
            ByteArrayInputStream inputStream = new ByteArrayInputStream(localDestinationKey);
       		i2pSession = i2pClient.createSession(inputStream, sessionProperties);
        	i2pSession.connect();
        }
        catch (IOException e) {
        	log.debug("Destination key file doesn't exist or isn't readable: " + e);
        } catch (I2PSessionException e) {
        	log.warn("Error creating I2PSession", e);
		}
        
		// if the local destination key can't be read or is invalid, create a new one
        if (i2pSession == null) {
			log.debug("Creating new local destination key");
			try {
				ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
				i2pClient.createDestination(arrayStream);
			    byte[] localDestinationArray = arrayStream.toByteArray();
				
				i2pSession = i2pClient.createSession(new ByteArrayInputStream(localDestinationArray), sessionProperties);
	        	i2pSession.connect();
	        	
	        	saveLocalDestinationKeys(destinationKeyFile, localDestinationArray);
			} catch (I2PException e) {
				log.error("Error creating local destination key or I2PSession.", e);
			} catch (IOException e) {
				log.error("Error writing local destination key to file.", e);
			}
        }
        
        Destination localDestination = i2pSession.getMyDestination();
        log.debug("Local destination key = " + localDestination.toBase64());
        log.debug("Local destination hash = " + localDestination.calculateHash().toBase64());
	}
	
	/**
	 * Initializes daemon threads, doesn't start them yet.
	 */
    private void initializeServices() {
        I2PPacketDispatcher dispatcher = new I2PPacketDispatcher();
        i2pSession.addSessionListener(dispatcher, I2PSession.PROTO_ANY, I2PSession.PORT_ANY);
        
        smtpService = new SMTPService();
        pop3Service = new POP3Service();
        relayPacketSender = new RelayPacketSender(sendQueue, relayPacketFolder, appContext);
        sendQueue = new I2PSendQueue(i2pSession, dispatcher);
        
        dht = new KademliaDHT(i2pSession.getMyDestination(), sendQueue, dispatcher, configuration.getPeerFile());
        
        dht.setStorageHandler(EncryptedEmailPacket.class, emailDhtStorageFolder);
        dht.setStorageHandler(IndexPacket.class, indexPacketDhtStorageFolder);
//TODO        dht.setStorageHandler(AddressPacket.class, );
        
        peerManager = new PeerManager();
        
        outboxProcessor = new OutboxProcessor(dht, outbox, configuration, peerManager, appContext);
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
			keyFile.renameTo(oldKeyFile);
		}
		else
		    keyFile.createNewFile();
		
        FileWriter fileWriter = new FileWriter(keyFile);
        fileWriter.write(Base64.encode(localDestinationArray));
        fileWriter.close();
	}
	
	public static void startUp() {
		getInstance();
	}
	
	public static void shutDown() {
		if (instance != null)
			instance.stopAllServices();
	}

	public static I2PBote getInstance() {
		if (instance == null)
			instance = new I2PBote();
		return instance;
	}
	
	public String getVersion() {
	    return VERSION;
	}
	
	public Identities getIdentities() {
	    return identities;
	}
	
	public String getLocalDestination() {
	    return i2pSession.getMyDestination().toBase64();
	}
	
	public void sendEmail(Email email) throws Exception {
/*XXX*/
email.updateHeaders();
String recipient = email.getAllRecipients().iterator().next();
if (recipient.indexOf('@')>=0)
    recipient = recipient.substring(0, recipient.indexOf('@'));
EmailDestination emailDestination = new EmailDestination(recipient);
Collection<UnencryptedEmailPacket> emailPackets = email.createEmailPackets(recipient);
Collection<EncryptedEmailPacket> encryptedPackets = EncryptedEmailPacket.encrypt(emailPackets, emailDestination, appContext);
for (EncryptedEmailPacket packet: encryptedPackets)
    dht.store(packet);
dht.store(new IndexPacket(encryptedPackets, emailDestination));
	    // TODO uncomment for next milestone, move code above into OutboxProcessor
/*		outbox.add(email);
		outboxProcessor.checkForEmail();*/
	}

    public synchronized void checkForMail() {
        if (!isCheckingForMail())
            mailCheckResults = Collections.synchronizedCollection(new ArrayList<Future<Boolean>>());
            for (EmailIdentity identity: getIdentities()) {
                Callable<Boolean> checkMailTask = new CheckEmailTask(identity, dht, peerManager, incompleteEmailFolder, appContext);
                Future<Boolean> result = mailCheckExecutor.submit(checkMailTask);
                mailCheckResults.add(result);
            }
    }

    public synchronized boolean isCheckingForMail() {
        if (mailCheckResults == null)
            return false;
        
        for (Future<Boolean> result: mailCheckResults)
            if (!result.isDone())
                return true;
        
        return false;
    }
    
    /**
     * Returns <code>true</code> if the last call to {@link checkForMail} has completed
     * and added new mail to the inbox.
     * If this method returns <code>true</code>, subsequent calls will always return
     * <code>false</code> until {@link checkForMail} is executed again.
     * @return
     */
    public synchronized boolean newMailReceived() {
        if (mailCheckResults == null)
            return false;
        if (isCheckingForMail())
            return false;
        
        try {
            for (Future<Boolean> result: mailCheckResults)
                if (result.get(1, TimeUnit.MILLISECONDS)) {
                    mailCheckResults = null;
                    return true;
                }
        }
        catch (Exception e) {
            log.error("Error while checking whether new mail has arrived.", e);
        }
        
        mailCheckResults = null;
        return false;
    }
    
    public EmailFolder getInbox() {
        return inbox;
    }

    public int getNumDhtPeers() {
        return dht.getNumPeers();
    }
    
    public int getNumRelayPeers() {
        return peerManager.getNumPeers();
    }
    
    private void startAllServices()  {
        dht.start();
		outboxProcessor.start();
		relayPacketSender.start();
		smtpService.start();
		pop3Service.start();
		sendQueue.start();
	}

	private void stopAllServices()  {
        dht.shutDown();
		outboxProcessor.shutDown();
		relayPacketSender.requestShutdown();
		smtpService.shutDown();
		pop3Service.shutDown();
        sendQueue.requestShutdown();
        mailCheckExecutor.shutdownNow();
	}
}
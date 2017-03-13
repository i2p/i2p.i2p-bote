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

package i2p.bote.network.kademlia;

import i2p.bote.I2PBote;
import i2p.bote.UniqueId;
import i2p.bote.Util;
import i2p.bote.folder.DeletionAwareDhtFolder;
import i2p.bote.network.DHT;
import i2p.bote.network.DhtException;
import i2p.bote.network.DhtPeerSource;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.DhtPeerStatsRenderer;
import i2p.bote.network.DhtResults;
import i2p.bote.network.DhtStorageHandler;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.PacketBatch;
import i2p.bote.network.PacketListener;
import i2p.bote.network.PeerFileAnchor;
import i2p.bote.network.kademlia.SBucket.BucketSection;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.PeerList;
import i2p.bote.packet.ResponsePacket;
import i2p.bote.packet.StatusCode;
import i2p.bote.packet.dht.DeleteRequest;
import i2p.bote.packet.dht.DeletionInfoPacket;
import i2p.bote.packet.dht.DeletionQuery;
import i2p.bote.packet.dht.DeletionRecord;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.FindClosePeersPacket;
import i2p.bote.packet.dht.RetrieveRequest;
import i2p.bote.packet.dht.StoreRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;
import net.i2p.util.RandomSource;
import net.i2p.util.SecureFileOutputStream;

/**
 * The main class of the Kademlia implementation. All the high-level Kademlia logic
 * is in here.<br/>
 * In addition to standard Kademlia, the sibling list feature of S-Kademlia is implemented.
 * <p/>
 * Resources used:<br/>
 * <ol>
 *   <li>http://pdos.csail.mit.edu/~petar/papers/maymounkov-kademlia-lncs.pdf<br/>
 *   <li>http://xlattice.sourceforge.net/components/protocol/kademlia/specs.html<br/>
 *   <li>http://en.wikipedia.org/wiki/Kademlia<br/>
 *   <li>http://www.barsoom.org/papers/infocom-2006-kad.pdf<br/>
 *   <li>http://doc.tm.uka.de/SKademlia_2007.pdf<br/>
 *   <li>OverSim (http://www.oversim.org/), which includes a S/Kademlia implementation<br/>
 * </ol>
 */
public class KademliaDHT extends I2PAppThread implements DHT, PacketListener {
    private static final int RESPONSE_TIMEOUT = 60;   // Max. number of seconds to wait for replies to retrieve requests
    
    private Log log = new Log(KademliaDHT.class);
    private I2PSendQueue sendQueue;
    private I2PPacketDispatcher i2pReceiver;
    private File peerFile;
    private DhtPeerSource externalPeerSource;
    private ReplicateThread replicateThread;   // is notified of <code>store</code> calls
    private CountDownLatch readySignal;   // switches to 0 when bootstrapping is done
    private Destination localDestination;
    private Hash localDestinationHash;
    private Set<KademliaPeer> initialPeers;
    private BucketManager bucketManager;
    private Map<Class<? extends DhtStorablePacket>, DhtStorageHandler> storageHandlers;

    /**
     * 
     * @param sendQueue
     * @param i2pReceiver
     * @param peerFile
     * @param externalPeerSource Provides seedless peers
     */
    public KademliaDHT(I2PSendQueue sendQueue, I2PPacketDispatcher i2pReceiver, File peerFile, DhtPeerSource externalPeerSource) {
        super("Kademlia");
        
        this.sendQueue = sendQueue;
        this.i2pReceiver = i2pReceiver;
        this.peerFile = peerFile;
        this.externalPeerSource = externalPeerSource;
        
        readySignal = new CountDownLatch(1);
        localDestination = sendQueue.getLocalDestination();
        localDestinationHash = localDestination.calculateHash();
        initialPeers = new ConcurrentHashSet<KademliaPeer>();
        // Read the built-in peer file
        URL builtInPeerFile = PeerFileAnchor.getBuiltInPeersFile();
        List<String> builtInPeers = Util.readLines(builtInPeerFile);
        addPeers(builtInPeers);
        // Read the updateable peer file if it exists
        if (peerFile.exists()) {
            List<String> receivedPeers = Util.readLines(peerFile);
            addPeers(receivedPeers);
        }
        else
            log.info("Peer file doesn't exist, using built-in peers only (File not found: <" + peerFile.getAbsolutePath() + ">)");
        
        bucketManager = new BucketManager(localDestinationHash);
        storageHandlers = new ConcurrentHashMap<Class<? extends DhtStorablePacket>, DhtStorageHandler>();
        replicateThread = new ReplicateThread(localDestination, sendQueue, i2pReceiver, bucketManager);
    }
    
    /**
     * Creates peer destinations from a <code>String</code> each, and adds them to <code>initialPeers</code>.
     * @param peerFileEntries A list of <code>String</code>s as they appear in the peer file
     */
    private void addPeers(List<String> peerFileEntries) {
        for (String line: peerFileEntries) {
            if (!line.trim().isEmpty() && !line.startsWith("#"))
                try {
                    Destination destination = new Destination(line);
                    
                    // don't add the local destination as a peer
                    if (!destination.equals(localDestination)) {
                        KademliaPeer peer = new KademliaPeer(destination, 0);
                        initialPeers.add(peer);
                    }
                }
                catch (DataFormatException e) {
                    log.error("Invalid destination key in line " + line, e);
                }
        }
    }
    
    /**
     * Queries the DHT for the <code>k</code> peers closest to a given key.
     * This method blocks.
     * @see ClosestNodesLookupTask
     */
    private List<Destination> getClosestNodes(Hash key) throws InterruptedException {
        bucketManager.updateLastLookupTime(key);
        
        ClosestNodesLookupTask lookupTask = new ClosestNodesLookupTask(key, sendQueue, i2pReceiver, bucketManager);
        return lookupTask.call();
    }

    @Override
    public DhtResults findOne(Hash key, Class<? extends DhtStorablePacket> dataType) throws InterruptedException {
        return find(key, dataType, false);
    }

    @Override
    public DhtResults findAll(Hash key, Class<? extends DhtStorablePacket> dataType) throws InterruptedException {
        return find(key, dataType, true);
    }

    @Override
    public UniqueId findDeleteAuthorizationKey(Hash dhtKey, Hash verificationHash) throws InterruptedException {
        final Collection<Destination> closeNodes = getClosestNodes(dhtKey);
        log.info("Querying " + closeNodes.size() + " peers with DeletionQueries for Kademlia key " + dhtKey);
        
        DhtStorageHandler storageHandler = storageHandlers.get(EncryptedEmailPacket.class);
        if (storageHandler instanceof DeletionAwareDhtFolder) {
            DeletionAwareDhtFolder<?> folder = (DeletionAwareDhtFolder<?>)storageHandler;
            UniqueId delAuthorization = folder.getDeleteAuthorization(dhtKey);
            if (delAuthorization != null)
                return delAuthorization;
        }
        else
            log.error("StorageHandler for EncryptedEmailPackets is not a DeletionAwareDhtFolder!");
        
        // Send the DeletionQueries
        PacketBatch batch = new PacketBatch();
        for (Destination node: closeNodes)
            if (!localDestination.equals(node))   // local has already been taken care of
                batch.putPacket(new DeletionQuery(dhtKey), node);
        sendQueue.send(batch);
        batch.awaitSendCompletion();

        // wait for replies
        batch.awaitFirstReply(RESPONSE_TIMEOUT, TimeUnit.SECONDS);
        log.info(batch.getResponses().size() + " response packets received for deletion query for hash " + dhtKey);
        
        sendQueue.remove(batch);
        
        Map<Destination, DataPacket> responses = batch.getResponses();
        for (DataPacket response: responses.values())
            if (response instanceof DeletionInfoPacket) {
                DeletionInfoPacket delInfo = (DeletionInfoPacket)response;
                DeletionRecord delRecord = delInfo.getEntry(dhtKey);
                if (delRecord != null) {
                    boolean valid = Util.isDeleteAuthorizationValid(verificationHash, delRecord.delAuthorization);
                    if (valid)
                        return delRecord.delAuthorization;
                }
            }
        return null;
    }
    
    @Override
    public void setStorageHandler(Class<? extends DhtStorablePacket> packetType, DhtStorageHandler storageHandler) {
        storageHandlers.put(packetType, storageHandler);
        replicateThread.addDhtStoreToReplicate(storageHandler);
    }

    @Override
    public CountDownLatch readySignal() {
        return readySignal;
    }
    
    @Override
    public boolean isReady() {
        try {
            return readySignal().await(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted during a zero-second wait!", e);
            return false;
        }
    }

    @Override
    public DhtPeerStats getPeerStats(DhtPeerStatsRenderer renderer) {
        return bucketManager.getPeerStats(renderer);
    }
    
    private DhtResults find(Hash key, Class<? extends DhtStorablePacket> dataType, boolean exhaustive) throws InterruptedException {
        final Collection<Destination> closeNodes = getClosestNodes(key);
        log.info("Querying localhost + " + closeNodes.size() + " peers for data type " + dataType.getSimpleName() + ", Kademlia key " + key);
        
        DhtStorablePacket localResult = findLocally(key, dataType);
        // if a local packet exists and one result is requested, return the local packet
        if (!exhaustive && localResult!=null) {
            log.debug("Locally stored packet found for hash " + key + " and data type " + dataType.getSimpleName());
            DhtResults results = new DhtResults();
            results.put(localDestination, localResult);
            return results;
        }
        
        // Send the retrieve requests
        PacketBatch batch = new PacketBatch();
        for (Destination node: closeNodes)
            if (!localDestination.equals(node))   // local has already been taken care of
                batch.putPacket(new RetrieveRequest(key, dataType), node);
        sendQueue.send(batch);
        batch.awaitSendCompletion();

        // wait for replies
        if (exhaustive)
            batch.awaitAllResponses(RESPONSE_TIMEOUT, TimeUnit.SECONDS);
        else
            batch.awaitFirstReply(RESPONSE_TIMEOUT, TimeUnit.SECONDS);
        log.info(batch.getResponses().size() + " response packets received for hash " + key + " and data type " + dataType.getSimpleName());
        
        sendQueue.remove(batch);
        
        return getDhtResults(batch, localResult);
    }

    private DhtStorablePacket findLocally(Hash key, Class<? extends DhtStorablePacket> dataType) {
        DhtStorageHandler storageHandler = storageHandlers.get(dataType);
        if (storageHandler != null)
            return storageHandler.retrieve(key);
        else
            return null;
    }
    
    /**
     * Returns all <code>DhtStorablePacket</code> packets that have been received as a response to a send batch,
     * plus <code>localResult</code> if it is non-<code>null</code>.
     * @param batch
     * @param localResult
     */
    private DhtResults getDhtResults(PacketBatch batch, DhtStorablePacket localResult) {
        Map<Destination, DataPacket> responses = batch.getResponses();
        
        DhtResults results = new DhtResults();
        for (Entry<Destination, DataPacket> result: responses.entrySet()) {
            DataPacket packet = result.getValue();
            if (packet instanceof DhtStorablePacket)
                results.put(result.getKey(), (DhtStorablePacket)packet);
        }
        
        int totalResponses = responses.size();
        if (localResult != null) {
            results.put(localDestination, localResult);
            totalResponses++;
        }
        results.setTotalResponses(totalResponses);
        
        return results;
    }
    
    @Override
    public void store(DhtStorablePacket packet) throws DhtException, InterruptedException {
        Hash key = packet.getDhtKey();
        log.info("Looking up nodes to store a " + packet.getClass().getSimpleName() + " with key " + key);
        
        List<Destination> closeNodes = getClosestNodes(key);
        if (closeNodes.isEmpty())
            throw new DhtException("Cannot store packet because no storage nodes found.");
        // store on local node if appropriate
        if (!closeNodes.contains(localDestination))
            if (closeNodes.size()<KademliaConstants.K || isCloser(localDestination, closeNodes.get(0), key))
                closeNodes.add(localDestination);
            
        log.info("Storing a " + packet.getClass().getSimpleName() + " with key " + key + " on " + closeNodes.size() + " nodes");
        
        PacketBatch batch = new PacketBatch();
        for (Destination node: closeNodes)
            if (localDestination.equals(node))
                storeLocally(packet, null);
            else {
                StoreRequest storeRequest = new StoreRequest(packet);   // use a separate packet id for each request
                batch.putPacket(storeRequest, node);
            }
        sendQueue.send(batch);
        
        batch.awaitSendCompletion();
        // TODO awaitAllResponses, repeat if necessary
        
        sendQueue.remove(batch);
    }

    /**
     * Returns <code>true</code> if <code>dest1</code> is closer to <code>key</code> than <code>dest2</code>.
     * @param key
     * @param destination
     * @param peers
     */
    private boolean isCloser(Destination dest1, Destination dest2, Hash key) {
        return new PeerDistanceComparator(key).compare(dest1, dest2) < 0;
    }
    
    /**
     * Connects to the Kademlia network; blocks until done.
     */
    private void bootstrap() {
        new BootstrapTask(i2pReceiver).run();
    }
    
    private class BootstrapTask implements Runnable, PacketListener {
        private Log log = new Log(BootstrapTask.class);
        I2PPacketDispatcher i2pReceiver;
        
        public BootstrapTask(I2PPacketDispatcher i2pReceiver) {
            this.i2pReceiver = i2pReceiver;
        }
        
        @Override
        public void run() {
            log.info("Bootstrap start");
            i2pReceiver.addPacketListener(this);
        outerLoop:
            while (!Thread.interrupted())
                try {
                    // add any known seedless peers
                    if (externalPeerSource != null)
                        for (Destination destination: externalPeerSource.getPeers())
                            initialPeers.add(new KademliaPeer(destination));
                    
                    for (KademliaPeer bootstrapNode: initialPeers) {
                        bootstrapNode.setFirstSeen(System.currentTimeMillis());   // Set the "first seen" time to the current time before every bootstrap attempt
                        bootstrapNode.responseReceived();   // unlock the peer so ClosestNodesLookupTask will give it a chance
                        bucketManager.addOrUpdate(bootstrapNode);
                        log.info("Trying " + Util.toBase32(bootstrapNode) + " for bootstrapping.");
                        Collection<Destination> closestNodes = getClosestNodes(localDestinationHash);
                        
                        if (closestNodes.isEmpty()) {
                            log.info("No response from bootstrap node " + Util.toBase32(bootstrapNode));
                            bucketManager.remove(bootstrapNode);
                        }
                        else {
                            log.info("Response from bootstrap node received, refreshing all buckets. Bootstrap node = " + Util.toBase32(bootstrapNode));
                            refreshAll();
                            log.info("Bootstrapping finished. Number of peers = " + bucketManager.getPeerCount());
                            for (Destination peer: bucketManager.getAllPeers())
                                log.debug("  Peer: " + Util.toBase32(peer));
                            break outerLoop;
                        }
                    }
                    
                    log.warn("Can't bootstrap off any known peer, will retry shortly.");
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            i2pReceiver.removePacketListener(this);
            readySignal.countDown();
            I2PBote.getInstance().networkStatusChanged();
                
            log.debug("BootstrapTask exiting.");
        }

        /**
         * When a previously unknown peer contacts us, this method adds it to <code>initialPeers</code>
         * so it can be used as a bootstrap node.
         * @param packet
         * @param sender
         * @param receiveTime
         */
        @Override
        public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
            initialPeers.add(new KademliaPeer(sender, receiveTime));
        }
    }
    
    /**
     * "refresh all k-buckets further away than the closest neighbor. This refresh is just a
     * lookup of a random key that is within that k-bucket range."
     * @throws InterruptedException 
     */
    private void refreshAll() throws InterruptedException {
        for (KBucket bucket: Util.synchronizedCopy(bucketManager))
            refresh(bucket);
    }
    
    /**
     * Refreshes all buckets whose <code>lastLookupTime</code> is too old.
     * @throws InterruptedException 
     */
    private void refreshOldBuckets() throws InterruptedException {
        long now = System.currentTimeMillis();
        
        // refresh k-buckets
        for (KBucket bucket: Util.synchronizedCopy(bucketManager))
            if (now > bucket.getLastLookupTime() + KademliaConstants.BUCKET_REFRESH_INTERVAL) {
                log.info("Refreshing k-bucket: " + bucket);
                refresh(bucket);
            }
        
        // Refresh the s-bucket by doing a lookup for a random key in each section of the bucket.
        // For example, if k=20 and s=100, there would be a lookup for a random key between
        // the 0th and the 20th sibling (i=0), another one for a random key between the 20th
        // and the 40th sibling (i=1), etc., and finally a lookup for a random key between the
        // 80th and the 100th sibling (i=4).
        SBucket sBucket = bucketManager.getSBucket();
        BucketSection[] sections = sBucket.getSections();
        for (int i=0; i<sections.length; i++) {
            BucketSection section = sections[i];
            if (now > section.getLastLookupTime() + KademliaConstants.BUCKET_REFRESH_INTERVAL) {
                log.info("Refreshing s-bucket section " + i + " of " + sections.length + " (last refresh: " + new Date(section.getLastLookupTime()) + ")");
                refresh(section);
            }
        }
    }
    
    private void refresh(KBucket bucket) throws InterruptedException {
        Hash key = createRandomHash(bucket.getStartId(), bucket.getEndId());
        getClosestNodes(key);
    }

    private void refresh(BucketSection section) throws InterruptedException {
        Hash key = createRandomHash(section.getStart(), section.getEnd());
        getClosestNodes(key);
    }

    /**
     * Returns a random value <code>r</code> such that <code>min &lt;= r &lt; max</code>.
     * @param min
     * @param max
     */
    private Hash createRandomHash(BigInteger min, BigInteger max) {
        BigInteger hashValue;
        if (min.compareTo(max) >= 0)
            hashValue = min;
        else {
            hashValue = new BigInteger(Hash.HASH_LENGTH*8, RandomSource.getInstance());   // a random number between 0 and 2^256-1
            hashValue = min.add(hashValue.mod(max.subtract(min)));   // a random number equal to or greater than min, and less than max
        }
        byte[] hashArray = hashValue.toByteArray();
        if (hashArray.length>Hash.HASH_LENGTH+1 || (hashArray.length==Hash.HASH_LENGTH+1 && hashArray[0]!=0))   // it's okay for the array length to be Hash.HASH_LENGTH if the zeroth byte only contains the sign bit
            log.error("Hash value too big to fit in " + Hash.HASH_LENGTH + " bytes: " + hashValue);
        byte[] hashArrayPadded = new byte[Hash.HASH_LENGTH];
        if (hashArray.length == Hash.HASH_LENGTH + 1)
            System.arraycopy(hashArray, 1, hashArrayPadded, 0, Hash.HASH_LENGTH);
        else
            System.arraycopy(hashArray, 0, hashArrayPadded, Hash.HASH_LENGTH-hashArray.length, hashArray.length);
        return new Hash(hashArrayPadded);
    }
    
    /**
     * Writes all peers to a file, sorted in descending order of uptime.
     * @param file
     */
    private void writePeersSorted(File file) {
        List<KademliaPeer> peers = bucketManager.getAllPeers();
        if (peers.isEmpty())
            return;
        
        sortByUptime(peers);
        
        log.info("Writing peers to file: <" + file.getAbsolutePath() + ">");
        writePeers(peers, file);
    }

    private void writePeers(List<KademliaPeer> peers, File file) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new SecureFileOutputStream(file.getAbsolutePath())));
            writer.write("# Each line is one Base64-encoded I2P destination.");
            writer.newLine();
            writer.write("# Do not edit while I2P-Bote is running as it will be overwritten.");
            writer.newLine();
            for (KademliaPeer peer: peers) {
                writer.write(peer.toBase64());
                writer.newLine();
            }
        }
        catch (IOException e) {
            log.error("Can't write peers to file <" + file.getAbsolutePath() + ">", e);
        }
        finally {
            if (writer != null)
                try {
                    writer.close();
                }
                catch (IOException e) {
                    log.error("Can't close BufferedWriter for file <" + file.getAbsolutePath() + ">", e);
                }
        }
    }
    
    /**
     * Sorts a list of peers in descending order of "first seen" time.
     * Locked peers are placed after the last unlocked peer.
     * @param peers
     */
    private void sortByUptime(List<KademliaPeer> peers) {
        Collections.sort(peers, new Comparator<KademliaPeer>() {
            @Override
            public int compare(KademliaPeer peer1, KademliaPeer peer2) {
                if (peer1.isLocked() || peer2.isLocked()) {
                    int n1 = peer1.isLocked() ? 0 : 1;
                    int n2 = peer2.isLocked() ? 0 : 1;
                    return n2 - n1;
                }
                else
                    return Long.valueOf(peer2.getFirstSeen()).compareTo(peer1.getFirstSeen());
            }
        });
    }
    
    private void sendPeerList(FindClosePeersPacket packet, Destination destination) {
        Collection<Destination> closestPeers = bucketManager.getClosestPeers(packet.getKey(), KademliaConstants.K);
        PeerList peerList = new PeerList(closestPeers);
        sendQueue.sendResponse(peerList, destination, packet.getPacketId());
    }

    // PacketListener implementation
    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        if (packet instanceof FindClosePeersPacket)
            sendPeerList((FindClosePeersPacket)packet, sender);
        else if (packet instanceof StoreRequest) {
            DhtStorablePacket packetToStore = ((StoreRequest)packet).getPacketToStore();
            storeLocally(packetToStore, sender);
        }
        else if (packet instanceof RetrieveRequest) {
            RetrieveRequest retrieveRequest = (RetrieveRequest)packet;
            DhtStorageHandler storageHandler = storageHandlers.get(retrieveRequest.getDataType());
            if (storageHandler != null) {
                DhtStorablePacket storedPacket = storageHandler.retrieve(retrieveRequest.getKey());
                // if requested packet found, send it to the requester
                Collection<ResponsePacket> response = ResponsePacket.create(storedPacket, StatusCode.OK, retrieveRequest.getPacketId());
                if (storedPacket != null)
                    log.debug("Packet found for retrieve request: [" + retrieveRequest + "], replying to sender: [" + Util.toBase32(sender) + "]");
                else
                    log.debug("No matching packet found for retrieve request: [" + retrieveRequest + "]");
                sendQueue.send(response, sender);
            }
            else
                log.warn("No storage handler found for type " + packet.getClass().getSimpleName() + ".");
        }
        else if (packet instanceof DeleteRequest) {
            DeleteRequest delRequest = (DeleteRequest)packet;
            DhtStorageHandler storageHandler = storageHandlers.get(delRequest.getDataType());
            if (storageHandler instanceof DeletionAwareDhtFolder<?>)
                ((DeletionAwareDhtFolder<?>)storageHandler).process(delRequest);
        }
        else if (packet instanceof DeletionQuery) {
            DhtStorageHandler storageHandler = storageHandlers.get(EncryptedEmailPacket.class);
            if (storageHandler instanceof DeletionAwareDhtFolder) {
                Hash dhtKey = ((DeletionQuery)packet).getDhtKey();
                UniqueId delAuthorization = ((DeletionAwareDhtFolder<?>)storageHandler).getDeleteAuthorization(dhtKey);
                // If we know the Delete Authorization for the DHT key, send it to the peer
                if (delAuthorization != null) {
                    DeletionInfoPacket delInfo = new DeletionInfoPacket();
                    delInfo.put(dhtKey, delAuthorization);
                    Collection<ResponsePacket> response = ResponsePacket.create(delInfo, StatusCode.OK, packet.getPacketId());
                    sendQueue.send(response, sender);
                }
            }
        }
        
        // bucketManager is not registered as a PacketListener, so notify it here
        bucketManager.packetReceived(packet, sender, receiveTime);
    }
    
    /**
     * Stores a DHT packet locally. The folder the packet is stored in depends on the packet type.
     * @param packetToStore
     * @param sender The peer that sent the store request; can be <code>null</code> for the local node
     */
    private void storeLocally(DhtStorablePacket packetToStore, Destination sender) {
        if (packetToStore != null) {
            DhtStorageHandler storageHandler = storageHandlers.get(packetToStore.getClass());
            if (storageHandler != null) {
                // If another peer is trying to store a packet that we know has been deleted, let them know and don't store the packet.
                if (storageHandler instanceof DeletionAwareDhtFolder<?> && sender!=null) {
                    DeletionAwareDhtFolder<?> folder = (DeletionAwareDhtFolder<?>)storageHandler;
                    DeleteRequest delRequest = folder.storeAndCreateDeleteRequest(packetToStore);
                    if (delRequest != null)
                        sendQueue.send(delRequest, sender);
                }
                else
                    storageHandler.store(packetToStore);
                replicateThread.packetStored(storageHandler, packetToStore);
            }
            else
                log.warn("No storage handler found for type " + packetToStore.getClass().getSimpleName() + ".");
        }
    }
    
    @Override
    public void run() {
        i2pReceiver.addPacketListener(this);
        bootstrap();
        replicateThread.start();
        
        while (!Thread.interrupted()) {
            try {
                if (bucketManager.getUnlockedPeerCount() == 0) {
                    log.info("All peers are gone. Re-bootstrapping.");
                    bootstrap();
                }
                refreshOldBuckets();
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in KademliaDHT loop", e);
            }
        }
        
        replicateThread.interrupt();
        i2pReceiver.removePacketListener(this);
        writePeersSorted(peerFile);
        log.debug("KademliaDHT thread exiting.");
    }
}

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

import i2p.bote.network.DHT;
import i2p.bote.network.DhtStorageHandler;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.PacketBatch;
import i2p.bote.network.PacketListener;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.PeerList;
import i2p.bote.packet.ResponsePacket;
import i2p.bote.packet.StatusCode;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.FindClosePeersPacket;
import i2p.bote.packet.dht.RetrieveRequest;
import i2p.bote.packet.dht.StoreRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

/**
 * The main class of the Kademlia implementation. All the high-level Kademlia logic
 * is in here.
 * 
 * Resources used:
 *   [1] http://pdos.csail.mit.edu/~petar/papers/maymounkov-kademlia-lncs.pdf
 *   [2] http://xlattice.sourceforge.net/components/protocol/kademlia/specs.html
 *   [3] http://en.wikipedia.org/wiki/Kademlia
 *   [4] http://www.barsoom.org/papers/infocom-2006-kad.pdf
 *   [5] http://doc.tm.uka.de/SKademlia_2007.pdf
 *   [6] OverSim (http://www.oversim.org/), which includes a S/Kademlia implementation
 *   
 */
public class KademliaDHT implements DHT, PacketListener {
    private static final URL BUILT_IN_PEER_FILE = KademliaDHT.class.getResource("built-in-peers.txt");
    
    private Log log = new Log(KademliaDHT.class);
    private Hash localDestinationHash;
    private I2PSendQueue sendQueue;
    private I2PPacketDispatcher i2pReceiver;
    private File peerFile;
    private Collection<KademliaPeer> initialPeers;
    private BucketManager bucketManager;
    private Map<Class<? extends DhtStorablePacket>, DhtStorageHandler> storageHandlers;

    public KademliaDHT(Destination localDestination, I2PSendQueue sendQueue, I2PPacketDispatcher i2pReceiver, File peerFile) {
        localDestinationHash = localDestination.calculateHash();
        this.sendQueue = sendQueue;
        this.i2pReceiver = i2pReceiver;
        this.peerFile = peerFile;
        
        initialPeers = Collections.synchronizedList(new ArrayList<KademliaPeer>());
        // Read the built-in peer file
        readPeers(BUILT_IN_PEER_FILE);
        // Read the updateable peer file
        readPeers(peerFile);
        
        bucketManager = new BucketManager(sendQueue, initialPeers, localDestination.calculateHash());
        storageHandlers = new ConcurrentHashMap<Class<? extends DhtStorablePacket>, DhtStorageHandler>();
    }
    
    /**
     * Returns the S nodes closest to a given key by querying peers.
     * This method blocks. It returns after <code>CLOSEST_NODES_LOOKUP_TIMEOUT+1</code> seconds at
     * the longest.
     *
     * The number of pending requests never exceeds ALPHA. According to [4], this is the most efficient.
     * 
     * If there are less than <code>s</code> results after the kademlia lookup finishes, nodes from
     * the sibling list are used.
     */
    private Collection<Destination> getClosestNodes(Hash key) {
        ClosestNodesLookupTask lookupTask = new ClosestNodesLookupTask(key, sendQueue, i2pReceiver, bucketManager);
        lookupTask.run();
        return lookupTask.getResults();
    }

    @Override
    public DhtStorablePacket findOne(Hash key, Class<? extends DhtStorablePacket> dataType) {
        Collection<DhtStorablePacket> results = find(key, dataType, false);
        if (results.isEmpty())
            return null;
        else
            return results.iterator().next();
    }

    @Override
    public Collection<DhtStorablePacket> findAll(Hash key, Class<? extends DhtStorablePacket> dataType) {
        return find(key, dataType, true);
    }

    @Override
    public void setStorageHandler(Class<? extends DhtStorablePacket> packetType, DhtStorageHandler storageHandler) {
        storageHandlers.put(packetType, storageHandler);
    }

    @Override
    public boolean isConnected() {
        return getNumPeers() > 0;
    }
    
    @Override
    public int getNumPeers() {
        return bucketManager.getPeerCount();
    }
    
    private Collection<DhtStorablePacket> find(Hash key, Class<? extends DhtStorablePacket> dataType, boolean exhaustive) {
        final Collection<Destination> closeNodes = getClosestNodes(key);
        log.debug("Querying " + closeNodes.size() + " nodes for data type " + dataType + ", Kademlia key " + key);
        
        final Collection<I2PBotePacket> receivedPackets = new ConcurrentHashSet<I2PBotePacket>();   // avoid adding duplicate packets
        
/*        PacketListener packetListener = new PacketListener() {
            @Override
            public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
                // add packet to list of received packets if the packet is in response to a RetrieveRequest
                if (packet instanceof RetrieveRequest && closeNodes.contains(sender))
                    receivedPackets.add(packet);
            }
        };
        i2pReceiver.addPacketListener(packetListener);*/
        
        // Send the retrieve requests
        PacketBatch batch = new PacketBatch();
        for (Destination node: closeNodes)
            batch.putPacket(new RetrieveRequest(key, dataType), node);
        sendQueue.send(batch);
        try {
            batch.awaitSendCompletion();
        }
        catch (InterruptedException e) {
            log.warn("Interrupted while waiting for Retrieve Requests to be sent.", e);
        }

        // wait for replies
        try {
            if (exhaustive)
                TimeUnit.SECONDS.sleep(60);   // TODO make a static field
            else
                batch.awaitFirstReply(60, TimeUnit.SECONDS);   // TODO make a static field
        }
        catch (InterruptedException e) {
            log.warn("Interrupted while waiting for responses to Retrieve Requests.", e);
        }
        log.debug(batch.getResponses().size() + " response packets received for hash " + key + " and data type " + dataType);
        
        sendQueue.remove(batch);
//        i2pReceiver.removePacketListener(packetListener);
        
        ConcurrentHashSet<DhtStorablePacket> storablePackets = getStorablePackets(batch);
        DhtStorablePacket localResult = findLocally(key, dataType);
        if (localResult != null) {
            log.debug("Locally stored packet found for hash " + key + " and data type " + dataType);
            storablePackets.add(localResult);
        }
        return storablePackets;
    }

    private DhtStorablePacket findLocally(Hash key, Class<? extends DhtStorablePacket> dataType) {
        DhtStorageHandler storageHandler = storageHandlers.get(dataType);
        if (storageHandler != null)
            return storageHandler.retrieve(key);
        else
            return null;
    }
    
    /**
     * Returns all <code>DhtStorablePacket</code> packets that have been received as a response to a send batch.
     * @param batch
     * @return
     */
    private ConcurrentHashSet<DhtStorablePacket> getStorablePackets(PacketBatch batch) {
        ConcurrentHashSet<DhtStorablePacket> storablePackets = new ConcurrentHashSet<DhtStorablePacket>();
        for (I2PBotePacket packet: batch.getResponses())
            if (packet instanceof DhtStorablePacket)
                storablePackets.add((DhtStorablePacket)packet);
        return storablePackets;
    }
    
    @Override
    public void store(DhtStorablePacket packet) throws NoSuchAlgorithmException {
        Hash key = packet.getDhtKey();
        
        Collection<Destination> closeNodes = getClosestNodes(key);
        log.debug("Storing a " + packet.getClass().getSimpleName() + " with key " + key + " on " + closeNodes.size() + " nodes");
        
        HashCash hashCash = HashCash.mintCash("", 1);   // TODO
        StoreRequest storeRequest = new StoreRequest(hashCash, packet);
        PacketBatch batch = new PacketBatch();
        for (Destination node: closeNodes)
            batch.putPacket(storeRequest, node);
        sendQueue.send(batch);
        
        try {
            batch.awaitSendCompletion();
            // TODO awaitAllResponses, repeat if necessary
        }
        catch (InterruptedException e) {
            log.warn("Interrupted while waiting for responses to Storage Requests to be sent.", e);
        }
        
        sendQueue.remove(batch);
    }

    @Override
    public void start() {
        i2pReceiver.addPacketListener(this);
        bucketManager.start();
        bootstrap();
    }
    
    @Override
    public void shutDown() {
        i2pReceiver.removePacketListener(this);
        bucketManager.requestShutdown();
        writePeersToFile(peerFile);
    }
    
    private void bootstrap() {
        new BootstrapTask().start();
    }
    
    private class BootstrapTask extends Thread {
        public BootstrapTask() {
            super("Bootstrap");
            setDaemon(true);
        }
        
        @Override
        public void run() {
            log.debug("Bootstrap start");
            while (true) {
                for (KademliaPeer bootstrapNode: initialPeers) {
                    bootstrapNode.setLastReception(-1);
                    bucketManager.addOrUpdate(bootstrapNode);
                    Collection<Destination> closestNodes = getClosestNodes(localDestinationHash);
                    // if last reception time is not set, the node didn't respond, so remove it
                    if (bootstrapNode.getLastReception() <= 0)
                        bucketManager.remove(bootstrapNode);
                    
                    if (closestNodes.isEmpty()) {
                        log.debug("No response from bootstrap node " + bootstrapNode);
                        bucketManager.remove(bootstrapNode);
                    }
                    else {
                        bucketManager.refreshAll();
                        log.info("Bootstrapping finished. Number of peers = " + bucketManager.getPeerCount());
                        return;
                    }
                }
                
                log.warn("Can't bootstrap off any known peer, will retry shortly.");
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    log.error("Interrupted while pausing after unsuccessful bootstrap attempt.", e);
                }
            }
        }
    }
    
    /**
     * Writes all peers to a file in descending order of "last seen" time.
     * @param peerFile
     */
    private void writePeersToFile(File peerFile) {
        // TODO
    }

    private void readPeers(URL url) {
        log.info("Reading peers from URL: '" + url + "'");
        InputStream stream = null;
        try {
            stream = url.openStream();
            readPeers(stream);
        }
        catch (IOException e) {
            log.error("Error reading peers from URL.", e);
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Can't close input stream.", e);
                }
        }
    }
    
    private void readPeers(File peerFile) {
        log.info("Reading peers from file: '" + peerFile.getAbsolutePath() + "'");
        InputStream stream = null;
        try {
            stream = new FileInputStream(peerFile);
            readPeers(stream);
        } catch (IOException e) {
            log.error("Error reading peers from file.", e);
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Can't close input stream.", e);
                }
        }
    }
    
    /**
     * Reads peer destinations from an <code>InputStream</code> and writes them to <code>initialPeers</code>.
     * @param inputStream
     */
    private void readPeers(InputStream inputStream) throws IOException {
        BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(inputStream));
        
        int numPeersBefore = initialPeers.size();
        while (true) {
            String line = null;
            line = inputBuffer.readLine();
            if (line == null)
                break;
            
            if (!line.startsWith("#"))
                try {
                	Destination destination = new Destination(line);
                	KademliaPeer peer = new KademliaPeer(destination, 0);
                	
                    // don't add the local destination as a peer
                    if (!peer.getDestinationHash().equals(localDestinationHash))
                        initialPeers.add(peer);
                }
                catch (DataFormatException e) {
                    log.error("Invalid destination key in line " + line, e);
                }
        }
        log.debug(initialPeers.size()-numPeersBefore + " peers read.");
    }
    
    private void sendPeerList(FindClosePeersPacket packet, Destination destination) {
        // TODO don't include the requesting peer
        Collection<KademliaPeer> closestPeers = bucketManager.getClosestPeers(packet.getKey(), KademliaConstants.K);
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
            if (packetToStore != null) {
                DhtStorageHandler storageHandler = storageHandlers.get(packetToStore.getClass());
                if (storageHandler != null)
                    storageHandler.store(packetToStore);
                else
                    log.warn("No storage handler found for type " + packetToStore.getClass().getSimpleName() + ".");
            }
        }
        else if (packet instanceof RetrieveRequest) {
            RetrieveRequest retrieveRequest = (RetrieveRequest)packet;
            DhtStorageHandler storageHandler = storageHandlers.get(retrieveRequest.getDataType());
            if (storageHandler != null) {
                DhtStorablePacket storedPacket = storageHandler.retrieve(retrieveRequest.getKey());
                // if requested packet found, send it to the requester
                if (storedPacket != null) {
                    log.debug("Packet found for retrieve request: [" + retrieveRequest + "], replying to sender: [" + sender + "]");
                    ResponsePacket response = new ResponsePacket(storedPacket, StatusCode.OK, retrieveRequest.getPacketId());
                    sendQueue.send(response, sender);
                }
                else
                    log.debug("No matching packet found for retrieve request: [" + retrieveRequest + "]");
            }
            else
                log.warn("No storage handler found for type " + packet.getClass().getSimpleName() + ".");
        }
        
        // bucketManager is not registered as a PacketListener, so notify it here
        bucketManager.packetReceived(packet, sender, receiveTime);
    }
}
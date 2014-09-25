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

import i2p.bote.Util;
import i2p.bote.network.BanList;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.PacketBatch;
import i2p.bote.network.PacketListener;
import i2p.bote.network.PeerFileAnchor;
import i2p.bote.network.RelayPeer;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.PeerList;
import i2p.bote.packet.relay.PeerListRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;
import net.i2p.util.SecureFileOutputStream;

/**
 * Relay peers are managed independently of the DHT peers because:
 * <ul>
 * <li/>They need to be uniformly distributed across the key space to prevent leaking
 *      information about the local destination key to nodes that could link it to a
 *      local email destination.
 * <li/>We're interested in the highest-uptime peers, regardless of their I2P destination.
 * <li/>Using relay peers for DHT bootstrapping could make it easier for
 *      malicious relay peers to mount a partitioning attack (not 100% sure about this).
 * </ul>
 */
public class RelayPeerManager extends I2PAppThread implements PacketListener {
    private static final int MAX_PEERS = 50;   // maximum number of peers to keep track of
    private static final int MAX_PEERS_TO_SEND = 20;   // maximum number of peers to send in a peer list (the bigger a datagram, the less chance of it getting through)
    private static final int MIN_REACHABILITY = 80;   // percentage of requests sent to a peer / responses received back
    private static final int UPDATE_INTERVAL_SHORT = 2;   // time in minutes between updating peers if no high-reachability peers are known
    private static final int UPDATE_INTERVAL_LONG = 60;   // time in minutes between updating peers if at least one high-reachability peer is known
    
    private Log log = new Log(RelayPeerManager.class);
    private I2PSendQueue sendQueue;
    private Destination localDestination;
    private File peerFile;
    private final Set<RelayPeer> peers;

    public RelayPeerManager(I2PSendQueue sendQueue, Destination localDestination, File peerFile) {
        super("RelayPeerMgr");
        
        this.peerFile = peerFile;
        this.sendQueue = sendQueue;
        this.localDestination = localDestination;
        peers = new HashSet<RelayPeer>();
        
        // Read the updateable peer file if it exists
        if (peerFile.exists()) {
            List<String> receivedPeers = Util.readLines(peerFile);
            addPeers(receivedPeers);
        }
        else
            log.info("Peer file doesn't exist, using built-in peers (File not found: <" + peerFile.getAbsolutePath() + ">)");
        // If no peers have been read, use the built-in peer file
        if (peers.isEmpty()) {
            URL builtInPeerFile = PeerFileAnchor.getBuiltInPeersFile();
            List<String> builtInPeers = Util.readLines(builtInPeerFile);
            addPeers(builtInPeers);
        }
    }
    
    /**
     * Creates peer destinations from a <code>String</code> each, and adds them to <code>peers</code>.
     * @param peerFileEntries A list of <code>String</code>s as they appear in the peer file
     */
    private void addPeers(List<String> peerFileEntries) {
        synchronized(peers) {
            for (String line: peerFileEntries) {
                if (peers.size() >= MAX_PEERS)
                    return;
                if (!line.startsWith("#")) {
                    RelayPeer peer = parsePeerFileEntry(line);
                    if (peer!=null && !localDestination.equals(peer))
                        peers.add(peer);
                }
            }
        }
    }
    
    /**
     * Creates a <code>RelayPeer</code> from an entry of the peer file.
     * An entry is an I2P destination which can (but doesn't have to) be
     * followed by "requests sent" and "responses received" numbers.
     * Returns <code>null</code> if the entry cannot be parsed.
     * @param line
     */
    private RelayPeer parsePeerFileEntry(String line) {
        String[] fields = line.split("\\t");
        
        try {
            Destination destination = new Destination(fields[0]);
            RelayPeer peer = new RelayPeer(destination);
            for (int i=1; i<fields.length; i++) {
                boolean didRespond = Boolean.valueOf(fields[i]);
                peer.addReachabilitySample(didRespond);
            }
            return peer;
        }
        catch (DataFormatException e) {
            log.error("Invalid I2P destination: <" + fields[0] + ">");
            return null;
        }
        catch (NumberFormatException e) {
            log.error("Invalid number in line: <" + line + ">");
            return null;
        }
    }
    
    private void writePeers(File file) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new SecureFileOutputStream(file.getAbsolutePath())));
            writer.write("# Each line is in the format: <dest> [resp1] [resp2] ..."); writer.newLine();
            writer.write("#   dest  = the I2P destination"); writer.newLine();
            writer.write("#   resp* = true or false, depending on whether the peer responded"); writer.newLine();
            writer.write("# The fields are separated by a tab character."); writer.newLine();
            writer.write("# Lines starting with a # are ignored."); writer.newLine();
            writer.write("# Do not edit this file while I2P-Bote is running as it will be overwritten."); writer.newLine();
            for (RelayPeer peer: peers) {
                writer.write(peer.toBase64());
                for (boolean responded: peer.getAllSamples())
                    writer.write("\t" + responded);
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
     * Returns <code>numPeers</code> randomly selected peers with a reachability
     * of <code>MIN_REACHABILITY</code> or higher. If less than <code>numPeers</code>
     * suitable peers are available, this method blocks until there are enough.
     */
    public List<Destination> getRandomPeers(int numPeers) {
        while (!Thread.interrupted()) {
            List<Destination> goodPeers = getGoodPeers();
            if (goodPeers.size() >= numPeers) {
                Collections.shuffle(goodPeers);
                goodPeers = goodPeers.subList(0, numPeers);
                return goodPeers;
            }
            // if not enough peers, wait and try again
            log.debug("Not enough relay peers available, trying again in 1 minute.");
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return Collections.emptyList();   // thread is shutting down
    }

    /** Returns all high-reachability peers */
    private List<Destination> getGoodPeers() {
        List<Destination> goodPeers = new ArrayList<Destination>();
        synchronized(peers) {
            for (RelayPeer peer: peers)
                if (peer.getReachability() > MIN_REACHABILITY)
                    goodPeers.add(peer);
        }
        return goodPeers;
    }
    
    /** Returns up to <code>num</code> high-reachability peers */
    private List<Destination> getGoodPeers(int num) {
        List<Destination> goodPeers = getGoodPeers();
        while (goodPeers.size() > num)
            goodPeers.remove(0);
        return goodPeers;
    }
    
    public Set<RelayPeer> getAllPeers() {
        return peers;
    }
    
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                // ask all peers for their peer lists
                Set<RelayPeer> peersToQuery = new HashSet<RelayPeer>(peers);
                PacketBatch batch = new PacketBatch();
                for (RelayPeer peer: peersToQuery)
                    batch.putPacket(new PeerListRequest(), peer);   // don't reuse request packets because PacketBatch will not add the same one more than once
                sendQueue.send(batch);
                batch.awaitSendCompletion();
                batch.awaitAllResponses(2, TimeUnit.MINUTES);
                
                // build a Set of destinations 
                Set<Destination> responders = batch.getResponses().keySet();
                // update reachability data
                log.debug("Relay peer stats:");
                for (RelayPeer peer: peersToQuery) {   // use peersToQuery (not peers) so new peers that have been added in the meantime are not wrongly considered unresponsive
                    boolean didRespond = responders.contains(peer);
                    peer.addReachabilitySample(didRespond);
                    if (log.shouldLog(Log.DEBUG)) {
                        StringBuilder logMessage = new StringBuilder("  ");
                        logMessage.append(Util.toBase32(peer));
                        logMessage.append(" ");
                        for (boolean responded: peer.getAllSamples())
                            logMessage.append(responded?'*':'.');
                        log.debug(logMessage.toString());
                    }
                }
                
                // make a Set with the new peers
                Set<Destination> receivedPeers = new HashSet<Destination>();
                BanList banList = BanList.getInstance();
                for (DataPacket response: batch.getResponses().values()) {
                    if (!(response instanceof PeerList))
                        continue;
                    PeerList peerList = (PeerList)response;
                    for (Destination peer: peerList.getPeers())
                        if (!banList.isBanned(peer))
                            receivedPeers.add(peer);
                }
                log.debug("Received a total of " + receivedPeers.size() + " relay peers in " + batch.getResponses().size() + " packets.");
                
                // replace low-reachability peers with new peers (a PeerList is supposed to contain only high-reachability peers)
                synchronized(peers) {
                    // add all received peers, then remove low-reachability ones (all of which are existing peers)
                    for (Destination newPeer: receivedPeers)
                        if (!localDestination.equals(newPeer))
                            peers.add(new RelayPeer(newPeer));
                    for (Iterator<RelayPeer> iterator=peers.iterator(); iterator.hasNext();) {
                        if (peers.size() <= MAX_PEERS)
                            break;
                        RelayPeer peer = iterator.next();
                        if (!peer.getAllSamples().isEmpty() && peer.getReachability()<MIN_REACHABILITY)   // don't remove the peer before it has had a chance to respond to a request
                            iterator.remove();
                    }
                }
                log.debug("Number of relay peers is now " + peers.size());
                
                // if no good peers available, ask more often
                if (getGoodPeers().isEmpty())
                    TimeUnit.MINUTES.sleep(UPDATE_INTERVAL_SHORT);
                else
                    TimeUnit.MINUTES.sleep(UPDATE_INTERVAL_LONG);
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in RelayPeerManager loop", e);
            }
        }
        
        writePeers(peerFile);
        log.debug("RelayPeerManager thread exiting.");
    }

    @Override
    public void packetReceived(CommunicationPacket packet, Destination sender, long receiveTime) {
        BanList banList = BanList.getInstance();
        banList.update(sender, packet);
        synchronized(peers) {
            if (banList.isBanned(sender)) {
                peers.remove(sender);
                return;
            }
            
            // respond to PeerListRequests
            if (packet instanceof PeerListRequest) {
                // send up to MAX_PEERS_TO_SEND high-reachability peers minus the sender itself
                List<Destination> peersToSend = new ArrayList<Destination>();
                peersToSend.addAll(getGoodPeers(MAX_PEERS_TO_SEND));
                peersToSend.remove(sender);
                PeerList response = new PeerList(peersToSend);
                log.debug("Sending a PeerList containing " + peersToSend.size() + " peers in response to a PeerListRequest from " + Util.toShortenedBase32(sender));
                sendQueue.sendResponse(response, sender, packet.getPacketId());
            }
            
            // If there are less than MAX_PEERS/2 peers, add the sender (which can be a relay peer or a DHT peer)
            // as a relay peer. The other MAX_PEERS/2 are reserved for peers from PeerListRequests since they are preferrable.
            if (peers.size() < MAX_PEERS/2)
                peers.add(new RelayPeer(sender));
        }
    }
}
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

package i2p.bote.network;

import i2p.bote.Util;
import i2p.bote.packet.CommunicationPacket;
import i2p.bote.packet.I2PBotePacket;
import i2p.bote.packet.MalformedCommunicationPacket;
import i2p.bote.packet.MalformedPacketException;

import java.util.ArrayList;
import java.util.List;

import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.I2PSessionMuxedListener;
import net.i2p.client.datagram.I2PDatagramDissector;
import net.i2p.client.datagram.I2PInvalidDatagramException;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.Log;

/**
 * An {@link I2PSessionMuxedListener} that receives datagrams from the I2P network
 * and notifies {@link PacketListener}s.
 */
public class I2PPacketDispatcher implements I2PSessionMuxedListener {
    private Log log = new Log(I2PPacketDispatcher.class);
    private List<PacketListener> packetListeners;

    public I2PPacketDispatcher() {
        packetListeners = new ArrayList<PacketListener>();
    }
    
    public void addPacketListener(PacketListener listener) {
        synchronized(packetListeners) {
            packetListeners.add(listener);
        }
    }
    
    public void removePacketListener(PacketListener listener) {
        synchronized(packetListeners) {
            packetListeners.remove(listener);
        }
    }
    
    private void firePacketReceivedEvent(CommunicationPacket packet, Destination sender) {
        synchronized(packetListeners) {
            for (PacketListener listener: packetListeners)
                listener.packetReceived(packet, sender, System.currentTimeMillis());
        }
    }
    
    // I2PSessionMuxedListener implementation follows
    
    @Override
    public void reportAbuse(I2PSession session, int severity) {
    }
    
    @Override
    public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromPort, int toPort) {
        if (proto == I2PSession.PROTO_DATAGRAM)
            messageAvailable(session, msgId, size);
    }

    @Override
    public void messageAvailable(I2PSession session, int msgId, long size) {
        byte[] msg = new byte[0];
        try {
            msg = session.receiveMessage(msgId);
        } catch (I2PSessionException e) {
            log.error("Can't get new message from I2PSession.", e);
        }
        if (msg == null) {
            log.error("I2PSession returned a null message: msgId=" + msgId + ", size=" + size + ", " + session);
            return;
        }
        
        I2PDatagramDissector datagramDissector = new I2PDatagramDissector();
        try {
            datagramDissector.loadI2PDatagram(msg);
            datagramDissector.verifySignature();   // TODO keep this line or remove it?
            byte[] payload = datagramDissector.extractPayload();
            Destination sender = datagramDissector.getSender();

            dispatchPacket(payload, sender);
        }
        catch (DataFormatException e) {
            log.error("Invalid datagram received.", e);
        }
        catch (I2PInvalidDatagramException e) {
            log.error("Datagram failed verification.", e);
        }
        catch (Exception e) {
            log.error("Error processing datagram.", e);
        }
    }

    /**
     * Creates a packet from a byte array and fires listeners.
     * @param packetData
     * @param sender
     */
    private void dispatchPacket(byte[] packetData, Destination sender) {
        CommunicationPacket packet;
        try {
            packet = CommunicationPacket.createPacket(packetData);
            logPacket(packet, sender);
            firePacketReceivedEvent(packet, sender);
        } catch (MalformedPacketException e) {
            log.warn("Ignoring unparseable packet.", e);
            firePacketReceivedEvent(new MalformedCommunicationPacket(), sender);
        }
    }
    
    private void logPacket(I2PBotePacket packet, Destination sender) {
        String senderHash = Util.toShortenedBase32(sender);
        log.debug("I2P packet received: [" + packet + "] Sender: [" + senderHash + "], notifying " + packetListeners.size() + " PacketListeners.");
    }
    
    @Override
    public void errorOccurred(I2PSession session, String message, Throwable error) {
        log.error("Router says: " + message, error);
    }
    
    @Override
    public void disconnected(I2PSession session) {
        log.warn("I2P session disconnected.");
    }
}
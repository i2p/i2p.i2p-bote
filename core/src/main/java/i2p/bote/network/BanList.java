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

import static i2p.bote.Util._t;
import i2p.bote.packet.I2PBotePacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.i2p.data.Destination;

public class BanList {
    private static BanList instance;
    private Map<Destination, String> bannedPeers;
    
    public synchronized static BanList getInstance() {
        if (instance == null)
            instance = new BanList();
        return instance;
    }
    
    private BanList() {
        bannedPeers = new ConcurrentHashMap<Destination, String>();
    }
    
    private void ban(Destination destination, String reason) {
        bannedPeers.put(destination, reason);
    }
    
    private void unban(Destination destination) {
        bannedPeers.remove(destination);
    }
    
    public boolean isBanned(Destination destination) {
        return bannedPeers.containsKey(destination);
    }
    
    public String getBanReason(Destination destination) {
        return bannedPeers.get(destination);
    }

    public Collection<BannedPeer> getAll() {
        Collection<BannedPeer> peerCollection = new ArrayList<BannedPeer>();
        for (Entry<Destination, String> entry: bannedPeers.entrySet())
            peerCollection.add(new BannedPeer(entry.getKey(), entry.getValue()));
        return peerCollection;
    }
    
    /**
     * @param peer
     * @param packet A packet received from a peer
     */
    public void update(Destination peer, I2PBotePacket packet) {
        if (packet.isProtocolVersionOk())
            unban(peer);
        else
            ban(peer, _t("Wrong protocol version:") + " " + packet.getProtocolVersion());
    }
}

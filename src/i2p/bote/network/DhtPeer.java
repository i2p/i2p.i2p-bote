package i2p.bote.network;

import net.i2p.data.Destination;

public interface DhtPeer {
    
    Destination getDestination();
    
    long getActiveSince();

    int getStaleCounter();
    
    boolean isBanned();
    
    String getBanReason();
}
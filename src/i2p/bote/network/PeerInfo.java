package i2p.bote.network;

import net.i2p.data.Destination;

public class PeerInfo {
    private Destination destination;
    private long activeSince;
    private int consecutiveTimeouts;
    private boolean banned;
    private String banReason;
    
    public PeerInfo(Destination destination, long activeSince, int consecutiveTimeouts, boolean banned, String banReason) {
        this.destination = destination;
        this.activeSince = activeSince;
        this.consecutiveTimeouts = consecutiveTimeouts;
        this.banned = banned;
        this.banReason = banReason;
    }
    
    public void setDestination(Destination destination) {
        this.destination = destination;
    }
    
    public Destination getDestination() {
        return destination;
    }
    
    public void setActiveSince(long activeSince) {
        this.activeSince = activeSince;
    }

    public long getActiveSince() {
        return activeSince;
    }

    public void setConsecutiveTimeouts(int consecutiveTimeouts) {
        this.consecutiveTimeouts = consecutiveTimeouts;
    }
    
    public int getConsecutiveTimeouts() {
        return consecutiveTimeouts;
    }
    
    public void setBanned(boolean isBanned) {
        this.banned = isBanned;
    }
    
    public boolean isBanned() {
        return banned;
    }
    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }
    
    public String getBanReason() {
        return banReason;
    }
}
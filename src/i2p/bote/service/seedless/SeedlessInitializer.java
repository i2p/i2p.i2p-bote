package i2p.bote.service.seedless;

import java.util.Collection;
import java.util.Collections;

import i2p.bote.network.DhtPeerSource;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.data.Destination;
import net.i2p.util.I2PAppThread;

/**
 * Stubbed-out SeedlessInitializer
 */
public class SeedlessInitializer extends I2PAppThread implements DhtPeerSource {
    public SeedlessInitializer(I2PSocketManager socketManager) {
        super("SeedlessInit");
    }

    @Override
    public void run() {}

    @Override
    public Collection<Destination> getPeers() {
        return Collections.emptyList();
    }
}

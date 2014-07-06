package i2p.bote.service;

import net.i2p.util.I2PAppThread;

import i2p.bote.Configuration;
import i2p.bote.network.NetworkStatusSource;

public class UpdateChecker extends I2PAppThread {
    public UpdateChecker(NetworkStatusSource networkStatusSource, Configuration configuration) {
        super("UpdateCheckr");
    }

    public synchronized boolean isUpdateAvailable() {
        return false;
    }

    @Override
    public void run() {}
}

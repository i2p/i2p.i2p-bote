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

package i2p.bote.service.seedless;

import i2p.bote.I2PBote;
import i2p.bote.network.DhtPeerSource;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.data.Destination;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * Waits for Seedless to become available and then starts the other Seedless
 * background threads (<code>SeedlessAnnounce</code>,
 * <code>SeedlessRequestPeers</code>, <code>SeedlessScrapePeers</code>, and
 * <code>SeedlessScrapeServers</code>).
 * <p/>
 * The only reason why this is an <code>I2PBoteThread</code> rather than just a
 * <code>Runnable</code> is so it can be handled the same way as all the other
 * background threads (see {@link I2PBote}).
 */
public class SeedlessInitializer extends I2PAppThread implements DhtPeerSource {
    private Log log = new Log(SeedlessInitializer.class);
    private I2PSocketManager socketManager;
    private SeedlessAnnounce seedlessAnnounce;
    private SeedlessRequestPeers seedlessRequestPeers;
    private SeedlessScrapePeers seedlessScrapePeers;
    private SeedlessScrapeServers seedlessScrapeServers;
    
    public SeedlessInitializer(I2PSocketManager socketManager) {
        super("SeedlessInit");
        this.socketManager = socketManager;
    }
    
    @Override
    public void run() {
        SeedlessParameters seedlessParameters = SeedlessParameters.getInstance();
        
        while (!Thread.interrupted()) {
            try {
                // the following call may take some time
                if (seedlessParameters.isSeedlessAvailable()) {
                    log.info("Seedless found.");
                    seedlessRequestPeers = new SeedlessRequestPeers(seedlessParameters, 60);
                    seedlessRequestPeers.start();
                    seedlessScrapePeers = new SeedlessScrapePeers(seedlessParameters, 10);
                    seedlessScrapePeers.start();
                    seedlessScrapeServers = new SeedlessScrapeServers(seedlessParameters, 10);
                    seedlessScrapeServers.start();
                    seedlessAnnounce = new SeedlessAnnounce(socketManager, seedlessScrapeServers, 60);
                    seedlessAnnounce.start();
                    break;
                }
                else
                    log.info("Seedless NOT found. Trying again shortly.");
                
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /** Interrupts this and all other Seedless threads */
    @Override
    public void interrupt() {
        if (seedlessAnnounce != null)
            seedlessAnnounce.interrupt();
        if (seedlessRequestPeers != null)
            seedlessRequestPeers.interrupt();
        if (seedlessScrapePeers != null)
            seedlessScrapePeers.interrupt();
        if (seedlessScrapeServers != null)
            seedlessScrapeServers.interrupt();
        super.interrupt();
    }
    
    @Override
    public Collection<Destination> getPeers() {
        if (seedlessScrapePeers == null)
            return Collections.emptyList();
        else
            return seedlessScrapePeers.getPeers();
    }
}
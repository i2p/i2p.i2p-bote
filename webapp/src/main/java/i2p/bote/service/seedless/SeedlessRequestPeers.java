/*
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *                     Version 2, December 2004
 *
 *  Copyright (C) sponge
 *    Planet Earth
 *  Everyone is permitted to copy and distribute verbatim or modified
 *  copies of this license document, and changing it is allowed as long
 *  as the name is changed.
 *
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *   0. You just DO WHAT THE FUCK YOU WANT TO.
 *
 * See...
 *
 * 	http://sam.zoy.org/wtfpl/
 * 	and
 * 	http://en.wikipedia.org/wiki/WTFPL
 *
 * ...for any additional details and license questions.
 */

package i2p.bote.service.seedless;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 *
 * @author sponge
 */
class SeedlessRequestPeers extends I2PAppThread {
    private Log log = new Log(SeedlessRequestPeers.class);
    private SeedlessParameters seedlessParameters;
    private long interval;   // in milliseconds
    private long lastSeedlessRequestPeers = 0;
    private long lastTime;
    private long timeSinceLastCheck;
    
    /**
     *
     * @param interval In minutes
     */
    SeedlessRequestPeers(SeedlessParameters seedlessParameters, int interval) {
        super("SeedlsReqPrs");
        this.seedlessParameters = seedlessParameters;
        this.interval = TimeUnit.MINUTES.toMillis(interval);
    }

    @Override
    public void run() {
        while (!Thread.interrupted())
            try {
                lastTime = lastSeedlessRequestPeers;
                timeSinceLastCheck = System.currentTimeMillis() - lastTime;
                if (lastTime == 0 || timeSinceLastCheck > this.interval) {
                    doSeedlessRequestPeers();
                } else {
                    TimeUnit.MILLISECONDS.sleep(interval - timeSinceLastCheck);
                }
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in SeedlessRequestPeers loop", e);
            }
        
        log.debug("SeedlessRequestPeers thread exiting.");
    }
    
    private synchronized void doSeedlessRequestPeers() {
        HttpURLConnection h;
        log.debug("doSeedlessRequestPeers");
        try {
            ProxyRequest proxy = new ProxyRequest();
            h = proxy.doURLRequest(seedlessParameters.getSeedlessUrl(), seedlessParameters.getPeersRequestHeader(), null, -1, "admin", seedlessParameters.getConsolePassword());
            if(h != null) {
                h.getResponseCode();
            }

        } catch(IOException ex) {
        }
        log.debug("doSeedlessRequestPeers Done.");
        lastSeedlessRequestPeers = System.currentTimeMillis();
    }
}
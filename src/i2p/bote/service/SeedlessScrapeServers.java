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

package i2p.bote.service;

import i2p.bote.I2PBote;
import java.util.concurrent.TimeUnit;

import net.i2p.util.Log;

/**
 *
 * @author sponge
 */
public class SeedlessScrapeServers extends I2PBoteThread {
    private Log log = new Log(SeedlessScrapeServers.class);
    private long interval;   // in milliseconds
    /**
     *
     * @param interval In minutes
     */
    public SeedlessScrapeServers(int interval) {
        super("SeedlessScrapeServers");
        this.interval = TimeUnit.MINUTES.toMillis(interval);
    }

    @Override
    public void run() {
        long lastTime;
        long timeSinceLastCheck;
        while (!shutdownRequested()) {
            I2PBote boteInstance = I2PBote.getInstance();
            lastTime = boteInstance.getlastSeedlessScrapeServers();
            timeSinceLastCheck = System.currentTimeMillis() - lastTime;
            if (lastTime == 0 || timeSinceLastCheck > this.interval) {
                boteInstance.doSeedlessScrapeServers();
            } else {
                awaitShutdownRequest(interval - timeSinceLastCheck, TimeUnit.MILLISECONDS);
            }
            
        }
    }

}

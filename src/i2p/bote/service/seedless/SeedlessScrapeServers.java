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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import net.i2p.data.Base64;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 *
 * @author sponge
 */
class SeedlessScrapeServers extends I2PAppThread {
    private Log log = new Log(SeedlessScrapeServers.class);
    private SeedlessParameters seedlessParameters;
    private long interval;   // in milliseconds
    private long lastSeedlessScrapeServers = 0;
    private List<String> seedlessServers = new ArrayList<String>();
    private long lastTime;
    private long timeSinceLastCheck;
    
    /**
     * @param seedlessParameters
     * @param interval In minutes
     */
    SeedlessScrapeServers(SeedlessParameters seedlessParameters, int interval) {
        super("SeedlsScpSvr");
        this.seedlessParameters = seedlessParameters;
        this.interval = TimeUnit.MINUTES.toMillis(interval);
    }

    @Override
    public void run() {
        while (!Thread.interrupted())
            try {
                lastTime = lastSeedlessScrapeServers;
                timeSinceLastCheck = System.currentTimeMillis() - lastTime;
                if (lastTime == 0 || timeSinceLastCheck > this.interval) {
                    doSeedlessScrapeServers();
                } else {
                    TimeUnit.MILLISECONDS.sleep(interval - timeSinceLastCheck);
                }
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in SeedlessScrapeServers loop", e);
            }
        
        log.debug("SeedlessScrapeServers thread exiting.");
   }

    private synchronized void doSeedlessScrapeServers() {
        HttpURLConnection h;
        int i;
        String foo;
        List<String> metadatas = new ArrayList<String>();
        List<String> ip32s = new ArrayList<String>();
        InputStream in;
        BufferedReader data;
        String line;
        String ip32;

        log.debug("doSeedlessScrapeServers");
        try {
            ProxyRequest proxy = new ProxyRequest();
            h = proxy.doURLRequest(seedlessParameters.getSeedlessUrl(), seedlessParameters.getServersLocateHeader(), null, -1, "admin", seedlessParameters.getConsolePassword());
            if(h != null) {
                i = h.getResponseCode();
                if(i == 200) {
                    in = h.getInputStream();
                    data = new BufferedReader(new InputStreamReader(in));
                    while((line = data.readLine()) != null) {
                        metadatas.add(line);
                    }
                    Iterator<String> it = metadatas.iterator();
                    while(it.hasNext()) {
                        foo = it.next();
                        ip32 = Base64.decodeToString(foo).split(" ")[0];
                        if(!ip32s.contains(ip32)) {
                            ip32s.add(ip32.trim());
                        }
                    }
                }
            }

        } catch(IOException ex) {
        }
        Collections.shuffle(ip32s, new Random());
        seedlessServers = ip32s;
        log.debug("doSeedlessScrapeServers Done");
        lastSeedlessScrapeServers = System.currentTimeMillis();
    }
    
    synchronized List<String> getSeedlessServers() {
        return seedlessServers;
    }
}
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.i2p.I2PAppContext;
import net.i2p.data.Base64;
import net.i2p.data.Destination;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * 
 * @author sponge
 */
class SeedlessScrapePeers extends I2PAppThread {
    private Log log = new Log(SeedlessScrapePeers.class);
    private SeedlessParameters seedlessParameters;
    private long interval;   // in milliseconds
    private long lastSeedlessScrapePeers = 0;
    private List<Destination> peers;
    private long lastTime;
    private long timeSinceLastCheck;
    
    /**
     *
     * @param interval In minutes
     */
    SeedlessScrapePeers(SeedlessParameters seedlessParameters, int interval) {
        super("SeedlsScpPrs");
        this.seedlessParameters = seedlessParameters;
        this.interval = TimeUnit.MINUTES.toMillis(interval);
        peers = new ArrayList<Destination>();
    }

    @Override
    public void run() {
        while (!Thread.interrupted())
            try {
                lastTime = lastSeedlessScrapePeers;
                timeSinceLastCheck = System.currentTimeMillis() - lastTime;
                if (lastTime == 0 || timeSinceLastCheck > this.interval) {
                    doSeedlessScrapePeers();
                } else {
                    TimeUnit.MILLISECONDS.sleep(interval - timeSinceLastCheck);
                }
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in SeedlessScrapePeers loop", e);
            }
        
        log.debug("SeedlessScrapePeers thread exiting.");
    }
    
    private synchronized void doSeedlessScrapePeers() {
        HttpURLConnection h;
        int i;
        String foo;
        List<String> metadatas = new ArrayList<String>();
        List<String> ip32s = new ArrayList<String>();
        InputStream in;
        BufferedReader data;
        String line;
        String ip32;
        log.debug("doSeedlessScrapePeers");

        try {
            ProxyRequest proxy = new ProxyRequest();
            h = proxy.doURLRequest(seedlessParameters.getSeedlessUrl(), seedlessParameters.getPeersLocateHeader(), null, -1, "admin", seedlessParameters.getConsolePassword());
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
                        ip32 = Base64.decodeToString(foo).split(" ")[0].trim();
                        if(!ip32s.contains(ip32)) {
                            ip32s.add(ip32);
                        }
                    }
                }
            }

        } catch(IOException ex) {
        }
        
        for (String b32Peer: ip32s) {
            Destination peer = lookup(b32Peer);
            if (peer != null)
                synchronized(this) {
                    peers.add(peer);
                }
        }
        
        log.debug("doSeedlessScrapePeers Done.");
        lastSeedlessScrapePeers = System.currentTimeMillis();
    }
    
    /** Returns <code>null</code> if the peer was not found. */
    private Destination lookup(String b32Peer) {
        Destination destination = I2PAppContext.getGlobalContext().namingService().lookup(b32Peer);
        if (destination == null)
            log.warn ("Can't find peer in floodfill: " + b32Peer);
        return destination;
    }

    synchronized List<Destination> getPeers() {
        return peers;
    }
}
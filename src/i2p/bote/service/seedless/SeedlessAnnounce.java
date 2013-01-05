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

import i2p.bote.I2PBote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import net.i2p.I2PAppContext;
import net.i2p.I2PException;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 *
 * @author sponge
 */
class SeedlessAnnounce extends I2PAppThread {
    private Log log = new Log(SeedlessAnnounce.class);
    private long interval;   // in milliseconds
    private long lastSeedlessAnnounce = 0;
    private String announceString = "GET /Seedless/seedless HTTP/1.0\r\nX-Seedless: announce " + Base64.encode("i2p-bote X" + I2PBote.PROTOCOL_VERSION + "X") + "\r\n\r\n";
    private I2PSocketManager socketManager;
    private SeedlessScrapeServers seedlessScrapeServers;
    private long lastTime;
    private long timeSinceLastCheck;
    
    /**
     *
     * @param socketManager
     * @param seedlessScrapeServers
     * @param interval In minutes
     */
    SeedlessAnnounce(I2PSocketManager socketManager, SeedlessScrapeServers seedlessScrapeServers, int interval) {
        super("SeedlsAnounc");
        this.socketManager = socketManager;
        this.seedlessScrapeServers = seedlessScrapeServers;
        this.interval = TimeUnit.MINUTES.toMillis(interval);
    }

    @Override
    public void run() {
        while (!Thread.interrupted())
            try {
                lastTime = lastSeedlessAnnounce;
                timeSinceLastCheck = System.currentTimeMillis() - lastTime;
                if (lastTime == 0 || timeSinceLastCheck > this.interval) {
                    doSeedlessAnnounce();
                } else {
                    TimeUnit.MILLISECONDS.sleep(interval - timeSinceLastCheck);
                }
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in SeedlessAnnounce loop", e);
            }
        
        log.debug("SeedlessAnnounce thread exiting.");
    }

    private synchronized void doSeedlessAnnounce() {
        List<String> seedlessServers = seedlessScrapeServers.getSeedlessServers();
        if(seedlessServers.isEmpty()) {
            // try again in a minute.
            log.error("SeedlessServers.isEmpty, will retry shortly.");
            lastSeedlessAnnounce = System.currentTimeMillis() - (interval - TimeUnit.MINUTES.toMillis(1));
            return;
        }
        // Announce to 10 servers.
        // We do this over the i2pSocket.
        int successful = Math.min(10, seedlessServers.size());
        log.debug("Try to announce to " + successful + " Seedless Servers");
        Collections.shuffle(seedlessServers, new Random());
        Iterator<String> it = seedlessServers.iterator();
        String line;
        I2PSocket I2P;
        InputStream Iin;
        OutputStream Iout;
        BufferedReader data;
        Boolean didsomething = false;
        BufferedWriter output;
        while(successful > 0 && it.hasNext()) {
            lastSeedlessAnnounce = System.currentTimeMillis();
            String b32 = it.next();
            Destination dest = null;
            I2P = null;

            try {
                lastSeedlessAnnounce = System.currentTimeMillis();
                // deprecated dest = I2PTunnel.destFromName(b32);
                dest = I2PAppContext.getGlobalContext().namingService().lookup(b32);
                lastSeedlessAnnounce = System.currentTimeMillis();
                if (dest == null) {
                    log.debug("Could not find the destination: <" + b32 + ">");
                    continue;
                }
                line = dest.toBase64();
                dest = new Destination();
                dest.fromBase64(line);
                I2P = socketManager.connect(dest);
                // I2P.setReadTimeout(0); // temp bugfix, this *SHOULD* be the default
                // make readers/writers
                Iin = I2P.getInputStream();
                Iout = I2P.getOutputStream();
                output = new BufferedWriter(new OutputStreamWriter(Iout));
                output.write(announceString);
                output.flush();
                data = new BufferedReader(new InputStreamReader(Iin));
                // Check for success.
                line = data.readLine();
                if(line != null) {
                    if(line.contains(" 200 ")) {
                        log.debug("Announced to " + b32);
                        successful--;
                        didsomething = true;
                    } else {
                        log.debug("Announce to " + b32 + " Failed with Error " + line);
                        log.debug("We sent " + announceString);
                    }
                }
                while((line = data.readLine()) != null) {
                }

            } catch(DataFormatException ex) {
                log.debug("Not base64!", ex);
            } catch(ConnectException ex) {
                log.debug("ConnectException", ex);
            } catch(NoRouteToHostException ex) {
                log.debug("NoRouteToHostException", ex);
            } catch(InterruptedIOException ex) {
                log.debug("InterruptedIOException", ex);
            } catch(IOException ex) {
                log.debug("IOException", ex);
                ex.printStackTrace();
            } catch(I2PException ex) {
                log.debug("I2PException", ex);
            }
            if(I2P != null) {
                try {
                    I2P.close();
                } catch(IOException ex) {
                    // don't care.
                }
            }
        }
        if(!didsomething) {
            // try again in 1 minute.
            lastSeedlessAnnounce = System.currentTimeMillis() - (interval - TimeUnit.MINUTES.toMillis(1));
            return;
        }

        lastSeedlessAnnounce = System.currentTimeMillis();
    }
}
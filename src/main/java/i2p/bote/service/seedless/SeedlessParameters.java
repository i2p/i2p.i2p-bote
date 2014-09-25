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
 *  http://sam.zoy.org/wtfpl/
 *  and
 *  http://en.wikipedia.org/wiki/WTFPL
 *
 * ...for any additional details and license questions.
 */

package i2p.bote.service.seedless;

import i2p.bote.I2PBote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import net.i2p.data.Base64;
import net.i2p.data.Hash;
import net.i2p.router.RouterContext;
import net.i2p.router.startup.ClientAppConfig;
import net.i2p.util.Log;

class SeedlessParameters {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "7657";
    private static SeedlessParameters instance;
    
    private boolean ready;
    private String host;
    private String port;
    private String svcURL;
    private String cpass;
    private String peersReqHeader;
    private String peersLocHeader;
    private String serversLocHeader;
    
    private SeedlessParameters() {
    }
    
    static SeedlessParameters getInstance() {
        if (instance == null)
            instance = new SeedlessParameters();
        return instance;
    }
    
    boolean isSeedlessAvailable() {
        if (!ready)
            init();
        return ready;
    }
    
    private void init() {
        Log log = new Log(SeedlessParameters.class);
        
        /*
         * Of course we can do reflection, but...
         * Reflection is powerful, but should not be used indiscriminately.
         * If it is possible to perform an operation without using reflection,
         * then it is preferable to avoid using it. The following concerns
         * should be kept in mind when accessing code via reflection.
         *
         * http://java.sun.com/docs/books/tutorial/reflect/index.html
         *
         */
        RouterContext _context = ContextHelper.getContext(null);
        String apass = null;
        // 1: Get the console IP:port
        if (_context != null) {
            List<ClientAppConfig> clients = ClientAppConfig.getClientApps(_context);
            for(int cur = 0; cur < clients.size(); cur++) {
                ClientAppConfig ca = clients.get(cur);
    
                if("net.i2p.router.web.RouterConsoleRunner".equals(ca.className)) {
                    port = ca.args.split(" ")[0];
                    host = ca.args.split(" ")[1];
                    if(host.contains(",")) {
                        String checks[] = host.split(",");
                        host = null;
                        for(int h = 0; h < checks.length; h++) {
                            if(!checks[h].contains(":")) {
                                host = checks[h];
                            }
                        }
    
                    }
                }
            }
        }
        if(port == null || host == null) {
            log.error("No router console found, trying default host/port: " + DEFAULT_HOST + ":" + DEFAULT_PORT);
            host = DEFAULT_HOST;
            port = DEFAULT_PORT;
        }
        else {
            ready = false;
        }
        // 2: Get console password
        apass = getPassword();
        log.info("Testing Seedless API");
        // 3: Check for the console API, if it exists, wait 'till it's status is ready.
        // and set the needed settings. Repeat test 10 times with some delay between when it fails.
        String url = "http://" + host + ":" + port + "/SeedlessConsole/";
        String svcurl = url + "Service";
        int tries = 10;
        HttpURLConnection h;
        int i;
        while(tries > 0) {
            try {
                ProxyRequest proxy = new ProxyRequest();
                h = proxy.doURLRequest(url, null, null, -1, "admin", apass);
                if(h != null) {
                    i = h.getResponseCode();
                    if(i == 200) {
                        log.info("Seedless, API says OK");
                        break;
                    }
                }

            } catch(IOException ex) {
            }

            tries--;
        }
        if(tries > 0) {
            // Now wait for it to be ready.
            // but not forever!
            log.info("Waiting for Seedless to become ready...");
            tries = 60; // ~2 minutes.
            String foo;
            while(!ready && tries > 0) {
                tries--;
                try {
                    ProxyRequest proxy = new ProxyRequest();
                    h = proxy.doURLRequest(svcurl, "stat ping!", null, -1, "admin", apass);
                    if(h != null) {
                        i = h.getResponseCode();
                        if(i == 200) {
                            foo = h.getHeaderField("X-Seedless");
                            ready = Boolean.parseBoolean(foo);
                        }
                    }

                } catch(IOException ex) {
                }
                if(!ready) {
                    try {
                        Thread.sleep(2000); // sleep for 2 seconds
                    } catch(InterruptedException ex) {
                        ready = false;
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

            }
        }
        if(ready) {
            svcURL = svcurl;
            cpass = apass;
            peersReqHeader = "scan " + Base64.encode("i2p-bote X" + I2PBote.PROTOCOL_VERSION + "X");
            peersLocHeader = "locate " + Base64.encode("i2p-bote X" + I2PBote.PROTOCOL_VERSION + "X");
            serversLocHeader = "locate " + Base64.encode("seedless i2p-bote");
        }
    }

    // Tobad this isn't public... oh well, I steal it :-)
    private static String getPassword() {
        List<RouterContext> contexts = RouterContext.listContexts();
        if(contexts != null) {
            for(int i = 0; i < contexts.size(); i++) {
                RouterContext ctx = contexts.get(i);
                String password = ctx.getProperty("consolePassword");
                if(password != null) {
                    password = password.trim();
                    if(password.length() > 0) {
                        return password;
                    }
                }
            }
            // no password in any context
            return null;
        } else {
            // no contexts?!
            return null;
        }
    }
    
    String getSeedlessUrl() {
        return svcURL;
    }
    
    String getPeersRequestHeader() {
        return peersReqHeader;
    }
    
    String getPeersLocateHeader() {
        return peersLocHeader;
    }
    
    String getServersLocateHeader() {
        return serversLocHeader;
    }
    
    String getConsolePassword() {
        return cpass;
    }
    
    private static class ContextHelper {

        /** @throws IllegalStateException if no context available */
        public static RouterContext getContext(String contextId) {
            Log log = new Log(ContextHelper.class);
            
            List<RouterContext>contexts = RouterContext.listContexts();
            if((contexts == null) || (contexts.isEmpty())) {
                log.warn("No contexts. This is usually because the router is either starting up or shutting down, " +
                        "or because I2P-Bote is running in a different JVM than the router.");
                return null;
            }
            if((contextId == null) || (contextId.trim().length() <= 0)) {
                return (RouterContext)contexts.get(0);
            }
            for(int i = 0; i < contexts.size(); i++) {
                RouterContext context = (RouterContext)contexts.get(i);
                Hash hash = context.routerHash();
                if(hash == null) {
                    continue;
                }
                if(hash.toBase64().startsWith(contextId)) {
                    return context;
                }
            }
            // not found, so just give them the first we can find
            return (RouterContext)contexts.get(0);
        }
    }
}
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.List;

import net.i2p.data.Base64;
import net.i2p.data.Hash;
import net.i2p.util.Log;

class SeedlessParameters {
    private static final String DEFAULT_ADDR = "localhost:7657";
    private static SeedlessParameters instance;
    
    private boolean ready;
    private String addr;
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
        
        ContextHelper ctx = new ContextHelper(null);
        // 1: Get the console IP:port
        addr = ctx.getConsoleAddress();
        if(addr == null) {
            log.error("No router console found, trying default host/port: " + DEFAULT_ADDR);
            addr = DEFAULT_ADDR;
        }
        else {
            ready = false;
        }
        // 2: Get console password
        String apass = ctx.getConsolePassword();
        log.info("Testing Seedless API");
        // 3: Check for the console API, if it exists, wait 'till it's status is ready.
        // and set the needed settings. Repeat test 10 times with some delay between when it fails.
        String url = "http://" + addr + "/SeedlessConsole/";
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
        Object _context;

        public ContextHelper(String contextId) {
            _context = getContext(contextId);
        }

        private Object getContext(String contextId) {
            Log log = new Log(ContextHelper.class);

            try {
                Class<?> clazz = Class.forName("net.i2p.router.RouterContext");
                Method listContexts = clazz.getDeclaredMethod("listContexts");
                List<Object> contexts = (List<Object>) listContexts.invoke(null);
                if((contexts == null) || (contexts.isEmpty())) {
                    log.warn("No contexts. This is usually because the router is either starting up or shutting down, " +
                            "or because I2P-Bote is running in a different JVM than the router.");
                    return null;
                }
                if((contextId == null) || (contextId.trim().length() <= 0)) {
                    return contexts.get(0);
                }
                for(int i = 0; i < contexts.size(); i++) {
                    Object context = contexts.get(i);
                    Method routerHash = clazz.getDeclaredMethod("routerHash");
                    Hash hash = (Hash) routerHash.invoke(context);
                    if(hash == null) {
                        continue;
                    }
                    if(hash.toBase64().startsWith(contextId)) {
                        return context;
                    }
                }
                // not found, so just give them the first we can find
                return contexts.get(0);
            } catch (ClassNotFoundException e) {
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }

            return null;
        }

        private String getConsoleAddress() {
            String host = null;
            String port = null;
            if (_context != null) {
                try {
                    Class<?> clazz = Class.forName("net.i2p.router.startup.ClientAppConfig");
                    Class<?> clazzRC = Class.forName("net.i2p.router.RouterContext");
                    Method getClientApps = clazz.getDeclaredMethod("getClientApps", clazzRC);
                    List<Object> clients = (List<Object>) getClientApps.invoke(null, _context);
                    for(int cur = 0; cur < clients.size(); cur++) {
                        Object ca = clients.get(cur);
                        Field fClassName = clazz.getField("className");
                        String className = (String) fClassName.get(ca);

                        if("net.i2p.router.web.RouterConsoleRunner".equals(className)) {
                            Field fArgs = clazz.getField("args");
                            String args = (String) fArgs.get(ca);
                            port = args.split(" ")[0];
                            host = args.split(" ")[1];
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
                } catch (ClassNotFoundException e) {
                } catch (NoSuchMethodException e) {
                } catch (NoSuchFieldException e) {
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                }
            }
            return (host != null && port != null) ? host + ":" + port : null;
        }

        // Too bad this isn't public... oh well, I steal it :-)
        private String getConsolePassword() {
            if (_context == null)
                return null;

            String password = null;
            try {
                Class<?> clazz = Class.forName("net.i2p.router.RouterContext");
                Method getProperty = clazz.getDeclaredMethod("getProperty", String.class);
                password = (String) getProperty.invoke(_context, "consolePassword");
                if (password != null) {
                    password = password.trim();
                }
            } catch (ClassNotFoundException e) {
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            return password;
        }
    }
}

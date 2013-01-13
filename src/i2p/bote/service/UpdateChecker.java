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

package i2p.bote.service;

import i2p.bote.Configuration;
import i2p.bote.network.NetworkStatusSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.i2p.I2PAppContext;
import net.i2p.crypto.TrustedUpdate;
import net.i2p.data.DataHelper;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;
import net.i2p.util.PartialEepGet;

/**
 * Periodically checks for an updated version of I2P-Bote.<br/>
 * It may be worth moving this code into the router so other plugins
 * can use it.
 */
public class UpdateChecker extends I2PAppThread {
    private static final int XPI2P_HEADER_LENGTH = 56;
    private static final String PLUGIN_CONFIG_PATH = "plugins/i2pbote/plugin.config";   // relative to the I2P config dir
    
    private Log log = new Log(UpdateChecker.class);
    private NetworkStatusSource networkStatusSource;
    private Configuration configuration;
    private boolean updateAvailable;
    
    /**
     * @param configuration
     */
    public UpdateChecker(NetworkStatusSource networkStatusSource, Configuration configuration) {
        super("UpdateCheckr");
        this.networkStatusSource = networkStatusSource;
        this.configuration = configuration;
    }

    private void checkForUpdate() {
        String eeproxyHost = configuration.getEeproxyHost();
        int eeproxyPort = configuration.getEeproxyPort();
        String updateUrl = configuration.getUpdateUrl();
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        I2PAppContext context = I2PAppContext.getGlobalContext();
        PartialEepGet eepGet = new PartialEepGet(context, eeproxyHost, eeproxyPort, response, updateUrl, XPI2P_HEADER_LENGTH);
        
        boolean success = eepGet.fetch();
        if (!success) {
            log.error("Can't check update URL: " + updateUrl);
            return;
        }

        try {
            String currentVersion = getCurrentVersion(context);
            byte[] responseBytes = response.toByteArray();
            String newVersion = TrustedUpdate.getVersionString(new ByteArrayInputStream(responseBytes));
            if (TrustedUpdate.needsUpdate(currentVersion, newVersion))
                updateAvailable = true;
        }
        catch (IOException e) {
            log.error("Can't compare plugin versions: " + e.getLocalizedMessage());
        }
    }

    /** See <code>i2p.i2p/apps/routerconsole/java/src/net/i2p/router/web/PluginStarter.java</code> */
    private String getCurrentVersion(I2PAppContext context) throws IOException {
        File cfgFile = new File(context.getConfigDir(), PLUGIN_CONFIG_PATH);
        Properties rv = new Properties();
        DataHelper.loadProps(rv, cfgFile);
        return rv.getProperty("version");
    }
    
    public synchronized boolean isUpdateAvailable() {
        return updateAvailable;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                if (!networkStatusSource.isConnected())   // if not connected, use a shorter wait interval
                    TimeUnit.MINUTES.sleep(1);
                else {
                    checkForUpdate();
                    TimeUnit.MINUTES.sleep(configuration.getUpdateCheckInterval());
                }
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in UpdateChecker loop", e);
            }
        }
        
        log.debug("UpdateChecker thread exiting.");
    }
}
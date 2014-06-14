package i2p.bote.android.service;

import net.i2p.android.router.service.IRouterState;
import net.i2p.client.I2PClient;
import net.i2p.data.DataHelper;
import net.i2p.util.OrderedProperties;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import i2p.bote.android.R;

public class Init {
    private final Context ctx;
    private final String myDir;

    public enum RouterChoice {
        INTERNAL,
        ANDROID,
        REMOTE;
    }

    public Init(Context c) {
        ctx = c;
        // This needs to be changed so that we can have an alternative place
        myDir = c.getFilesDir().getAbsolutePath();
    }

    /**
     * Parses settings and prepares the system for starting the Bote service.
     * @return true if we should use the internal router, false otherwise.
     */
    public RouterChoice initialize(IRouterState stateService) {
        // Set up the locations so Router and WorkingDir can find them
        // We do this again here, in the event settings were changed.
        System.setProperty("i2p.dir.base", myDir);
        System.setProperty("i2p.dir.config", myDir);
        System.setProperty("wrapper.logfile", myDir + "/wrapper.log");

        mergeResourceToFile(R.raw.router_config, "router.config", null);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        RouterChoice routerChoice;
        String i2cpHost, i2cpPort;
        if (prefs.getBoolean("i2pbote.router.auto", true)) {
            if (stateService != null) {
                routerChoice = RouterChoice.ANDROID;
                // TODO fetch settings from I2P Android
                i2cpHost = "127.0.0.1";
                i2cpPort = "7654";
            } else {
                routerChoice = RouterChoice.INTERNAL;
                i2cpHost = "internal";
                i2cpPort = "internal";
            }
        } else {
            // Check manual settings
            String which = prefs.getString("i2pbote.router.use", "internal");
            if ("internal".equals(which)) {
                routerChoice = RouterChoice.INTERNAL;
                i2cpHost = "internal";
                i2cpPort = "internal";
            } else if ("android".equals(which)) {
                routerChoice = RouterChoice.ANDROID;
                // TODO fetch settings from I2P Android
                i2cpHost = "127.0.0.1";
                i2cpPort = "7654";
            } else { // Remote router
                routerChoice = RouterChoice.REMOTE;
                i2cpHost = prefs.getString("i2pbote.i2cp.tcp.host", "127.0.0.1");
                i2cpPort = prefs.getString("i2pbote.i2cp.tcp.port", "7654");
            }
        }
        // Set the I2CP host/port
        System.setProperty(I2PClient.PROP_TCP_HOST, i2cpHost);
        System.setProperty(I2PClient.PROP_TCP_PORT, i2cpPort);

        return routerChoice;
    }

    /**
     *  Load defaults from resource,
     *  then add props from settings,
     *  and write back
     *
     *  @param f relative to base dir
     *  @param props local overrides or null
     */
    public void mergeResourceToFile(int resID, String f, Properties overrides) {
        InputStream in = null;
        InputStream fin = null;

        try {
            in = ctx.getResources().openRawResource(resID);
            Properties props = new OrderedProperties();
            try {
                fin = new FileInputStream(new File(myDir, f));
                DataHelper.loadProps(props, fin);
            } catch (IOException ioe) {
            }

            // write in default settings
            DataHelper.loadProps(props, in);

            // override with user settings
            if (overrides != null)
                props.putAll(overrides);
            File path = new File(myDir, f);
            DataHelper.storeProps(props, path);
        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException ioe) {
            }
            if (fin != null) try {
                fin.close();
            } catch (IOException ioe) {
            }
        }
    }
}

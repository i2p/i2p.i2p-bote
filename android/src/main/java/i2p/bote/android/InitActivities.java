package i2p.bote.android;

import android.content.Context;

import java.security.Security;

public class InitActivities {
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private final String myDir;

    public InitActivities(Context c) {
        this(c.getFilesDir().getAbsolutePath());
    }

    public InitActivities(String i2pBaseDir) {
        myDir = i2pBaseDir;
    }

    public void initialize() {
        // Don't initialize twice
        if (System.getProperty("i2pbote.initialized", "false").equals("true"))
            return;

        // Set up the locations so settings can find them
        System.setProperty("i2p.dir.base", myDir);
        System.setProperty("i2p.dir.config", myDir);
        System.setProperty("wrapper.logfile", myDir + "/wrapper.log");

        System.setProperty("i2pbote.initialized", "true");
    }
}

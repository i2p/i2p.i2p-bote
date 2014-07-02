package i2p.bote.android;

import android.content.Context;

public class InitActivities {
    private final Context ctx;
    private final String myDir;

    public InitActivities(Context c) {
        ctx = c;
        // This needs to be changed so that we can have an alternative place
        myDir = c.getFilesDir().getAbsolutePath();
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

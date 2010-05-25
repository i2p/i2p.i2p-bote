package i2p.bote;

import i2p.bote.folder.ExpirationListener;
import i2p.bote.service.I2PBoteThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExpirationThread extends I2PBoteThread {
    private List<ExpirationListener> expirationListeners;

    public ExpirationThread() {
        super("ExpiratnThrd");
        setPriority(MIN_PRIORITY);
        expirationListeners = Collections.synchronizedList(new ArrayList<ExpirationListener>());
    }

    public void addExpirationListener(ExpirationListener listener) {
        expirationListeners.add(listener);
    }
    
    @Override
    public void run() {
        while (!shutdownRequested()) {
            for (ExpirationListener listener: expirationListeners)
                listener.deleteExpired();
            awaitShutdownRequest(1, TimeUnit.DAYS);
        }
    }
}
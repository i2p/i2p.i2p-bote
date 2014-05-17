package i2p.bote.android.service;

import i2p.bote.I2PBote;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BoteService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        I2PBote.getInstance().startUp();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        I2PBote.getInstance().shutDown();
    }
}

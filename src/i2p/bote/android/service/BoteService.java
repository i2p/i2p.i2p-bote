package i2p.bote.android.service;

import java.util.List;

import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.RouterLaunch;
import i2p.bote.I2PBote;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BoteService extends Service {
    boolean mUseInternalRouter;
    RouterContext mRouterContext;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Init from settings
        Init init = new Init(this);
        mUseInternalRouter = init.initialize();
        if (mUseInternalRouter)
            new Thread(new RouterStarter()).start();
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
        if (mUseInternalRouter)
            new Thread(new RouterStopper()).start();
    }

    private class RouterStarter implements Runnable {
        public void run() {
            RouterLaunch.main(null);
            List<RouterContext> contexts = RouterContext.listContexts();
            mRouterContext = contexts.get(0);
            mRouterContext.router().setKillVMOnEnd(false);
        }
    }

    private class RouterStopper implements Runnable {
        public void run() {
            RouterContext ctx = mRouterContext;
            if (ctx != null)
                ctx.router().shutdown(Router.EXIT_HARD);
        }
    }
}

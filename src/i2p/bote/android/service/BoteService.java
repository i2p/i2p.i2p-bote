package i2p.bote.android.service;

import java.util.List;

import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.RouterLaunch;
import i2p.bote.I2PBote;
import i2p.bote.android.service.Init.RouterChoice;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BoteService extends Service {
    public static final String ROUTER_CHOICE = "router_choice";

    RouterChoice mRouterChoice;
    RouterContext mRouterContext;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mRouterChoice = (RouterChoice) intent.getSerializableExtra(ROUTER_CHOICE);
        if (mRouterChoice == RouterChoice.INTERNAL)
            new Thread(new RouterStarter()).start();
        I2PBote.getInstance().startUp();
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        I2PBote.getInstance().shutDown();
        if (mRouterChoice == RouterChoice.INTERNAL)
            new Thread(new RouterStopper()).start();
    }


    //
    // Internal router helpers
    //

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

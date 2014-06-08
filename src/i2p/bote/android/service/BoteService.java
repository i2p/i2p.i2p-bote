package i2p.bote.android.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.mail.MessagingException;

import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.RouterLaunch;
import i2p.bote.I2PBote;
import i2p.bote.android.EmailListActivity;
import i2p.bote.android.R;
import i2p.bote.android.ViewEmailActivity;
import i2p.bote.android.service.Init.RouterChoice;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class BoteService extends Service implements FolderListener {
    public static final String ROUTER_CHOICE = "router_choice";
    public static final int NOTIF_ID_NEW_EMAIL = 80739047;

    RouterChoice mRouterChoice;
    RouterContext mRouterContext;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mRouterChoice = (RouterChoice) intent.getSerializableExtra(ROUTER_CHOICE);
        if (mRouterChoice == RouterChoice.INTERNAL)
            new Thread(new RouterStarter()).start();

        I2PBote.getInstance().startUp();

        I2PBote.getInstance().getInbox().addFolderListener(this);

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        I2PBote.getInstance().getInbox().removeFolderListener(this);

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

    // FolderListener

    @Override
    public void elementAdded(String messageId) {
        notifyUnread();
    }

    @Override
    public void elementUpdated() {
        notifyUnread();
    }

    @Override
    public void elementRemoved(String messageId) {
        notifyUnread();
    }

    private void notifyUnread() {
        NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true);

        try {
            EmailFolder inbox = I2PBote.getInstance().getInbox();
            List<Email> newEmails = BoteHelper.getNewEmails(inbox);
            int numNew = newEmails.size();
            switch (numNew) {
            case 0:
                nm.cancel(NOTIF_ID_NEW_EMAIL);
                return;

            case 1:
                Email email = newEmails.get(0);
                b.setContentTitle(BoteHelper.getNameAndShortDestination(
                        email.getOneFromAddress()));
                b.setContentText(email.getSubject());

                Intent vei = new Intent(this, ViewEmailActivity.class);
                vei.putExtra(ViewEmailActivity.FOLDER_NAME, inbox.getName());
                vei.putExtra(ViewEmailActivity.MESSAGE_ID, email.getMessageID());
                vei.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pvei = PendingIntent.getActivity(this, 0, vei, PendingIntent.FLAG_UPDATE_CURRENT);
                b.setContentIntent(pvei);
                break;

            default:
                b.setContentTitle(getResources().getQuantityString(
                        R.plurals.n_new_emails, numNew, numNew));

                String bigText = "";
                for (Email ne : newEmails) {
                    bigText += BoteHelper.getNameAndShortDestination(
                            ne.getOneFromAddress());
                    bigText += ": " + ne.getSubject() + "\n";
                }
                b.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));

                Intent eli = new Intent(this, EmailListActivity.class);
                eli.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent peli = PendingIntent.getActivity(this, 0, eli, PendingIntent.FLAG_UPDATE_CURRENT);
                b.setContentIntent(peli);
            }
        } catch (PasswordException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        nm.notify(NOTIF_ID_NEW_EMAIL, b.build());
    }
}

package de.danoeh.antennapod.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.gpoddernet.GpodnetService;
import de.danoeh.antennapod.gpoddernet.GpodnetServiceAuthenticationException;
import de.danoeh.antennapod.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.gpoddernet.model.GpodnetSubscriptionChange;
import de.danoeh.antennapod.preferences.GpodnetPreferences;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.NetworkUtils;

import java.util.Date;
import java.util.List;

/**
 * Synchronizes local subscriptions with gpodder.net service. The service should be started with an ACTION_UPLOAD_CHANGES,
 * ACTION_DOWNLOAD_CHANGES or ACTION_SYNC as an action argument. This class also provides static methods for starting the GpodnetSyncService.
 */
public class GpodnetSyncService extends Service {
    private static final String TAG = "GpodnetSyncService";

    private static final long WAIT_INTERVAL = 5000L;

    public static final String ARG_ACTION = "action";

    /**
     * Starts a new upload action. The service will not upload immediately, but wait for a certain amount of time in
     * case any other upload requests occur.
     */
    public static final String ACTION_UPLOAD_CHANGES = "de.danoeh.antennapod.intent.action.upload_changes";
    /**
     * Starts a new download action. The service will download all changes in the subscription list since the last sync.
     * New subscriptions will be added to the database, removed subscriptions will be removed from the database
     */
    public static final String ACTION_DOWNLOAD_CHANGES = "de.danoeh.antennapod.intent.action.download_changes";
    /**
     * Starts a new upload action immediately and a new download action after that.
     */
    public static final String ACTION_SYNC = "de.danoeh.antennapod.intent.action.sync";

    private GpodnetService service;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = (intent != null) ? intent.getStringExtra(ARG_ACTION) : null;
        if (action != null && action.equals(ACTION_UPLOAD_CHANGES)) {
            Log.d(TAG, String.format("Waiting %d milliseconds before uploading changes", WAIT_INTERVAL));

            uploadWaiterThread.restart();
        } else if (action != null && action.equals(ACTION_DOWNLOAD_CHANGES)) {
            new Thread() {
                @Override
                public void run() {
                    downloadChanges();
                }
            }.start();
        } else if (action != null && action.equals(ACTION_SYNC)) {
            new Thread() {
                @Override
                public void run() {
                    uploadChanges();
                    downloadChanges();
                }
            }.start();
        }
        return START_FLAG_REDELIVERY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (AppConfig.DEBUG) Log.d(TAG, "onDestroy");
        uploadWaiterThread.interrupt();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private GpodnetService tryLogin() throws GpodnetServiceException {
        if (service == null) {
            service = new GpodnetService();
            service.authenticate(GpodnetPreferences.getUsername(), GpodnetPreferences.getPassword());
        }
        return service;
    }

    private synchronized void downloadChanges() {
        if (GpodnetPreferences.loggedIn() && NetworkUtils.networkAvailable(GpodnetSyncService.this)) {
            if (AppConfig.DEBUG) Log.d(TAG, "Downloading changes");
            try {
                GpodnetService service = tryLogin();
                GpodnetSubscriptionChange changes = service.getSubscriptionChanges(GpodnetPreferences.getUsername(), GpodnetPreferences.getDeviceID(), GpodnetPreferences.getLastSyncTimestamp());
                if (AppConfig.DEBUG) Log.d(TAG, "Changes " + changes.toString());

                GpodnetPreferences.setLastSyncTimestamp(changes.getTimestamp());
                List<String> subscriptionList = DBReader.getFeedListDownloadUrls(GpodnetSyncService.this);

                for (String downloadUrl : changes.getAdded()) {
                    if (!subscriptionList.contains(downloadUrl)) {
                        Feed feed = new Feed(downloadUrl, new Date());
                        DownloadRequester.getInstance().downloadFeed(GpodnetSyncService.this, feed);
                    }
                }
                for (String downloadUrl : changes.getRemoved()) {
                    DBTasks.removeFeedWithDownloadUrl(GpodnetSyncService.this, downloadUrl);
                }
            } catch (GpodnetServiceException e) {
                e.printStackTrace();
                updateErrorNotification(e);
            } catch (DownloadRequestException e) {
                e.printStackTrace();
            }
        }
        stopSelf();
    }

    private synchronized void uploadChanges() {
        if (GpodnetPreferences.loggedIn() && NetworkUtils.networkAvailable(GpodnetSyncService.this)) {
            try {
                if (AppConfig.DEBUG) Log.d(TAG, "Uploading subscription list");
                GpodnetService service = tryLogin();
                List<String> subscriptions = DBReader.getFeedListDownloadUrls(GpodnetSyncService.this);

                if (AppConfig.DEBUG) Log.d(TAG, "Uploading subscriptions: " + subscriptions.toString());

                service.uploadSubscriptions(GpodnetPreferences.getUsername(), GpodnetPreferences.getDeviceID(), subscriptions);
            } catch (GpodnetServiceException e) {
                e.printStackTrace();
                updateErrorNotification(e);
            }
        }
        stopSelf();
    }

    private void updateErrorNotification(GpodnetServiceException exception) {
        if (AppConfig.DEBUG) Log.d(TAG, "Posting error notification");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        final String title;
        final String description;
        final int id;
        if (exception instanceof GpodnetServiceAuthenticationException) {
            title = getString(R.string.gpodnetsync_auth_error_title);
            description = getString(R.string.gpodnetsync_auth_error_descr);
            id = R.id.notification_gpodnet_sync_autherror;
        } else {
            title = getString(R.string.gpodnetsync_error_title);
            description = getString(R.string.gpodnetsync_error_descr) + exception.getMessage();
            id = R.id.notification_gpodnet_sync_error;
        }
        Notification notification = builder.setContentTitle(title)
                .setContentText(description)
                .setSmallIcon(R.drawable.stat_notify_sync_error)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }

    private WaiterThread uploadWaiterThread = new WaiterThread(WAIT_INTERVAL) {
        @Override
        public void onWaitCompleted() {
            uploadChanges();
        }
    };

    private abstract class WaiterThread {
        private long waitInterval;
        private Thread thread;

        private WaiterThread(long waitInterval) {
            this.waitInterval = waitInterval;
            reinit();
        }

        public abstract void onWaitCompleted();

        public void exec() {
            if (!thread.isAlive()) {
                thread.start();
            }
        }

        private void reinit() {
            if (thread != null && thread.isAlive()) {
                Log.d(TAG, "Interrupting waiter thread");
                thread.interrupt();
            }
            thread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(waitInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!isInterrupted()) {
                        synchronized (this) {
                            onWaitCompleted();
                        }
                    }
                }
            };
        }

        public void restart() {
            reinit();
            exec();
        }

        public void interrupt() {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
    }

    public static void sendActionDownloadIntent(Context context) {
        if (GpodnetPreferences.loggedIn()) {
            Intent intent = new Intent(context, GpodnetSyncService.class);
            intent.putExtra(ARG_ACTION, ACTION_DOWNLOAD_CHANGES);
            context.startService(intent);
        }
    }

    public static void sendActionUploadIntent(Context context) {
        if (GpodnetPreferences.loggedIn()) {
            Intent intent = new Intent(context, GpodnetSyncService.class);
            intent.putExtra(ARG_ACTION, ACTION_UPLOAD_CHANGES);
            context.startService(intent);
        }
    }

    public static void sendSyncIntent(Context context) {
        if (GpodnetPreferences.loggedIn()) {
            Intent intent = new Intent(context, GpodnetSyncService.class);
            intent.putExtra(ARG_ACTION, ACTION_SYNC);
            context.startService(intent);
        }
    }
}

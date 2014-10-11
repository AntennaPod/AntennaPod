package de.danoeh.antennapod.core.asynctask;

import android.app.Activity;
import android.content.*;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides access to the DownloadService's list of items that are currently being downloaded.
 * The DownloadObserver object should be created in the activity's onCreate() method. resume() and pause()
 * should be called in the activity's onResume() and onPause() methods
 */
public class DownloadObserver {
    private static final String TAG = "DownloadObserver";

    /**
     * Time period between update notifications.
     */
    public static final int WAITING_INTERVAL_MS = 3000;

    private volatile Activity activity;
    private final Handler handler;
    private final Callback callback;

    private DownloadService downloadService = null;
    private AtomicBoolean mIsBound = new AtomicBoolean(false);

    private Thread refresherThread;
    private AtomicBoolean refresherThreadRunning = new AtomicBoolean(false);


    /**
     * Creates a new download observer.
     *
     * @param activity Used for registering receivers
     * @param handler  All callback methods are executed on this handler. The handler MUST run on the GUI thread.
     * @param callback Callback methods for posting content updates
     * @throws java.lang.IllegalArgumentException if one of the arguments is null.
     */
    public DownloadObserver(Activity activity, Handler handler, Callback callback) {
        Validate.notNull(activity);
        Validate.notNull(handler);
        Validate.notNull(callback);

        this.activity = activity;
        this.handler = handler;
        this.callback = callback;
    }

    public void onResume() {
        if (BuildConfig.DEBUG) Log.d(TAG, "DownloadObserver resumed");
        activity.registerReceiver(contentChangedReceiver, new IntentFilter(DownloadService.ACTION_DOWNLOADS_CONTENT_CHANGED));
        connectToDownloadService();
    }

    public void onPause() {
        if (BuildConfig.DEBUG) Log.d(TAG, "DownloadObserver paused");
        try {
            activity.unregisterReceiver(contentChangedReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        try {
            activity.unbindService(mConnection);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        stopRefresher();
    }

    private BroadcastReceiver contentChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // reconnect to DownloadService if connection has been closed
            if (downloadService == null) {
                connectToDownloadService();
            }
            callback.onContentChanged();
            startRefresher();
        }
    };

    public interface Callback {
        void onContentChanged();

        void onDownloadDataAvailable(List<Downloader> downloaderList);
    }

    private void connectToDownloadService() {
        activity.bindService(new Intent(activity, DownloadService.class), mConnection, 0);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName className) {
            downloadService = null;
            mIsBound.set(false);
            stopRefresher();
            Log.i(TAG, "Closed connection with DownloadService.");
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadService = ((DownloadService.LocalBinder) service)
                    .getService();
            mIsBound.set(true);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Connection to service established");
            List<Downloader> downloaderList = downloadService.getDownloads();
            if (downloaderList != null && !downloaderList.isEmpty()) {
                callback.onDownloadDataAvailable(downloaderList);
                startRefresher();
            }
        }
    };

    private void stopRefresher() {
        if (refresherThread != null) {
            refresherThread.interrupt();
        }
    }

    private void startRefresher() {
        if (refresherThread == null || refresherThread.isInterrupted()) {
            refresherThread = new Thread(new RefresherThread());
            refresherThread.start();
        }
    }

    private class RefresherThread implements Runnable {

        public void run() {
            refresherThreadRunning.set(true);
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(WAITING_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Refresher thread was interrupted");
                }
                if (mIsBound.get()) {
                    postUpdate();
                }
            }
            refresherThreadRunning.set(false);
        }

        private void postUpdate() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onContentChanged();
                    if (downloadService != null) {
                        List<Downloader> downloaderList = downloadService.getDownloads();
                        if (downloaderList == null || downloaderList.isEmpty()) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });
        }
    }

    public void setActivity(Activity activity) {
        Validate.notNull(activity);
        this.activity = activity;
    }

}


package de.danoeh.antennapod.asynctask;

import android.app.Activity;
import android.content.*;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.service.download.Downloader;

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
    public static final int WAITING_INTERVAL_MS = 1000;

    private final Activity activity;
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
        if (activity == null) throw new IllegalArgumentException("activity = null");
        if (handler == null) throw new IllegalArgumentException("handler = null");
        if (callback == null) throw new IllegalArgumentException("callback = null");

        this.activity = activity;
        this.handler = handler;
        this.callback = callback;
    }

    public void onResume() {
        if (AppConfig.DEBUG) Log.d(TAG, "DownloadObserver resumed");
        activity.registerReceiver(contentChangedReceiver, new IntentFilter(DownloadService.ACTION_DOWNLOADS_CONTENT_CHANGED));
        activity.bindService(new Intent(activity, DownloadService.class), mConnection, 0);
    }

    public void onPause() {
        if (AppConfig.DEBUG) Log.d(TAG, "DownloadObserver paused");
        activity.unregisterReceiver(contentChangedReceiver);
        activity.unbindService(mConnection);
        stopRefresher();
    }

    private BroadcastReceiver contentChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            callback.onContentChanged();
            startRefresher();
        }
    };

    public interface Callback {
        void onContentChanged();

        void onDownloadDataAvailable(List<Downloader> downloaderList);
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
            if (AppConfig.DEBUG)
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
                    List<Downloader> downloaderList = downloadService.getDownloads();
                    if (downloaderList == null || downloaderList.isEmpty()) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
    }

}

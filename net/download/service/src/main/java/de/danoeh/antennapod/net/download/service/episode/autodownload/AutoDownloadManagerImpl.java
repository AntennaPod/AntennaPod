package de.danoeh.antennapod.net.download.service.episode.autodownload;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AutoDownloadManagerImpl extends AutoDownloadManager {
    private static final String TAG = "AutoDownloadManager";

    /**
     * Executor service used by the autodownloadUndownloadedEpisodes method.
     */
    private static final ExecutorService autodownloadExec;

    private static AutomaticDownloadAlgorithm downloadAlgorithm = new AutomaticDownloadAlgorithm();

    static {
        autodownloadExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    /**
     * Looks for non-downloaded episodes in the queue or list of unread items and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     *
     * @param context  Used for accessing the DB.
     * @return A Future that can be used for waiting for the methods completion.
     */
    public Future<?> autodownloadUndownloadedItems(final Context context) {
        Log.d(TAG, "autodownloadUndownloadedItems");
        return autodownloadExec.submit(downloadAlgorithm.autoDownloadUndownloadedItems(context));
    }

    /**
     * Removed downloaded episodes outside of the queue if the episode cache is full. Episodes with a smaller
     * 'playbackCompletionDate'-value will be deleted first.
     * <p/>
     * This method should NOT be executed on the GUI thread.
     *
     * @param context Used for accessing the DB.
     */
    public void performAutoCleanup(final Context context) {
        EpisodeCleanupAlgorithmFactory.build().performCleanup(context);
    }
}

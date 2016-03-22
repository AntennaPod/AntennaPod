package de.danoeh.antennapod.core.storage;

import android.content.Context;

public interface AutomaticDownloadAlgorithm {

    /**
     * Looks for undownloaded episodes and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     *
     * @param context  Used for accessing the DB.
     * @return A Runnable that will be submitted to an ExecutorService.
     */
    Runnable autoDownloadUndownloadedItems(Context context);
}

package de.danoeh.antennapod.core.util;

import android.app.Activity;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;

import java.io.File;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Utility functions for handling storage errors
 */
public class StorageUtils {
    private static final String TAG = "StorageUtils";

    public static boolean storageAvailable() {
        File dir = UserPreferences.getDataFolder(null);
        if (dir != null) {
            return dir.exists() && dir.canRead() && dir.canWrite();
        } else {
            Log.d(TAG, "Storage not available: data folder is null");
            return false;
        }
    }

    /**
     * Checks if external storage is available. If external storage isn't
     * available, the current activity is finsished an an error activity is
     * launched.
     *
     * @param activity the activity which would be finished if no storage is
     *                 available
     * @return true if external storage is available
     */
    public static boolean checkStorageAvailability(Activity activity) {
        boolean storageAvailable = storageAvailable();
        if (!storageAvailable) {
            activity.finish();
            activity.startActivity(ClientConfig.applicationCallbacks.getStorageErrorActivity(activity));
        }
        return storageAvailable;
    }

    /**
     * Get the number of free bytes that are available on the external storage.
     */
    public static long getFreeSpaceAvailable() {
        File dataFolder = UserPreferences.getDataFolder(null);
        if (dataFolder != null) {
            return getFreeSpaceAvailable(dataFolder.getAbsolutePath());
        } else {
            return 0;
        }
    }

    /**
     * Get the number of free bytes that are available on the external storage.
     */
    public static long getFreeSpaceAvailable(String path) {
        StatFs stat = new StatFs(path);
        long availableBlocks;
        long blockSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            availableBlocks = stat.getAvailableBlocksLong();
            blockSize = stat.getBlockSizeLong();
        } else {
            availableBlocks = stat.getAvailableBlocks();
            blockSize = stat.getBlockSize();
        }
        return availableBlocks * blockSize;
    }
}

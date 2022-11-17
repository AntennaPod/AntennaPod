package de.danoeh.antennapod.core.util;

import android.os.StatFs;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.io.File;

/**
 * Utility functions for handling storage errors
 */
public class StorageUtils {
    private StorageUtils(){}

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
        long availableBlocks = stat.getAvailableBlocksLong();
        long blockSize = stat.getBlockSizeLong();
        return availableBlocks * blockSize;
    }

    public static long getTotalSpaceAvailable(String path) {
        StatFs stat = new StatFs(path);
        long blockCount = stat.getBlockCountLong();
        long blockSize = stat.getBlockSizeLong();
        return blockCount * blockSize;
    }
}

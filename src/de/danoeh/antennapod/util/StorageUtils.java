package de.danoeh.antennapod.util;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.StatFs;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.activity.StorageErrorActivity;
import de.danoeh.antennapod.preferences.UserPreferences;

/** Utility functions for handling storage errors */
public class StorageUtils {
	private static final String TAG = "StorageUtils";

	public static boolean storageAvailable(Context context) {
		File dir = UserPreferences.getDataFolder(context, null);
		if (dir != null) {
			return dir.exists() && dir.canRead() && dir.canWrite();
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Storage not available: data folder is null");
			return false;
		}
	}

	/**
	 * Checks if external storage is available. If external storage isn't
	 * available, the current activity is finsished an an error activity is
	 * launched.
	 *
	 * @param activity
	 *            the activity which would be finished if no storage is
	 *            available
	 * @return true if external storage is available
	 */
	public static boolean checkStorageAvailability(Activity activity) {
		boolean storageAvailable = storageAvailable(activity);
		if (!storageAvailable) {
			activity.finish();
			activity.startActivity(new Intent(activity,
					StorageErrorActivity.class));
		}
		return storageAvailable;
	}

	/** Get the number of free bytes that are available on the external storage. */
	public static long getFreeSpaceAvailable() {
		StatFs stat = new StatFs(UserPreferences.getDataFolder(
				PodcastApp.getInstance(), null).getAbsolutePath());
		long availableBlocks = stat.getAvailableBlocks();
		long blockSize = stat.getBlockSize();
		return availableBlocks * blockSize;
	}
}

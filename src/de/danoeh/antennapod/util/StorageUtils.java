package de.danoeh.antennapod.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.os.StatFs;
import de.danoeh.antennapod.activity.StorageErrorActivity;

/** Utility functions for handling storage errors */
public class StorageUtils {
	public static boolean storageAvailable() {
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED);
	}
	
	/**Checks if external storage is available. If external storage isn't
	 * available, the current activity is finsished an an error activity is launched.
	 * @param activity the activity which would be finished if no storage is available
	 * @return true if external storage is available
	 */
	public static boolean checkStorageAvailability(Activity activity) {
		boolean storageAvailable = storageAvailable();
		if (!storageAvailable) {
			activity.finish();
			activity.startActivity(new Intent(activity, StorageErrorActivity.class));
		}
		return storageAvailable;
	}
	
	/** Get the number of free bytes that are available on the external storage. */
	public static long getFreeSpaceAvailable() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long availableBlocks = stat.getAvailableBlocks();
		long blockSize = stat.getBlockSize();
		return availableBlocks * blockSize;
	}
	
	public static boolean externalStorageMounted() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
}

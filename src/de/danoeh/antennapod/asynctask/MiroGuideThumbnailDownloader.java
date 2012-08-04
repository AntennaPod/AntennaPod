package de.danoeh.antennapod.asynctask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.miroguide.model.MiroGuideChannel;
import de.danoeh.antennapod.util.BitmapDecoder;

/** Downlods thumbnails from the MiroGuide and stores them in a DiskLruCache */
public class MiroGuideThumbnailDownloader extends BitmapDecodeWorkerTask {
	private static final String TAG = "MiroGuideThumbnailDownloader";

	private Exception exception;

	private MiroGuideChannel miroChannel;

	public MiroGuideThumbnailDownloader(Handler handler, ImageView target,
			MiroGuideChannel miroChannel, int length) {
		super(handler, target, miroChannel.getThumbnailUrl(), length);
		this.miroChannel = miroChannel;
	}

	@Override
	protected void onPostExecute() {
		if (exception == null) {
			super.onPostExecute();
		} else {
			Log.e(TAG, "Failed to download thumbnail");
		}
	}

	public void run() {
		// Download file to cache folder
		URL url = null;
		try {
			url = new URL(fileUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			endBackgroundTask();
		}
		File destination = new File(PodcastApp.getInstance().getCacheDir(),
				Integer.toString(fileUrl.hashCode()));
		try {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Downloading " + fileUrl);
			URLConnection connection = url.openConnection();
			connection.connect();
			byte inputBuffer[] = new byte[1024];
			BufferedInputStream input = new BufferedInputStream(
					connection.getInputStream());
			FileOutputStream output = new FileOutputStream(destination);

			int count = 0;
			while ((count = input.read(inputBuffer)) != -1) {
				output.write(inputBuffer, 0, count);
			}
			output.close();
			if (AppConfig.DEBUG)
				Log.d(TAG, "MiroGuide thumbnail downloaded");
			// Get a smaller version of the bitmap and store it inside the
			// LRU
			// Cache
			bitmap = BitmapDecoder.decodeBitmap(PREFERRED_LENGTH,
					destination.getPath());
			if (bitmap != null) {
				storeBitmapInCache(bitmap);
			}

		} catch (IOException e) {
			e.printStackTrace();
			miroChannel.setThumbnailUrl(null);
			endBackgroundTask();
		} finally {
			if (destination.exists()) {
				destination.delete();
			}
		}
		endBackgroundTask();
	}

	@Override
	protected boolean tagsMatching(ImageView target) {
		return target.getTag() == miroChannel;
	}
}

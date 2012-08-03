package de.danoeh.antennapod.asynctask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.jakewharton.DiskLruCache;
import com.jakewharton.DiskLruCache.Editor;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.miroguide.model.MiroChannel;
import de.danoeh.antennapod.util.BitmapDecoder;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

/** Downlods thumbnails from the MiroGuide and stores them in a DiskLruCache */
public class MiroGuideThumbnailDownloader extends BitmapDecodeWorkerTask {
	private static final String TAG = "MiroGuideThumbnailDownloader";

	private static final String CACHE_DIR = "miroguide_thumbnails";
	private static final int CACHE_SIZE = 20 * 1024 * 1024;
	private static final int VALUE_SIZE = 500 * 1024;

	private Exception exception;
	
	private MiroChannel miroChannel;

	public MiroGuideThumbnailDownloader(ImageView target,
			MiroChannel miroChannel, int length) {
		super(target, miroChannel.getThumbnailUrl(), length);
		this.miroChannel = miroChannel;
	}

	private static DiskLruCache openThubmnailDiskCache()
			throws NameNotFoundException, IOException {
		Context appContext = PodcastApp.getInstance();
		return DiskLruCache.open(
				appContext.getExternalFilesDir(CACHE_DIR),
				appContext.getPackageManager().getPackageInfo(
						appContext.getPackageName(), 0).versionCode,
				VALUE_SIZE, CACHE_SIZE);
	}

	@Override
	protected Void doInBackground(Void... params) {
		// Download file to cache folder
		URL url = null;
		try {
			url = new URL(fileUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		File destination = new File(PodcastApp.getInstance().getCacheDir(),
				Integer.toString(fileUrl.hashCode()));
		try {
			DiskLruCache diskCache = openThubmnailDiskCache();
			Editor editor = diskCache.edit(fileUrl);
			if (editor != null) {
				HttpURLConnection connection = (HttpURLConnection) url
						.openConnection();
				byte inputBuffer[] = new byte[10 * 1024];
				InputStream input = new BufferedInputStream(
						connection.getInputStream());
				FileOutputStream output = new FileOutputStream(destination);

				int count = 0;
				while ((count = input.read(inputBuffer, 0, 10 * 1024)) != -1) {
					output.write(inputBuffer, 0, count);
				}
				output.close();

				// Get a smaller version of the bitmap and store it inside the
				// LRU
				// Cache
				Bitmap bitmap = BitmapDecoder.decodeBitmap(PREFERRED_LENGTH,
						fileUrl);
				if (bitmap != null) {
					OutputStream imageOut = editor.newOutputStream(0);
					bitmap.compress(Bitmap.CompressFormat.PNG, 80, imageOut);
					editor.commit();
					storeBitmapInCache(bitmap);
				}
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "No editor object available");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (destination.exists()) {
				destination.delete();
			}
		}
		return null;
	}

	@Override
	protected boolean tagsMatching(ImageView target) {
		return target.getTag() == null || target.getTag() == miroChannel;
	}

	@Override
	protected void storeBitmapInCache(Bitmap bitmap) {
		
	}
}

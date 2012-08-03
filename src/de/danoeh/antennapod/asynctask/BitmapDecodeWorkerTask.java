package de.danoeh.antennapod.asynctask;

import java.io.File;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.util.BitmapDecoder;

public abstract class BitmapDecodeWorkerTask extends Thread {

	protected int PREFERRED_LENGTH;

	public static final int LENGTH_BASE_COVER = 200;
	public static final int LENGTH_BASE_THUMBNAIL = 100;

	private static final String TAG = "BitmapDecodeWorkerTask";
	private ImageView target;
	private Bitmap bitmap;
	private Bitmap decodedBitmap;

	protected int baseLength;

	protected String fileUrl;

	private Handler handler;

	public BitmapDecodeWorkerTask(Handler handler, ImageView target,
			String fileUrl, int length) {
		super();
		this.handler = handler;
		this.target = target;
		this.fileUrl = fileUrl;
		this.baseLength = length;
		this.PREFERRED_LENGTH = (int) (length * PodcastApp.getLogicalDensity());
	}

	/**
	 * Should return true if tag of the imageview is still the same it was
	 * before the bitmap was decoded
	 */
	abstract protected boolean tagsMatching(ImageView target);

	protected void onPostExecute() {
		// check if imageview is still supposed to display this image
		if (tagsMatching(target)) {
			target.setImageBitmap(bitmap);
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Not displaying image");
		}
	}

	@Override
	public void run() {
		File f = null;
		if (fileUrl != null) {
			f = new File(fileUrl);
		}
		if (fileUrl != null && f.exists()) {
			bitmap = BitmapDecoder.decodeBitmap(PREFERRED_LENGTH, fileUrl);
			if (bitmap != null) {
				storeBitmapInCache(bitmap);
			} else {
				Log.w(TAG, "Could not load bitmap. Using default image.");
				bitmap = BitmapFactory.decodeResource(target.getResources(),
						R.drawable.default_cover);
			}
			if (AppConfig.DEBUG)
				Log.d(TAG, "Finished loading bitmaps");
		} else {
			onInvalidFileUrl();
		}
		endBackgroundTask();
	}

	protected final void endBackgroundTask() {
		handler.post(new Runnable() {

			@Override
			public void run() {
				onPostExecute();
			}

		});
	}

	protected void onInvalidFileUrl() {
		Log.e(TAG, "FeedImage has no valid file url. Using default image");
		bitmap = BitmapFactory.decodeResource(target.getResources(),
				R.drawable.default_cover);
	}

	protected void storeBitmapInCache(Bitmap bitmap) {
		FeedImageLoader loader = FeedImageLoader.getInstance();
		if (baseLength == LENGTH_BASE_COVER) {
			loader.addBitmapToCoverCache(fileUrl, bitmap);
		} else if (baseLength == LENGTH_BASE_THUMBNAIL) {
			loader.addBitmapToThumbnailCache(fileUrl, bitmap);
		}
	}
}

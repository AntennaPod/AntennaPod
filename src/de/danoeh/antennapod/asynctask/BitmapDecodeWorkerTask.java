package de.danoeh.antennapod.asynctask;

import java.io.File;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.util.BitmapDecoder;

public abstract class BitmapDecodeWorkerTask extends
		AsyncTask<Void, Void, Void> {

	private int PREFERRED_LENGTH;
	
	public static final int LENGTH_BASE_COVER = 200;
	public static final int LENGTH_BASE_THUMBNAIL = 100;

	private static final String TAG = "BitmapDecodeWorkerTask";
	private ImageView target;
	private Bitmap bitmap;
	private Bitmap decodedBitmap;

	protected int baseLength;

	private String fileUrl;

	public BitmapDecodeWorkerTask(ImageView target, String fileUrl, int length) {
		super();
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

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		// check if imageview is still supposed to display this image
		if (tagsMatching(target)) {
			target.setImageBitmap(bitmap);
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Not displaying image");
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	private int calculateSampleSize(int width, int height) {
		int max = Math.max(width, height);
		if (max < PREFERRED_LENGTH) {
			return 1;
		} else {
			// find first sample size where max / sampleSize <
			// PREFERRED_LENGTH
			for (int sampleSize = 1, power = 0;; power++, sampleSize = (int) Math
					.pow(2, power)) {
				int newLength = max / sampleSize;
				if (newLength <= PREFERRED_LENGTH) {
					if (newLength > 0) {
						return sampleSize;
					} else {
						return sampleSize - 1;
					}
				}
			}
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
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
		return null;
	}
	
	protected void onInvalidFileUrl() {
		Log.e(TAG, "FeedImage has no valid file url. Using default image");
		bitmap = BitmapFactory.decodeResource(target.getResources(),
				R.drawable.default_cover);
	}
	
	protected abstract void storeBitmapInCache(Bitmap bitmap);

	@SuppressLint("NewApi")
	public void executeAsync() {
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			execute();
		}
	}
}

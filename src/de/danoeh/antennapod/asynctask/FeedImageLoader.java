package de.danoeh.antennapod.asynctask;

import java.io.File;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.R;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

/** Caches and loads FeedImage bitmaps in the background */
public class FeedImageLoader {
	private static final String TAG = "FeedImageLoader";
	private static FeedImageLoader singleton;

	/**
	 * Stores references to loaded bitmaps. Bitmaps can be accessed by the id of
	 * the FeedImage the bitmap belongs to.
	 */

	final int memClass = ((ActivityManager) PodcastApp.getInstance()
			.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

	// Use 1/8th of the available memory for this memory cache.
	final int cacheSize = 1024 * 1024 * memClass / 8;
	private LruCache<Long, Bitmap> imageCache;

	private FeedImageLoader() {
		Log.d(TAG, "Creating cache with size " + cacheSize);
		imageCache = new LruCache<Long, Bitmap>(cacheSize) {

			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(Long key, Bitmap value) {
				if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 12)
					return value.getByteCount();
				else
					return (value.getRowBytes() * value.getHeight());

			}

		};
	}

	public static FeedImageLoader getInstance() {
		if (singleton == null) {
			singleton = new FeedImageLoader();
		}
		return singleton;
	}

	public void loadBitmap(FeedImage image, ImageView target) {
		if (image != null) {
			Bitmap bitmap = getBitmapFromCache(image.getId());
			if (bitmap != null) {
				target.setImageBitmap(bitmap);
			} else {
				target.setImageResource(R.drawable.default_cover);
				BitmapWorkerTask worker = new BitmapWorkerTask(target);
				if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
					worker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, image);
				} else {
					worker.execute(image);
				}
			}
		} else {
			target.setImageResource(R.drawable.default_cover);
		}
	}

	public void addBitmapToCache(long id, Bitmap bitmap) {
		imageCache.put(id, bitmap);
	}

	public void wipeImageCache() {
		imageCache.evictAll();
	}

	public boolean isInCache(FeedImage image) {
		return imageCache.get(image.getId()) != null;
	}

	public Bitmap getBitmapFromCache(long id) {
		return imageCache.get(id);
	}

	class BitmapWorkerTask extends AsyncTask<FeedImage, Void, Void> {
		/** The preferred width and height of a bitmap. */
		private static final int PREFERRED_LENGTH = 300;

		private static final String TAG = "BitmapWorkerTask";
		private ImageView target;
		private Bitmap bitmap;
		private Bitmap decodedBitmap;

		public BitmapWorkerTask(ImageView target) {
			super();
			this.target = target;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			target.setImageBitmap(bitmap);
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
		protected Void doInBackground(FeedImage... params) {
			File f = null;
			if (params[0].getFile_url() != null) {
				f = new File(params[0].getFile_url());
			}
			if (params[0].getFile_url() != null && f.exists()) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(params[0].getFile_url(), options);
				int sampleSize = calculateSampleSize(options.outWidth,
						options.outHeight);

				options.inJustDecodeBounds = false;
				options.inSampleSize = sampleSize;
				decodedBitmap = BitmapFactory.decodeFile(
						params[0].getFile_url(), options);
				if (decodedBitmap == null) {
					Log.i(TAG,
							"Bitmap could not be decoded in custom sample size. Trying default sample size (path was "
									+ params[0].getFile_url() + ")");
					decodedBitmap = BitmapFactory.decodeFile(params[0]
							.getFile_url());
				}
				bitmap = Bitmap.createScaledBitmap(decodedBitmap,
						PREFERRED_LENGTH, PREFERRED_LENGTH, false);

				addBitmapToCache(params[0].getId(), bitmap);
				Log.d(TAG, "Finished loading bitmaps");
			} else {
				Log.e(TAG,
						"FeedImage has no valid file url. Using default image");
				bitmap = BitmapFactory.decodeResource(target.getResources(),
						R.drawable.default_cover);
				if (params[0].getFile_url() != null
						&& !DownloadRequester.getInstance().isDownloadingFile(
								params[0])) {
					FeedManager.getInstance().notifyInvalidImageFile(
							PodcastApp.getInstance(), params[0]);
				}
			}
			return null;
		}
	}

}

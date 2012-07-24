package de.danoeh.antennapod.asynctask;

import java.io.File;

import de.danoeh.antennapod.AppConfig;
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

	public static final int LENGTH_BASE_COVER = 200;
	public static final int LENGTH_BASE_THUMBNAIL = 100;

	/**
	 * Stores references to loaded bitmaps. Bitmaps can be accessed by the id of
	 * the FeedImage the bitmap belongs to.
	 */

	final int memClass = ((ActivityManager) PodcastApp.getInstance()
			.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

	// Use 1/8th of the available memory for this memory cache.
	final int coverCacheSize = 1024 * 1024 * memClass / 10;
	final int thumbnailCacheSize = 1024 * 1024 * memClass / 6;

	private LruCache<Long, Bitmap> coverCache;
	private LruCache<Long, Bitmap> thumbnailCache;

	private FeedImageLoader() {
		coverCache = new LruCache<Long, Bitmap>(coverCacheSize) {

			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(Long key, Bitmap value) {
				if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 12)
					return value.getByteCount();
				else
					return (value.getRowBytes() * value.getHeight());

			}

		};

		thumbnailCache = new LruCache<Long, Bitmap>(thumbnailCacheSize) {

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

	@SuppressLint("NewApi")
	public void loadCoverBitmap(FeedImage image, ImageView target) {
		if (image != null) {
			Bitmap bitmap = getBitmapFromCoverCache(image.getId());
			if (bitmap != null) {
				target.setImageBitmap(bitmap);
			} else {
				target.setImageResource(R.drawable.default_cover);
				BitmapWorkerTask worker = new BitmapWorkerTask(target, image,
						LENGTH_BASE_COVER);
				if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
					worker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					worker.execute();
				}
			}
		} else {
			target.setImageResource(R.drawable.default_cover);
		}
	}

	@SuppressLint("NewApi")
	public void loadThumbnailBitmap(FeedImage image, ImageView target) {
		if (image != null) {
			Bitmap bitmap = getBitmapFromThumbnailCache(image.getId());
			if (bitmap != null) {
				target.setImageBitmap(bitmap);
			} else {
				target.setImageResource(R.drawable.default_cover);
				BitmapWorkerTask worker = new BitmapWorkerTask(target, image,
						LENGTH_BASE_THUMBNAIL);
				if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
					worker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					worker.execute();
				}
			}
		} else {
			target.setImageResource(R.drawable.default_cover);
		}
	}

	public void wipeImageCache() {
		coverCache.evictAll();
		thumbnailCache.evictAll();
	}

	public boolean isInThumbnailCache(FeedImage image) {
		return thumbnailCache.get(image.getId()) != null;
	}

	public Bitmap getBitmapFromThumbnailCache(long id) {
		return thumbnailCache.get(id);
	}

	public void addBitmapToThumbnailCache(long id, Bitmap bitmap) {
		thumbnailCache.put(id, bitmap);
	}

	public boolean isInCoverCache(FeedImage image) {
		return coverCache.get(image.getId()) != null;
	}

	public Bitmap getBitmapFromCoverCache(long id) {
		return coverCache.get(id);
	}

	public void addBitmapToCoverCache(long id, Bitmap bitmap) {
		coverCache.put(id, bitmap);
	}

	class BitmapWorkerTask extends AsyncTask<Void, Void, Void> {

		private int PREFERRED_LENGTH;

		private static final String TAG = "BitmapWorkerTask";
		private ImageView target;
		private Bitmap bitmap;
		private Bitmap decodedBitmap;
		
		private int baseLength;

		private FeedImage image;

		public BitmapWorkerTask(ImageView target, FeedImage image, int length) {
			super();
			this.target = target;
			this.image = image;
			this.baseLength = length;
			this.PREFERRED_LENGTH = (int) (length * PodcastApp
					.getLogicalDensity());
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			// check if imageview is still supposed to display this image
			if (target.getTag() == null || target.getTag() == image) {
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
			if (image.getFile_url() != null) {
				f = new File(image.getFile_url());
			}
			if (image.getFile_url() != null && f.exists()) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(image.getFile_url(), options);
				int sampleSize = calculateSampleSize(options.outWidth,
						options.outHeight);

				options.inJustDecodeBounds = false;
				options.inSampleSize = sampleSize;
				decodedBitmap = BitmapFactory.decodeFile(image.getFile_url(),
						options);
				if (decodedBitmap == null) {
					Log.i(TAG,
							"Bitmap could not be decoded in custom sample size. Trying default sample size (path was "
									+ image.getFile_url() + ")");
					decodedBitmap = BitmapFactory.decodeFile(image
							.getFile_url());
				}
				if (decodedBitmap != null) {
					bitmap = Bitmap.createScaledBitmap(decodedBitmap,
							PREFERRED_LENGTH, PREFERRED_LENGTH, false);
					if (baseLength == LENGTH_BASE_COVER) {
						addBitmapToCoverCache(image.getId(), bitmap);
					} else if (baseLength == LENGTH_BASE_THUMBNAIL) {
						addBitmapToThumbnailCache(image.getId(), bitmap);
					}
				} else {
					Log.w(TAG, "Could not load bitmap. Using default image.");
					bitmap = BitmapFactory.decodeResource(
							target.getResources(), R.drawable.default_cover);
				}
				if (AppConfig.DEBUG)
					Log.d(TAG, "Finished loading bitmaps");
			} else {
				Log.e(TAG,
						"FeedImage has no valid file url. Using default image");
				bitmap = BitmapFactory.decodeResource(target.getResources(),
						R.drawable.default_cover);
				if (image.getFile_url() != null
						&& !DownloadRequester.getInstance().isDownloadingFile(
								image)) {
					FeedManager.getInstance().notifyInvalidImageFile(
							PodcastApp.getInstance(), image);
				}
			}
			return null;
		}
	}

}

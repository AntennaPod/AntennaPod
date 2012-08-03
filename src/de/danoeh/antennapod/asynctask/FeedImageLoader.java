package de.danoeh.antennapod.asynctask;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.jakewharton.DiskLruCache;
import com.jakewharton.DiskLruCache.Snapshot;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.miroguide.model.MiroChannel;
import de.danoeh.antennapod.storage.DownloadRequester;

/** Caches and loads FeedImage bitmaps in the background */
public class FeedImageLoader {
	private static final String TAG = "FeedImageLoader";
	private static FeedImageLoader singleton;

	public static final int LENGTH_BASE_COVER = 200;
	public static final int LENGTH_BASE_THUMBNAIL = 100;

	private static final String CACHE_DIR = "miroguide_thumbnails";
	private static final int CACHE_SIZE = 20 * 1024 * 1024;
	private static final int VALUE_SIZE = 500 * 1024;

	/**
	 * Stores references to loaded bitmaps. Bitmaps can be accessed by the id of
	 * the FeedImage the bitmap belongs to.
	 */

	final int memClass = ((ActivityManager) PodcastApp.getInstance()
			.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

	// Use 1/8th of the available memory for this memory cache.
	final int coverCacheSize = 1024 * 1024 * memClass / 10;
	final int thumbnailCacheSize = 1024 * 1024 * memClass / 6;

	private LruCache<String, Bitmap> coverCache;
	private LruCache<String, Bitmap> thumbnailCache;

	private FeedImageLoader() {
		coverCache = new LruCache<String, Bitmap>(coverCacheSize) {

			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(String key, Bitmap value) {
				if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 12)
					return value.getByteCount();
				else
					return (value.getRowBytes() * value.getHeight());

			}

		};

		thumbnailCache = new LruCache<String, Bitmap>(thumbnailCacheSize) {

			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(String key, Bitmap value) {
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

	public static DiskLruCache openThubmnailDiskCache() throws IOException {

		Context appContext = PodcastApp.getInstance();
		DiskLruCache cache = null;
		try {
			cache = DiskLruCache.open(
					appContext.getExternalFilesDir(CACHE_DIR),
					appContext.getPackageManager().getPackageInfo(
							appContext.getPackageName(), 0).versionCode,
					VALUE_SIZE, CACHE_SIZE);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return cache;
	}

	public void loadCoverBitmap(FeedImage image, ImageView target) {
		if (image.getFile_url() != null) {
			Bitmap bitmap = getBitmapFromCoverCache(image.getFile_url());
			if (bitmap != null) {
				target.setImageBitmap(bitmap);
			} else {
				target.setImageResource(R.drawable.default_cover);
				FeedImageDecodeWorkerTask worker = new FeedImageDecodeWorkerTask(
						target, image, LENGTH_BASE_COVER);
				worker.executeAsync();
			}
		} else {
			target.setImageResource(R.drawable.default_cover);
		}
	}

	public void loadThumbnailBitmap(FeedImage image, ImageView target) {
		if (image.getFile_url() != null) {
			Bitmap bitmap = getBitmapFromThumbnailCache(image.getFile_url());
			if (bitmap != null) {
				target.setImageBitmap(bitmap);
			} else {
				target.setImageResource(R.drawable.default_cover);
				FeedImageDecodeWorkerTask worker = new FeedImageDecodeWorkerTask(
						target, image, LENGTH_BASE_THUMBNAIL);
				worker.executeAsync();
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
		return thumbnailCache.get(image.getFile_url()) != null;
	}

	public Bitmap getBitmapFromThumbnailCache(String key) {
		return thumbnailCache.get(key);
	}

	public void addBitmapToThumbnailCache(String key, Bitmap bitmap) {
		thumbnailCache.put(key, bitmap);
	}

	public boolean isInCoverCache(FeedImage image) {
		return coverCache.get(image.getFile_url()) != null;
	}

	public Bitmap getBitmapFromCoverCache(String key) {
		return coverCache.get(key);
	}

	public void addBitmapToCoverCache(String key, Bitmap bitmap) {
		coverCache.put(key, bitmap);
	}

	class FeedImageDecodeWorkerTask extends BitmapDecodeWorkerTask {

		private static final String TAG = "FeedImageDecodeWorkerTask";

		protected FeedImage image;

		public FeedImageDecodeWorkerTask(ImageView target, FeedImage image,
				int length) {
			super(target, image.getFile_url(), length);
			this.image = image;
		}

		@Override
		protected boolean tagsMatching(ImageView target) {
			return target.getTag() == null || target.getTag() == image;
		}

		@Override
		protected void onInvalidFileUrl() {
			super.onInvalidFileUrl();
			if (image.getFile_url() != null
					&& !DownloadRequester.getInstance()
							.isDownloadingFile(image)) {
				FeedManager.getInstance().notifyInvalidImageFile(
						PodcastApp.getInstance(), image);
			}

		}

	}

	class MiroGuideDiskCacheLoader extends BitmapDecodeWorkerTask {
		private static final String TAG = "MiroGuideDiskCacheLoader";
		private Exception exception;

		private MiroChannel channel;

		public MiroGuideDiskCacheLoader(ImageView target, MiroChannel channel,
				int length) {
			super(target, channel.getThumbnailUrl(), length);
			this.channel = channel;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				DiskLruCache cache = openThubmnailDiskCache();
				Snapshot snapshot = cache.get(fileUrl);
				storeBitmapInCache(BitmapFactory.decodeStream(snapshot
						.getInputStream(0)));
			} catch (IOException e) {
				e.printStackTrace();
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (exception != null) {
				super.onPostExecute(result);
			} else {
				Log.e(TAG, "Failed to load bitmap from disk cache");
			}
		}

		@Override
		protected boolean tagsMatching(ImageView target) {
			return target.getTag() == null || target.getTag() == channel;
		}

	}

}

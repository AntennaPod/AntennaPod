package de.danoeh.antennapod.asynctask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DownloadRequester;

/** Caches and loads FeedImage bitmaps in the background */
public class FeedImageLoader {
	private static final String TAG = "FeedImageLoader";
	private static FeedImageLoader singleton;

	public static final int IMAGE_TYPE_THUMBNAIL = 0;
	public static final int IMAGE_TYPE_COVER = 1;

	private Handler handler;
	private ExecutorService executor;

	/**
	 * Stores references to loaded bitmaps. Bitmaps can be accessed by the id of
	 * the FeedImage the bitmap belongs to.
	 */

	final int memClass = ((ActivityManager) PodcastApp.getInstance()
			.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

	// Use 1/8th of the available memory for this memory cache.
	final int coverCacheSize = 1024 * 1024 * memClass / 8;
	final int thumbnailCacheSize = 1024 * 1024 * memClass / 8;

	private LruCache<String, CachedBitmap> coverCache;
	private LruCache<String, CachedBitmap> thumbnailCache;

	private FeedImageLoader() {
		handler = new Handler();
		executor = createExecutor();

		coverCache = new LruCache<String, CachedBitmap>(coverCacheSize) {

			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(String key, CachedBitmap value) {
				if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 12)
					return value.getBitmap().getByteCount();
				else
					return (value.getBitmap().getRowBytes() * value.getBitmap()
							.getHeight());

			}

		};

		thumbnailCache = new LruCache<String, CachedBitmap>(thumbnailCacheSize) {

			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(String key, CachedBitmap value) {
				if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 12)
					return value.getBitmap().getByteCount();
				else
					return (value.getBitmap().getRowBytes() * value.getBitmap()
							.getHeight());

			}

		};
	}

	private ExecutorService createExecutor() {
		return Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors() + 1, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});
	}

	public static FeedImageLoader getInstance() {
		if (singleton == null) {
			singleton = new FeedImageLoader();
		}
		return singleton;
	}

	/**
	 * Load a bitmap from the cover cache. If the bitmap is not in the cache, it
	 * will be loaded from the disk. This method should either be called if the
	 * ImageView's size has already been set or inside a Runnable which is
	 * posted to the ImageView's message queue.
	 */
	public void loadCoverBitmap(FeedImage image, ImageView target) {
		loadCoverBitmap(image, target, target.getHeight());
	}

	/**
	 * Load a bitmap from the cover cache. If the bitmap is not in the cache, it
	 * will be loaded from the disk. This method should either be called if the
	 * ImageView's size has already been set or inside a Runnable which is
	 * posted to the ImageView's message queue.
	 */
	public void loadCoverBitmap(FeedImage image, ImageView target, int length) {
		if (image != null && image.getFile_url() != null) {
			CachedBitmap cBitmap = getBitmapFromCoverCache(image.getFile_url());
			if (cBitmap != null && cBitmap.getLength() >= length) {
				target.setImageBitmap(cBitmap.getBitmap());
			} else {
				target.setImageResource(R.drawable.default_cover);
				FeedImageDecodeWorkerTask worker = new FeedImageDecodeWorkerTask(
						handler, target, image, length, IMAGE_TYPE_COVER);
				executor.submit(worker);
			}
		} else {
			target.setImageResource(R.drawable.default_cover);
		}
	}

	/**
	 * Load a bitmap from the thumbnail cache. If the bitmap is not in the
	 * cache, it will be loaded from the disk. This method should either be
	 * called if the ImageView's size has already been set or inside a Runnable
	 * which is posted to the ImageView's message queue.
	 */
	public void loadThumbnailBitmap(FeedImage image, ImageView target) {
		loadThumbnailBitmap(image, target, target.getHeight());
	}

	/**
	 * Load a bitmap from the thumbnail cache. If the bitmap is not in the
	 * cache, it will be loaded from the disk. This method should either be
	 * called if the ImageView's size has already been set or inside a Runnable
	 * which is posted to the ImageView's message queue.
	 */
	public void loadThumbnailBitmap(FeedImage image, ImageView target,
			int length) {
		if (image != null && image.getFile_url() != null) {
			CachedBitmap cBitmap = getBitmapFromThumbnailCache(image.getFile_url());
			if (cBitmap != null && cBitmap.getLength() >= length) {
				target.setImageBitmap(cBitmap.getBitmap());
			} else {
				target.setImageResource(R.drawable.default_cover);
				FeedImageDecodeWorkerTask worker = new FeedImageDecodeWorkerTask(
						handler, target, image, length, IMAGE_TYPE_THUMBNAIL);
				executor.submit(worker);
			}
		} else {
			target.setImageResource(R.drawable.default_cover);
		}
	}

	public void clearExecutorQueue() {
		executor.shutdownNow();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Executor was shut down.");
		executor = createExecutor();

	}

	public void wipeImageCache() {
		coverCache.evictAll();
		thumbnailCache.evictAll();
	}

	public boolean isInThumbnailCache(FeedImage image) {
		return thumbnailCache.get(image.getFile_url()) != null;
	}

	private CachedBitmap getBitmapFromThumbnailCache(String key) {
		return thumbnailCache.get(key);
	}

	public void addBitmapToThumbnailCache(String key, CachedBitmap bitmap) {
		thumbnailCache.put(key, bitmap);
	}

	public boolean isInCoverCache(FeedImage image) {
		return coverCache.get(image.getFile_url()) != null;
	}

	private CachedBitmap getBitmapFromCoverCache(String key) {
		return coverCache.get(key);
	}

	public void addBitmapToCoverCache(String key, CachedBitmap bitmap) {
		coverCache.put(key, bitmap);
	}

	class FeedImageDecodeWorkerTask extends BitmapDecodeWorkerTask {

		private static final String TAG = "FeedImageDecodeWorkerTask";

		protected FeedImage image;

		public FeedImageDecodeWorkerTask(Handler handler, ImageView target,
				FeedImage image, int length, int imageType) {
			super(handler, target, image.getFile_url(), length, imageType);
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

}

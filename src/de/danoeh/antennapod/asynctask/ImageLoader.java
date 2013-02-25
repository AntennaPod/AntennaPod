package de.danoeh.antennapod.asynctask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedImage;

/** Caches and loads FeedImage bitmaps in the background */
public class ImageLoader {
	private static final String TAG = "ImageLoader";
	private static ImageLoader singleton;

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
	final int thumbnailCacheSize = 1024 * 1024 * memClass / 8;

	private LruCache<String, CachedBitmap> coverCache;
	private LruCache<String, CachedBitmap> thumbnailCache;

	private ImageLoader() {
		handler = new Handler();
		executor = createExecutor();

		coverCache = new LruCache<String, CachedBitmap>(1);

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

	public static ImageLoader getInstance() {
		if (singleton == null) {
			singleton = new ImageLoader();
		}
		return singleton;
	}

	/**
	 * Load a bitmap from the cover cache. If the bitmap is not in the cache, it
	 * will be loaded from the disk. This method should either be called if the
	 * ImageView's size has already been set or inside a Runnable which is
	 * posted to the ImageView's message queue.
	 */
	public void loadCoverBitmap(String fileUrl, ImageView target) {
		loadCoverBitmap(fileUrl, target, target.getHeight());
	}

	public void loadCoverBitmap(FeedImage image, ImageView target) {
		loadCoverBitmap((image != null) ? image.getFile_url() : null, target,
				target.getHeight());
	}

	public void loadCoverBitmap(FeedImage image, ImageView target, int length) {
		loadCoverBitmap((image != null) ? image.getFile_url() : null, target,
				length);
	}

	/**
	 * Load a bitmap from the cover cache. If the bitmap is not in the cache, it
	 * will be loaded from the disk. This method should either be called if the
	 * ImageView's size has already been set or inside a Runnable which is
	 * posted to the ImageView's message queue.
	 */
	public void loadCoverBitmap(String fileUrl, ImageView target, int length) {
		final int defaultCoverResource = getDefaultCoverResource(target
				.getContext());

		if (fileUrl != null) {
			CachedBitmap cBitmap = getBitmapFromCoverCache(fileUrl);
			if (cBitmap != null && cBitmap.getLength() >= length) {
				target.setImageBitmap(cBitmap.getBitmap());
			} else {
				target.setImageResource(defaultCoverResource);
				BitmapDecodeWorkerTask worker = new BitmapDecodeWorkerTask(
						handler, target, fileUrl, length, IMAGE_TYPE_COVER);
				executor.submit(worker);
			}
		} else {
			target.setImageResource(defaultCoverResource);
		}
	}

	/**
	 * Load a bitmap from the thumbnail cache. If the bitmap is not in the
	 * cache, it will be loaded from the disk. This method should either be
	 * called if the ImageView's size has already been set or inside a Runnable
	 * which is posted to the ImageView's message queue.
	 */
	public void loadThumbnailBitmap(String fileUrl, ImageView target) {
		loadThumbnailBitmap(fileUrl, target, target.getHeight());
	}

	public void loadThumbnailBitmap(FeedImage image, ImageView target) {
		loadThumbnailBitmap((image != null) ? image.getFile_url() : null,
				target, target.getHeight());
	}

	public void loadThumbnailBitmap(FeedImage image, ImageView target,
			int length) {
		loadThumbnailBitmap((image != null) ? image.getFile_url() : null,
				target, length);
	}

	/**
	 * Load a bitmap from the thumbnail cache. If the bitmap is not in the
	 * cache, it will be loaded from the disk. This method should either be
	 * called if the ImageView's size has already been set or inside a Runnable
	 * which is posted to the ImageView's message queue.
	 */
	public void loadThumbnailBitmap(String fileUrl, ImageView target, int length) {
		final int defaultCoverResource = getDefaultCoverResource(target
				.getContext());

		if (fileUrl != null) {
			CachedBitmap cBitmap = getBitmapFromThumbnailCache(fileUrl);
			if (cBitmap != null && cBitmap.getLength() >= length) {
				target.setImageBitmap(cBitmap.getBitmap());
			} else {
				target.setImageResource(defaultCoverResource);
				BitmapDecodeWorkerTask worker = new BitmapDecodeWorkerTask(
						handler, target, fileUrl, length, IMAGE_TYPE_THUMBNAIL);
				executor.submit(worker);
			}
		} else {
			target.setImageResource(defaultCoverResource);
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

	public boolean isInThumbnailCache(String fileUrl) {
		return thumbnailCache.get(fileUrl) != null;
	}

	private CachedBitmap getBitmapFromThumbnailCache(String key) {
		return thumbnailCache.get(key);
	}

	public void addBitmapToThumbnailCache(String key, CachedBitmap bitmap) {
		thumbnailCache.put(key, bitmap);
	}

	public boolean isInCoverCache(String fileUrl) {
		return coverCache.get(fileUrl) != null;
	}

	private CachedBitmap getBitmapFromCoverCache(String key) {
		return coverCache.get(key);
	}

	public void addBitmapToCoverCache(String key, CachedBitmap bitmap) {
		coverCache.put(key, bitmap);
	}

	private int getDefaultCoverResource(Context context) {
		TypedArray res = context
				.obtainStyledAttributes(new int[] { R.attr.default_cover });
		final int defaultCoverResource = res.getResourceId(0, 0);
		res.recycle();
		return defaultCoverResource;
	}

}

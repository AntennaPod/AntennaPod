package de.danoeh.antennapod.asynctask;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.widget.ImageView;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DownloadRequester;

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

	public void loadCoverBitmap(FeedImage image, ImageView target) {
		if (image != null) {
			Bitmap bitmap = getBitmapFromCoverCache(image.getId());
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
		if (image != null) {
			Bitmap bitmap = getBitmapFromThumbnailCache(image.getId());
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
		protected void storeBitmapInCache(Bitmap bitmap) {
			if (baseLength == LENGTH_BASE_COVER) {
				addBitmapToCoverCache(image.getId(), bitmap);
			} else if (baseLength == LENGTH_BASE_THUMBNAIL) {
				addBitmapToThumbnailCache(image.getId(), bitmap);
			}
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

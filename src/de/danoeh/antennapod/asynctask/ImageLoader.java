package de.danoeh.antennapod.asynctask;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Caches and loads FeedImage bitmaps in the background
 */
public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private static ImageLoader singleton;

    public static final int IMAGE_TYPE_THUMBNAIL = 0;
    public static final int IMAGE_TYPE_COVER = 1;

    /**
     * Used by loadThumbnailBitmap and loadCoverBitmap to denote an ImageView that displays the default image resource.
     * This is the case if the given source to load the image from was null or did not return any image data.
     */
    private static final Object DEFAULT_IMAGE_RESOURCE_TAG = new Object();

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
                .availableProcessors(), new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
    }

    public static synchronized ImageLoader getInstance() {
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
    public void loadCoverBitmap(ImageWorkerTaskResource source, ImageView target) {
        loadCoverBitmap(source, target, target.getHeight());
    }

    /**
     * Load a bitmap from the cover cache. If the bitmap is not in the cache, it
     * will be loaded from the disk. This method should either be called if the
     * ImageView's size has already been set or inside a Runnable which is
     * posted to the ImageView's message queue.
     */
    public void loadCoverBitmap(ImageWorkerTaskResource source,
                                ImageView target, int length) {
        final int defaultCoverResource = getDefaultCoverResource(target
                .getContext());
        final String cacheKey;
        if (source != null && (cacheKey = source.getImageLoaderCacheKey()) != null) {
            final Object currentTag = target.getTag(R.id.imageloader_key);
            if (currentTag == null || !cacheKey.equals(currentTag)) {
                target.setTag(R.id.imageloader_key, cacheKey);
                CachedBitmap cBitmap = getBitmapFromCoverCache(cacheKey);
                if (cBitmap != null && cBitmap.getLength() >= length) {
                    target.setImageBitmap(cBitmap.getBitmap());
                } else {
                    target.setImageResource(defaultCoverResource);
                    BitmapDecodeWorkerTask worker = new BitmapDecodeWorkerTask(
                            handler, target, source, length, IMAGE_TYPE_COVER);
                    executor.submit(worker);
                }
            }
        } else {
            target.setImageResource(defaultCoverResource);
            target.setTag(R.id.imageloader_key, DEFAULT_IMAGE_RESOURCE_TAG);
        }
    }

    /**
     * Load a bitmap from the thumbnail cache. If the bitmap is not in the
     * cache, it will be loaded from the disk. This method should either be
     * called if the ImageView's size has already been set or inside a Runnable
     * which is posted to the ImageView's message queue.
     */
    public void loadThumbnailBitmap(ImageWorkerTaskResource source,
                                    ImageView target) {
        loadThumbnailBitmap(source, target, target.getHeight());
    }

    /**
     * Load a bitmap from the thumbnail cache. If the bitmap is not in the
     * cache, it will be loaded from the disk. This method should either be
     * called if the ImageView's size has already been set or inside a Runnable
     * which is posted to the ImageView's message queue.
     */
    public void loadThumbnailBitmap(ImageWorkerTaskResource source,
                                    ImageView target, int length) {
        final int defaultCoverResource = getDefaultCoverResource(target
                .getContext());
        final String cacheKey;
        if (source != null && (cacheKey = source.getImageLoaderCacheKey()) != null) {
            final Object currentTag = target.getTag(R.id.imageloader_key);
            if (currentTag == null || !cacheKey.equals(currentTag)) {
                target.setTag(R.id.imageloader_key, cacheKey);
                CachedBitmap cBitmap = getBitmapFromThumbnailCache(cacheKey);
                if (cBitmap != null && cBitmap.getLength() >= length) {
                    target.setImageBitmap(cBitmap.getBitmap());
                } else {
                    target.setImageResource(defaultCoverResource);
                    BitmapDecodeWorkerTask worker = new BitmapDecodeWorkerTask(
                            handler, target, source, length, IMAGE_TYPE_THUMBNAIL);
                    executor.submit(worker);
                }
            }
        } else {
            target.setImageResource(defaultCoverResource);
            target.setTag(R.id.imageloader_key, DEFAULT_IMAGE_RESOURCE_TAG);
        }
    }

    public void clearExecutorQueue() {
        executor.shutdownNow();
        if (BuildConfig.DEBUG)
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
        return android.R.color.transparent;
    }

    /**
     * Used by the BitmapDecodeWorker task to retrieve the source of the bitmap.
     */
    public interface ImageWorkerTaskResource {
        /**
         * Opens a new InputStream that can be decoded as a bitmap by the
         * BitmapFactory.
         */
        public InputStream openImageInputStream();

        /**
         * Returns an InputStream that points to the beginning of the image
         * resource. Implementations can either create a new InputStream or
         * reset the existing one, depending on their implementation of
         * openInputStream. If a new InputStream is returned, the one given as a
         * parameter MUST be closed.
         *
         * @param input The input stream that was returned by openImageInputStream()
         */
        public InputStream reopenImageInputStream(InputStream input);

        /**
         * Returns a string that identifies the image resource. Example: file
         * path of an image
         */
        public String getImageLoaderCacheKey();
    }

}

package de.danoeh.antennapod.core.asynctask;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import com.squareup.picasso.Cache;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides access to Picasso instances.
 */
public class PicassoProvider {
    private static final String TAG = "PicassoProvider";

    private static final boolean DEBUG = false;

    private static ExecutorService executorService;
    private static Cache memoryCache;

    private static synchronized ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(3);
        }
        return executorService;
    }

    private static synchronized Cache getMemoryCache(Context context) {
        if (memoryCache == null) {
            memoryCache = new LruCache(context);
        }
        return memoryCache;
    }

    private static volatile boolean picassoSetup = false;

    public static synchronized void setupPicassoInstance(Context appContext) {
        if (picassoSetup) {
            return;
        }
        Picasso picasso = new Picasso.Builder(appContext)
                .indicatorsEnabled(DEBUG)
                .loggingEnabled(DEBUG)
                .downloader(new OkHttpDownloader(appContext))
                .addRequestHandler(new MediaRequestHandler(appContext))
                .executor(getExecutorService())
                .memoryCache(getMemoryCache(appContext))
                .listener(new Picasso.Listener() {
                    @Override
                    public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
                        Log.e(TAG, "Failed to load Uri:" + uri.toString());
                        e.printStackTrace();
                    }
                })
                .build();
        Picasso.setSingletonInstance(picasso);
        picassoSetup = true;
    }

    private static class MediaRequestHandler extends RequestHandler {

        final Context context;

        public MediaRequestHandler(Context context) {
            super();
            this.context = context;
        }

        @Override
        public boolean canHandleRequest(Request data) {
            return StringUtils.equals(data.uri.getScheme(), PicassoImageResource.SCHEME_MEDIA);
        }

        @Override
        public Result load(Request data) throws IOException {
            Bitmap bitmap = null;
            MediaMetadataRetriever mmr = null;
            try {
                mmr = new MediaMetadataRetriever();
                mmr.setDataSource(data.uri.getPath());
                byte[] image = mmr.getEmbeddedPicture();
                if (image != null) {
                    bitmap = decodeStreamFromByteArray(data, image);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to decode image in media file", e);
            } finally {
                if (mmr != null) {
                    mmr.release();
                }
            }

            if (bitmap == null) {
                // check for fallback Uri
                String fallbackParam = data.uri.getQueryParameter(PicassoImageResource.PARAM_FALLBACK);

                if (fallbackParam != null) {
                    Uri fallback = Uri.parse(fallbackParam);
                    bitmap = decodeStreamFromFile(data, fallback);
                }
            }
            return new Result(bitmap, Picasso.LoadedFrom.DISK);

        }

        /* Copied/Adapted from Picasso RequestHandler classes  */

        private Bitmap decodeStreamFromByteArray(Request data, byte[] bytes) throws IOException {

            final BitmapFactory.Options options = createBitmapOptions(data);
            final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            in.mark(0);
            if (requiresInSampleSize(options)) {
                try {
                    BitmapFactory.decodeStream(in, null, options);
                } finally {
                    in.reset();
                }
                calculateInSampleSize(data.targetWidth, data.targetHeight, options, data);
            }
            try {
                return BitmapFactory.decodeStream(in, null, options);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        private Bitmap decodeStreamFromFile(Request data, Uri uri) throws IOException {
            ContentResolver contentResolver = context.getContentResolver();
            final BitmapFactory.Options options = createBitmapOptions(data);
            if (requiresInSampleSize(options)) {
                InputStream is = null;
                try {
                    is = contentResolver.openInputStream(uri);
                    BitmapFactory.decodeStream(is, null, options);
                } finally {
                    IOUtils.closeQuietly(is);
                }
                calculateInSampleSize(data.targetWidth, data.targetHeight, options, data);
            }
            InputStream is = contentResolver.openInputStream(uri);
            try {
                return BitmapFactory.decodeStream(is, null, options);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        private BitmapFactory.Options createBitmapOptions(Request data) {
            final boolean justBounds = data.hasSize();
            final boolean hasConfig = data.config != null;
            BitmapFactory.Options options = null;
            if (justBounds || hasConfig) {
                options = new BitmapFactory.Options();
                options.inJustDecodeBounds = justBounds;
                if (hasConfig) {
                    options.inPreferredConfig = data.config;
                }
            }
            return options;
        }

        private static boolean requiresInSampleSize(BitmapFactory.Options options) {
            return options != null && options.inJustDecodeBounds;
        }

        private static void calculateInSampleSize(int reqWidth, int reqHeight, BitmapFactory.Options options,
                                                  Request request) {
            calculateInSampleSize(reqWidth, reqHeight, options.outWidth, options.outHeight, options,
                    request);
        }

        private static void calculateInSampleSize(int reqWidth, int reqHeight, int width, int height,
                                                  BitmapFactory.Options options, Request request) {
            int sampleSize = 1;
            if (height > reqHeight || width > reqWidth) {
                final int heightRatio;
                final int widthRatio;
                if (reqHeight == 0) {
                    sampleSize = (int) Math.floor((float) width / (float) reqWidth);
                } else if (reqWidth == 0) {
                    sampleSize = (int) Math.floor((float) height / (float) reqHeight);
                } else {
                    heightRatio = (int) Math.floor((float) height / (float) reqHeight);
                    widthRatio = (int) Math.floor((float) width / (float) reqWidth);
                    sampleSize = request.centerInside
                            ? Math.max(heightRatio, widthRatio)
                            : Math.min(heightRatio, widthRatio);
                }
            }
            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;
        }
    }
}

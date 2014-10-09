package de.danoeh.antennapod.asynctask;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    private static Picasso defaultPicassoInstance;
    private static Picasso mediaMetadataPicassoInstance;

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

    /**
     * Returns a Picasso instance that uses an OkHttpDownloader. This instance can only load images
     * from image files.
     * <p/>
     * This instance should be used as long as no images from media files are loaded.
     */
    public static synchronized Picasso getDefaultPicassoInstance(Context context) {
        Validate.notNull(context);
        if (defaultPicassoInstance == null) {
            defaultPicassoInstance = new Picasso.Builder(context)
                    .indicatorsEnabled(DEBUG)
                    .loggingEnabled(DEBUG)
                    .downloader(new OkHttpDownloader(context))
                    .executor(getExecutorService())
                    .memoryCache(getMemoryCache(context))
                    .listener(new Picasso.Listener() {
                        @Override
                        public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
                            Log.e(TAG, "Failed to load Uri:" + uri.toString());
                            e.printStackTrace();
                        }
                    })
                    .build();
        }
        return defaultPicassoInstance;
    }

    /**
     * Returns a Picasso instance that uses a MediaMetadataRetriever if the given Uri is a media file
     * and a default OkHttpDownloader otherwise.
     */
    public static synchronized Picasso getMediaMetadataPicassoInstance(Context context) {
        Validate.notNull(context);
        if (mediaMetadataPicassoInstance == null) {
            mediaMetadataPicassoInstance = new Picasso.Builder(context)
                    .indicatorsEnabled(DEBUG)
                    .loggingEnabled(DEBUG)
                    .downloader(new MediaMetadataDownloader(context))
                    .executor(getExecutorService())
                    .memoryCache(getMemoryCache(context))
                    .listener(new Picasso.Listener() {
                        @Override
                        public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
                            Log.e(TAG, "Failed to load Uri:" + uri.toString());
                            e.printStackTrace();
                        }
                    })
                    .build();
        }
        return mediaMetadataPicassoInstance;
    }

    private static class MediaMetadataDownloader implements Downloader {

        private static final String TAG = "MediaMetadataDownloader";

        private final OkHttpDownloader okHttpDownloader;

        public MediaMetadataDownloader(Context context) {
            Validate.notNull(context);
            okHttpDownloader = new OkHttpDownloader(context);
        }

        @Override
        public Response load(Uri uri, boolean b) throws IOException {
            if (StringUtils.equals(uri.getScheme(), PicassoImageResource.SCHEME_MEDIA)) {
                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(uri.getLastPathSegment()));
                if (StringUtils.startsWith(type, "image")) {
                    File imageFile = new File(uri.toString());
                    return new Response(new BufferedInputStream(new FileInputStream(imageFile)), true, imageFile.length());
                } else {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(uri.getPath());
                    byte[] data = mmr.getEmbeddedPicture();
                    mmr.release();

                    if (data != null) {
                        return new Response(new ByteArrayInputStream(data), true, data.length);
                    } else {

                        // check for fallback Uri
                        String fallbackParam = uri.getQueryParameter(PicassoImageResource.PARAM_FALLBACK);

                        if (fallbackParam != null) {
                            String fallback = Uri.decode(Uri.parse(fallbackParam).getPath());
                            if (fallback != null) {
                                File imageFile = new File(fallback);
                                return new Response(new BufferedInputStream(new FileInputStream(imageFile)), true, imageFile.length());
                            }
                        }
                        return null;
                    }
                }
            }
            return okHttpDownloader.load(uri, b);
        }
    }
}

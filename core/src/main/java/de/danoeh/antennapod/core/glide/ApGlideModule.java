package de.danoeh.antennapod.core.glide;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.module.AppGlideModule;

import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import java.io.InputStream;

import com.bumptech.glide.request.RequestOptions;
import java.nio.ByteBuffer;

/**
 * {@see com.bumptech.glide.integration.okhttp.OkHttpGlideModule}
 */
@GlideModule
public class ApGlideModule extends AppGlideModule {
    private static final String TAG = "ApGlideModule";
    private static final long MEGABYTES = 1024 * 1024;
    private static final long GIGABYTES = 1024 * 1024 * 1024;

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setDefaultRequestOptions(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888));
        @SuppressLint("UsableSpace")
        long spaceAvailable = context.getCacheDir().getUsableSpace();
        long imageCacheSize = (spaceAvailable > 2 * GIGABYTES) ? (250 * MEGABYTES) : (50 * MEGABYTES);
        Log.d(TAG, "Free space on cache dir: " + spaceAvailable + ", using image cache size: " + imageCacheSize);
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, imageCacheSize));
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.replace(String.class, InputStream.class, new MetadataRetrieverLoader.Factory(context));
        registry.append(String.class, InputStream.class, new GenerativePlaceholderImageModelLoader.Factory());
        registry.append(String.class, InputStream.class, new ApOkHttpUrlLoader.Factory());
        registry.append(String.class, InputStream.class, new NoHttpStringLoader.StreamFactory());

        registry.append(EmbeddedChapterImage.class, ByteBuffer.class, new ChapterImageModelLoader.Factory());
        registry.register(Bitmap.class, PaletteBitmap.class, new PaletteBitmapTranscoder());
    }
}

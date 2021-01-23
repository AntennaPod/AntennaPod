package de.danoeh.antennapod.core.glide;

import android.content.Context;

import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.model.StringLoader;
import com.bumptech.glide.module.AppGlideModule;

import de.danoeh.antennapod.core.util.EmbeddedChapterImage;
import java.io.InputStream;

import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import java.nio.ByteBuffer;

/**
 * {@see com.bumptech.glide.integration.okhttp.OkHttpGlideModule}
 */
@GlideModule
public class ApGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setDefaultRequestOptions(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888));
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context,
                UserPreferences.getImageCacheSize()));
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.replace(String.class, InputStream.class, new MetadataRetrieverLoader.Factory(context));
        registry.append(String.class, InputStream.class, new ApOkHttpUrlLoader.Factory());
        registry.append(String.class, InputStream.class, new StringLoader.StreamFactory());

        registry.append(EmbeddedChapterImage.class, ByteBuffer.class, new ChapterImageModelLoader.Factory());
    }
}

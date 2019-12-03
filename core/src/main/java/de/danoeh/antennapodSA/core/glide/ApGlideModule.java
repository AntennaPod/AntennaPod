package de.danoeh.antennapodSA.core.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

import java.io.InputStream;

import de.danoeh.antennapodSA.core.preferences.UserPreferences;

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
        registry.replace(String.class, InputStream.class, new ApOkHttpUrlLoader.Factory());
    }
}

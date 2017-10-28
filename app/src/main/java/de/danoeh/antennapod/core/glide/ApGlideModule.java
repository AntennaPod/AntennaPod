package de.danoeh.antennapod.core.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.module.GlideModule;

import java.io.InputStream;

import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * {@see com.bumptech.glide.integration.okhttp.OkHttpGlideModule}
 */
public class ApGlideModule implements GlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context,
                UserPreferences.getImageCacheSize()));
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(String.class, InputStream.class, new ApOkHttpUrlLoader.Factory());
    }

}

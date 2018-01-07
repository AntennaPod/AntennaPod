package de.danoeh.antennapod.adapter;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.glide.ApGlideSettings;

class CoverTarget extends GlideDrawableImageViewTarget {

    private final WeakReference<String> fallback;
    private final WeakReference<TextView> placeholder;
    private final WeakReference<ImageView> cover;
    private final WeakReference<MainActivity> mainActivity;

    public CoverTarget(String fallbackUri, TextView txtvPlaceholder, ImageView imgvCover, MainActivity activity) {
        super(imgvCover);
        fallback = new WeakReference<>(fallbackUri);
        placeholder = new WeakReference<>(txtvPlaceholder);
        cover = new WeakReference<>(imgvCover);
        mainActivity = new WeakReference<>(activity);
    }

    @Override
    public void onLoadFailed(Exception e, Drawable errorDrawable) {
        String fallbackUri = fallback.get();
        TextView txtvPlaceholder = placeholder.get();
        ImageView imgvCover = cover.get();
        if (fallbackUri != null && txtvPlaceholder != null && imgvCover != null) {
            MainActivity activity = mainActivity.get();
            Glide.with(activity)
                    .load(fallbackUri)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .fitCenter()
                    .dontAnimate()
                    .into(new CoverTarget(null, txtvPlaceholder, imgvCover, activity));
        }
    }

    @Override
    public void onResourceReady(GlideDrawable drawable, GlideAnimation<? super GlideDrawable> anim) {
        super.onResourceReady(drawable, anim);
        TextView txtvPlaceholder = placeholder.get();
        if (txtvPlaceholder != null) {
            txtvPlaceholder.setVisibility(View.INVISIBLE);
        }
    }
}

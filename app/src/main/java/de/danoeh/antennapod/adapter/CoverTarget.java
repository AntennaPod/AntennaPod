package de.danoeh.antennapod.adapter;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;

import java.lang.ref.WeakReference;

import com.bumptech.glide.request.transition.Transition;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.glide.ApGlideSettings;

class CoverTarget extends CustomViewTarget<ImageView, Drawable> {

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
    public void onLoadFailed(Drawable errorDrawable) {
        String fallbackUri = fallback.get();
        TextView txtvPlaceholder = placeholder.get();
        ImageView imgvCover = cover.get();
        if (fallbackUri != null && txtvPlaceholder != null && imgvCover != null) {
            MainActivity activity = mainActivity.get();
            Glide.with(activity)
                    .load(fallbackUri)
                    .apply(new RequestOptions()
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                    .into(new CoverTarget(null, txtvPlaceholder, imgvCover, activity));
        }
    }

    @Override
    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
        TextView txtvPlaceholder = placeholder.get();
        if (txtvPlaceholder != null) {
            txtvPlaceholder.setVisibility(View.INVISIBLE);
        }
        ImageView ivCover = cover.get();
        ivCover.setImageDrawable(resource);
    }

    @Override
    protected void onResourceCleared(@Nullable Drawable placeholder) {
        ImageView ivCover = cover.get();
        ivCover.setImageDrawable(placeholder);
    }
}

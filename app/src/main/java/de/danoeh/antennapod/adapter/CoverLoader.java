package de.danoeh.antennapod.adapter;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;

import java.lang.ref.WeakReference;

import com.bumptech.glide.request.transition.Transition;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.glide.ApGlideSettings;

public class CoverLoader {
    private String uri;
    private String fallbackUri;
    private TextView txtvPlaceholder;
    private ImageView imgvCover;
    private boolean textAndImageCombined;
    private MainActivity activity;

    public CoverLoader(MainActivity activity) {
        this.activity = activity;
    }

    public CoverLoader withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public CoverLoader withFallbackUri(String uri) {
        fallbackUri = uri;
        return this;
    }

    public CoverLoader withCoverView(ImageView coverView) {
        imgvCover = coverView;
        return this;
    }

    public CoverLoader withPlaceholderView(TextView placeholderView) {
        txtvPlaceholder = placeholderView;
        return this;
    }

    /**
     * Set cover text and if it should be shown even if there is a cover image.
     *
     * @param placeholderView      Cover text.
     * @param textAndImageCombined Show cover text even if there is a cover image?
     */
    @NonNull
    public CoverLoader withPlaceholderView(@NonNull TextView placeholderView, boolean textAndImageCombined) {
        this.txtvPlaceholder = placeholderView;
        this.textAndImageCombined = textAndImageCombined;
        return this;
    }

    public void load() {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate();

        RequestBuilder<Drawable> builder = Glide.with(activity)
                .load(uri)
                .apply(options);

        if (fallbackUri != null && txtvPlaceholder != null && imgvCover != null) {
            builder = builder.error(Glide.with(activity)
                    .load(fallbackUri)
                    .apply(options));
        }

        builder.into(new CoverTarget(txtvPlaceholder, imgvCover, textAndImageCombined));
    }

    static class CoverTarget extends CustomViewTarget<ImageView, Drawable> {
        private final WeakReference<TextView> placeholder;
        private final WeakReference<ImageView> cover;
        private boolean textAndImageCombined;

        public CoverTarget(TextView txtvPlaceholder, ImageView imgvCover, boolean textAndImageCombined) {
            super(imgvCover);
            if (txtvPlaceholder != null) {
                txtvPlaceholder.setVisibility(View.VISIBLE);
            }
            placeholder = new WeakReference<>(txtvPlaceholder);
            cover = new WeakReference<>(imgvCover);
            this.textAndImageCombined = textAndImageCombined;
        }

        @Override
        public void onLoadFailed(Drawable errorDrawable) {

        }

        @Override
        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
            TextView txtvPlaceholder = placeholder.get();
            if (txtvPlaceholder != null) {
                if (textAndImageCombined) {
                    int bgColor = txtvPlaceholder.getContext().getResources().getColor(R.color.feed_text_bg);
                    txtvPlaceholder.setBackgroundColor(bgColor);
                } else {
                    txtvPlaceholder.setVisibility(View.INVISIBLE);
                }
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
}
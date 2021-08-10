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
    private int resource = 0;
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

    public CoverLoader withResource(int resource) {
        this.resource = resource;
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
        CoverTarget coverTarget = new CoverTarget(txtvPlaceholder, imgvCover, textAndImageCombined);

        if (resource != 0) {
            Glide.with(activity).clear(coverTarget);
            imgvCover.setImageResource(resource);
            CoverTarget.setPlaceholderVisibility(txtvPlaceholder, textAndImageCombined);
            return;
        }

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

        builder.into(coverTarget);
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
            setPlaceholderVisibility(placeholder.get(), textAndImageCombined);
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(resource);
        }

        @Override
        protected void onResourceCleared(@Nullable Drawable placeholder) {
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(placeholder);
        }

        static void setPlaceholderVisibility(TextView placeholder, boolean textAndImageCombined) {
            if (placeholder != null) {
                if (textAndImageCombined) {
                    int bgColor = placeholder.getContext().getResources().getColor(R.color.feed_text_bg);
                    placeholder.setBackgroundColor(bgColor);
                } else {
                    placeholder.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
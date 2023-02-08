package de.danoeh.antennapod.adapter;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.lang.ref.WeakReference;

public class CoverLoader {
    private int resource = 0;
    private String uri;
    private String fallbackUri;
    private ImageView imgvCover;
    private boolean textAndImageCombined;
    private MainActivity activity;
    private TextView fallbackTitle;

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

    public CoverLoader withPlaceholderView(TextView title) {
        this.fallbackTitle = title;
        return this;
    }

    /**
     * Set cover text and if it should be shown even if there is a cover image.
     * @param fallbackTitle Fallback title text
     * @param textAndImageCombined Show cover text even if there is a cover image?
     */
    @NonNull
    public CoverLoader withPlaceholderView(@NonNull TextView fallbackTitle, boolean textAndImageCombined) {
        this.fallbackTitle = fallbackTitle;
        this.textAndImageCombined = textAndImageCombined;
        return this;
    }

    public void load() {
        CoverTarget coverTarget = new CoverTarget(fallbackTitle, imgvCover, textAndImageCombined);

        if (resource != 0) {
            Glide.with(activity).clear(coverTarget);
            imgvCover.setImageResource(resource);
            CoverTarget.setTitleVisibility(fallbackTitle, textAndImageCombined);
            return;
        }

        RequestOptions options = new RequestOptions()
                .fitCenter()
                .dontAnimate();

        RequestBuilder<Drawable> builder = Glide.with(activity)
                .as(Drawable.class)
                .load(uri)
                .apply(options);

        if (fallbackUri != null && fallbackTitle != null && imgvCover != null) {
            builder = builder.error(Glide.with(activity)
                    .as(Drawable.class)
                    .load(fallbackUri)
                    .apply(options));
        }

        builder.into(coverTarget);
    }

    static class CoverTarget extends CustomViewTarget<ImageView, Drawable> {
        private final WeakReference<TextView> fallbackTitle;
        private final WeakReference<ImageView> cover;
        private final boolean textAndImageCombined;

        public CoverTarget(TextView fallbackTitle, ImageView coverImage, boolean textAndImageCombined) {
            super(coverImage);
            this.fallbackTitle = new WeakReference<>(fallbackTitle);
            cover = new WeakReference<>(coverImage);
            this.textAndImageCombined = textAndImageCombined;
        }

        @Override
        public void onLoadFailed(Drawable errorDrawable) {
            setTitleVisibility(fallbackTitle.get(), true);
        }

        @Override
        public void onResourceReady(@NonNull Drawable resource,
                                    @Nullable Transition<? super Drawable> transition) {
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(resource);
            setTitleVisibility(fallbackTitle.get(), textAndImageCombined);
        }

        @Override
        protected void onResourceCleared(@Nullable Drawable placeholder) {
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(placeholder);
            setTitleVisibility(fallbackTitle.get(),  textAndImageCombined);
        }

        static void setTitleVisibility(TextView fallbackTitle, boolean textAndImageCombined) {
            boolean showTitle = UserPreferences.shouldShowSubscriptionTitle();
            if (fallbackTitle != null) {
                fallbackTitle.setVisibility((textAndImageCombined && !showTitle) ? View.VISIBLE : View.GONE);
            }
        }
    }
}
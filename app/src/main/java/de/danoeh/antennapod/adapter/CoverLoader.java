package de.danoeh.antennapod.adapter;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;

import java.lang.ref.WeakReference;

import com.bumptech.glide.request.transition.Transition;

import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.glide.PaletteBitmap;

public class CoverLoader {
    private int resource = 0;
    private String uri;
    private String fallbackUri;
    private TextView title;
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
        this.title = title;
        return this;
    }

    /**
     * Set cover text and if it should be shown even if there is a cover image.
     *
     * @param title      Cover text.
     * @param textAndImageCombined Show cover text even if there is a cover image?
     */
    @NonNull
    public CoverLoader withPlaceholderView(
            @NonNull TextView title,
            @NonNull TextView fallbackTitle, boolean textAndImageCombined) {
        this.title = title;
        this.textAndImageCombined = textAndImageCombined;
        this.fallbackTitle = fallbackTitle;
        return this;
    }

    public void load() {
        CoverTarget coverTarget = new CoverTarget(title, fallbackTitle, imgvCover, textAndImageCombined);

        if (resource != 0) {
            Glide.with(activity).clear(coverTarget);
            imgvCover.setImageResource(resource);
            CoverTarget.setTitleVisibility(title, fallbackTitle, textAndImageCombined, null);
            return;
        }

        RequestOptions options = new RequestOptions()
                .fitCenter()
                .dontAnimate();

        RequestBuilder<PaletteBitmap> builder = Glide.with(activity)
                .as(PaletteBitmap.class)
                .load(uri)
                .apply(options);

        if (fallbackUri != null && title != null && imgvCover != null) {
            builder = builder.error(Glide.with(activity)
                    .as(PaletteBitmap.class)
                    .load(fallbackUri)
                    .apply(options));
        }

        builder.into(coverTarget);
    }

    static class CoverTarget extends CustomViewTarget<ImageView, PaletteBitmap> {
        private final WeakReference<TextView> placeholder;
        private final WeakReference<ImageView> cover;
        private final boolean textAndImageCombined;
        private final WeakReference<TextView> fallbackTitle;

        public CoverTarget(TextView title,
                           TextView fallbackTitle,
                           ImageView coverImage, boolean textAndImageCombined) {
            super(coverImage);
            placeholder = new WeakReference<>(title);
            cover = new WeakReference<>(coverImage);
            this.textAndImageCombined = textAndImageCombined;
            this.fallbackTitle = new WeakReference<>(fallbackTitle);
        }

        @Override
        public void onLoadFailed(Drawable errorDrawable) {
            setTitleVisibility(
                    this.placeholder.get(),
                    fallbackTitle.get(),
                    true, null);
        }

        @Override
        public void onResourceReady(@NonNull PaletteBitmap resource,
                                    @Nullable Transition<? super PaletteBitmap> transition) {
            ImageView ivCover = cover.get();
            ivCover.setImageBitmap(resource.bitmap);
            setTitleVisibility(placeholder.get(), fallbackTitle.get(),
                    textAndImageCombined, resource.palette);
        }

        @Override
        protected void onResourceCleared(@Nullable Drawable placeholder) {
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(placeholder);
            setTitleVisibility(this.placeholder.get(), fallbackTitle.get(),  textAndImageCombined, null);
        }

        static void setTitleVisibility(TextView placeholder, TextView fallbackTitle,
                                       boolean textAndImageCombined, Palette palette) {
            boolean showTitle = UserPreferences.shouldShowSubscriptionTitle();
            if (placeholder != null) {
                placeholder.setVisibility(showTitle ? View.VISIBLE : View.GONE);
            }
            if (fallbackTitle != null) {
                fallbackTitle.setVisibility(textAndImageCombined && !showTitle ? View.VISIBLE : View.GONE);
            }
        }
    }
}
package de.danoeh.antennapod.adapter;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
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

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.PaletteBitmap;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.ThemeUtils;

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
            CoverTarget.setPlaceholderVisibility(txtvPlaceholder, textAndImageCombined, null);
            return;
        }

        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate();

        RequestBuilder<PaletteBitmap> builder = Glide.with(activity)
                .as(PaletteBitmap.class)
                .load(uri)
                .apply(options);

        if (fallbackUri != null && txtvPlaceholder != null && imgvCover != null) {
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
        public void onResourceReady(@NonNull PaletteBitmap resource,
                                    @Nullable Transition<? super PaletteBitmap> transition) {
            ImageView ivCover = cover.get();
            ivCover.setImageBitmap(resource.bitmap);
            setPlaceholderVisibility(placeholder.get(), textAndImageCombined, resource.palette);
        }

        @Override
        protected void onResourceCleared(@Nullable Drawable placeholder) {
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(placeholder);
            setPlaceholderVisibility(this.placeholder.get(), textAndImageCombined, null);
        }

        static void setPlaceholderVisibility(TextView placeholder, boolean textAndImageCombined, Palette palette) {
            boolean showTitle = UserPreferences.shouldShowSubscriptionTitle();
            if (placeholder != null) {
                if (textAndImageCombined || showTitle) {
                    int bgColor = placeholder.getContext().getResources().getColor(R.color.feed_text_bg);
                    if (palette == null || !showTitle) {
                        placeholder.setBackgroundColor(bgColor);
                        placeholder.setTextColor(ThemeUtils.getColorFromAttr(placeholder.getContext(),
                                android.R.attr.textColorPrimary));
                        return;
                    }
                    int dominantColor = palette.getDominantColor(bgColor);
                    int textColor = placeholder.getContext().getResources().getColor(R.color.white);
                    if (ColorUtils.calculateLuminance(dominantColor) > 0.5) {
                        textColor = placeholder.getContext().getResources().getColor(R.color.black);
                    }
                    placeholder.setTextColor(textColor);
                    placeholder.setBackgroundColor(dominantColor);
                } else {
                    placeholder.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
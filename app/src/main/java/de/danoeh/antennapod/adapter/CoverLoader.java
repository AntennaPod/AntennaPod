package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;

import java.lang.ref.WeakReference;

import com.bumptech.glide.request.transition.Transition;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.glide.PaletteBitmap;

public class CoverLoader {
    private int resource = 0;
    private String uri;
    private String fallbackUri;
    private TextView txtvPlaceholder;
    private ImageView imgvCover;
    private boolean textAndImageCombined;
    private MainActivity activity;
    private boolean hasRoundedCorner = false;
    private TextView fallbackTitleView;

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
     * Set whether the cover image should have rounded corners; sharp corners by default.
     *
     */

    public CoverLoader setRoundedCorner() {
        hasRoundedCorner = true;
        return this;
    }

    /**
     * Set cover text and if it should be shown even if there is a cover image.
     *
     * @param placeholderView      Cover text.
     * @param textAndImageCombined Show cover text even if there is a cover image?
     */
    @NonNull
    public CoverLoader withPlaceholderView(
            @NonNull TextView placeholderView,
            @NonNull TextView fallbackTitleView, boolean textAndImageCombined) {
        this.txtvPlaceholder = placeholderView;
        this.textAndImageCombined = textAndImageCombined;
        this.fallbackTitleView = fallbackTitleView;
        return this;
    }

    public void load() {
        CoverTarget coverTarget = new CoverTarget(txtvPlaceholder, fallbackTitleView, imgvCover, textAndImageCombined);

        if (resource != 0) {
            Glide.with(activity).clear(coverTarget);
            imgvCover.setImageResource(resource);
            CoverTarget.setTitleVisibility(txtvPlaceholder, fallbackTitleView, textAndImageCombined, null);
            return;
        }

        RequestOptions options = new RequestOptions()
                .fitCenter()
                .dontAnimate();

        if (hasRoundedCorner) {
            options.transform(new FitCenter(), new RoundedCorners((int)
                    (8 * activity.getResources().getDisplayMetrics().density)));
        }

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
        private final WeakReference<TextView> tvFallbackTitle;

        public CoverTarget(TextView txtvPlaceholder,
                           TextView tvFallbackTitle,
                           ImageView imgvCover, boolean textAndImageCombined) {
            super(imgvCover);
            placeholder = new WeakReference<>(txtvPlaceholder);
            cover = new WeakReference<>(imgvCover);
            this.textAndImageCombined = textAndImageCombined;
            this.tvFallbackTitle = new WeakReference<>(tvFallbackTitle);
        }

        @Override
        public void onLoadFailed(Drawable errorDrawable) {
            setTitleVisibility(
                    this.placeholder.get(),
                    tvFallbackTitle.get(),
                    true, null);
        }

        @Override
        public void onResourceReady(@NonNull PaletteBitmap resource,
                                    @Nullable Transition<? super PaletteBitmap> transition) {
            ImageView ivCover = cover.get();
            ivCover.setImageBitmap(resource.bitmap);
            setTitleVisibility(placeholder.get(), tvFallbackTitle.get(),
                    textAndImageCombined, resource.palette);
        }

        @Override
        protected void onResourceCleared(@Nullable Drawable placeholder) {
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(placeholder);
            setTitleVisibility(this.placeholder.get(), tvFallbackTitle.get(),  textAndImageCombined, null);
        }

        static void setTitleVisibility(TextView placeholder, TextView fallbackTitle,
                                       boolean textAndImageCombined, Palette palette) {
            boolean showTitle = UserPreferences.shouldShowSubscriptionTitle();
            if (placeholder != null) {
                placeholder.setVisibility(showTitle
                        ? View.VISIBLE : View.GONE);
            }
            if (fallbackTitle != null) {
                fallbackTitle.setVisibility(textAndImageCombined && !showTitle
                        ? View.VISIBLE : View.GONE);
                if (!showTitle && textAndImageCombined) {
                    if (placeholder == null) {
                        return;
                    }
                    final Context context = placeholder.getContext();
                    int bgColor = ContextCompat.getColor(context, R.color.feed_text_bg);
                    if (palette == null) {
                        fallbackTitle.setTextColor(
                                ThemeUtils.getColorFromAttr(placeholder.getContext(),
                                android.R.attr.textColorPrimary));
                        return;
                    }
                    int dominantColor = palette.getDominantColor(bgColor);
                    int textColor = ContextCompat.getColor(context, R.color.white);
                    if (ColorUtils.calculateLuminance(dominantColor) > 0.5) {
                        textColor = ContextCompat.getColor(context, R.color.black);
                    }

                    fallbackTitle.setTextColor(textColor);

                }
            }
        }
    }
}
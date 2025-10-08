package de.danoeh.antennapod.ui;

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
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.episodes.ImageResourceUtils;
import de.danoeh.antennapod.ui.glide.ImageLoader;

import java.lang.ref.WeakReference;

public class CoverLoader {
    private FeedItem item;
    private Feed feed;
    private int resource = 0;
    private ImageView imgvCover;
    private boolean textAndImageCombined;
    private TextView fallbackTitle;

    public CoverLoader() {
    }

    public CoverLoader withItem(FeedItem item) {
        this.item = item;
        return this;
    }

    public CoverLoader withFeed(Feed feed) {
        this.feed = feed;
        return this;
    }

    public CoverLoader withResource(int resource) {
        this.resource = resource;
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
    public CoverLoader withPlaceholderView(TextView fallbackTitle, boolean textAndImageCombined) {
        this.fallbackTitle = fallbackTitle;
        this.textAndImageCombined = textAndImageCombined;
        return this;
    }

    public void load() {
        CoverTarget coverTarget = new CoverTarget(fallbackTitle, imgvCover, textAndImageCombined);

        if (resource != 0) {
            Glide.with(imgvCover).clear(coverTarget);
            imgvCover.setImageResource(resource);
            CoverTarget.setTitleVisibility(fallbackTitle, textAndImageCombined);
            return;
        }

        RequestOptions options = new RequestOptions()
                .fitCenter()
                .dontAnimate();

        if (item != null) {
            String primaryUrl = ImageResourceUtils.getEpisodeListImageLocation(item);
            RequestBuilder<Drawable> fallbackRequest = ImageLoader.load(imgvCover,
                    ImageResourceUtils.getFallbackImageLocation(item), item.getFeed()).apply(options);

            ImageLoader.load(imgvCover, primaryUrl, item.getFeed())
                    .apply(options)
                    .error(fallbackRequest)
                    .into(coverTarget);
        } else if (feed != null) {
            ImageLoader.load(imgvCover, feed.getImageUrl(), feed)
                    .apply(options)
                    .into(coverTarget);
        }
    }

    static class CoverTarget extends CustomViewTarget<ImageView, Drawable> {
        private final WeakReference<TextView> fallbackTitle;
        private final WeakReference<ImageView> cover;
        private final boolean textAndImageCombined;

        public CoverTarget(TextView fallbackTitle, ImageView coverImage, boolean textAndImageCombined) {
            super(coverImage);
            this.fallbackTitle = new WeakReference<>(fallbackTitle);
            this.cover = new WeakReference<>(coverImage);
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
            if (fallbackTitle != null) {
                fallbackTitle.setVisibility(textAndImageCombined ? View.VISIBLE : View.GONE);
            }
        }
    }
}
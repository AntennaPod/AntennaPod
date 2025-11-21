package de.danoeh.antennapod.ui;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.common.ImageModel;
import de.danoeh.antennapod.ui.common.ErrorImageModel;
import de.danoeh.antennapod.ui.glide.GlideApp;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;


public class CoverLoader {

    private RequestOptions requestOptions;

    public interface BitmapCallback {
        void onBitmapLoaded(android.graphics.Bitmap bitmap);
    }

    private FragmentActivity activity;
    private ImageModel imageModel;
    private int resource = 0;
    private ImageView coverView;

    /**
     * Creates a CoverLoader for loading images directly into an ImageView.
     *
     * @param coverView The ImageView to load the image into
     * @param model     The ImageModel containing image URLs and fallback text
     * @example <pre>
     *     // Load feed cover into ImageView
     *     ImageModel feedModel = CoverLoader.fromFeed(feed);
     *     new CoverLoader(imageView, feedModel).load();
     *
     *     // Load episode cover with custom options
     *     ImageModel episodeModel = CoverLoader.fromFeedItem(feedItem);
     *     new CoverLoader(imageView, episodeModel)
     *         .withRequestOptions(new RequestOptions().centerCrop())
     *         .load();
     *     </pre>
     */
    public CoverLoader(ImageView coverView, ImageModel model) {
        this.coverView = coverView;
        this.imageModel = model;
    }

    /**
     * Creates a CoverLoader for loading a drawable resource directly into an ImageView.
     *
     * @param coverView The ImageView to load the resource into
     * @param resource  The drawable resource ID to load
     * @example <pre>
     *     // Load default placeholder image for dummy view during initialization of a view
     *     new CoverLoader(imageView, R.drawable.ic_launcher).load();
     *     </pre>
     */
    public CoverLoader(ImageView coverView, int resource) {
        this.coverView = coverView;
        this.resource = resource;
    }

    /**
     * Creates a CoverLoader for bitmap generation using Activity context.
     * Use this constructor when you need to get() a bitmap rather than load into a view.
     *
     * @param activity The FragmentActivity context for Glide operations
     * @param model    The ImageModel containing image URLs and fallback text
     * @example <pre>
     *     // Generate bitmap for shortcuts
     *     ImageModel feedModel = CoverLoader.fromFeed(feed);
     *     new CoverLoader(activity, feedModel)
     *         .withRequestOptions(RequestOptions.overrideOf(128, 128))
     *         .get(bitmap -> createShortcut(feed, bitmap));
     *
     *     // Generate bitmap for notifications
     *     ImageModel episodeModel = CoverLoader.fromFeedItem(feedItem);
     *     new CoverLoader(activity, episodeModel)
     *         .get(bitmap -> showNotification(bitmap));
     *     </pre>
     */
    public CoverLoader(FragmentActivity activity, ImageModel model) {
        this.activity = activity;
        this.imageModel = model;
    }

    public CoverLoader withRequestOptions(RequestOptions options) {
        this.requestOptions = options;
        return this;
    }

    /**
     * Creates an ImageModel for a podcast feed's cover image.
     *
     * @param feed The feed to create an ImageModel for
     * @return ImageModel with the feed's image URL as primary source and feed title as fallback text.
     *     If the feed has no image URL, a generated placeholder will be created automatically.
     */
    public static ImageModel fromFeed(Feed feed) {
        String imageUrl = feed.getImageUrl();

        // For local feeds, don't use imageUrl if it's just a filename (not a valid URL)
        if (feed.isLocalFeed() && imageUrl != null && !imageUrl.startsWith("http")) {
            imageUrl = null; // Force placeholder generation for local feeds with invalid URLs
        }

        return new ImageModel(imageUrl, null, feed.getTitle());
    }

    public static ImageModel fromFeedItem(FeedItem item) {
        return new ImageModel(
                item.getImageUrl(),
                item.getFeed() != null ? item.getFeed().getImageUrl() : null,
                item.getFeed() != null ? item.getFeed().getTitle() : item.getTitle());
    }


    public static ImageModel fromMedia(Playable playable, Chapter chapter) {
        return new ImageModel(
                chapter.getImageUrl(),
                playable.getImageLocation(),
                chapter.getTitle());
    }
    /**
     * Set cover text and if it should be shown even if there is a cover image.
     *
     * @param fallbackTitle        Fallback title text
     * @param textAndImageCombined Show cover text even if there is a cover image?
     */

    public void load() {

        if (resource != 0) {
            coverView.setImageResource(resource);
            return;
        }

        RequestOptions myOptions;
        if (this.requestOptions != null) {
            myOptions = requestOptions.clone().apply(requestOptions);
        } else {
            myOptions = new RequestOptions()
                    .fitCenter()
                    .dontAnimate();
        }

        // Use ImageModel if available
        if (imageModel != null) {
            loadWithImageModel(myOptions);
        }

    }

    private void loadWithImageModel(RequestOptions options) {
        RequestBuilder<Drawable> builder = GlideApp.with(coverView)
                .as(Drawable.class)
                .load(imageModel)
                .apply(options);

        // Add error handling for failed network requests
        builder = builder.error(GlideApp.with(coverView)
                .as(Drawable.class)
                .load(new ErrorImageModel(imageModel.getFallbackText()))
                .apply(options));

        builder.into(coverView);
    }

    /**
     * Loads the image as a Bitmap with automatic error handling.
     * On success, calls callback with the loaded bitmap.
     * On error, automatically loads ErrorImageModel and calls callback with error bitmap.
     * 
     * @param callback BitmapCallback that receives the final bitmap (either success or error)
     */
    public void get(BitmapCallback callback) {
        if (activity == null) {
            throw new IllegalStateException("Activity context required for get() method");
        }
        
        RequestBuilder<android.graphics.Bitmap> builder = GlideApp.with(activity).asBitmap();
        
        if (imageModel != null) {
            builder = builder.load(imageModel);
        } else if (resource != 0) {
            builder = builder.load(resource);
        }
        
        if (requestOptions != null) {
            builder = builder.apply(requestOptions);
        }
        
        builder.into(new CustomTarget<android.graphics.Bitmap>() {
            @Override
            public void onResourceReady(@NonNull android.graphics.Bitmap resource, 
                                      @Nullable Transition<? super android.graphics.Bitmap> transition) {
                callback.onBitmapLoaded(resource);
            }
            
            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                // Load ErrorImageModel as fallback
                String fallbackText = imageModel != null ? imageModel.getFallbackText() : "";
                GlideApp.with(activity)
                        .asBitmap()
                        .load(new ErrorImageModel(fallbackText))
                        .apply(requestOptions != null ? requestOptions : new RequestOptions())
                        .into(new CustomTarget<android.graphics.Bitmap>() {
                            @Override
                            public void onResourceReady(
                                    @NonNull android.graphics.Bitmap resource,
                                    @Nullable Transition<? super android.graphics.Bitmap> transition) {
                                callback.onBitmapLoaded(resource);
                            }
                            
                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                // Fallback to null if even error image fails
                                callback.onBitmapLoaded(null);
                            }
                        });
            }
            
            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                // Called when target is cleared
            }
        });
    }

}
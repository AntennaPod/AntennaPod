package de.danoeh.antennapod.ui.glide;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;

/**
 * A centralized utility for creating Glide image loading requests.
 * This class contains the logic to handle images that require authentication.
 */
public final class ImageLoader {

    private ImageLoader() {
    }

    /**
     * Creates a Glide RequestBuilder for a given image URL and Feed.
     *
     * This method checks if the feed requires authentication and, if so, uses a custom
     * Glide model ({@link AuthenticatedImageUrl}) to provide the credentials for the request.
     * Otherwise, it uses the image URL directly.
     *
     * @param view      The view context for Glide.
     * @param imageUrl  The URL of the image to load.
     * @param feed      The Feed object, which may contain authentication credentials.
     * @return A Glide RequestBuilder that can be further customized.
     */
    public static RequestBuilder<Drawable> load(View view, String imageUrl, Feed feed) {
        Object model;
        FeedPreferences prefs = (feed != null) ? feed.getPreferences() : null;

        if (imageUrl != null && prefs != null && !TextUtils.isEmpty(prefs.getUsername())) {
            model = new AuthenticatedImageUrl(imageUrl,
                    prefs.getUsername(),
                    prefs.getPassword());
        } else {
            model = imageUrl;
        }
        return Glide.with(view)
                .as(Drawable.class)
                .load(model);
    }
}

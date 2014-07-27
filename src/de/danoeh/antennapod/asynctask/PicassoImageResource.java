package de.danoeh.antennapod.asynctask;

import android.net.Uri;

/**
 * Classes that implement this interface provide access to an image resource that can
 * be loaded by the Picasso library.
 */
public interface PicassoImageResource {

    /**
     * This scheme should be used by PicassoImageResources to
     * indicate that the image Uri points to a file that is not an image
     * (e.g. a media file). This workaround is needed so that the Picasso library
     * loads these Uri with a Downloader instead of trying to load it directly.
     * <p/>
     * For example implementations, see FeedMedia or ExternalMedia.
     */
    public static final String SCHEME_MEDIA = "media";

    /**
     * Returns a Uri to the image or null if no image is available.
     */
    public Uri getImageUri();
}

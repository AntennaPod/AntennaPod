package de.danoeh.antennapod.core.asynctask;

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
     * Parameter key for an encoded fallback Uri. This Uri MUST point to a local image file
     */
    public static final String PARAM_FALLBACK = "fallback";

    /**
     * Returns a Uri to the image or null if no image is available.
     * <p/>
     * The Uri can either be an HTTP-URL, a URL pointing to a local image file or
     * a non-image file (see SCHEME_MEDIA for more details).
     * <p/>
     * The Uri can also have an optional fallback-URL if loading the default URL
     * failed (see PARAM_FALLBACK).
     */
    public Uri getImageUri();
}

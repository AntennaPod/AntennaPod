package de.danoeh.antennapod.core.asynctask;

/**
 * Classes that implement this interface provide access to an image resource that can
 * be loaded by the Picasso library.
 */
public interface ImageResource {

    /**
     * Returns the location of the image or null if no image is available.
     * <p/>
     * The location can either be an URL or a local path
     */
    String getImageLocation();
}

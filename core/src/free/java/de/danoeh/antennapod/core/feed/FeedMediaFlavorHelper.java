package de.danoeh.antennapodSA.core.feed;

/**
 * Implements methods for FeedMedia that are flavor dependent.
 */
class FeedMediaFlavorHelper {
    private FeedMediaFlavorHelper(){}
    static boolean instanceOfRemoteMedia(Object o) {
        return false;
    }
}

package de.danoeh.antennapodSA.core.feed;

import de.danoeh.antennapodSA.core.cast.RemoteMedia;

/**
 * Implements methods for FeedMedia that are flavor dependent.
 */
public class FeedMediaFlavorHelper {
    private FeedMediaFlavorHelper(){}
    static boolean instanceOfRemoteMedia(Object o) {
        return o instanceof RemoteMedia;
    }
}

package de.danoeh.antennapod.core.util;

import de.danoeh.antennapod.core.BuildConfig;

/**
 * Helper class to handle the different build flavors.
 */
public enum Flavors {
    FREE,
    PLAY,
    UNKNOWN;

    public static final Flavors FLAVOR;

    static {
        if (BuildConfig.FLAVOR.equals("free")) {
            FLAVOR = FREE;
        } else if (BuildConfig.FLAVOR.equals("play")) {
            FLAVOR = PLAY;
        } else {
            FLAVOR = UNKNOWN;
        }
    }
}

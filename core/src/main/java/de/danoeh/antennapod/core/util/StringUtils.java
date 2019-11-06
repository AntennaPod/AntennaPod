package de.danoeh.antennapod.core.util;

/**
 * Utility functions for handling Strings.
 */
public class StringUtils {

    private StringUtils() {

    }

    public static boolean isBlank(String string) {
        if (string == null) {
            return false;
        }
        return string.trim().length() == 0;
    }
}

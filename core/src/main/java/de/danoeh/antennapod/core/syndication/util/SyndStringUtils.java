package de.danoeh.antennapod.core.syndication.util;

public class SyndStringUtils {
    private SyndStringUtils() {

    }

    /**
     * Trims all whitespace from beginning and ending of a String. {{@link String#trim()}} only trims spaces.
     */
    public static String trimAllWhitespace(String string) {
        return string.replaceAll("(^\\s*)|(\\s*$)", "");
    }
}

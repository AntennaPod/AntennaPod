package de.danoeh.antennapod.model.feed;

public enum DateSortType {
    ASC,
    DESC;

    public static DateSortType parseWithDefault(String value, DateSortType defaultValue) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}

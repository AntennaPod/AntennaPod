package de.danoeh.antennapod.ui.common;

import java.util.Objects;

public class FallbackImageData {

    public String getFeedDownloadUrl() {
        return feedDownloadUrl;
    }

    private final String feedDownloadUrl;
    private final String fallbackText;
    private final boolean showImageWithoutFallbackText;

    public FallbackImageData(String downloadUrl, String fallbackText, boolean showImageWithoutFallbackText) {
        this.feedDownloadUrl = downloadUrl;
        this.fallbackText = fallbackText != null ? fallbackText : "";
        this.showImageWithoutFallbackText = showImageWithoutFallbackText;
    }

    public String getFallbackText() {
        return fallbackText;
    }

    public boolean isShowImageWithoutFallbackText() {
        return showImageWithoutFallbackText;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        FallbackImageData that = (FallbackImageData) obj;
        return Objects.equals(fallbackText, that.fallbackText)
                && showImageWithoutFallbackText == that.showImageWithoutFallbackText;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fallbackText, showImageWithoutFallbackText);
    }

    @Override
    public String toString() {
        return "GenerativeErrorImageModel{fallbackText='" + fallbackText + "'}";
    }
}

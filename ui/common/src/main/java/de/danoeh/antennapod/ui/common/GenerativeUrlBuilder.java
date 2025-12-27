package de.danoeh.antennapod.ui.common;

import android.text.TextUtils;

import java.util.Objects;

import de.danoeh.antennapod.model.feed.Feed;

public class GenerativeUrlBuilder {

    private final String primaryUrl;
    private final String fallbackUrl;
    private final String fallbackText;
    private final String feedDownloadUrl;
    // show empty image for dummy view
    private final boolean showEmptyImage;
    private final boolean initialized;


    // This is to prevent the fallback text in the image e.g. for the blurred background
    // Else the feed name is in the text and in the image, which looks disturbing
    private final boolean showImageWithoutFallbackText;

    public GenerativeUrlBuilder(
            String primaryUrl,
            String fallbackText,
            String feedDownloadUrl,
            boolean initialized) {
        this(primaryUrl, null, fallbackText, feedDownloadUrl, initialized, false);
    }

    public GenerativeUrlBuilder(
            String primaryUrl,
            String fallbackText,
            String feedDownloadUrl) {
        this(primaryUrl, null, fallbackText, feedDownloadUrl, false);
    }

    public GenerativeUrlBuilder(
            String primaryUrl,
            String fallbackUrl,
            String fallbackText,
            String feedDownloadUrl) {
        this(primaryUrl, fallbackUrl, fallbackText, feedDownloadUrl, false);
    }

    public GenerativeUrlBuilder(
            String primaryUrl,
            String fallbackUrl,
            String fallbackText,
            String feedDownloadUrl,
            boolean showImageWithoutFallbackText) {
        this.primaryUrl = primaryUrl;
        this.fallbackUrl = fallbackUrl;
        this.fallbackText = fallbackText != null ? fallbackText : "";
        this.feedDownloadUrl = feedDownloadUrl != null ? feedDownloadUrl : "";
        this.initialized = true;
        this.showImageWithoutFallbackText = showImageWithoutFallbackText;
        this.showEmptyImage = TextUtils.isEmpty(primaryUrl)
                && TextUtils.isEmpty(fallbackUrl)
                && TextUtils.isEmpty(fallbackText);
    }

    public GenerativeUrlBuilder(
            String primaryUrl,
            String fallbackUrl,
            String fallbackText,
            String feedDownloadUrl,
            boolean initialized,
            boolean showImageWithoutFallbackText) {
        this.primaryUrl = primaryUrl;
        this.fallbackUrl = fallbackUrl;
        this.fallbackText = fallbackText != null ? fallbackText : "";
        this.feedDownloadUrl = feedDownloadUrl != null ? feedDownloadUrl : "";
        this.initialized = initialized;
        this.showImageWithoutFallbackText = showImageWithoutFallbackText;
        this.showEmptyImage = TextUtils.isEmpty(primaryUrl)
                && TextUtils.isEmpty(fallbackUrl)
                && TextUtils.isEmpty(fallbackText)
                && !initialized;
    }

    public String getPrimaryUrl() {
        return primaryUrl;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public String getFallbackText() {
        return fallbackText;
    }

    public String getFeedDownloadUrl() {
        return feedDownloadUrl;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isShowEmptyImage() {
        return showEmptyImage;
    }

    public boolean isShowImageWithoutFallbackText() {
        return showImageWithoutFallbackText;
    }


    public String buildUrl() {
        if (!TextUtils.isEmpty(primaryUrl)) {
            return primaryUrl;
        }
        if (!TextUtils.isEmpty(fallbackUrl)) {
            return fallbackUrl;
        }
        String text = fallbackText != null ? fallbackText : "";
        String feedUrl = feedDownloadUrl != null ? feedDownloadUrl : "";
        return Feed.PREFIX_GENERATIVE_COVER
                + feedUrl + "###"
                + (showEmptyImage ? "1" : "0")
                + (initialized ? "1" : "0")
                + (showImageWithoutFallbackText ? "1" : "0")
                + "/" + text;
    }

    @Override
    public String toString() {
        return "UrlWithGenerativeFallbackModel{"
                + "primaryUrl='" + primaryUrl + '\''
                + ", fallbackUrl='" + fallbackUrl + '\''
                + ", fallbackText='" + fallbackText + '\''
                + ", feedDownloadUrl='" + feedDownloadUrl + '\''
                + ", showEmptyImage=" + showEmptyImage
                + ", initialized=" + initialized
                + ", showImageWithoutFallbackText=" + showImageWithoutFallbackText
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GenerativeUrlBuilder that = (GenerativeUrlBuilder) o;
        return showEmptyImage == that.showEmptyImage
                && initialized == that.initialized
                && showImageWithoutFallbackText == that.showImageWithoutFallbackText
                && Objects.equals(primaryUrl, that.primaryUrl)
                && Objects.equals(fallbackUrl, that.fallbackUrl)
                && Objects.equals(fallbackText, that.fallbackText)
                && Objects.equals(feedDownloadUrl, that.feedDownloadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryUrl, fallbackUrl, fallbackText, feedDownloadUrl,
                showEmptyImage, initialized, showImageWithoutFallbackText);
    }

    public boolean hasFallbackUrl() {
        return fallbackUrl != null;
    }
}

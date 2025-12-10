package de.danoeh.antennapod.ui.common;

import androidx.annotation.NonNull;

import java.net.URLEncoder;
import java.util.Objects;

public class ImageModel {
    private final String primaryUrl;
    private final String fallbackUrl;
    private final String fallbackText;

    public ImageModel(String primaryUrl, String fallbackText) {
        this(primaryUrl, null, fallbackText);
    }

    public ImageModel(String primaryUrl, String fallbackUrl, String fallbackText) {
        this.primaryUrl = primaryUrl;
        this.fallbackUrl = fallbackUrl;
        this.fallbackText = fallbackText != null ? fallbackText : "";
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

    public boolean hasPrimaryUrl() {
        return primaryUrl != null && !primaryUrl.isEmpty();
    }

    public boolean hasFallbackUrl() {
        return fallbackUrl != null && !fallbackUrl.isEmpty();
    }

    public String getGenerativePlaceholderUrl() {
        try {
            return "generative://text=" + URLEncoder.encode(fallbackText, "UTF-8");
        } catch (Exception e) {
            return "generative://text=" + fallbackText;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ImageModel that = (ImageModel) obj;

        if (primaryUrl != null && that.primaryUrl != null) {
            return primaryUrl.equals(that.primaryUrl);
        }

        if (primaryUrl != null || that.primaryUrl != null) {
            return false;
        }

        if (fallbackUrl != null && that.fallbackUrl != null) {
            return fallbackUrl.equals(that.fallbackUrl);
        }

        if (fallbackUrl != null || that.fallbackUrl != null) {
            return false;
        }

        if (fallbackText != null && that.fallbackText != null) {
            return fallbackText.equals(that.fallbackText);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (primaryUrl != null) {
            return Objects.hash(primaryUrl);
        }
        if (fallbackUrl != null) {
            return Objects.hash(fallbackUrl);
        }
        if (fallbackText != null) {
            return Objects.hash(fallbackText);
        }
        return Objects.hash("");
    }

    @NonNull
    @Override
    public String toString() {
        if (primaryUrl != null) {
            return primaryUrl;
        }
        if (fallbackUrl != null) {
            return fallbackUrl;
        }
        if (fallbackText != null) {
            return fallbackText;
        }
        return "";
    }
}

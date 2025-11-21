package de.danoeh.antennapod.ui.common;

import java.util.Objects;

public class ErrorImageModel {
    private final String fallbackText;

    public ErrorImageModel(String fallbackText) {
        this.fallbackText = fallbackText != null ? fallbackText : "";
    }

    public String getFallbackText() {
        return fallbackText;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        ErrorImageModel that = (ErrorImageModel) obj;
        return Objects.equals(fallbackText, that.fallbackText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fallbackText);
    }

    @Override
    public String toString() {
        return "aErrorImageModel{fallbackText='" + fallbackText + "'}";
    }
}

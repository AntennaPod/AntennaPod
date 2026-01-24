package de.danoeh.antennapod.event.settings;

public record EqualizerPreferenceChangedEvent(boolean enabled, float[] gains) {
}

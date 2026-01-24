package de.danoeh.antennapod.event.settings;

public record CompressorPreferenceChangedEvent(
        boolean enabled,
        float threshold, float ratio, float attackTime, float releaseTime, float noiseGateThreshold, float postGain) {
}

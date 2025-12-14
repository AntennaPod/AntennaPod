package de.danoeh.antennapod.event.settings;

public class EqualizerPreferenceChangedEvent {
    private final boolean enabled;

    private final float[] gains;

    public EqualizerPreferenceChangedEvent(boolean enabled, float[] gains) {
        this.enabled = enabled;
        this.gains = gains;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float[] getGains() {
        return gains;
    }
}

package de.danoeh.antennapod.event.settings;

public class CompressorPreferenceChangedEvent {
    private final boolean enabled;
    private final float preGain;
    private final float threshold;
    private final float ratio;
    private final float postGain;

    public CompressorPreferenceChangedEvent(boolean enabled, float preGain, float threshold, float ratio,
                                            float postGain) {
        this.enabled = enabled;
        this.preGain = preGain;
        this.threshold = threshold;
        this.ratio = ratio;
        this.postGain = postGain;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getPreGain() {
        return preGain;
    }

    public float getThreshold() {
        return threshold;
    }

    public float getRatio() {
        return ratio;
    }

    public float getPostGain() {
        return postGain;
    }
}

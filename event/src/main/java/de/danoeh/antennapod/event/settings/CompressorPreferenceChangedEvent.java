package de.danoeh.antennapod.event.settings;

public class CompressorPreferenceChangedEvent {
    private final boolean enabled;
    private final float threshold;
    private final float ratio;
    private final float noiseGateThreshold;
    private final float attackTime;
    private final float releaseTime;
    private final float postGain;

    public CompressorPreferenceChangedEvent(boolean enabled,
                                            float threshold, float ratio, float attackTime, float releaseTime,
                                            float noiseGateThreshold, float postGain) {
        this.enabled = enabled;
        this.threshold = threshold;
        this.ratio = ratio;
        this.attackTime = attackTime;
        this.releaseTime = releaseTime;
        this.noiseGateThreshold = noiseGateThreshold;
        this.postGain = postGain;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getThreshold() {
        return threshold;
    }

    public float getRatio() {
        return ratio;
    }

    public float getAttackTime() {
        return attackTime;
    }

    public float getReleaseTime() {
        return releaseTime;
    }

    public float getNoiseGateThreshold() {
        return noiseGateThreshold;
    }

    public float getPostGain() {
        return postGain;
    }
}

package de.danoeh.antennapod.core.feed;

public enum VolumeReductionSetting {
    OFF(0, 1.0f),
    LIGHT(1, 0.5f),
    HEAVY(2, 0.2f);

    private final int value;
    private float reductionFactor;

    VolumeReductionSetting(int value, float reductionFactor) {
        this.value = value;
        this.reductionFactor = reductionFactor;
    }

    public static VolumeReductionSetting fromInteger(int value) {
        for (VolumeReductionSetting setting : values()) {
            if (setting.value == value) {
                return setting;
            }
        }
        throw new IllegalArgumentException("Cannot map value to VolumeReductionSetting: " + value);
    }

    public int toInteger() {
        return value;
    }

    public float getReductionFactor() {
        return reductionFactor;
    }
}

package de.danoeh.antennapod.model.feed;

public enum VolumeAdaptionSetting {
    OFF(0, 1.0f),
    LIGHT_REDUCTION(1, 0.5f),
    HEAVY_REDUCTION(2, 0.2f),
    LIGHT_BOOST(3, 1.5f),
    MEDIUM_BOOST(4, 2f),
    HEAVY_BOOST(5, 2.5f);

    private final int value;
    private float adaptionFactor;

    VolumeAdaptionSetting(int value, float adaptionFactor) {
        this.value = value;
        this.adaptionFactor = adaptionFactor;
    }

    public static VolumeAdaptionSetting fromInteger(int value) {
        for (VolumeAdaptionSetting setting : values()) {
            if (setting.value == value) {
                return setting;
            }
        }
        throw new IllegalArgumentException("Cannot map value to VolumeAdaptionSetting: " + value);
    }

    public int toInteger() {
        return value;
    }

    public float getAdaptionFactor() {
        return adaptionFactor;
    }
}

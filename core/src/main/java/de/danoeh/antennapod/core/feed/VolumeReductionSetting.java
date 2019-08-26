package de.danoeh.antennapod.core.feed;

public enum VolumeReductionSetting {
    OFF(0),
    LIGHT(1),
    HEAVY(2);

    private final int value;

    VolumeReductionSetting(int value) {
        this.value = value;
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
}

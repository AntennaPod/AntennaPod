package de.danoeh.antennapod.storage.preferences;

public enum SleepTimerType {
    CLOCK(0),
    EPISODES(1);

    public final int index;

    SleepTimerType(int index) {
        this.index = index;
    }

    public static SleepTimerType fromIndex(int index) {
        for (SleepTimerType stt : values()) {
            if (stt.index == index) {
                return stt;
            }
        }
        return SleepTimerType.EPISODES;
    }
}

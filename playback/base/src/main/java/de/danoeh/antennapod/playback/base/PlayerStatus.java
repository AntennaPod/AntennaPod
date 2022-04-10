package de.danoeh.antennapod.playback.base;

public enum PlayerStatus {
    INDETERMINATE(0),  // player is currently changing its state, listeners should wait until the state is left
    ERROR(-1),
    PREPARING(19),
    PAUSED(30),
    PLAYING(40),
    STOPPED(5),
    PREPARED(20),
    SEEKING(29),
    INITIALIZING(9),  // playback service is loading the Playable's metadata
    INITIALIZED(10);  // playback service was started, data source of media player was set

    private final int statusValue;
    private static final PlayerStatus[] fromOrdinalLookup;

    static {
        fromOrdinalLookup = PlayerStatus.values();
    }

    PlayerStatus(int val) {
        statusValue = val;
    }

    public boolean isAtLeast(PlayerStatus other) {
        return other == null || this.statusValue >= other.statusValue;
    }
}

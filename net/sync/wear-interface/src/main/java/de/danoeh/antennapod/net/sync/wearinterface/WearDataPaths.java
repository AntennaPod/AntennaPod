package de.danoeh.antennapod.net.sync.wearinterface;

public final class WearDataPaths {
    public static final String QUEUE = "/queue";
    public static final String DOWNLOADS = "/downloads";
    public static final String EPISODES = "/episodes";
    public static final String SUBSCRIPTIONS = "/subscriptions";
    public static final String FEED_EPISODES_PREFIX = "/feed_episodes/";
    public static final String PLAY_PREFIX = "/play/";
    public static final String NOW_PLAYING = "/now_playing";
    public static final String PAUSE = "/pause";

    public static String playPath(long itemId) {
        return PLAY_PREFIX + itemId;
    }

    public static String feedEpisodesPath(long feedId) {
        return FEED_EPISODES_PREFIX + feedId;
    }

    private WearDataPaths() {
    }
}

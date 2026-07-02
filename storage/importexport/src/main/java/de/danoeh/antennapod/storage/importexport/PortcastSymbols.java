package de.danoeh.antennapod.storage.importexport;

/**
 * Contains symbols for reading and writing PortCast documents.
 * See https://portcast.org/ for the protocol definition.
 */
final class PortcastSymbols {
    static final String PROTOCOL_VERSION = "0.2.0";

    static final String PORTCAST = "portcast";
    static final String GENERATED_AT = "generatedAt";
    static final String GENERATOR = "generator";
    static final String NAME = "name";
    static final String VERSION = "version";
    static final String URL = "url";
    static final String GENERATOR_NAME = "AntennaPod";
    static final String GENERATOR_URL = "https://antennapod.org";

    static final String SUBSCRIPTIONS = "subscriptions";
    static final String SUBSCRIPTION_ID = "subscriptionId";
    static final String FEED_URL = "feedUrl";
    static final String PODCAST_GUID = "podcastGuid";
    static final String TITLE = "title";
    static final String AUTHOR = "author";
    static final String IMAGE_URL = "imageUrl";
    static final String TAGS = "tags";
    static final String UPDATED_AT = "updatedAt";
    static final String NOTIFICATIONS_ENABLED = "notificationsEnabled";

    static final String EPISODES = "episodes";
    static final String EPISODE_STATE_ID = "episodeStateId";
    static final String SUBSCRIPTION_REF = "subscriptionRef";
    static final String EPISODE_REF = "episodeRef";
    static final String GUID = "guid";
    static final String ENCLOSURE_URL = "enclosureUrl";
    static final String STATUS = "status";
    static final String POSITION_SECONDS = "positionSeconds";
    static final String DURATION_SECONDS = "durationSeconds";
    static final String PUBLISHED_AT = "publishedAt";
    static final String LAST_PLAYED_AT = "lastPlayedAt";
    static final String STARRED = "starred";

    static final String STATUS_UNPLAYED = "unplayed";
    static final String STATUS_IN_PROGRESS = "in_progress";
    static final String STATUS_COMPLETED = "completed";

    static final String QUEUE = "queue";
    static final String POSITION = "position";

    static final String PREFERENCES = "preferences";
    static final String GLOBAL = "global";
    static final String PER_FEED = "perFeed";
    static final String PLAYBACK_RATE = "playbackRate";
    static final String SKIP_FORWARD_SECONDS = "skipForwardSeconds";
    static final String SKIP_BACKWARD_SECONDS = "skipBackwardSeconds";
    static final String SKIP_INTRO_SECONDS = "skipIntroSeconds";
    static final String SKIP_OUTRO_SECONDS = "skipOutroSeconds";
    static final String TRIM_SILENCE = "trimSilence";
    static final String BOOST_VOICE = "boostVoice";

    static final String COMPLETENESS = "completeness";
    static final String SECTION = "section";
    static final String SOURCE = "source";
    static final String LEVEL = "level";
    static final String LEVEL_FULL = "full";
    static final String LEVEL_CURRENT_STATE_ONLY = "current-state-only";

    private PortcastSymbols() {

    }
}

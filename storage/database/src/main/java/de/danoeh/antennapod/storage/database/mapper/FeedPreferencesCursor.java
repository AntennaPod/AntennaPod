package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.FeedFilter;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Converts a {@link Cursor} to a {@link FeedPreferences} object.
 */
public class FeedPreferencesCursor extends CursorWrapper {
    private final int indexId;
    private final int indexAutoDownload;
    private final int indexAutoRefresh;
    private final int indexAutoDeleteAction;
    private final int indexVolumeAdaption;
    private final int indexUsername;
    private final int indexPassword;
    private final int indexIncludeFilter;
    private final int indexExcludeFilter;
    private final int indexMinimalDurationFilter;
    private final int indexFeedPlaybackSpeed;
    private final int indexFeedSkipSilence;
    private final int indexAutoSkipIntro;
    private final int indexAutoSkipEnding;
    private final int indexEpisodeNotification;
    private final int indexNewEpisodesAction;
    private final int indexTags;

    public FeedPreferencesCursor(Cursor cursor) {
        super(cursor);
        indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_FEED_ID);
        indexAutoDownload = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTO_DOWNLOAD_ENABLED);
        indexAutoRefresh = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_KEEP_UPDATED);
        indexAutoDeleteAction = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTO_DELETE_ACTION);
        indexVolumeAdaption = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_VOLUME_ADAPTION);
        indexUsername = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_USERNAME);
        indexPassword = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PASSWORD);
        indexIncludeFilter = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_INCLUDE_FILTER);
        indexExcludeFilter = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_EXCLUDE_FILTER);
        indexMinimalDurationFilter = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_MINIMAL_DURATION_FILTER);
        indexFeedPlaybackSpeed = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_PLAYBACK_SPEED);
        indexFeedSkipSilence = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_SKIP_SILENCE);
        indexAutoSkipIntro = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_SKIP_INTRO);
        indexAutoSkipEnding = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_SKIP_ENDING);
        indexEpisodeNotification = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_EPISODE_NOTIFICATION);
        indexNewEpisodesAction = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_NEW_EPISODES_ACTION);
        indexTags = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_TAGS);
    }

    /**
     * Create a {@link FeedPreferences} instance from a database row (cursor).
     */
    @NonNull
    public FeedPreferences getFeedPreferences() {
        String tagsString = getString(indexTags);
        if (TextUtils.isEmpty(tagsString)) {
            tagsString = FeedPreferences.TAG_ROOT;
        }
        return new FeedPreferences(
                getLong(indexId),
                FeedPreferences.AutoDownloadSetting.fromInteger(getInt(indexAutoDownload)),
                getInt(indexAutoRefresh) > 0,
                FeedPreferences.AutoDeleteAction.fromCode(getInt(indexAutoDeleteAction)),
                VolumeAdaptionSetting.fromInteger(getInt(indexVolumeAdaption)),
                getString(indexUsername),
                getString(indexPassword),
                new FeedFilter(getString(indexIncludeFilter),
                        getString(indexExcludeFilter), getInt(indexMinimalDurationFilter)),
                getFloat(indexFeedPlaybackSpeed),
                getInt(indexAutoSkipIntro),
                getInt(indexAutoSkipEnding),
                FeedPreferences.SkipSilence.fromCode(getInt(indexFeedSkipSilence)),
                getInt(indexEpisodeNotification) > 0,
                FeedPreferences.NewEpisodesAction.fromCode(getInt(indexNewEpisodesAction)),
                new HashSet<>(Arrays.asList(tagsString.split(FeedPreferences.TAG_SEPARATOR))));
    }
}

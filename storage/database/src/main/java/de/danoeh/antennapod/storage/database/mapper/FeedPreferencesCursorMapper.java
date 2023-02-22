package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
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
public abstract class FeedPreferencesCursorMapper {
    /**
     * Create a {@link FeedPreferences} instance from a database row (cursor).
     */
    @NonNull
    public static FeedPreferences convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_FEED_ID);
        int indexAutoDownload = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTO_DOWNLOAD_ENABLED);
        int indexAutoRefresh = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_KEEP_UPDATED);
        int indexAutoDeleteAction = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTO_DELETE_ACTION);
        int indexVolumeAdaption = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_VOLUME_ADAPTION);
        int indexUsername = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_USERNAME);
        int indexPassword = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PASSWORD);
        int indexIncludeFilter = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_INCLUDE_FILTER);
        int indexExcludeFilter = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_EXCLUDE_FILTER);
        int indexMinimalDurationFilter = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_MINIMAL_DURATION_FILTER);
        int indexFeedPlaybackSpeed = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_PLAYBACK_SPEED);
        int indexAutoSkipIntro = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_SKIP_INTRO);
        int indexAutoSkipEnding = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_SKIP_ENDING);
        int indexEpisodeNotification = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_EPISODE_NOTIFICATION);
        int indexNewEpisodesAction = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_NEW_EPISODES_ACTION);
        int indexTags = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_TAGS);

        long feedId = cursor.getLong(indexId);
        boolean autoDownload = cursor.getInt(indexAutoDownload) > 0;
        boolean autoRefresh = cursor.getInt(indexAutoRefresh) > 0;
        FeedPreferences.AutoDeleteAction autoDeleteAction =
                FeedPreferences.AutoDeleteAction.fromCode(cursor.getInt(indexAutoDeleteAction));
        int volumeAdaptionValue = cursor.getInt(indexVolumeAdaption);
        VolumeAdaptionSetting volumeAdaptionSetting = VolumeAdaptionSetting.fromInteger(volumeAdaptionValue);
        String username = cursor.getString(indexUsername);
        String password = cursor.getString(indexPassword);
        String includeFilter = cursor.getString(indexIncludeFilter);
        String excludeFilter = cursor.getString(indexExcludeFilter);
        int minimalDurationFilter = cursor.getInt(indexMinimalDurationFilter);
        float feedPlaybackSpeed = cursor.getFloat(indexFeedPlaybackSpeed);
        int feedAutoSkipIntro = cursor.getInt(indexAutoSkipIntro);
        int feedAutoSkipEnding = cursor.getInt(indexAutoSkipEnding);
        FeedPreferences.NewEpisodesAction feedNewEpisodesAction =
                FeedPreferences.NewEpisodesAction.fromCode(cursor.getInt(indexNewEpisodesAction));
        boolean showNotification = cursor.getInt(indexEpisodeNotification) > 0;
        String tagsString = cursor.getString(indexTags);
        if (TextUtils.isEmpty(tagsString)) {
            tagsString = FeedPreferences.TAG_ROOT;
        }
        return new FeedPreferences(feedId,
                autoDownload,
                autoRefresh,
                autoDeleteAction,
                volumeAdaptionSetting,
                username,
                password,
                new FeedFilter(includeFilter, excludeFilter, minimalDurationFilter),
                feedPlaybackSpeed,
                feedAutoSkipIntro,
                feedAutoSkipEnding,
                showNotification,
                feedNewEpisodesAction,
                new HashSet<>(Arrays.asList(tagsString.split(FeedPreferences.TAG_SEPARATOR))));
    }
}

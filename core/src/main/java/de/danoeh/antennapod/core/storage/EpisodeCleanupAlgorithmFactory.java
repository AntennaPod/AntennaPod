package de.danoeh.antennapod.core.storage;

import de.danoeh.antennapod.core.preferences.UserPreferences;

public abstract class EpisodeCleanupAlgorithmFactory {
    public static EpisodeCleanupAlgorithm build() {
        if (!UserPreferences.isEnableAutodownload()) {
            return new APNullCleanupAlgorithm();
        }
        int cleanupValue = UserPreferences.getEpisodeCleanupValue();
        switch (cleanupValue) {
            case UserPreferences.EPISODE_CLEANUP_EXCEPT_FAVORITE:
                return new ExceptFavoriteCleanupAlgorithm();
            case UserPreferences.EPISODE_CLEANUP_QUEUE:
                return new APQueueCleanupAlgorithm();
            case UserPreferences.EPISODE_CLEANUP_NULL:
                return new APNullCleanupAlgorithm();
            default:
                return new APCleanupAlgorithm(cleanupValue);
        }
    }
}

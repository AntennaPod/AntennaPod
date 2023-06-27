package de.danoeh.antennapod.core.storage;

import android.content.Context;

import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public abstract class EpisodeCleanupAlgorithm {

    /**
     * Deletes downloaded episodes that are no longer needed. What episodes are deleted and how many
     * of them depends on the implementation.
     *
     * @param context     Can be used for accessing the database
     * @param numToRemove An additional parameter. This parameter is either returned by getDefaultCleanupParameter
     *                    or getPerformCleanupParameter.
     * @return The number of episodes that were deleted.
     */
    protected abstract int performCleanup(Context context, int numToRemove);

    public int performCleanup(Context context) {
        return performCleanup(context, getDefaultCleanupParameter());
    }

    /**
     * Returns a parameter for performCleanup. The implementation of this interface should decide how much
     * space to free to satisfy the episode cache conditions. If the conditions are already satisfied, this
     * method should not have any effects.
     */
    protected abstract int getDefaultCleanupParameter();

    /**
     * Cleans up just enough episodes to make room for the requested number
     *
     * @param context            Can be used for accessing the database
     * @param amountOfRoomNeeded the number of episodes we need space for
     * @return The number of epiosdes that were deleted
     */
    public int makeRoomForEpisodes(Context context, int amountOfRoomNeeded) {
        return performCleanup(context, getNumEpisodesToCleanup(amountOfRoomNeeded));
    }

    /**
     * @return the number of episodes/items that *could* be cleaned up, if needed
     */
    public abstract int getReclaimableItems();

    /**
     * @param amountOfRoomNeeded the number of episodes we want to download
     * @return the number of episodes to delete in order to make room
     */
    int getNumEpisodesToCleanup(final int amountOfRoomNeeded) {
        if (amountOfRoomNeeded >= 0
                && UserPreferences.getEpisodeCacheSize() != UserPreferences.EPISODE_CACHE_SIZE_UNLIMITED) {
            int downloadedEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.DOWNLOADED));
            if (downloadedEpisodes + amountOfRoomNeeded >= UserPreferences
                    .getEpisodeCacheSize()) {

                return downloadedEpisodes + amountOfRoomNeeded
                        - UserPreferences.getEpisodeCacheSize();
            }
        }
        return 0;
    }
}

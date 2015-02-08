package de.danoeh.antennapod.core.storage;

import android.content.Context;

import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;

public interface EpisodeCleanupAlgorithm<T> {

    /**
     * Deletes downloaded episodes that are no longer needed. What episodes are deleted and how many
     * of them depends on the implementation.
     *
     * @param context   Can be used for accessing the database
     * @param parameter An additional parameter. This parameter is either returned by getDefaultCleanupParameter
     *                  or getPerformCleanupParameter.
     * @return The number of episodes that were deleted.
     */
    public int performCleanup(Context context, T parameter);

    /**
     * Returns a parameter for performCleanup. The implementation of this interface should decide how much
     * space to free to satisfy the episode cache conditions. If the conditions are already satisfied, this
     * method should not have any effects.
     */
    public T getDefaultCleanupParameter(Context context);

    /**
     * Returns a parameter for performCleanup.
     *
     * @param items A list of FeedItems that are about to be downloaded. The implementation of this interface
     *              should decide how much space to free to satisfy the episode cache conditions.
     */
    public T getPerformCleanupParameter(Context context, List<FeedItem> items);
}

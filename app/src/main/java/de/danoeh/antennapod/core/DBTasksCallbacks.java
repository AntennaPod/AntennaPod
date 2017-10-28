package de.danoeh.antennapod.core;

import de.danoeh.antennapod.core.storage.AutomaticDownloadAlgorithm;
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithm;

/**
 * Callbacks for the DBTasks class of the storage module.
 */
public interface DBTasksCallbacks {

    /**
     * Returns the client's implementation of the AutomaticDownloadAlgorithm interface.
     */
    AutomaticDownloadAlgorithm getAutomaticDownloadAlgorithm();

    /**
     * Returns the client's implementation of the EpisodeCacheCleanupAlgorithm interface.
     */
    EpisodeCleanupAlgorithm getEpisodeCacheCleanupAlgorithm();
}

package de.danoeh.antennapodSA.config;

import de.danoeh.antennapodSA.core.DBTasksCallbacks;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.storage.APDownloadAlgorithm;
import de.danoeh.antennapodSA.core.storage.AutomaticDownloadAlgorithm;
import de.danoeh.antennapodSA.core.storage.EpisodeCleanupAlgorithm;

public class DBTasksCallbacksImpl implements DBTasksCallbacks {

    @Override
    public AutomaticDownloadAlgorithm getAutomaticDownloadAlgorithm() {
        return new APDownloadAlgorithm();
    }

    @Override
    public EpisodeCleanupAlgorithm getEpisodeCacheCleanupAlgorithm() {
        return UserPreferences.getEpisodeCleanupAlgorithm();
    }
}

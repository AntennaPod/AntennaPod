package de.danoeh.antennapod.config;

import android.support.annotation.NonNull;

import de.danoeh.antennapod.core.DBTasksCallbacks;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.APDownloadAlgorithm;
import de.danoeh.antennapod.core.storage.AutomaticDownloadAlgorithm;
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithm;

public class DBTasksCallbacksImpl implements DBTasksCallbacks {

    @NonNull
    @Override
    public AutomaticDownloadAlgorithm getAutomaticDownloadAlgorithm() {
        return new APDownloadAlgorithm();
    }

    @NonNull
    @Override
    public EpisodeCleanupAlgorithm getEpisodeCacheCleanupAlgorithm() {
        return UserPreferences.getEpisodeCleanupAlgorithm();
    }
}

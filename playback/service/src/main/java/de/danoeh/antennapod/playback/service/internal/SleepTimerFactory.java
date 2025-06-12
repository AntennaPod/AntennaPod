package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;

import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;

public class SleepTimerFactory {
    /**
     * Creates a new SleepTimer instance based on the current config
     * Will either create a sleep time that counts down clock seconds, counts
     * down playback seconds (similar to clock, but adjusted for playback speed)
     * or episode counter
     * @param context Context to use
     * @param sleepDurationOrEpisodes Either duration in millis or number of episodes
     * @return The selected SleepTimer type
     */
    public static SleepTimer createSleepTimer(final Context context, long sleepDurationOrEpisodes) {
        return switch (SleepTimerPreferences.getSleepTimerType()) {
            case CLOCK -> new ClockSleepTimer(context, sleepDurationOrEpisodes);
            case EPISODES -> new EpisodeSleepTimer(context, sleepDurationOrEpisodes);
        };
    }
}
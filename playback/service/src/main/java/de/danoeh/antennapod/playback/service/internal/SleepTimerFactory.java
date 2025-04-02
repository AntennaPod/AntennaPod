package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;

public class SleepTimerFactory {
    /**
     * Creates a new SleepTimer instance based on the current config
     * Will either create a sleep time that counts down clock seconds
     * @param context Context to use
     * @param sleepDurationOrEpisodes Either duration in millis or number of episodes
     * @return The selected SleepTimer type
     */
    public static SleepTimer createSleepTimer(final Context context, long sleepDurationOrEpisodes) {
        return new ClockSleepTimer(context, sleepDurationOrEpisodes);
    }
}

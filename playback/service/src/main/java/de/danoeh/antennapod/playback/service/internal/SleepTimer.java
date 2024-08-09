package de.danoeh.antennapod.playback.service.internal;

public interface SleepTimer {

    long NOTIFICATION_THRESHOLD = 10000;

    /**
     * @return Returns time left for this timer, in millis
     */
    long getTimeLeft();

    /**
     * Cancels (stops) current sleep timer forever, cannot be restarted.
     */
    void stop();

    /**
     * Temporarily pauses sleep timer
     */
    void pause();

    /**
     * Resume previously paused sleep timer
     */
    void resume();

    /**
     * Update sleep timer with new waiting time
     * @param waitingTimeOrEpisodes Waiting time in millis or episode count
     */
    void reset(long waitingTimeOrEpisodes);

    /**
     * @return True if sleep timer is active, false otherwise
     */
    boolean isActive();

    /**
     *
     * @param episodeRemainingMillis The number of milliseconds left in this episode
     * @return False if the sleep timer doesn't extend past this episode, true if it ends during (or at the end)
     *         of the current episode.
     */
    boolean isEndingThisEpisode(long episodeRemainingMillis);
}


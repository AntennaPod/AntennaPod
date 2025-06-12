package de.danoeh.antennapod.playback.service.internal;

public interface SleepTimer {

    long NOTIFICATION_THRESHOLD = 10000;

    /**
     * @return Returns time left for this sleep timer, both display value and in milis
     */
    TimerValue getTimeLeft();

    /**
     * Cancels (stops) current sleep timer forever, cannot be restarted.
     */
    void stop();

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
     * @param episodeRemainingMillis Remaining milliseconds of current episode
     * @return Returns true if the sleep timer will terminate sometime during this episode, false otherwise
     */
    boolean isEndingThisEpisode(long episodeRemainingMillis);

}

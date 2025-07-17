package de.danoeh.antennapod.playback.service.internal;

public interface SleepTimer {

    long NOTIFICATION_THRESHOLD = 10000;

    /**
     * @return Returns time left for this timer, in millis
     */
    long getTimeLeft();

    /**
     * Starts the sleep timer.
     * @param initialWaitingTime The waiting time for the sleep timer, either episodes or duration
     */
    void start(long initialWaitingTime);

    /**
     * Cancels (stops) current sleep timer forever, cannot be restarted.
     */
    void stop();

    /**
     * Update sleep timer with new waiting time
     * @param waitingTimeOrEpisodes Waiting time in millis or episode count
     */
    void updateRemainingTime(long waitingTimeOrEpisodes);

    /**
     * Resets sleep timer to original duration.
     */
    void reset();

    /**
     * @return True if sleep timer is active, false otherwise
     */
    boolean isActive();
}

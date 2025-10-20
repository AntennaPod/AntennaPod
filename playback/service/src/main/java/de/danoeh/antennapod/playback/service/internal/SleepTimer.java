package de.danoeh.antennapod.playback.service.internal;

import de.danoeh.antennapod.model.playback.TimerValue;

public interface SleepTimer {

    long NOTIFICATION_THRESHOLD = 10000;

    /**
     * @return Returns time left for this sleep timer, both display value and in milis
     */
    TimerValue getTimeLeft();

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

    /**
     * @param episodeRemainingMillis Remaining milliseconds of current episode
     * @return Returns true if the sleep timer will terminate sometime during this episode, false otherwise
     */
    boolean isEndingThisEpisode(long episodeRemainingMillis);

    /**
     * Called when sleep timer is asked if playback is allowed to proceed to next episode.
     * Should take into account the time left, episodes left, etc.
     * @return True if playback is allowed to continue to next episode, false otherwise
     */
    boolean shouldContinueToNextEpisode();

    void episodeFinishedPlayback();
}

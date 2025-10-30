package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.model.playback.TimerValue;

public class EpisodeSleepTimer extends ClockSleepTimer {

    public EpisodeSleepTimer(final Context context) {
        super(context);
    }

    @Override
    public boolean isEndingThisEpisode(long episodeRemainingMillis) {
        return getTimeLeft().getDisplayValue() == 1;
    }

    @Override
    public TimerValue getTimeLeft() {
        TimerValue x = super.getTimeLeft();
        return new TimerValue(x.getDisplayValue(), TimeUnit.DAYS.toMillis(x.getDisplayValue()));
    }

    @Override
    public void playbackPositionUpdate(PlaybackPositionEvent playbackPositionEvent) {
        long currentEpisodeTimeLeft = playbackPositionEvent.getDuration() - playbackPositionEvent.getPosition();

        final TimerValue current = getTimeLeft();

        if (isEndingThisEpisode(playbackPositionEvent.getPosition())) {
            // if we're ending this episode send the "correct" remaining time
            // this ensures that the last 10 seconds the playback volume will be reduced
            EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(new TimerValue(
                    current.getDisplayValue(), currentEpisodeTimeLeft)));

            if (currentEpisodeTimeLeft < NOTIFICATION_THRESHOLD) {
                notifyAboutExpiry();
            }
        } else {
            // if we have more than 1 episode left then just report the current values
            EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(current));
        }
    }

    @Override
    public void episodeFinishedPlayback() {
        // episode has finished, decrease the number of episodes left
        updateRemainingTime(getTimeLeft().getDisplayValue() - 1);
    }

    @Override
    public boolean shouldContinueToNextEpisode() {
        boolean cont = getTimeLeft().getDisplayValue() > 0; // number of episodes left
        // stop ourselves too if we're blocking playback
        if (!cont) {
            stop();
        }

        return cont;
    }
}

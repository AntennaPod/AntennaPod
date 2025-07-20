package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.playback.service.PlaybackEndedEvent;

public class EpisodeSleepTimer extends ClockSleepTimer {
    private static final String TAG = "EpisodeSleepTimer";

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
            EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(
                    current.getDisplayValue(), currentEpisodeTimeLeft));

            if (currentEpisodeTimeLeft < NOTIFICATION_THRESHOLD) {
                notifyAboutExpiry();
            }
            // try to stop the playback with one second remaining on the last episode
            // just so we keep this episode in queue
            if (currentEpisodeTimeLeft <= 1000) {
                Log.d(TAG, "Episodes sleep timer expired");
                stop();
            }
        } else {
            EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(
                    current.getDisplayValue(), current.getMilisValue()));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void onPlaybackEnded(PlaybackEndedEvent playbackEndedEvent) {
        handlePlayedEpisode();
    }

    private void handlePlayedEpisode() {
        updateRemainingTime(getTimeLeft().getDisplayValue() - 1);

        if (getTimeLeft().getDisplayValue() <= 0) {
            // we've ran out of episodes, playback will be stopped
            stop();
        }
    }

}
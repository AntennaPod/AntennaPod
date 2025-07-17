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

    public EpisodeSleepTimer(final Context context, long remainingEpisodes) {
        super(context, remainingEpisodes);
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
            EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(
                    current.getDisplayValue(), currentEpisodeTimeLeft));

            if (currentEpisodeTimeLeft < NOTIFICATION_THRESHOLD) {
                notifyAboutExpiry();
            }
            if (currentEpisodeTimeLeft <= 0) {
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
        setTimeLeft(getTimeLeft().getDisplayValue() - 1);

        if (getTimeLeft().getDisplayValue() <= 0) {
            // we've ran out of episodes, playback will be stopped
            stop();
        }
    }

}
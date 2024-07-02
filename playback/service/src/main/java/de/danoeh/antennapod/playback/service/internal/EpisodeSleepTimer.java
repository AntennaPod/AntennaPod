package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.event.playback.PlaybackEndedEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;

public class EpisodeSleepTimer extends BaseSleepTimer implements SleepTimer {
    private static final String TAG = "EpisodeSleepTimer";
    private final Context context;
    private static final long UPDATE_INTERVAL = 1000L;

    // if we're trying to extend the duration with more than this value, the duration probably is not in episodes
    // but in days instead
    private static final long REASONABLE_EPISODE_EXTENSION = 50;

    private boolean subscribed = false;
    private long remainingEpisodes = 0;
    private final long initialEpisodeCount;

    public EpisodeSleepTimer(final Context context, long remainingEpisodes) {
        this.context = context;
        this.remainingEpisodes = remainingEpisodes;
        this.initialEpisodeCount = remainingEpisodes;
        init();
    }

    private void init() {
        Log.d(TAG, "Initializing");
        EventBus.getDefault().register(this);
        subscribed = true;
        resume();

        EventBus.getDefault().post(SleepTimerUpdatedEvent.justEnabled(remainingEpisodes));
    }

    @Override
    public void run() {
        while (remainingEpisodes > 0) {
            try {
                Thread.sleep(UPDATE_INTERVAL);
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread was interrupted while waiting");
                e.printStackTrace();
                break;
            }

            EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(remainingEpisodes));
        }
    }

    @Override
    protected Context getContext() {
        return context;
    }

    @Override
    protected long getInitialTime() {
        return initialEpisodeCount;
    }

    @Override
    public long getTimeLeft() {
        if (subscribed) {
            // assume episode length is up to 24 hours, so we have 24 hours * episodes left
            // we will show the minimum between episode length and this
            return TimeUnit.DAYS.toMillis(remainingEpisodes);
        }

        return 0;
    }

    @Override
    public void stop() {
        Log.d(TAG, "Stopping");
        super.stop();

        if (subscribed) {
            EventBus.getDefault().unregister(this);
        }
        subscribed = false;
    }

    @Override
    public void reset(long waitingTimeOrEpisodes) {
        if (waitingTimeOrEpisodes > REASONABLE_EPISODE_EXTENSION) {
            // the waiting time is probably in days converted to milliseconds, we can't use that
            // someone called getTimeLeft() and reset the time with it
            // instead we remove the days from the value and use the remainder
            waitingTimeOrEpisodes = waitingTimeOrEpisodes
                    - TimeUnit.DAYS.toMillis(remainingEpisodes) + remainingEpisodes;
        }

        this.remainingEpisodes = waitingTimeOrEpisodes;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void onPlaybackEnded(PlaybackEndedEvent playbackEndedEvent) {
        handlePlayedEpisode();
    }

    private void handlePlayedEpisode() {
        remainingEpisodes--;

        //notify about "time left", which is actually remaining episodes
        EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(remainingEpisodes));

        if (remainingEpisodes == 1) {
            // one episode left, vibrate now
            Log.d(TAG, "A single episode remains to be played, vibrating to alert user");
            notifyAboutExpiry();
        } else if (remainingEpisodes <= 0) {
            // we've ran out of episodes, playback will be stopped
            stopShakeListener();
        }
    }

}

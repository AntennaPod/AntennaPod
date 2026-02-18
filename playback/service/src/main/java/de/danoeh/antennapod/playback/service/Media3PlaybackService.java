package de.danoeh.antennapod.playback.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionResult;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.danoeh.antennapod.playback.service.BuildConfig;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.BufferUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.playback.service.internal.MediaItemAdapter;
import de.danoeh.antennapod.playback.service.internal.MediaLibrarySessionCallback;
import de.danoeh.antennapod.playback.service.internal.PlayableUtils;
import de.danoeh.antennapod.playback.service.internal.SleepTimer;
import de.danoeh.antennapod.playback.service.internal.ClockSleepTimer;
import de.danoeh.antennapod.playback.service.internal.EpisodeSleepTimer;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerType;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.episodes.PlaybackSpeedUtils;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Media3PlaybackService extends MediaLibraryService {
    private static final String TAG = "M3PlaybackService";
    private static final long POSITION_SAVE_INTERVAL_MS = 5000;

    private Player player;
    private MediaLibrarySession mediaSession;
    private FeedMedia currentPlayable;
    private Disposable mediaLoaderDisposable;
    private Disposable positionObserverDisposable;
    private Disposable queueLoaderDisposable;
    private Disposable completionDisposable;
    private SleepTimer sleepTimer;
    private long lastPositionSaveTime = 0;
    private final Handler gapHandler = new Handler(Looper.getMainLooper());

    @UnstableApi
    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        DefaultMediaNotificationProvider notificationProvider = new DefaultMediaNotificationProvider(
            this,
            session -> R.id.notification_playing,
            NotificationUtils.CHANNEL_ID_PLAYING,
            R.string.notification_channel_playing
        );
        notificationProvider.setSmallIcon(R.drawable.ic_notification);
        setMediaNotificationProvider(notificationProvider);

        Player basePlayer = new ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(UserPreferences.getRewindSecs() * 1000L)
                .setSeekForwardIncrementMs(UserPreferences.getFastForwardSecs() * 1000L)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(), true)
                .build();
        player = new ForwardingPlayer(basePlayer) {
            @Override
            @NonNull
            public Player.Commands getAvailableCommands() {
                return super.getAvailableCommands()
                        .buildUpon()
                        .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build();
            }

            @Override
            public void setPlaybackSpeed(float speed) {
                super.setPlaybackSpeed(speed);
                PlaybackPreferences.setCurrentlyPlayingTemporaryPlaybackSpeed(speed);
                EventBus.getDefault().post(new SpeedChangedEvent(speed));
            }
        };
        player.addListener(playerListener);
        mediaSession = new MediaLibraryService.MediaLibrarySession.Builder(this, player, sessionCallback)
                .setSessionActivity(new MainActivityStarter(this).withOpenPlayer().getPendingIntent())
                .build();
    }

    MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback(this) {
        @Override
        @NonNull
        @UnstableApi
        public ListenableFuture<SessionResult> onCustomCommand(@NonNull MediaSession session,
                                                               @NonNull MediaSession.ControllerInfo controller,
                                                               @NonNull SessionCommand customCommand,
                                                               @NonNull Bundle args) {
            if (customCommand.customAction.equals(SESSION_COMMAND_PLAYBACK_SPEED.customAction)) {
                setNextPlaybackSpeed();
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(SESSION_COMMAND_SKIP_TO_NEXT.customAction)) {
                if (currentPlayable != null) {
                    startNextInQueue(currentPlayable.getItem());
                }
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(SESSION_COMMAND_NEXT_CHAPTER.customAction)) {
                seekToNextChapter();
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(MediaLibrarySessionCallback.SESSION_COMMAND_SLEEP_TIMER_SET.customAction)) {
                long timerValue = args.getLong(PlaybackController.MEDIA3_SLEEP_TIMER_VALUE_KEY, 0);
                if (timerValue > 0) {
                    setSleepTimer(timerValue);
                    return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
                }
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE));
            } else if (customCommand.customAction.equals(MediaLibrarySessionCallback.SESSION_COMMAND_SLEEP_TIMER_DISABLE.customAction)) {
                disableSleepTimer();
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(MediaLibrarySessionCallback.SESSION_COMMAND_SLEEP_TIMER_EXTEND.customAction)) {
                long extendValue = args.getLong(PlaybackController.MEDIA3_SLEEP_TIMER_VALUE_KEY, 0);
                extendSleepTimer(extendValue);
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            }
            return super.onCustomCommand(session, controller, customCommand, args);
        }
    };

    @UnstableApi
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_BUFFERING) {
                EventBus.getDefault().post(BufferUpdateEvent.started());
                PlaybackService.isRunning = true; // Immediately show as playing
                updatePlaybackPreferences();
            } else {
                EventBus.getDefault().post(BufferUpdateEvent.ended());
            }
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                saveCurrentPosition();
            }
            if (playbackState == Player.STATE_ENDED && currentPlayable != null) {
                FeedMedia media = currentPlayable;
                currentPlayable = null; // To avoid position updater saving position after we already reset it
                gapHandler.postDelayed(() -> startNextAfterCompletion(media), 2000);
            }
            EventBus.getDefault().post(new PlayerStatusEvent());
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            PlaybackService.isRunning = !Util.shouldShowPlayButton(player);
            if (PlaybackService.isRunning) {
                lastPositionSaveTime = System.currentTimeMillis();
                setupPositionObserver();
            } else {
                cancelPositionObserver();
                saveCurrentPosition();
                if (currentPlayable != null) {
                    SynchronizationQueue.getInstance().enqueueEpisodePlayed(currentPlayable, false);
                }
            }
            updatePlaybackPreferences();
            EventBus.getDefault().post(new PlayerStatusEvent());
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            ensureCurrentMediaLoaded();
            EventBus.getDefault().post(new PlayerStatusEvent());
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            if (NetworkUtils.wasDownloadBlocked(error)) {
                EventBus.getDefault().post(new PlayerErrorEvent(getString(R.string.download_error_blocked)));
                EventBus.getDefault().post(new MessageEvent(getString(R.string.download_error_blocked)));
            } else {
                Throwable cause = error.getCause();
                if (cause instanceof HttpDataSource.HttpDataSourceException) {
                    if (cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                }
                if (cause != null && "Source error".equals(cause.getMessage())) {
                    cause = cause.getCause();
                }
                if (cause != null && cause.getMessage() != null) {
                    EventBus.getDefault().post(new PlayerErrorEvent(cause.getMessage()));
                    String friendly = mapToUserMessage(cause.getMessage());
                    if (!TextUtils.isEmpty(friendly)) {
                        EventBus.getDefault().post(new MessageEvent(friendly));
                    }
                } else if (error.getMessage() != null && cause != null) {
                    EventBus.getDefault().post(new PlayerErrorEvent(
                            error.getMessage() + ": " + cause.getClass().getSimpleName()));
                    String friendly = mapToUserMessage(error.getMessage());
                    if (!TextUtils.isEmpty(friendly)) {
                        EventBus.getDefault().post(new MessageEvent(friendly));
                    }
                } else {
                    EventBus.getDefault().post(new PlayerErrorEvent(null));
                    String friendly = mapToUserMessage(error.getMessage());
                    if (!TextUtils.isEmpty(friendly)) {
                        EventBus.getDefault().post(new MessageEvent(friendly));
                    }
                }
            }
        }
    };

    @Nullable
    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @UnstableApi
    @Override
    public void onDestroy() {
        PlaybackService.isRunning = false;
        cancelPositionObserver();
        EventBus.getDefault().unregister(this);
        if (mediaLoaderDisposable != null) {
            mediaLoaderDisposable.dispose();
            mediaLoaderDisposable = null;
        }
        if (queueLoaderDisposable != null) {
            queueLoaderDisposable.dispose();
            queueLoaderDisposable = null;
        }
        if (completionDisposable != null) {
            completionDisposable.dispose();
            completionDisposable = null;
        }
        gapHandler.removeCallbacksAndMessages(null);
        saveCurrentPosition();
        if (player != null) {
            player.removeListener(playerListener);
            player.release();
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }

    private void setupPositionObserver() {
        if (positionObserverDisposable != null) {
            positionObserverDisposable.dispose();
        }

        positionObserverDisposable = Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        ignored -> {
                            if (currentPlayable != null && player != null) {
                                long position = player.getCurrentPosition();
                                long duration = player.getDuration();
                                if (duration > 0) {
                                    EventBus.getDefault().post(
                                            new PlaybackPositionEvent((int) position, (int) duration));
                                    long currentTime = System.currentTimeMillis();
                                    if (currentTime - lastPositionSaveTime >= POSITION_SAVE_INTERVAL_MS) {
                                        saveCurrentPosition();
                                        lastPositionSaveTime = currentTime;
                                    }
                                }
                            }
                        }, error -> Log.e(TAG, "Position observer error", error)
                );
    }

    private void cancelPositionObserver() {
        if (positionObserverDisposable != null) {
            positionObserverDisposable.dispose();
            positionObserverDisposable = null;
        }
    }

    private void setSleepTimer(long waitingTime) {
        Log.d(TAG, "Setting sleep timer (media3) to " + waitingTime);
        if (waitingTime <= 0) {
            return;
        }
        if (sleepTimer != null && sleepTimer.isActive()) {
            sleepTimer.updateRemainingTime(waitingTime);
            return;
        }
        sleepTimer = SleepTimerPreferences.getSleepTimerType() == SleepTimerType.CLOCK
                ? new ClockSleepTimer(getApplicationContext())
                : new EpisodeSleepTimer(getApplicationContext());
        sleepTimer.start(waitingTime);
    }

    private void extendSleepTimer(long extendTime) {
        if (sleepTimer == null || !sleepTimer.isActive()) {
            return;
        }
        long millisLeft = sleepTimer.getTimeLeft().getMillisValue();
        sleepTimer.updateRemainingTime(millisLeft + extendTime);
    }

    private void disableSleepTimer() {
        if (sleepTimer != null) {
            sleepTimer.stop();
            sleepTimer = null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void sleepTimerUpdate(SleepTimerUpdatedEvent event) {
        if (player == null) {
            return;
        }
        if (event.isOver()) {
            player.pause();
            player.setVolume(1.0f);
            long newPosition = player.getCurrentPosition() - SleepTimer.NOTIFICATION_THRESHOLD / 2;
            player.seekTo(Math.max(newPosition, 0));
        } else if (event.getMillisTimeLeft() < SleepTimer.NOTIFICATION_THRESHOLD) {
            final float[] multiplicators = {0.1f, 0.2f, 0.3f, 0.3f, 0.3f, 0.4f, 0.4f, 0.4f, 0.6f, 0.8f};
            int idx = Math.max(0, Math.min(multiplicators.length - 1,
                    (int) (event.getMillisTimeLeft() / 1000)));
            player.setVolume(multiplicators[idx]);
        } else if (event.isCancelled()) {
            player.setVolume(1.0f);
        }
    }

    private void ensureCurrentMediaLoaded() {
        if (player == null || player.getCurrentMediaItem() == null) {
            return;
        }
        try {
            long mediaId = Long.parseLong(player.getCurrentMediaItem().mediaId);
            if (currentPlayable == null || currentPlayable.getId() != mediaId) {
                if (mediaLoaderDisposable != null) {
                    mediaLoaderDisposable.dispose();
                }
                mediaLoaderDisposable = Single.fromCallable(() ->
                                DBReader.getFeedMedia(mediaId))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(media -> {
                            currentPlayable = media;
                            if (player != null) {
                                currentPlayable.setPosition((int) player.getCurrentPosition());
                            }
                            currentPlayable.onPlaybackStart();
                            if (currentPlayable.getItem() != null
                                    && !currentPlayable.getItem().isTagged(FeedItem.TAG_QUEUE)) {
                                DBWriter.addQueueItem(this, currentPlayable.getItem());
                            }
                            float speed = PlaybackSpeedUtils.getCurrentPlaybackSpeed(currentPlayable);
                            if (player != null) {
                                player.setPlaybackSpeed(speed);
                            }
                            updatePlaybackPreferences();
                            EventBus.getDefault().post(new PlayerStatusEvent());
                        },
                                error -> Log.e(TAG, "Failed to load current media", error));

            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid media ID: " + (player != null && player.getCurrentMediaItem() != null
                    ? player.getCurrentMediaItem().mediaId : "null"), e);
        }
    }

    private void updatePlaybackPreferences() {
        if (currentPlayable == null || player == null) {
            return;
        }
        PlaybackPreferences.writeMediaPlaying(currentPlayable);
        int status = Util.shouldShowPlayButton(player) ? PlaybackPreferences.PLAYER_STATUS_PAUSED
                : PlaybackPreferences.PLAYER_STATUS_PLAYING;
        PlaybackPreferences.setCurrentPlayerStatus(status);
    }

    private void saveCurrentPosition() {
        if (currentPlayable == null || player == null) {
            return;
        }
        try {
            if (player.getCurrentMediaItem() == null
                    || currentPlayable.getId() != Long.parseLong(player.getCurrentMediaItem().mediaId)) {
                return;
            }
        } catch (NumberFormatException e) {
            return;
        }
        long position = player.getCurrentPosition();
        long timestamp = System.currentTimeMillis();
        PlayableUtils.saveCurrentPosition(currentPlayable, (int) position, timestamp);
    }

    private void onPlaybackEnd(FeedMedia media) {
        if (media == null) {
            return;
        }

        FeedItem item = media.getItem();
        int smartMarkAsPlayedSecs = UserPreferences.getSmartMarkAsPlayedSecs();
        boolean almostEnded = media.getDuration() > 0
                && media.getPosition() >= media.getDuration() - smartMarkAsPlayedSecs * 1000;

        if (almostEnded) {
            SynchronizationQueue.getInstance().enqueueEpisodePlayed(media, true);
            if (item != null) {
                DBWriter.markItemPlayed(FeedItem.PLAYED, true, item);
                DBWriter.removeQueueItem(this, true, item);
            }
            DBWriter.addItemToPlaybackHistory(media);
        } else {
            SynchronizationQueue.getInstance().enqueueEpisodePlayed(media, false);
        }
    }

    /**
     * Handles end-of-playback: first decide on the next item while the finished item is still in the queue,
     * then perform cleanup/removal of the finished item, and finally start the next if available.
     */
    @UnstableApi
    private void startNextAfterCompletion(FeedMedia finishedMedia) {
        FeedItem finishedItem = finishedMedia != null ? finishedMedia.getItem() : null;
        if (completionDisposable != null) {
            completionDisposable.dispose();
        }

        // If we lost the item context, just finish cleanup and stop.
        if (finishedItem == null) {
            onPlaybackEnd(finishedMedia);
            return;
        }

        completionDisposable = Single.fromCallable(() -> resolveNextPlayable(finishedItem))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(nextMedia -> {
                            onPlaybackEnd(finishedMedia);
                            if (nextMedia == null || player == null) {
                                return;
                            }
                            currentPlayable = nextMedia;
                            currentPlayable.onPlaybackStart();
                            MediaItem mediaItem = MediaItemAdapter.fromPlayable(nextMedia);
                            PlaybackPreferences.writeMediaPlaying(nextMedia);
                            boolean playWhenReady = shouldAutoStart(nextMedia, finishedItem);
                            player.setPlayWhenReady(playWhenReady);
                            player.setMediaItem(mediaItem);
                            player.seekTo(nextMedia.getPosition());
                            player.prepare();
                        }, error -> {
                            Log.e(TAG, "Failed to resolve next playable after completion", error);
                            onPlaybackEnd(finishedMedia);
                        }
                );
    }

    private void setNextPlaybackSpeed() {
        if (player == null) {
            return;
        }
        List<Float> selectedSpeeds = UserPreferences.getPlaybackSpeedArray();
        if (selectedSpeeds.size() <= 1) {
            return;
        }

        float currentSpeed = player.getPlaybackParameters().speed;
        int speedPosition = selectedSpeeds.indexOf(currentSpeed);
        float newSpeed;

        if (speedPosition == selectedSpeeds.size() - 1 || speedPosition == -1) {
            newSpeed = selectedSpeeds.get(0);
        } else {
            newSpeed = selectedSpeeds.get(speedPosition + 1);
        }

        player.setPlaybackSpeed(newSpeed);
    }

    @UnstableApi
    private void seekToNextChapter() {
        if (currentPlayable == null || player == null) {
            return;
        }
        List<Chapter> chapters = currentPlayable.getChapters();
        if (chapters == null) {
            if (currentPlayable.getItem() != null) {
                startNextInQueue(currentPlayable.getItem());
            }
            return;
        }

        int nextChapter = Chapter.getAfterPosition(chapters, (int) player.getCurrentPosition()) + 1;

        if (chapters.size() < nextChapter + 1) {
            if (currentPlayable.getItem() != null) {
                startNextInQueue(currentPlayable.getItem());
            }
            return;
        }

        player.seekTo(chapters.get(nextChapter).getStart());
    }

    /**
     * Loads the next item, and starts it if continuous playback is enabled.
     */
    @UnstableApi
    private void startNextInQueue(FeedItem item) {
        if (queueLoaderDisposable != null) {
            queueLoaderDisposable.dispose();
        }
        if (item == null) {
            return;
        }
        queueLoaderDisposable = Single.fromCallable(() -> {
            return resolveNextPlayable(item);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        nextMedia -> {
                            if (nextMedia != null) {
                                currentPlayable = nextMedia;
                                currentPlayable.onPlaybackStart();
                                MediaItem mediaItem = MediaItemAdapter.fromPlayable(nextMedia);
                                PlaybackPreferences.writeMediaPlaying(nextMedia);
                                boolean playWhenReady = shouldAutoStart(nextMedia, item);
                                player.setPlayWhenReady(playWhenReady);
                                player.setMediaItem(mediaItem);
                                player.seekTo(nextMedia.getPosition());
                                player.prepare();
                            }
                        },
                        error -> Log.e(TAG, "Failed to load next queue item", error)
                );
    }

    private FeedMedia resolveNextPlayable(FeedItem currentItem) {
        if (currentItem == null) {
            return null;
        }
        int autoAdvanceMode = PlaybackPreferences.getAutoAdvanceMode();
        boolean followQueue = UserPreferences.isFollowQueue();
        boolean queueMode = autoAdvanceMode == PlaybackPreferences.AUTO_ADVANCE_QUEUE;
        boolean podcastMode = autoAdvanceMode == PlaybackPreferences.AUTO_ADVANCE_PODCAST;
        boolean currentInQueue = currentItem.isTagged(FeedItem.TAG_QUEUE);

        // Always refresh feed to pick up the latest preferences (e.g., autoplay toggle changes).
        Feed feed = DBReader.getFeed(currentItem.getFeedId(), false, 0, Integer.MAX_VALUE);
        if (feed != null) {
            currentItem.setFeed(feed);
        }
        logDebug("resolveNextPlayable autoMode=" + autoAdvanceMode + " followQueue=" + followQueue
            + " currentInQueue=" + currentInQueue + " queueMode=" + queueMode
            + " podcastMode=" + podcastMode
            + " currentItem=" + currentItem.getId());

        if (queueMode) {
            // Respect the latest continuous playback pref (followQueue) when advancing in queue mode.
            followQueue = UserPreferences.isFollowQueue();
            if (!followQueue || !currentInQueue) {
                logDebug("resolveNextPlayable stop: queueMode but followQueue=" + followQueue
                        + " currentInQueue=" + currentInQueue);
                return null;
            }
            FeedItem nextItem = DBReader.getNextInQueue(currentItem);
            logDebug("resolveNextPlayable queueMode nextItem=" + (nextItem != null ? nextItem.getId() : "null"));
            if (nextItem == null) {
                List<FeedItem> queue = DBReader.getQueue();
                int size = queue != null ? queue.size() : 0;
                logDebug("resolveNextPlayable queueMode fallback queueSize=" + size);
                if (size == 0) {
                    return null;
                }
                int currentIndex = -1;
                for (int i = 0; i < size; i++) {
                    if (queue.get(i).getId() == currentItem.getId()) {
                        currentIndex = i;
                        break;
                    }
                }
                if (currentIndex >= 0 && currentIndex + 1 < size) {
                    nextItem = queue.get(currentIndex + 1);
                    logDebug("resolveNextPlayable queueMode fallbackAfterCurrent nextItem=" + nextItem.getId());
                } else if (currentIndex == -1) {
                    nextItem = queue.get(0);
                    logDebug("resolveNextPlayable queueMode fallbackStart nextItem=" + nextItem.getId());
                } else {
                    return null;
                }
            }
            if (nextItem == null || nextItem.getMedia() == null) {
                return null;
            }
            if (!nextItem.getMedia().localFileAvailable() && !NetworkUtils.isStreamingAllowed()
                    && !nextItem.getFeed().isLocalFeed()) {
                logDebug("resolveNextPlayable queueMode streaming blocked nextItem=" + nextItem.getId());
                postStreamingBlocked();
                return null;
            }
            return nextItem.getMedia();
        }

        if (podcastMode) {
            if (!followQueue) {
                logDebug("resolveNextPlayable stop: podcastMode but continuous playback OFF");
                return null;
            }
            FeedItem nextFeedItem = getNextFeedItem(currentItem);
            logDebug("resolveNextPlayable podcastMode nextItem=" + (nextFeedItem != null ? nextFeedItem.getId() : "null"));
            if (nextFeedItem == null || nextFeedItem.getMedia() == null) {
                return null;
            }
            FeedMedia media = nextFeedItem.getMedia();
            if (!media.localFileAvailable() && !NetworkUtils.isStreamingAllowed()
                    && !nextFeedItem.getFeed().isLocalFeed()) {
                logDebug("resolveNextPlayable podcastMode streaming blocked nextItem=" + nextFeedItem.getId());
                postStreamingBlocked();
                return null;
            }
            return media;
        }

        return null;
    }

    private boolean shouldAutoStart(FeedMedia nextMedia, FeedItem currentItem) {
        int autoAdvanceMode = PlaybackPreferences.getAutoAdvanceMode();
        boolean queueMode = autoAdvanceMode == PlaybackPreferences.AUTO_ADVANCE_QUEUE;
        boolean currentInQueue = currentItem != null && currentItem.isTagged(FeedItem.TAG_QUEUE);
        boolean followQueue = UserPreferences.isFollowQueue();
        if (!followQueue) {
            return false;
        }
        // Queue-centric playback should only auto-start when the current item belongs to the queue.
        return !queueMode || currentInQueue;
    }

    private void postStreamingBlocked() {
        EventBus.getDefault().post(new MessageEvent(getString(R.string.confirm_mobile_streaming_notification_message)));
    }

    private void logDebug(String message) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        Log.d(TAG, message);
        try {
            File logFile = new File(getApplicationContext().getExternalFilesDir(null), "autoplay_debug.log");
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(System.currentTimeMillis() + ": [Media3] " + message + "\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "logDebug write failed", e);
        }
    }

    private String mapToUserMessage(String raw) {
        if (!NetworkUtils.networkAvailable()) {
            return getString(R.string.error_no_network_playback);
        }
        if (TextUtils.isEmpty(raw)) {
            return getString(R.string.error_playback_generic);
        }
        String lower = raw.toLowerCase(Locale.US);
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return getString(R.string.error_connection_timeout);
        }
        if (lower.contains("unknownhost") || lower.contains("unknown host")
                || lower.contains("unable to resolve") || lower.contains("host") || lower.contains("unreachable")) {
            return getString(R.string.error_unreachable_host);
        }
        if (lower.contains("reset by peer") || lower.contains("connection closed") || lower.contains("broken pipe")) {
            return getString(R.string.error_stream_dropped);
        }
        return getString(R.string.error_playback_generic_with_reason, raw);
    }

    @Nullable
    private FeedItem getNextFeedItem(@NonNull FeedItem currentItem) {
        Feed feed = currentItem.getFeed();
        if (feed == null || feed.getItems() == null || feed.getItems().isEmpty()) {
            feed = DBReader.getFeed(currentItem.getFeedId(), false, 0, Integer.MAX_VALUE);
        }
        if (feed == null) {
            return null;
        }

        List<FeedItem> items = feed.getItems();
        if (items == null || items.isEmpty()) {
            FeedItemFilter filter = feed.getItemFilter() != null
                    ? feed.getItemFilter() : FeedItemFilter.unfiltered();
            items = DBReader.getFeedItemList(feed, filter, feed.getSortOrder(), 0, Integer.MAX_VALUE);
            feed.setItems(items);
        }
        if (items == null || items.isEmpty()) {
            return null;
        }

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == currentItem.getId()) {
                if (i + 1 < items.size()) {
                    return items.get(i + 1);
                }
                return null;
            }
        }
        return null;
    }
}

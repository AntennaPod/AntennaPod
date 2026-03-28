package de.danoeh.antennapod.playback.service;

import android.media.audiofx.LoudnessEnhancer;
import android.os.Bundle;
import android.util.Log;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.util.Pair;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionResult;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.event.StreamingConfirmationEvent;
import de.danoeh.antennapod.event.settings.VolumeAdaptionChangedEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.BufferUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.PlaybackServiceEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.cast.CastPlayerWrapper;
import de.danoeh.antennapod.playback.service.internal.ExoPlayerUtils;
import de.danoeh.antennapod.playback.service.internal.MediaLibrarySessionCallback;
import de.danoeh.antennapod.playback.service.internal.PlayableUtils;
import de.danoeh.antennapod.playback.service.internal.SkipUtils;
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
import de.danoeh.antennapod.ui.widget.WidgetUpdater;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Media3PlaybackService extends MediaLibraryService {
    private static final String TAG = "M3PlaybackService";
    private static final long POSITION_SAVE_INTERVAL_MS = 5000;
    private ExoPlayer exoPlayer;
    private Player player;
    private MediaLibrarySession mediaSession;
    private FeedMedia currentPlayable;
    private String pendingStreamMediaId;
    private boolean allowStreamingThisTime = false;
    private Disposable mediaLoaderDisposable;
    private Disposable positionObserverDisposable;
    private Disposable queueLoaderDisposable;
    private long lastPositionSaveTime = 0;
    private SleepTimer sleepTimer;
    @Nullable
    private LoudnessEnhancer loudnessEnhancer = null;
    private float volumeAdaptionFactor = 1.0f;

    @UnstableApi
    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        DefaultMediaNotificationProvider notificationProvider = new DefaultMediaNotificationProvider(this,
                session -> R.id.notification_playing,
                NotificationUtils.CHANNEL_ID_PLAYING, R.string.notification_channel_playing);
        notificationProvider.setSmallIcon(R.drawable.ic_notification);
        setMediaNotificationProvider(notificationProvider);

        exoPlayer = ExoPlayerUtils.buildPlayer(this);
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onAudioSessionIdChanged(int audioSessionId) {
                initLoudnessEnhancer(audioSessionId);
            }
        });
        initLoudnessEnhancer(exoPlayer.getAudioSessionId());
        Player maybeCastPlayer = CastPlayerWrapper.wrap(exoPlayer, this);
        player = new ForwardingPlayer(maybeCastPlayer) {
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
            public void play() {
                if (handleStreamingConfirmation()) {
                    return;
                } else if (shouldBlockForStreamingConfirmation()) {
                    showStreamingConfirmation(currentPlayable);
                    return;
                }
                super.play();
            }

            @Override
            public void setPlaybackSpeed(float speed) {
                super.setPlaybackSpeed(speed);
                PlaybackPreferences.setCurrentlyPlayingTemporaryPlaybackSpeed(speed);
                EventBus.getDefault().post(new SpeedChangedEvent(speed));
            }

            @Override
            public void seekBack() {
                seekTo(Math.max(0, getCurrentPosition() - UserPreferences.getRewindSecs() * 1000L));
            }

            @Override
            public void seekForward() {
                seekTo(Math.min(getDuration(), getCurrentPosition() + UserPreferences.getFastForwardSecs() * 1000L));
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
            } else if (customCommand.customAction.equals(SESSION_COMMAND_SKIP_SILENCE.customAction)) {
                boolean enabled = MediaLibrarySessionCallback.getBoolean(args, false);
                PlaybackPreferences.setCurrentlyPlayingTemporarySkipSilence(enabled);
                exoPlayer.setSkipSilenceEnabled(enabled);
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(SESSION_COMMAND_SET_SLEEP_TIMER.customAction)) {
                startSleepTimer(MediaLibrarySessionCallback.getLong(args, 0));
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(SESSION_COMMAND_DISABLE_SLEEP_TIMER.customAction)) {
                disableSleepTimer();
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(SESSION_COMMAND_EXTEND_SLEEP_TIMER.customAction)) {
                extendSleepTimer(MediaLibrarySessionCallback.getLong(args, 0));
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
                onPlaybackEnd(media);
                if (sleepTimer != null && sleepTimer.isActive()) {
                    sleepTimer.episodeFinishedPlayback();
                    if (!sleepTimer.shouldContinueToNextEpisode()) {
                        player.stop();
                        player.clearMediaItems();
                        PlaybackPreferences.writeNoMediaPlaying();
                        EventBus.getDefault().post(
                                new PlaybackServiceEvent(PlaybackServiceEvent.Action.SERVICE_SHUT_DOWN));
                        EventBus.getDefault().post(new PlayerStatusEvent());
                        return;
                    }
                }
                startNextInQueue(media.getItem());
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
            WidgetUpdater.WidgetState widgetState = new WidgetUpdater.WidgetState(currentPlayable,
                    PlaybackService.isRunning ? PlayerStatus.PLAYING : PlayerStatus.PAUSED,
                    (int) player.getContentPosition(), (int) player.getDuration(),
                    player.getPlaybackParameters().speed);
            WidgetUpdater.updateWidget(Media3PlaybackService.this, widgetState);
            updatePlaybackPreferences();
            EventBus.getDefault().post(new PlayerStatusEvent());

            // Auto-enable sleep timer when playback starts
            if (PlaybackService.isRunning && sleepTimer == null && SleepTimerPreferences.autoEnable()) {
                int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (SleepTimerPreferences.isInTimeRange(
                        SleepTimerPreferences.autoEnableFrom(),
                        SleepTimerPreferences.autoEnableTo(),
                        currentHour)) {
                    startSleepTimer(SleepTimerPreferences.timerMillisOrEpisodes());
                }
            }
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            ensureCurrentMediaLoaded();
            EventBus.getDefault().post(new PlayerStatusEvent());
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            EventBus.getDefault().post(new PlayerErrorEvent(
                    ExoPlayerUtils.translateErrorReason(error, Media3PlaybackService.this)));
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
        if (sleepTimer != null) {
            sleepTimer.stop();
            sleepTimer = null;
        }
        EventBus.getDefault().unregister(this);
        if (mediaLoaderDisposable != null) {
            mediaLoaderDisposable.dispose();
            mediaLoaderDisposable = null;
        }
        if (queueLoaderDisposable != null) {
            queueLoaderDisposable.dispose();
            queueLoaderDisposable = null;
        }
        saveCurrentPosition();
        if (loudnessEnhancer != null) {
            loudnessEnhancer.release();
            loudnessEnhancer = null;
        }
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
                            if (currentPlayable == null || player == null) {
                                return;
                            }
                            long position = player.getCurrentPosition();
                            long duration = player.getDuration();
                            float speed = player.getPlaybackParameters().speed;
                            if (duration > 0) {
                                EventBus.getDefault().post(
                                        new PlaybackPositionEvent((int) position, (int) duration));
                                WidgetUpdater.WidgetState widgetState = new WidgetUpdater.WidgetState(currentPlayable,
                                        Util.shouldShowPlayButton(player) ? PlayerStatus.PAUSED : PlayerStatus.PLAYING,
                                        (int) position, (int) duration, speed);
                                WidgetUpdater.updateWidget(this, widgetState);
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastPositionSaveTime >= POSITION_SAVE_INTERVAL_MS) {
                                    saveCurrentPosition();
                                    lastPositionSaveTime = currentTime;
                                }
                                if (SkipUtils.skipEndingIfNecessary(this, currentPlayable, position, duration, speed)) {
                                    player.seekTo(player.getDuration());
                                }
                            }
                        }, error -> Log.e(TAG, "Position observer error", error));
    }

    private void cancelPositionObserver() {
        if (positionObserverDisposable != null) {
            positionObserverDisposable.dispose();
            positionObserverDisposable = null;
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void ensureCurrentMediaLoaded() {
        if (player == null || player.getCurrentMediaItem() == null) {
            return;
        }
        if (MediaItemAdapter.MEDIA_ID_CONFIRM_STREAMING.equals(player.getCurrentMediaItem().mediaId)) {
            return;
        }
        pendingStreamMediaId = null;
        try {
            long mediaId = Long.parseLong(player.getCurrentMediaItem().mediaId);
            if (currentPlayable == null || currentPlayable.getId() != mediaId) {
                if (mediaLoaderDisposable != null) {
                    mediaLoaderDisposable.dispose();
                }
                mediaLoaderDisposable = Single.fromCallable(() -> DBReader.getFeedMedia(mediaId))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(media -> {
                            currentPlayable = media;
                            if (player == null) {
                                return;
                            }
                            if (needsStreaming(media) && !NetworkUtils.isStreamingAllowed()
                                    && !allowStreamingThisTime) {
                                showStreamingConfirmation(media);
                                return;
                            }
                            allowStreamingThisTime = false;
                            currentPlayable.setPosition((int) player.getCurrentPosition());
                            currentPlayable.onPlaybackStart();
                            if (currentPlayable.getItem() != null
                                    && !currentPlayable.getItem().isTagged(FeedItem.TAG_QUEUE)) {
                                DBWriter.addQueueItem(this, currentPlayable.getItem());
                            }
                            float speed = PlaybackSpeedUtils.getCurrentPlaybackSpeed(currentPlayable);
                            player.setPlaybackSpeed(speed);
                            boolean enabled = PlaybackSpeedUtils.getCurrentSkipSilencePreference(
                                    currentPlayable) == FeedPreferences.SkipSilence.AGGRESSIVE;
                            PlaybackPreferences.setCurrentlyPlayingTemporarySkipSilence(enabled);
                            exoPlayer.setSkipSilenceEnabled(enabled);
                            if (currentPlayable.getItem() != null
                                    && currentPlayable.getItem().getFeed() != null) {
                                volumeAdaptionFactor = currentPlayable.getItem().getFeed()
                                        .getPreferences().getVolumeAdaptionSetting().getAdaptionFactor();
                                applyVolumeAdaption(1.0f);
                            }
                            updatePlaybackPreferences();
                            EventBus.getDefault().post(new PlayerStatusEvent());
                        },
                                error -> Log.e(TAG, "Failed to load current media", error));

            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid media ID: " + (player != null && player.getCurrentMediaItem() != null
                    ? player.getCurrentMediaItem().mediaId
                    : "null"), e);
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

        SynchronizationQueue.getInstance().enqueueEpisodePlayed(media, almostEnded);
        if (almostEnded) {
            if (item != null) {
                DBWriter.markItemPlayed(FeedItem.PLAYED, true, item);
                DBWriter.removeQueueItem(this, true, item);
                FeedPreferences.AutoDeleteAction action = item.getFeed().getPreferences().getCurrentAutoDelete();
                boolean autoDeleteEnabledGlobally = UserPreferences.isAutoDelete()
                        && (!item.getFeed().isLocalFeed() || UserPreferences.isAutoDeleteLocal());
                boolean shouldAutoDelete = action == FeedPreferences.AutoDeleteAction.ALWAYS
                        || (action == FeedPreferences.AutoDeleteAction.GLOBAL && autoDeleteEnabledGlobally);
                if (shouldAutoDelete && (!item.isTagged(FeedItem.TAG_FAVORITE)
                        || !UserPreferences.shouldFavoriteKeepEpisode())) {
                    DBWriter.deleteFeedMediaOfItem(this, media);
                }
            }
            DBWriter.addItemToPlaybackHistory(media);
        }
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

    private boolean shouldBlockForStreamingConfirmation() {
        return currentPlayable != null
                && (player.getPlaybackState() == Player.STATE_READY
                    || player.getPlaybackState() == Player.STATE_BUFFERING)
                && needsStreaming(currentPlayable)
                && !NetworkUtils.isStreamingAllowed();
    }

    private boolean handleStreamingConfirmation() {
        if (pendingStreamMediaId != null && player.getCurrentMediaItem() != null
                && MediaItemAdapter.MEDIA_ID_CONFIRM_STREAMING.equals(player.getCurrentMediaItem().mediaId)) {
            final String mediaId = pendingStreamMediaId;
            pendingStreamMediaId = null;
            allowStreamingThisTime = true;
            MediaItem mediaItem = new MediaItem.Builder()
                    .setMediaId(mediaId)
                    .build();
            PlaybackController.bindToMedia3Service(
                    Media3PlaybackService.this, controller -> {
                        controller.setMediaItem(mediaItem);
                        controller.prepare();
                        controller.play();
                    });
            return true;
        }
        return false;
    }

    @UnstableApi
    private void showStreamingConfirmation(FeedMedia media) {
        pendingStreamMediaId = String.valueOf(media.getId());
        currentPlayable = null;
        MediaItem confirmItem = MediaItemAdapter.buildStreamingConfirmationItem(this,
                R.raw.no_streaming,
                getString(R.string.confirm_mobile_streaming_notification_title),
                getString(R.string.confirm_mobile_streaming_notification_message));
        player.setMediaItem(confirmItem);
        player.setPlayWhenReady(false);
        player.prepare();
        EventBus.getDefault().post(new StreamingConfirmationEvent());
    }

    private static boolean needsStreaming(FeedMedia media) {
        return !media.localFileAvailable() && !URLUtil.isContentUrl(media.getStreamUrl());
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
        queueLoaderDisposable = Maybe.fromCallable(() -> {
            FeedItem nextItem = DBReader.getNextInQueue(item);
            if (nextItem != null && nextItem.getMedia() != null) {
                return new Pair<>(nextItem.getMedia(), MediaItemAdapter.fromPlayable(Media3PlaybackService.this, nextItem.getMedia()));
            }
            return null;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        pair -> {
                            final FeedMedia nextMedia = pair.first;
                            final MediaItem nextMediaItem = pair.second;
                            if (needsStreaming(nextMedia) && !NetworkUtils.isStreamingAllowed()
                                    && !allowStreamingThisTime && UserPreferences.isFollowQueue()) {
                                showStreamingConfirmation(nextMedia);
                                return;
                            }
                            allowStreamingThisTime = false;

                            currentPlayable = nextMedia;
                            currentPlayable.onPlaybackStart();
                            PlaybackPreferences.writeMediaPlaying(nextMedia);
                            if (nextMedia.getItem() != null && nextMedia.getItem().getFeed() != null) {
                                volumeAdaptionFactor = nextMedia.getItem().getFeed()
                                        .getPreferences().getVolumeAdaptionSetting().getAdaptionFactor();
                                applyVolumeAdaption(1.0f);
                            }
                            player.setPlayWhenReady(UserPreferences.isFollowQueue());
                            player.setMediaItem(nextMediaItem);
                            player.seekTo(SkipUtils.skipIntroIfNecessary(this, nextMedia));
                            player.prepare();
                        },
                        error -> Log.e(TAG, "Failed to load next queue item", error),
                        () -> {
                            player.stop();
                            player.clearMediaItems();
                            PlaybackPreferences.writeNoMediaPlaying();
                            EventBus.getDefault().post(
                                    new PlaybackServiceEvent(PlaybackServiceEvent.Action.SERVICE_SHUT_DOWN));
                        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSleepTimerUpdated(SleepTimerUpdatedEvent event) {
        if (event.isOver()) {
            Log.d(TAG, "Sleep timer expired, pausing playback");
            if (player != null) {
                applyVolumeAdaption(1.0f);
                player.pause();
            }
            sleepTimer = null;
        } else if (event.isCancelled()) {
            applyVolumeAdaption(1.0f);
        } else if (!event.wasJustEnabled()) {
            long millisLeft = event.getMillisTimeLeft();
            if (millisLeft < SleepTimer.NOTIFICATION_THRESHOLD && millisLeft > 0) {
                float volume = (float) millisLeft / SleepTimer.NOTIFICATION_THRESHOLD;
                applyVolumeAdaption(Math.max(0.1f, volume));
            } else {
                applyVolumeAdaption(1.0f);
            }
        }
    }

    private void startSleepTimer(long timeOrEpisodes) {
        if (sleepTimer != null) {
            sleepTimer.stop();
        }
        if (SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES) {
            sleepTimer = new EpisodeSleepTimer(this);
        } else {
            sleepTimer = new ClockSleepTimer(this);
        }
        sleepTimer.start(timeOrEpisodes);
    }

    private void disableSleepTimer() {
        if (sleepTimer != null) {
            sleepTimer.stop();
            sleepTimer = null;
        }
        if (player != null) {
            applyVolumeAdaption(1.0f);
        }
    }

    private void extendSleepTimer(long additionalTime) {
        if (sleepTimer != null && sleepTimer.isActive()) {
            long currentLeft = sleepTimer.getTimeLeft().getDisplayValue();
            sleepTimer.updateRemainingTime(currentLeft + additionalTime);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void volumeAdaptionChanged(VolumeAdaptionChangedEvent event) {
        if (currentPlayable != null && currentPlayable.getItem() != null
                && currentPlayable.getItem().getFeed() != null
                && currentPlayable.getItem().getFeed().getId() == event.getFeedId()) {
            currentPlayable.getItem().getFeed().getPreferences()
                    .setVolumeAdaptionSetting(event.getVolumeAdaptionSetting());
            volumeAdaptionFactor = event.getVolumeAdaptionSetting().getAdaptionFactor();
            applyVolumeAdaption(1.0f);
        }
    }

    private void initLoudnessEnhancer(int audioSessionId) {
        if (!VolumeAdaptionSetting.isBoostSupported()) {
            return;
        }
        LoudnessEnhancer oldEnhancer = this.loudnessEnhancer;
        try {
            LoudnessEnhancer newEnhancer = new LoudnessEnhancer(audioSessionId);
            if (oldEnhancer != null) {
                newEnhancer.setEnabled(oldEnhancer.getEnabled());
                if (oldEnhancer.getEnabled()) {
                    newEnhancer.setTargetGain((int) oldEnhancer.getTargetGain());
                }
                oldEnhancer.release();
            }
            this.loudnessEnhancer = newEnhancer;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            this.loudnessEnhancer = null;
        }
    }

    private void applyVolumeAdaption(float baseVolume) {
        float v = baseVolume * volumeAdaptionFactor;
        if (v > 1) {
            player.setVolume(1.0f);
            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.setEnabled(true);
                    loudnessEnhancer.setTargetGain((int) (1000 * (v - 1)));
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        } else {
            player.setVolume(v);
            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.setEnabled(false);
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
    }
}

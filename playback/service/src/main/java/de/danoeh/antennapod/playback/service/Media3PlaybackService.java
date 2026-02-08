package de.danoeh.antennapod.playback.service;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionResult;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.playback.service.internal.MediaItemAdapter;
import de.danoeh.antennapod.playback.service.internal.MediaLibrarySessionCallback;
import de.danoeh.antennapod.playback.service.internal.PlayableUtils;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
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

import java.util.List;
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
    private long lastPositionSaveTime = 0;

    @UnstableApi
    @Override
    public void onCreate() {
        super.onCreate();
        DefaultMediaNotificationProvider notificationProvider
                = new DefaultMediaNotificationProvider(this, session -> R.id.notification_playing,
                NotificationUtils.CHANNEL_ID_PLAYING, R.string.notification_channel_playing);
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
            }
            return super.onCustomCommand(session, controller, customCommand, args);
        }
    };

    @UnstableApi
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                saveCurrentPosition();
            }
            if (playbackState == Player.STATE_ENDED && currentPlayable != null) {
                FeedMedia media = currentPlayable;
                currentPlayable = null; // To avoid position updater saving position after we already reset it
                onPlaybackEnd(media);
                startNextInQueue(media.getItem());
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            PlaybackService.isRunning = isPlaying;
            if (isPlaying) {
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
                } else if (error.getMessage() != null && cause != null) {
                    EventBus.getDefault().post(new PlayerErrorEvent(
                            error.getMessage() + ": " + cause.getClass().getSimpleName()));
                } else {
                    EventBus.getDefault().post(new PlayerErrorEvent(null));
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
        if (mediaLoaderDisposable != null) {
            mediaLoaderDisposable.dispose();
            mediaLoaderDisposable = null;
        }
        if (queueLoaderDisposable != null) {
            queueLoaderDisposable.dispose();
            queueLoaderDisposable = null;
        }
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
        int status = player.isPlaying() ? PlaybackPreferences.PLAYER_STATUS_PLAYING
                : PlaybackPreferences.PLAYER_STATUS_PAUSED;
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
            FeedItem nextItem = DBReader.getNextInQueue(item);
            return nextItem != null && nextItem.getMedia() != null ? nextItem.getMedia() : null;
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
                                player.setPlayWhenReady(UserPreferences.isFollowQueue());
                                player.setMediaItem(mediaItem);
                                player.seekTo(nextMedia.getPosition());
                                player.prepare();
                            }
                        },
                        error -> Log.e(TAG, "Failed to load next queue item", error)
                );
    }
}

package de.danoeh.antennapod.playback.service;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.playback.service.internal.PlayableUtils;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Media3PlaybackService extends MediaSessionService {
    private static final String TAG = "M3PlaybackService";
    private static final long POSITION_SAVE_INTERVAL_MS = 5000;
    private final SessionCommand SESSION_COMMAND_REWIND
            = new SessionCommand("rewind", Bundle.EMPTY);
    private final SessionCommand SESSION_COMMAND_FAST_FORWARD
            = new SessionCommand("fast_forward", Bundle.EMPTY);
    private final SessionCommand SESSION_COMMAND_PLAYBACK_SPEED
            = new SessionCommand("playback_speed", Bundle.EMPTY);
    private final SessionCommand SESSION_COMMAND_SKIP_TO_NEXT
            = new SessionCommand("skip_to_next", Bundle.EMPTY);
    private final SessionCommand SESSION_COMMAND_NEXT_CHAPTER
            = new SessionCommand("next_chapter", Bundle.EMPTY);

    private Player player;
    private MediaSession mediaSession;
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
        };
        player.addListener(playerListener);
        mediaSession = new MediaSession.Builder(this, player)
                .setCallback(callback)
                .build();
    }

    MediaSession.Callback callback = new MediaSession.Callback() {
        @Override
        @NonNull
        @UnstableApi
        public MediaSession.ConnectionResult onConnect(@NonNull MediaSession session,
                                                       @NonNull MediaSession.ControllerInfo controller) {
            SessionCommands sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SESSION_COMMAND_REWIND)
                    .add(SESSION_COMMAND_FAST_FORWARD)
                    .add(SESSION_COMMAND_PLAYBACK_SPEED)
                    .add(SESSION_COMMAND_SKIP_TO_NEXT)
                    .add(SESSION_COMMAND_NEXT_CHAPTER)
                    .build();
            Player.Commands playerCommands = new Player.Commands.Builder()
                    .addAllCommands()
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build();
            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setCustomLayout(buildCustomLayout())
                    .setAvailablePlayerCommands(playerCommands)
                    .build();
        }

        @Override
        @UnstableApi
        public void onPostConnect(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
            session.setCustomLayout(buildCustomLayout());
        }

        @Override
        @NonNull
        @UnstableApi
        public ListenableFuture<SessionResult> onCustomCommand(@NonNull MediaSession session,
                                                               @NonNull MediaSession.ControllerInfo controller,
                                                               @NonNull SessionCommand customCommand,
                                                               @NonNull Bundle args) {
            if (customCommand.customAction.equals(SESSION_COMMAND_REWIND.customAction)) {
                player.seekBack();
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(SESSION_COMMAND_FAST_FORWARD.customAction)) {
                player.seekForward();
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if (customCommand.customAction.equals(SESSION_COMMAND_PLAYBACK_SPEED.customAction)) {
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
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED));
        }

        @Override
        @NonNull
        @UnstableApi
        public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(
                @NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller,
                @NonNull List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
            int index = startIndex == C.INDEX_UNSET ? 0 : startIndex;
            MediaSession.MediaItemsWithStartPosition fallbackResult =
                    new MediaSession.MediaItemsWithStartPosition(mediaItems, index, startPositionMs);

            if (mediaItems.isEmpty()) {
                return Futures.immediateFuture(fallbackResult);
            }
            long mediaId;
            try {
                mediaId = Long.parseLong(mediaItems.get(index).mediaId);
            } catch (NumberFormatException e) {
                return Futures.immediateFuture(fallbackResult);
            }

            if (mediaLoaderDisposable != null) {
                mediaLoaderDisposable.dispose();
            }
            SettableFuture<MediaSession.MediaItemsWithStartPosition> future = SettableFuture.create();
            mediaLoaderDisposable = Single.fromCallable(() -> DBReader.getFeedMedia(mediaId))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            media -> {
                                currentPlayable = media;
                                MediaSession.MediaItemsWithStartPosition result =
                                        new MediaSession.MediaItemsWithStartPosition(mediaItems, index,
                                                media.getPosition() > 0 ? media.getPosition() : startPositionMs);
                                future.set(result);
                            },
                            error -> {
                                Log.e(TAG, "Failed to load media with id " + mediaId, error);
                                future.set(fallbackResult);
                            }
                    );
            return future;
        }
    };

    private void setupPositionObserver() {
        if (positionObserverDisposable != null) {
            positionObserverDisposable.dispose();
        }

        positionObserverDisposable = Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(number -> {
                    if (currentPlayable != null && player != null) {
                        int position = (int) player.getCurrentPosition();
                        int duration = (int) player.getDuration();
                        if (duration > 0) {
                            EventBus.getDefault().post(new PlaybackPositionEvent(position, duration));

                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastPositionSaveTime >= POSITION_SAVE_INTERVAL_MS) {
                                saveCurrentPosition();
                                lastPositionSaveTime = currentTime;
                            }
                        }
                    }
                });
    }

    private void cancelPositionObserver() {
        if (positionObserverDisposable != null) {
            positionObserverDisposable.dispose();
            positionObserverDisposable = null;
        }
    }

    @UnstableApi
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                saveCurrentPosition();
            }
            if (playbackState == Player.STATE_ENDED && currentPlayable != null) {
                onPlaybackEnd(currentPlayable);
                startNextInQueue(currentPlayable.getItem());
            }
            EventBus.getDefault().post(new PlayerStatusEvent());
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            PlaybackService.isRunning = isPlaying;
            if (isPlaying) {
                if (currentPlayable != null && player != null) {
                    currentPlayable.setPosition((int) player.getCurrentPosition());
                    currentPlayable.onPlaybackStart();
                }
                lastPositionSaveTime = System.currentTimeMillis();
                setupPositionObserver();
            } else {
                cancelPositionObserver();
                saveCurrentPosition();
                SynchronizationQueue.getInstance().enqueueEpisodePlayed(currentPlayable, false);
            }
            updatePlaybackPreferences();
            EventBus.getDefault().post(new PlayerStatusEvent());
        }
    };

    private void updatePlaybackPreferences() {
        if (currentPlayable == null || player == null) {
            return;
        }
        PlaybackPreferences.writeMediaPlaying(currentPlayable);
        int status = player.isPlaying() ? PlaybackPreferences.PLAYER_STATUS_PLAYING : PlaybackPreferences.PLAYER_STATUS_PAUSED;
        PlaybackPreferences.setCurrentPlayerStatus(status);
    }

    private void saveCurrentPosition() {
        if (currentPlayable == null || player == null) {
            return;
        }
        int position = (int) player.getCurrentPosition();
        long timestamp = System.currentTimeMillis();
        PlayableUtils.saveCurrentPosition(currentPlayable, position, timestamp);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @UnstableApi
    private ImmutableList<CommandButton> buildCustomLayout() {
        ImmutableList.Builder<CommandButton> buttons = ImmutableList.builder();

        buttons.add(new CommandButton.Builder()
                .setSessionCommand(SESSION_COMMAND_REWIND)
                .setIconResId(R.drawable.ic_notification_fast_rewind)
                .setDisplayName(getString(R.string.rewind_label))
                .build());

        buttons.add(new CommandButton.Builder()
                .setSessionCommand(SESSION_COMMAND_FAST_FORWARD)
                .setIconResId(R.drawable.ic_notification_fast_forward)
                .setDisplayName(getString(R.string.fast_forward_label))
                .build());

        if (UserPreferences.showPlaybackSpeedOnFullNotification()) {
            buttons.add(new CommandButton.Builder()
                    .setSessionCommand(SESSION_COMMAND_PLAYBACK_SPEED)
                    .setIconResId(R.drawable.ic_notification_playback_speed)
                    .setDisplayName(getString(R.string.playback_speed))
                    .build());
        }

        if (UserPreferences.showNextChapterOnFullNotification()) {
            if (currentPlayable != null && currentPlayable.getChapters() != null) {
                buttons.add(new CommandButton.Builder()
                        .setSessionCommand(SESSION_COMMAND_NEXT_CHAPTER)
                        .setIconResId(R.drawable.ic_notification_next_chapter)
                        .setDisplayName(getString(R.string.next_chapter))
                        .build());
            }
        }

        if (UserPreferences.showSkipOnFullNotification()) {
            buttons.add(new CommandButton.Builder()
                    .setSessionCommand(SESSION_COMMAND_SKIP_TO_NEXT)
                    .setIconResId(R.drawable.ic_notification_skip)
                    .setDisplayName(getString(R.string.skip_episode_label))
                    .build());
        }

        return buttons.build();
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
                                MediaItem mediaItem = MediaItemAdapter.fromPlayable(nextMedia);
                                player.setMediaItem(mediaItem);
                                player.seekTo(nextMedia.getPosition());
                                player.prepare();
                                if (UserPreferences.isFollowQueue()) {
                                    player.play();
                                }
                            }
                        },
                        error -> Log.e(TAG, "Failed to load next queue item", error)
                );
    }
}

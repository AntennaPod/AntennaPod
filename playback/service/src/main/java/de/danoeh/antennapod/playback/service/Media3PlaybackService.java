package de.danoeh.antennapod.playback.service;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.service.internal.PlayableUtils;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
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

    private ExoPlayer player;
    private MediaSession mediaSession;
    private Playable currentPlayable;
    private Disposable mediaLoaderDisposable;
    private Disposable positionObserverDisposable;
    private long lastPositionSaveTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.addListener(playerListener);
        mediaSession = new MediaSession.Builder(this, player)
                .setCallback(callback)
                .build();
    }

    MediaSession.Callback callback = new MediaSession.Callback() {
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

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                saveCurrentPosition();
            }
            EventBus.getDefault().post(new PlayerStatusEvent());
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

    @Override
    public void onDestroy() {
        PlaybackService.isRunning = false;
        cancelPositionObserver();
        if (mediaLoaderDisposable != null) {
            mediaLoaderDisposable.dispose();
            mediaLoaderDisposable = null;
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
}

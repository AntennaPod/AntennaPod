package de.danoeh.antennapod.playback.service;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.service.internal.PlayableUtils;
import de.danoeh.antennapod.storage.database.DBReader;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.List;

public class Media3PlaybackService extends MediaSessionService {
    private static final String TAG = "M3PlaybackService";
    private static final long POSITION_SAVE_INTERVAL_MS = 5000;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private Playable currentPlayable;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Disposable mediaLoaderDisposable;

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

    private final Runnable positionSaver = new Runnable() {
        @Override
        public void run() {
            saveCurrentPosition();
            if (player != null && player.isPlaying()) {
                handler.postDelayed(this, POSITION_SAVE_INTERVAL_MS);
            }
        }
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                saveCurrentPosition();
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (isPlaying) {
                handler.removeCallbacks(positionSaver);
                handler.post(positionSaver);
            } else {
                handler.removeCallbacks(positionSaver);
                saveCurrentPosition();
            }
        }
    };

    private void saveCurrentPosition() {
        if (currentPlayable != null && player != null) {
            int position = (int) player.getCurrentPosition();
            long timestamp = System.currentTimeMillis();
            PlayableUtils.saveCurrentPosition(currentPlayable, position, timestamp);
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

    @Override
    public void onDestroy() {
        handler.removeCallbacks(positionSaver);
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

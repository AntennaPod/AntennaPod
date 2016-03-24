package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;

import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import de.danoeh.antennapod.core.cast.CastConsumer;
import de.danoeh.antennapod.core.cast.CastConsumerImpl;
import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.CastUtils;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Implementation of PlaybackServiceMediaPlayer suitable for remote playback on Cast Devices.
 */
public class RemotePSMP extends PlaybackServiceMediaPlayer {

    public static final String TAG = "RemotePSMP";

    private final CastManager castMgr;

    private volatile Playable media;
    private volatile MediaInfo remoteMedia;
    private volatile MediaType mediaType;

    private volatile PlayerStatus statusBeforeSeeking;

    private volatile AtomicBoolean startWhenPrepared;

    /**
     * Some asynchronous calls might change the state of the MediaPlayer object. Therefore calls in other threads
     * have to wait until these operations have finished.
     */
    private final ReentrantLock playerLock;
    private final ThreadPoolExecutor executor;

    public RemotePSMP(@NonNull Context context, @NonNull PSMPCallback callback) {
        super(context, callback);

        castMgr = CastManager.getInstance();
        statusBeforeSeeking = null;
        media = null;
        mediaType = null;
        this.startWhenPrepared = new AtomicBoolean(false);

        playerLock = new ReentrantLock();
        executor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.MINUTES, new LinkedBlockingDeque<>(),
                (r, executor) -> Log.d(TAG, "Rejected execution of runnable"));

        castMgr.addCastConsumer(castConsumer);
        //TODO
    }

    private CastConsumer castConsumer = new CastConsumerImpl() {
        @Override
        public void onRemoteMediaPlayerMetadataUpdated() {
            //TODO check this is indeed a correct behavior
            onRemoteMediaPlayerStatusUpdated();
        }

        @Override
        public void onRemoteMediaPlayerStatusUpdated() {
            MediaStatus status = castMgr.getMediaStatus();
            Playable currentMedia = localVersion(status.getMediaInfo());
            switch (status.getPlayerState()) {
                case MediaStatus.PLAYER_STATE_PLAYING:
                    setPlayerStatus(PlayerStatus.PLAYING, currentMedia);
                    break;
                case MediaStatus.PLAYER_STATE_PAUSED:
                    setPlayerStatus(PlayerStatus.PAUSED, currentMedia);
                    break;
                case MediaStatus.PLAYER_STATE_BUFFERING:
                    //TODO
                    break;
                case MediaStatus.PLAYER_STATE_IDLE:
                    int reason = status.getIdleReason();
                    switch (reason) {
                        case MediaStatus.IDLE_REASON_CANCELED:
                            setPlayerStatus(PlayerStatus.STOPPED, currentMedia);
                            break;
                        case MediaStatus.IDLE_REASON_INTERRUPTED:
                            setPlayerStatus(PlayerStatus.PREPARING, currentMedia);
                            break;
                        case MediaStatus.IDLE_REASON_NONE:
                            setPlayerStatus(PlayerStatus.INITIALIZED, currentMedia);
                            break;
                        case MediaStatus.IDLE_REASON_FINISHED:
                            //TODO endPlayback and start a new one
                            break;
                        case MediaStatus.IDLE_REASON_ERROR:
                            //TODO what do they mean by error? Can we easily recover by sending a new command?
                    }
                    break;
                case MediaStatus.PLAYER_STATE_UNKNOWN:
                    //TODO

            }
        }

        @Override
        public void onStreamVolumeChanged(double value, boolean isMute) {
            //TODO
        }

        @Override
        public void onMediaLoadResult(int statusCode) {
            if (playerStatus == PlayerStatus.PREPARING) {
                if (statusCode == CastStatusCodes.SUCCESS) {
                    executor.execute(RemotePSMP.this::onPrepared);
                } else {
                    Log.d(TAG, "Remote media failed to load");
                    setPlayerStatus(PlayerStatus.INITIALIZED, media);
                }
            } else {
                Log.d(TAG, "onMediaLoadResult called, but Player Status wasn't in preparing state, so we ignore the result");
            }
        }
    };

    private Playable localVersion(MediaInfo info){
        // TODO compare with current media. If it doesn't match, then either find a local version for it
        // or create an appropriate one.
        return media;
    }

    @Override
    public void playMediaObject(@NonNull final Playable playable, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        Log.d(TAG, "playMediaObject() called");
        executor.execute(() -> {
            playerLock.lock();
            try {
                playMediaObject(playable, false, stream, startWhenPrepared, prepareImmediately);
            } finally {
                playerLock.unlock();
            }
        });
    }

    /**
     * Internal implementation of playMediaObject. This method has an additional parameter that allows the caller to force a media player reset even if
     * the given playable parameter is the same object as the currently playing media.
     * <p/>
     * This method requires the playerLock and is executed on the caller's thread.
     *
     * @see #playMediaObject(de.danoeh.antennapod.core.util.playback.Playable, boolean, boolean, boolean)
     */
    private void playMediaObject(@NonNull final Playable playable, final boolean forceReset, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        if (!playerLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("method requires playerLock");
        }

        if (media != null) {
            if (!forceReset && media.getIdentifier().equals(playable.getIdentifier())
                    && playerStatus == PlayerStatus.PLAYING) {
                // episode is already playing -> ignore method call
                Log.d(TAG, "Method call to playMediaObject was ignored: media file already playing.");
                return;
            } else {
                // set temporarily to pause in order to update list with current position
                try {
                    if (castMgr.isRemoteMediaPlaying()) {
                        setPlayerStatus(PlayerStatus.PAUSED, media);
                    }
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    Log.e(TAG, "Unable to determine whether media was playing, falling back to stored player status", e);
                    // this might end up just being pointless if we need to query the remote device for the position
                    if (playerStatus == PlayerStatus.PLAYING) {
                        setPlayerStatus(PlayerStatus.PAUSED, media);
                    }
                }
                smartMarkAsPlayed(media);


                setPlayerStatus(PlayerStatus.INDETERMINATE, null);
            }
        }

        if (CastUtils.isCastable(playable)) {
            this.media = playable;
            remoteMedia = CastUtils.convertFromFeedMedia((FeedMedia) media);
            //this.stream = stream;
            this.mediaType = media.getMediaType();
            this.startWhenPrepared.set(startWhenPrepared);
            setPlayerStatus(PlayerStatus.INITIALIZING, media);
            try {
                media.loadMetadata();
                executor.execute(() -> updateMediaSessionMetadata(media));
                setPlayerStatus(PlayerStatus.INITIALIZED, media);
                if (prepareImmediately) {
                    prepareSync();
                }
            } catch (Playable.PlayableException e) {
                Log.e(TAG, "Error while loading media metadata", e);
                setPlayerStatus(PlayerStatus.STOPPED, null);
            }
        }
    }

    @Override
    public void resume() {
        //TODO
    }

    @Override
    public void pause(boolean abandonFocus, boolean reinit) {
        //TODO
    }

    @Override
    public void prepare() {
        executor.submit( () -> {
            playerLock.lock();
            prepareSync();
            playerLock.unlock();

        });
    }

    private void prepareSync() {
        if (playerStatus == PlayerStatus.INITIALIZED) {
            Log.d(TAG, "Preparing media player");
            setPlayerStatus(PlayerStatus.PREPARING, media);
            try {
                castMgr.loadMedia(remoteMedia, startWhenPrepared.get(), media.getPosition());
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                Log.e(TAG, "Error loading media", e);
                setPlayerStatus(PlayerStatus.INITIALIZED, media);
            }
        }
    }

    /**
     * Called after media player has been prepared. This method is executed on the caller's thread.
     */
    void onPrepared() {
        playerLock.lock();

        if (playerStatus != PlayerStatus.PREPARING) {
            playerLock.unlock();
            Log.w(TAG, "onPrepared() called, but player is not in PREPARING state anymore");
            return;
        }
        Log.d(TAG, "Resource loaded");
        if (media.getDuration() == 0) {
            Log.d(TAG, "Setting duration of media");
            try {
                media.setDuration((int) castMgr.getMediaDuration());
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                Log.e(TAG, "Unable to get remote media's duration");
            }
        }
        setPlayerStatus(PlayerStatus.PREPARED, media);
        playerLock.unlock();
    }

    @Override
    public void reinit() {
        //TODO
    }

    @Override
    public void seekTo(int t) {
        //TODO
    }

    @Override
    public void seekDelta(int d) {
        //TODO
    }

    @Override
    public void seekToChapter(@NonNull Chapter c) {
        //TODO
    }

    @Override
    public int getDuration() {
        //TODO
        return 0;
    }

    @Override
    public int getPosition() {
        try {
            if (!playerLock.tryLock(50, TimeUnit.MILLISECONDS)) {
                return INVALID_TIME;
            }
        } catch (InterruptedException e) {
            return INVALID_TIME;
        }

        int retVal = INVALID_TIME;
        boolean prepared;
        try {
            prepared = castMgr.isRemoteMediaLoaded();
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to check if remote media is loaded", e);
            prepared = playerStatus.isAtLeast(PlayerStatus.PREPARED);
        }
        if (prepared) {
            try {
                retVal = (int) castMgr.getCurrentMediaPosition();
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                Log.e(TAG, "Unable to determine remote media's position", e);
            }
        }
        if(retVal <= 0 && media != null && media.getPosition() > 0) {
            retVal = media.getPosition();
        }

        playerLock.unlock();
        Log.d(TAG, "getPosition() -> " + retVal);
        return retVal;
    }

    @Override
    public boolean isStartWhenPrepared() {
        //TODO
        return false;
    }

    @Override
    public void setStartWhenPrepared(boolean startWhenPrepared) {
        //TODO
    }

    //TODO I believe some parts of the code make the same decision skipping this check, so that
    //should be changed as well
    @Override
    public boolean canSetSpeed() {
        return false;
    }

    @Override
    public void setSpeed(float speed) {
        throw new UnsupportedOperationException("Setting playback speed unsupported for Remote Playback");
    }

    @Override
    public float getPlaybackSpeed() {
        return 1;
    }

    //TODO make device volume being selected by the hardware keys
    @Override
    public void setVolume(float volumeLeft, float volumeRight) {
        Log.d(TAG, "Setting the Stream volume on Remote Media Player");
        double volume = (volumeLeft+volumeRight)/2;
        if (volume > 1.0) {
            volume = 1.0;
        }
        if (volume < 0.0) {
            volume = 0.0;
        }
        try {
            castMgr.setStreamVolume(volume);
        } catch (TransientNetworkDisconnectionException | NoConnectionException | CastException e) {
            Log.e(TAG, "Unable to set the volume", e);
        }
    }

    @Override
    public boolean canDownmix() {
        return false;
    }

    @Override
    public void setDownmix(boolean enable) {
        throw new UnsupportedOperationException("Setting downmix unsupported in Remote Media Player");
    }

    @Override
    public MediaType getCurrentMediaType() {
        //TODO
        return null;
    }

    @Override
    public boolean isStreaming() {
        return true;
    }

    @Override
    public void shutdown() {
        castMgr.removeCastConsumer(castConsumer);
        executor.shutdown();
        //TODO
    }

    @Override
    public void shutdownAsync() {
        //TODO
        this.shutdown();
    }

    @Override
    public void setVideoSurface(SurfaceHolder surface) {
        //TODO
    }

    @Override
    public void resetVideoSurface() {
        //TODO
    }

    @Override
    public Pair<Integer, Integer> getVideoSize() {
        //TODO
        return null;
    }

    @Override
    public Playable getPlayable() {
        return media;
    }

    @Override
    protected void setPlayable(Playable playable) {
        //TODO this looks very wrong
        if (playable != media) {
            media = playable;
            remoteMedia = !(media instanceof FeedMedia) ? null : CastUtils.convertFromFeedMedia((FeedMedia) media);
        }
    }

    @Override
    public void endPlayback(boolean wasSkipped, boolean switchingPlayers) {
        //TODO
    }

    @Override
    public void stop() {
        //TODO
    }
}

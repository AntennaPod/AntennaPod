package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.media.MediaPlayer;
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

import de.danoeh.antennapod.core.cast.CastConsumer;
import de.danoeh.antennapod.core.cast.CastConsumerImpl;
import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.cast.RemoteMedia;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.cast.CastUtils;
import de.danoeh.antennapod.core.util.RewindAfterPauseUtils;
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

    private final AtomicBoolean isBuffering;

    private final AtomicBoolean startWhenPrepared;

    /**
     * Some asynchronous calls might change the state of the MediaPlayer object. Therefore calls in other threads
     * have to wait until these operations have finished.
     */
    //private final ReentrantLock playerLock;
    private final ThreadPoolExecutor executor;

    public RemotePSMP(@NonNull Context context, @NonNull PSMPCallback callback) {
        super(context, callback);

        castMgr = CastManager.getInstance();
        media = null;
        mediaType = null;
        startWhenPrepared = new AtomicBoolean(false);
        isBuffering = new AtomicBoolean(false);

        //playerLock = new ReentrantLock();
        executor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.MINUTES, new LinkedBlockingDeque<>(),
                (r, executor) -> Log.d(TAG, "Rejected execution of runnable"));

        try {
            if (castMgr.isConnected() && castMgr.isRemoteMediaLoaded()) {
                // updates the state, but does not start playing new media if it was going to
                onRemoteMediaPlayerStatusUpdated(
                        ((playNextEpisode, wasSkipped, switchingPlayers) ->
                        this.callback.endPlayback(false, wasSkipped, switchingPlayers)));
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to do initial check for loaded media", e);
        }

        castMgr.addCastConsumer(castConsumer);
        //TODO
    }

    private CastConsumer castConsumer = new CastConsumerImpl() {
        @Override
        public void onRemoteMediaPlayerMetadataUpdated() {
            RemotePSMP.this.onRemoteMediaPlayerStatusUpdated(callback::endPlayback);
        }

        @Override
        public void onRemoteMediaPlayerStatusUpdated() {
            RemotePSMP.this.onRemoteMediaPlayerStatusUpdated(callback::endPlayback);
        }

        @Override
        public void onMediaLoadResult(int statusCode) {
            if (playerStatus == PlayerStatus.PREPARING) {
                if (statusCode == CastStatusCodes.SUCCESS) {
                    setPlayerStatus(PlayerStatus.PREPARED, media);
                    if (media.getDuration() == 0) {
                        Log.d(TAG, "Setting duration of media");
                        try {
                            media.setDuration((int) castMgr.getMediaDuration());
                        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                            Log.e(TAG, "Unable to get remote media's duration");
                        }
                    }
                } else if (statusCode != CastStatusCodes.REPLACED){
                    Log.d(TAG, "Remote media failed to load");
                    setPlayerStatus(PlayerStatus.INITIALIZED, media);
                }
            } else {
                Log.d(TAG, "onMediaLoadResult called, but Player Status wasn't in preparing state, so we ignore the result");
            }
        }
    };

    private void setBuffering(boolean buffering) {
        if (buffering && isBuffering.compareAndSet(false, true)) {
            callback.onMediaPlayerInfo(MediaPlayer.MEDIA_INFO_BUFFERING_START);
        } else if (!buffering && isBuffering.compareAndSet(true, false)) {
            callback.onMediaPlayerInfo(MediaPlayer.MEDIA_INFO_BUFFERING_END);
        }
    }

    private Playable localVersion(MediaInfo info){
        if (info == null) {
            return null;
        }
        if (CastUtils.matches(info, media)) {
            return media;
        }
        return CastUtils.getPlayable(info, true);
    }

    private MediaInfo remoteVersion(Playable playable) {
        if (playable == null) {
            return null;
        }
        if (CastUtils.matches(remoteMedia, playable)) {
            return remoteMedia;
        }
        if (playable instanceof FeedMedia) {
            return CastUtils.convertFromFeedMedia((FeedMedia) playable);
        }
        if (playable instanceof RemoteMedia) {
            return ((RemoteMedia) playable).extractMediaInfo();
        }
        return null;
    }

    private void onRemoteMediaPlayerStatusUpdated(@NonNull EndPlaybackCall endPlaybackCall) {
        MediaStatus status = castMgr.getMediaStatus();
        if (status == null) {
            setBuffering(false);
            setPlayerStatus(PlayerStatus.INDETERMINATE, null);
            return;
        }
        Playable currentMedia = localVersion(status.getMediaInfo());
        if (currentMedia != null) {
            long position = status.getStreamPosition();
            if (position > 0 && currentMedia.getPosition() == 0) {
                currentMedia.setPosition((int) position);
            }
        }
        int state = status.getPlayerState();
        setBuffering(state == MediaStatus.PLAYER_STATE_BUFFERING);
        switch (state) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                setPlayerStatus(PlayerStatus.PLAYING, currentMedia);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                setPlayerStatus(PlayerStatus.PAUSED, currentMedia);
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                setPlayerStatus(playerStatus, currentMedia);
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
                        boolean playing = playerStatus == PlayerStatus.PLAYING;
                        setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
                        endPlaybackCall.endPlayback(playing, false, false);
                        break;
                    case MediaStatus.IDLE_REASON_ERROR:
                        //Let's assume it's a media format error. Skipping...
                        setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
                        endPlaybackCall.endPlayback(startWhenPrepared.get(), true, false);
                }
                break;
            case MediaStatus.PLAYER_STATE_UNKNOWN:
                //is this right?
                setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
        }
    }

    @Override
    public void playMediaObject(@NonNull final Playable playable, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        Log.d(TAG, "playMediaObject() called");
        playMediaObject(playable, false, stream, startWhenPrepared, prepareImmediately);
    }

    /**
     * Internal implementation of playMediaObject. This method has an additional parameter that allows the caller to force a media player reset even if
     * the given playable parameter is the same object as the currently playing media.
     *
     * @see #playMediaObject(de.danoeh.antennapod.core.util.playback.Playable, boolean, boolean, boolean)
     */
    private void playMediaObject(@NonNull final Playable playable, final boolean forceReset, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        if (!CastUtils.isCastable(playable)) {
            Log.d(TAG, "media provided is not compatible with cast device");
            return;
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

        this.media = playable;
        remoteMedia = remoteVersion(playable);
        //this.stream = stream;
        this.mediaType = media.getMediaType();
        this.startWhenPrepared.set(startWhenPrepared);
        setPlayerStatus(PlayerStatus.INITIALIZING, media);
        try {
            media.loadMetadata();
            executor.execute(() -> callback.updateMediaSessionMetadata(media));
            setPlayerStatus(PlayerStatus.INITIALIZED, media);
            if (prepareImmediately) {
                prepare();
            }
        } catch (Playable.PlayableException e) {
            Log.e(TAG, "Error while loading media metadata", e);
            setPlayerStatus(PlayerStatus.STOPPED, null);
        }
    }

    @Override
    public void resume() {
        try {
            setVolume(UserPreferences.getLeftVolume(), UserPreferences.getRightVolume());
            if (playerStatus == PlayerStatus.PREPARED && media.getPosition() > 0) {
                int newPosition = RewindAfterPauseUtils.calculatePositionWithRewind(
                        media.getPosition(),
                        media.getLastPlayedTime());
                castMgr.play(newPosition);
            }
            castMgr.play();
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to resume remote playback", e);
        }
    }

    @Override
    public void pause(boolean abandonFocus, boolean reinit) {
        boolean playing = true;
        try {
            playing = castMgr.isRemoteMediaPlaying();
            if (playing) {
                castMgr.pause();
            }
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to pause", e);
        }
        if (playing && reinit) {
            reinit();
        }
    }

    @Override
    public void prepare() {
        if (playerStatus == PlayerStatus.INITIALIZED) {
            Log.d(TAG, "Preparing media player");
            setPlayerStatus(PlayerStatus.PREPARING, media);
            try {
                int position = media.getPosition();
                if (position > 0) {
                    position = RewindAfterPauseUtils.calculatePositionWithRewind(
                            position,
                            media.getLastPlayedTime());
                }
                setVolume(UserPreferences.getLeftVolume(), UserPreferences.getRightVolume());
                castMgr.loadMedia(remoteMedia, startWhenPrepared.get(), position);
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                Log.e(TAG, "Error loading media", e);
                setPlayerStatus(PlayerStatus.INITIALIZED, media);
            }
        }
    }

    @Override
    public void reinit() {
        if (media != null) {
            playMediaObject(media, true, false, startWhenPrepared.get(), false);
        } else {
            Log.d(TAG, "Call to reinit was ignored: media was null");
        }
    }

    @Override
    public void seekTo(int t) {
        //TODO check other seek implementations and see if there's no issue with sending too many seek commands to the remote media player
        try {
            if (castMgr.isRemoteMediaLoaded()) {
                setPlayerStatus(PlayerStatus.SEEKING, media);
                castMgr.seek(t);
            } else if (media != null && playerStatus == PlayerStatus.INITIALIZED){
                media.setPosition(t);
                startWhenPrepared.set(false);
                prepare();
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to seek", e);
        }
    }

    @Override
    public void seekDelta(int d) {
        int position = getPosition();
        if (position != INVALID_TIME) {
            seekTo(position + d);
        } else {
            Log.e(TAG, "getPosition() returned INVALID_TIME in seekDelta");
        }
    }

    @Override
    public int getDuration() {
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
                retVal = (int) castMgr.getMediaDuration();
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                Log.e(TAG, "Unable to determine remote media's duration", e);
            }
        }
        if(retVal == INVALID_TIME && media != null && media.getDuration() > 0) {
            retVal = media.getDuration();
        }
        Log.d(TAG, "getDuration() -> " + retVal);
        return retVal;
    }

    @Override
    public int getPosition() {
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
        if(retVal <= 0 && media != null && media.getPosition() >= 0) {
            retVal = media.getPosition();
        }
        Log.d(TAG, "getPosition() -> " + retVal);
        return retVal;
    }

    @Override
    public boolean isStartWhenPrepared() {
        return startWhenPrepared.get();
    }

    @Override
    public void setStartWhenPrepared(boolean startWhenPrepared) {
        this.startWhenPrepared.set(startWhenPrepared);
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
        return mediaType;
    }

    @Override
    public boolean isStreaming() {
        return true;
    }

    @Override
    public void shutdown() {
        castMgr.removeCastConsumer(castConsumer);
        executor.shutdown();
    }

    @Override
    public void shutdownQuietly() {
        executor.execute(this::shutdown);
        executor.shutdown();
    }

    @Override
    public void setVideoSurface(SurfaceHolder surface) {
        throw new UnsupportedOperationException("Setting Video Surface unsupported in Remote Media Player");
    }

    @Override
    public void resetVideoSurface() {
        throw new UnsupportedOperationException("Resetting Video Surface unsupported in Remote Media Player");
    }

    @Override
    public Pair<Integer, Integer> getVideoSize() {
        return null;
    }

    @Override
    public Playable getPlayable() {
        return media;
    }

    @Override
    protected void setPlayable(Playable playable) {
        if (playable != media) {
            media = playable;
            remoteMedia = remoteVersion(playable);
        }
    }

    @Override
    public void endPlayback(boolean wasSkipped, boolean switchingPlayers) {
        boolean isPlaying = playerStatus == PlayerStatus.PLAYING;
        if (playerStatus != PlayerStatus.INDETERMINATE) {
            setPlayerStatus(PlayerStatus.INDETERMINATE, media);
        }
        callback.endPlayback(isPlaying, wasSkipped, switchingPlayers);
    }

    @Override
    public void stop() {
        if (playerStatus == PlayerStatus.INDETERMINATE) {
            setPlayerStatus(PlayerStatus.STOPPED, null);
        } else {
            Log.d(TAG, "Ignored call to stop: Current player state is: " + playerStatus);
        }
    }

    @Override
    protected boolean shouldLockWifi() {
        return false;
    }

    private interface EndPlaybackCall {
        boolean endPlayback(boolean playNextEpisode, boolean wasSkipped, boolean switchingPlayers);
    }
}

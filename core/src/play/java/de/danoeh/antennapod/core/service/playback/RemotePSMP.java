package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;

import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.cast.CastConsumer;
import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.cast.CastUtils;
import de.danoeh.antennapod.core.cast.DefaultCastConsumer;
import de.danoeh.antennapod.core.cast.RemoteMedia;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.RewindAfterPauseUtils;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Implementation of PlaybackServiceMediaPlayer suitable for remote playback on Cast Devices.
 */
public class RemotePSMP extends PlaybackServiceMediaPlayer {

    public static final String TAG = "RemotePSMP";

    public static final int CAST_ERROR = 3001;

    public static final int CAST_ERROR_PRIORITY_HIGH = 3005;

    private final CastManager castMgr;

    private volatile Playable media;
    private volatile MediaInfo remoteMedia;
    private volatile MediaType mediaType;

    private final AtomicBoolean isBuffering;

    private final AtomicBoolean startWhenPrepared;

    public RemotePSMP(@NonNull Context context, @NonNull PSMPCallback callback) {
        super(context, callback);

        castMgr = CastManager.getInstance();
        media = null;
        mediaType = null;
        startWhenPrepared = new AtomicBoolean(false);
        isBuffering = new AtomicBoolean(false);

        try {
            if (castMgr.isConnected() && castMgr.isRemoteMediaLoaded()) {
                // updates the state, but does not start playing new media if it was going to
                onRemoteMediaPlayerStatusUpdated(
                        ((p, playNextEpisode, wasSkipped, switchingPlayers) ->
                        this.callback.endPlayback(p, false, wasSkipped, switchingPlayers)));
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to do initial check for loaded media", e);
        }

        castMgr.addCastConsumer(castConsumer);
        //TODO
    }

    private CastConsumer castConsumer = new DefaultCastConsumer() {
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

        @Override
        public void onApplicationStatusChanged(String appStatus) {
            if (playerStatus != PlayerStatus.PLAYING) {
                Log.d(TAG, "onApplicationStatusChanged, but no media was playing");
                return;
            }
            boolean playbackEnded = false;
            try {
                int standbyState = castMgr.getApplicationStandbyState();
                Log.d(TAG, "standbyState: " + standbyState);
                playbackEnded = standbyState == Cast.STANDBY_STATE_YES;
            } catch (IllegalStateException e) {
                Log.d(TAG, "unable to get standbyState on onApplicationStatusChanged()");
            }
            if (playbackEnded) {
                setPlayerStatus(PlayerStatus.INDETERMINATE, media);
                callback.endPlayback(media, true, false, false);
            }
        }

        @Override
        public void onFailed(int resourceId, int statusCode) {
            callback.onMediaPlayerInfo(CAST_ERROR, resourceId);
        }
    };

    private void setBuffering(boolean buffering) {
        if (buffering && isBuffering.compareAndSet(false, true)) {
            callback.onMediaPlayerInfo(MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        } else if (!buffering && isBuffering.compareAndSet(true, false)) {
            callback.onMediaPlayerInfo(MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
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
            Log.d(TAG, "Received null MediaStatus");
            //setBuffering(false);
            //setPlayerStatus(PlayerStatus.INDETERMINATE, null);
            return;
        } else {
            Log.d(TAG, "Received remote status/media update. New state=" + status.getPlayerState());
        }
        Playable currentMedia = localVersion(status.getMediaInfo());
        boolean updateUI = currentMedia != media;
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
                        // check if we're already loading something else
                        if (!updateUI || media == null) {
                            setPlayerStatus(PlayerStatus.STOPPED, currentMedia);
                        } else {
                            updateUI = false;
                        }
                        break;
                    case MediaStatus.IDLE_REASON_INTERRUPTED:
                        // check if we're already loading something else
                        if (!updateUI || media == null) {
                            setPlayerStatus(PlayerStatus.PREPARING, currentMedia);
                        } else {
                            updateUI = false;
                        }
                        break;
                    case MediaStatus.IDLE_REASON_NONE:
                        setPlayerStatus(PlayerStatus.INITIALIZED, currentMedia);
                        break;
                    case MediaStatus.IDLE_REASON_FINISHED:
                        boolean playing = playerStatus == PlayerStatus.PLAYING;
                        setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
                        endPlaybackCall.endPlayback(currentMedia,playing, false, false);
                        // endPlayback already updates the UI, so no need to trigger it ourselves
                        updateUI = false;
                        break;
                    case MediaStatus.IDLE_REASON_ERROR:
                        Log.w(TAG, "Got an error status from the Chromecast. Skipping, if possible, to the next episode...");
                        setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
                        callback.onMediaPlayerInfo(CAST_ERROR_PRIORITY_HIGH,
                                R.string.cast_failed_media_error_skipping);
                        endPlaybackCall.endPlayback(currentMedia, startWhenPrepared.get(), true, false);
                        // endPlayback already updates the UI, so no need to trigger it ourselves
                        updateUI = false;
                }
                break;
            case MediaStatus.PLAYER_STATE_UNKNOWN:
                //is this right?
                setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
                break;
            default:
                Log.e(TAG, "Remote media state undetermined!");
                setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
        }
        if (updateUI) {
            callback.onMediaChanged(true);
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
            callback.onMediaPlayerInfo(CAST_ERROR_PRIORITY_HIGH, R.string.cast_not_castable);
            try {
                playable.loadMetadata();
            } catch (Playable.PlayableException e) {
                Log.e(TAG, "Unable to load metadata of playable", e);
            }
            callback.endPlayback(playable, startWhenPrepared, true, false);
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
            callback.onMediaChanged(true);
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
            // TODO see comment on prepare()
            // setVolume(UserPreferences.getLeftVolume(), UserPreferences.getRightVolume());
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
        try {
            if (castMgr.isRemoteMediaPlaying()) {
                castMgr.pause();
            }
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to pause", e);
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
                // TODO We're not supporting user set stream volume yet, as we need to make a UI
                // that doesn't allow changing playback speed or have different values for left/right
                //setVolume(UserPreferences.getLeftVolume(), UserPreferences.getRightVolume());
                castMgr.loadMedia(remoteMedia, startWhenPrepared.get(), position);
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                Log.e(TAG, "Error loading media", e);
                setPlayerStatus(PlayerStatus.INITIALIZED, media);
            }
        }
    }

    @Override
    public void reinit() {
        Log.d(TAG, "reinit() called");
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
    }

    @Override
    public void shutdownQuietly() {
        shutdown();
    }

    @Override
    public void setVideoSurface(SurfaceHolder surface) {
        throw new UnsupportedOperationException("Setting Video Surface unsupported in Remote Media Player");
    }

    @Override
    public void resetVideoSurface() {
        Log.e(TAG, "Resetting Video Surface unsupported in Remote Media Player");
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
    protected void endPlayback(boolean wasSkipped) {
        Log.d(TAG, "endPlayback() called");
        boolean isPlaying = playerStatus == PlayerStatus.PLAYING;
        try {
            isPlaying = castMgr.isRemoteMediaPlaying();
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Could not determine if media is playing", e);
        }
        // TODO make sure we stop playback whenever there's no next episode.
        if (playerStatus != PlayerStatus.INDETERMINATE) {
            setPlayerStatus(PlayerStatus.INDETERMINATE, media);
        }
        callback.endPlayback(media, isPlaying, wasSkipped, switchingPlayers);
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
        boolean endPlayback(Playable media, boolean playNextEpisode, boolean wasSkipped, boolean switchingPlayers);
    }
}

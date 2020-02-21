package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.media.MediaPlayer;
import androidx.annotation.NonNull;
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

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.cast.CastConsumer;
import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.cast.CastUtils;
import de.danoeh.antennapod.core.cast.DefaultCastConsumer;
import de.danoeh.antennapod.core.cast.RemoteMedia;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.UserPreferences;
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
    private volatile MediaType mediaType;
    private volatile MediaInfo remoteMedia;
    private volatile int remoteState;

    private final AtomicBoolean isBuffering;

    private final AtomicBoolean startWhenPrepared;

    public RemotePSMP(@NonNull Context context, @NonNull PSMPCallback callback) {
        super(context, callback);

        castMgr = CastManager.getInstance();
        media = null;
        mediaType = null;
        startWhenPrepared = new AtomicBoolean(false);
        isBuffering = new AtomicBoolean(false);
        remoteState = MediaStatus.PLAYER_STATE_UNKNOWN;
    }

    public void init() {
        try {
            if (castMgr.isConnected() && castMgr.isRemoteMediaLoaded()) {
                onRemoteMediaPlayerStatusUpdated();
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to do initial check for loaded media", e);
        }

        castMgr.addCastConsumer(castConsumer);
    }

    private CastConsumer castConsumer = new DefaultCastConsumer() {
        @Override
        public void onRemoteMediaPlayerMetadataUpdated() {
            RemotePSMP.this.onRemoteMediaPlayerStatusUpdated();
        }

        @Override
        public void onRemoteMediaPlayerStatusUpdated() {
            RemotePSMP.this.onRemoteMediaPlayerStatusUpdated();
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
                // This is an unconventional thing to occur...
                Log.w(TAG, "Somehow, Chromecast went from playing directly to standby mode");
                endPlayback(false, false, true, true);
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

    private void onRemoteMediaPlayerStatusUpdated() {
        MediaStatus status = castMgr.getMediaStatus();
        if (status == null) {
            Log.d(TAG, "Received null MediaStatus");
            return;
        } else {
            Log.d(TAG, "Received remote status/media update. New state=" + status.getPlayerState());
        }
        int state = status.getPlayerState();
        int oldState = remoteState;
        remoteMedia = status.getMediaInfo();
        boolean mediaChanged = !CastUtils.matches(remoteMedia, media);
        boolean stateChanged = state != oldState;
        if (!mediaChanged && !stateChanged) {
            Log.d(TAG, "Both media and state haven't changed, so nothing to do");
            return;
        }
        Playable currentMedia = mediaChanged ? localVersion(remoteMedia) : media;
        Playable oldMedia = media;
        int position = (int) status.getStreamPosition();
        // check for incompatible states
        if ((state == MediaStatus.PLAYER_STATE_PLAYING || state == MediaStatus.PLAYER_STATE_PAUSED)
                && currentMedia == null) {
            Log.w(TAG, "RemoteMediaPlayer returned playing or pausing state, but with no media");
            state = MediaStatus.PLAYER_STATE_UNKNOWN;
            stateChanged = oldState != MediaStatus.PLAYER_STATE_UNKNOWN;
        }

        if (stateChanged) {
            remoteState = state;
        }

        if (mediaChanged && stateChanged && oldState == MediaStatus.PLAYER_STATE_PLAYING &&
                state != MediaStatus.PLAYER_STATE_IDLE) {
            callback.onPlaybackPause(null, INVALID_TIME);
            // We don't want setPlayerStatus to handle the onPlaybackPause callback
            setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
        }

        setBuffering(state == MediaStatus.PLAYER_STATE_BUFFERING);

        switch (state) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                if (!stateChanged) {
                    //These steps are necessary because they won't be performed by setPlayerStatus()
                    if (position >= 0) {
                        currentMedia.setPosition(position);
                    }
                    currentMedia.onPlaybackStart();
                }
                setPlayerStatus(PlayerStatus.PLAYING, currentMedia, position);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                setPlayerStatus(PlayerStatus.PAUSED, currentMedia, position);
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                setPlayerStatus((mediaChanged || playerStatus == PlayerStatus.PREPARING) ?
                        PlayerStatus.PREPARING : PlayerStatus.SEEKING,
                        currentMedia,
                        currentMedia != null ? currentMedia.getPosition() : INVALID_TIME);
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                int reason = status.getIdleReason();
                switch (reason) {
                    case MediaStatus.IDLE_REASON_CANCELED:
                        // Essentially means stopped at the request of a user
                        callback.onPlaybackEnded(null, true);
                        setPlayerStatus(PlayerStatus.STOPPED, currentMedia);
                        if (oldMedia != null) {
                            if (position >= 0) {
                                oldMedia.setPosition(position);
                            }
                            callback.onPostPlayback(oldMedia, false, false, false);
                        }
                        // onPlaybackEnded pretty much takes care of updating the UI
                        return;
                    case MediaStatus.IDLE_REASON_INTERRUPTED:
                        // Means that a request to load a different media was sent
                        // Not sure if currentMedia already reflects the to be loaded one
                        if (mediaChanged && oldState == MediaStatus.PLAYER_STATE_PLAYING) {
                            callback.onPlaybackPause(null, INVALID_TIME);
                            setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
                        }
                        setPlayerStatus(PlayerStatus.PREPARING, currentMedia);
                        break;
                    case MediaStatus.IDLE_REASON_NONE:
                        // This probably only happens when we connected but no command has been sent yet.
                        setPlayerStatus(PlayerStatus.INITIALIZED, currentMedia);
                        break;
                    case MediaStatus.IDLE_REASON_FINISHED:
                        // This is our onCompletionListener...
                        if (mediaChanged && currentMedia != null) {
                            media = currentMedia;
                        }
                        endPlayback(true, false, true, true);
                        return;
                    case MediaStatus.IDLE_REASON_ERROR:
                        Log.w(TAG, "Got an error status from the Chromecast. Skipping, if possible, to the next episode...");
                        callback.onMediaPlayerInfo(CAST_ERROR_PRIORITY_HIGH,
                                R.string.cast_failed_media_error_skipping);
                        endPlayback(false, false, true, true);
                        return;
                }
                break;
            case MediaStatus.PLAYER_STATE_UNKNOWN:
                if (playerStatus != PlayerStatus.INDETERMINATE || media != currentMedia) {
                    setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
                }
                break;
            default:
                Log.wtf(TAG, "Remote media state undetermined!");
        }
        if (mediaChanged) {
            callback.onMediaChanged(true);
            if (oldMedia != null) {
                callback.onPostPlayback(oldMedia, false, false, currentMedia != null);
            }
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
            Playable nextPlayable = playable;
            do {
                nextPlayable = callback.getNextInQueue(nextPlayable);
            } while (nextPlayable != null && !CastUtils.isCastable(nextPlayable));
            if (nextPlayable != null) {
                playMediaObject(nextPlayable, forceReset, stream, startWhenPrepared, prepareImmediately);
            }
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
                boolean isPlaying = playerStatus == PlayerStatus.PLAYING;
                int position = media.getPosition();
                try {
                    isPlaying = castMgr.isRemoteMediaPlaying();
                    position = (int) castMgr.getCurrentMediaPosition();
                } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                    Log.e(TAG, "Unable to determine whether media was playing, falling back to stored player status", e);
                }
                if (isPlaying) {
                    callback.onPlaybackPause(media, position);
                }
                if (!media.getIdentifier().equals(playable.getIdentifier())) {
                    final Playable oldMedia = media;
                    callback.onPostPlayback(oldMedia, false, false, true);
                }

                setPlayerStatus(PlayerStatus.INDETERMINATE, null);
            }
        }

        this.media = playable;
        remoteMedia = remoteVersion(playable);
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
            } else {
                castMgr.play();
            }
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

    // As things are right now, changing the return value of this function is not enough to ensure
    // all other components recognize it.
    @Override
    public boolean canSetSpeed() {
        return false;
    }

    @Override
    public void setPlaybackParams(float speed, boolean skipSilence) {
        //Can be safely ignored as neither set speed not skipSilence is supported
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
    protected Future<?> endPlayback(boolean hasEnded, boolean wasSkipped, boolean shouldContinue,
                                    boolean toStoppedState) {
        Log.d(TAG, "endPlayback() called");
        boolean isPlaying = playerStatus == PlayerStatus.PLAYING;
        if (playerStatus != PlayerStatus.INDETERMINATE) {
            setPlayerStatus(PlayerStatus.INDETERMINATE, media);
        }
        if (media != null && wasSkipped) {
            // current position only really matters when we skip
            int position = getPosition();
            if (position >= 0) {
                media.setPosition(position);
            }
        }
        final Playable currentMedia = media;
        Playable nextMedia = null;
        if (shouldContinue) {
            nextMedia = callback.getNextInQueue(currentMedia);

            boolean playNextEpisode = isPlaying && nextMedia != null && UserPreferences.isFollowQueue();
            if (playNextEpisode) {
                Log.d(TAG, "Playback of next episode will start immediately.");
            } else if (nextMedia == null){
                Log.d(TAG, "No more episodes available to play");
            } else {
                Log.d(TAG, "Loading next episode, but not playing automatically.");
            }

            if (nextMedia != null) {
                callback.onPlaybackEnded(nextMedia.getMediaType(), !playNextEpisode);
                // setting media to null signals to playMediaObject() that we're taking care of post-playback processing
                media = null;
                playMediaObject(nextMedia, false, true /*TODO for now we always stream*/, playNextEpisode, playNextEpisode);
            }
        }
        if (shouldContinue || toStoppedState) {
            boolean shouldPostProcess = true;
            if (nextMedia == null) {
                try {
                    castMgr.stop();
                    shouldPostProcess = false;
                } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException e) {
                    Log.e(TAG, "Unable to stop playback", e);
                    callback.onPlaybackEnded(null, true);
                    stop();
                }
            }
            if (shouldPostProcess) {
                // Otherwise we rely on the chromecast callback to tell us the playback has stopped.
                callback.onPostPlayback(currentMedia, hasEnded, wasSkipped, nextMedia != null);
            }
        } else if (isPlaying) {
            callback.onPlaybackPause(currentMedia,
                    currentMedia != null ? currentMedia.getPosition() : INVALID_TIME);
        }

        FutureTask<?> future = new FutureTask<>(() -> {}, null);
        future.run();
        return future;
    }

    private void stop() {
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
}

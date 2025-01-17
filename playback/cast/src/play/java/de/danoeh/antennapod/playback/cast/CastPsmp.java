package de.danoeh.antennapod.playback.cast;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;
import com.google.android.gms.cast.MediaError;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaSeekOptions;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.event.playback.BufferUpdateEvent;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.model.playback.RemoteMedia;
import de.danoeh.antennapod.playback.base.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.base.RewindAfterPauseUtils;
import de.danoeh.antennapod.ui.episodes.PlaybackSpeedUtils;
import org.greenrobot.eventbus.EventBus;

/**
 * Implementation of PlaybackServiceMediaPlayer suitable for remote playback on Cast Devices.
 */
@SuppressLint("VisibleForTests")
public class CastPsmp extends PlaybackServiceMediaPlayer {

    public static final String TAG = "CastPSMP";

    private volatile Playable media;
    private volatile MediaType mediaType;
    private volatile MediaInfo remoteMedia;
    private volatile int remoteState;
    private final CastContext castContext;
    private final RemoteMediaClient remoteMediaClient;

    private final AtomicBoolean isBuffering;

    private final AtomicBoolean startWhenPrepared;

    @Nullable
    public static PlaybackServiceMediaPlayer getInstanceIfConnected(@NonNull Context context,
                                                                    @NonNull PSMPCallback callback) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return null;
        }
        try {
            if (CastContext.getSharedInstance(context).getCastState() == CastState.CONNECTED) {
                return new CastPsmp(context, callback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public CastPsmp(@NonNull Context context, @NonNull PSMPCallback callback) {
        super(context, callback);

        castContext = CastContext.getSharedInstance(context);
        remoteMediaClient = castContext.getSessionManager().getCurrentCastSession().getRemoteMediaClient();
        remoteMediaClient.registerCallback(remoteMediaClientCallback);
        media = null;
        mediaType = null;
        startWhenPrepared = new AtomicBoolean(false);
        isBuffering = new AtomicBoolean(false);
        remoteState = MediaStatus.PLAYER_STATE_UNKNOWN;
    }

    private final RemoteMediaClient.Callback remoteMediaClientCallback = new RemoteMediaClient.Callback() {
        @Override
        public void onMetadataUpdated() {
            super.onMetadataUpdated();
            onRemoteMediaPlayerStatusUpdated();
        }

        @Override
        public void onPreloadStatusUpdated() {
            super.onPreloadStatusUpdated();
            onRemoteMediaPlayerStatusUpdated();
        }

        @Override
        public void onStatusUpdated() {
            super.onStatusUpdated();
            onRemoteMediaPlayerStatusUpdated();
        }

        @Override
        public void onMediaError(@NonNull MediaError mediaError) {
            EventBus.getDefault().postSticky(new PlayerErrorEvent(mediaError.getReason()));
        }
    };

    private void setBuffering(boolean buffering) {
        if (buffering && isBuffering.compareAndSet(false, true)) {
            EventBus.getDefault().post(BufferUpdateEvent.started());
        } else if (!buffering && isBuffering.compareAndSet(true, false)) {
            EventBus.getDefault().post(BufferUpdateEvent.ended());
        }
    }

    private Playable localVersion(MediaInfo info) {
        if (info == null || info.getMetadata() == null) {
            return null;
        }
        if (CastUtils.matches(info, media)) {
            return media;
        }
        String streamUrl = info.getMetadata().getString(CastUtils.KEY_STREAM_URL);
        return streamUrl == null ? CastUtils.makeRemoteMedia(info) : callback.findMedia(streamUrl);
    }

    private MediaInfo remoteVersion(Playable playable) {
        if (playable == null) {
            return null;
        }
        if (CastUtils.matches(remoteMedia, playable)) {
            return remoteMedia;
        }
        if (playable instanceof FeedMedia) {
            return MediaInfoCreator.from((FeedMedia) playable);
        }
        if (playable instanceof RemoteMedia) {
            return MediaInfoCreator.from((RemoteMedia) playable);
        }
        return null;
    }

    private void onRemoteMediaPlayerStatusUpdated() {
        MediaStatus status = remoteMediaClient.getMediaStatus();
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

        if (mediaChanged && stateChanged && oldState == MediaStatus.PLAYER_STATE_PLAYING
                && state != MediaStatus.PLAYER_STATE_IDLE) {
            callback.onPlaybackPause(null, Playable.INVALID_TIME);
            // We don't want setPlayerStatus to handle the onPlaybackPause callback
            setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
        }

        setBuffering(state == MediaStatus.PLAYER_STATE_BUFFERING);
        setPlaybackParams(PlaybackSpeedUtils.getCurrentPlaybackSpeed(currentMedia),
                getSkipSilence());

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
                setPlayerStatus((mediaChanged || playerStatus == PlayerStatus.PREPARING)
                                ? PlayerStatus.PREPARING : PlayerStatus.SEEKING, currentMedia,
                        currentMedia != null ? currentMedia.getPosition() : Playable.INVALID_TIME);
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
                            callback.onPlaybackPause(null, Playable.INVALID_TIME);
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
                        Log.w(TAG, "Got an error status from the Chromecast. "
                                + "Skipping, if possible, to the next episode...");
                        EventBus.getDefault().postSticky(new PlayerErrorEvent("Chromecast error code 1"));
                        endPlayback(false, false, true, true);
                        return;
                    default:
                        return;
                }
                break;
            case MediaStatus.PLAYER_STATE_UNKNOWN:
                if (playerStatus != PlayerStatus.INDETERMINATE || media != currentMedia) {
                    setPlayerStatus(PlayerStatus.INDETERMINATE, currentMedia);
                }
                break;
            default:
                Log.w(TAG, "Remote media state undetermined!");
        }
        if (mediaChanged) {
            callback.onMediaChanged(true);
            if (oldMedia != null) {
                callback.onPostPlayback(oldMedia, false, false, currentMedia != null);
            }
        }
    }

    @Override
    public void playMediaObject(@NonNull final Playable playable, final boolean stream,
                                final boolean startWhenPrepared, final boolean prepareImmediately) {
        Log.d(TAG, "playMediaObject() called");
        playMediaObject(playable, false, stream, startWhenPrepared, prepareImmediately);
    }

    /**
     * Internal implementation of playMediaObject. This method has an additional parameter that
     * allows the caller to force a media player reset even if
     * the given playable parameter is the same object as the currently playing media.
     *
     * @see #playMediaObject(Playable, boolean, boolean, boolean)
     */
    private void playMediaObject(@NonNull final Playable playable, final boolean forceReset,
                         final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        if (!CastUtils.isCastable(playable, castContext.getSessionManager().getCurrentCastSession())) {
            Log.d(TAG, "media provided is not compatible with cast device");
            EventBus.getDefault().postSticky(new PlayerErrorEvent("Media not compatible with cast device"));
            Playable nextPlayable = playable;
            do {
                nextPlayable = callback.getNextInQueue(nextPlayable);
            } while (nextPlayable != null && !CastUtils.isCastable(nextPlayable,
                    castContext.getSessionManager().getCurrentCastSession()));
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
                boolean isPlaying = remoteMediaClient.isPlaying();
                int position = (int) remoteMediaClient.getApproximateStreamPosition();
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
        callback.ensureMediaInfoLoaded(media);
        callback.onMediaChanged(true);
        setPlayerStatus(PlayerStatus.INITIALIZED, media);
        if (prepareImmediately) {
            prepare();
        }
    }

    @Override
    public void resume() {
        int newPosition = RewindAfterPauseUtils.calculatePositionWithRewind(
                        media.getPosition(),
                        media.getLastPlayedTime());
        seekTo(newPosition);
        remoteMediaClient.play();
    }

    @Override
    public void pause(boolean abandonFocus, boolean reinit) {
        remoteMediaClient.pause();
    }

    @Override
    public void prepare() {
        if (playerStatus == PlayerStatus.INITIALIZED) {
            Log.d(TAG, "Preparing media player");
            setPlayerStatus(PlayerStatus.PREPARING, media);
            int position = media.getPosition();
            if (position > 0) {
                position = RewindAfterPauseUtils.calculatePositionWithRewind(
                        position,
                        media.getLastPlayedTime());
            }
            remoteMediaClient.load(new MediaLoadRequestData.Builder()
                    .setMediaInfo(remoteMedia)
                    .setAutoplay(startWhenPrepared.get())
                    .setCurrentTime(position).build());
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
        new Exception("Seeking to " + t).printStackTrace();
        remoteMediaClient.seek(new MediaSeekOptions.Builder()
                .setPosition(t).build());
    }

    @Override
    public void seekDelta(int d) {
        int position = getPosition();
        if (position != Playable.INVALID_TIME) {
            seekTo(position + d);
        } else {
            Log.e(TAG, "getPosition() returned INVALID_TIME in seekDelta");
        }
    }

    @Override
    public int getDuration() {
        int retVal = (int) remoteMediaClient.getStreamDuration();
        if (retVal == Playable.INVALID_TIME && media != null && media.getDuration() > 0) {
            retVal = media.getDuration();
        }
        return retVal;
    }

    @Override
    public int getPosition() {
        int retVal = (int) remoteMediaClient.getApproximateStreamPosition();
        if (retVal <= 0 && media != null && media.getPosition() >= 0) {
            retVal = media.getPosition();
        }
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

    @Override
    public void setPlaybackParams(final float speed, final FeedPreferences.SkipSilence skipSilence) {
        double playbackRate = (float) Math.max(MediaLoadOptions.PLAYBACK_RATE_MIN,
                Math.min(MediaLoadOptions.PLAYBACK_RATE_MAX, speed));
        EventBus.getDefault().post(new SpeedChangedEvent((float) playbackRate));
        remoteMediaClient.setPlaybackRate(playbackRate);
    }

    @Override
    public float getPlaybackSpeed() {
        MediaStatus status = remoteMediaClient.getMediaStatus();
        return status != null ? (float) status.getPlaybackRate() : 1.0f;
    }

    @Override
    public FeedPreferences.SkipSilence getSkipSilence() {
        // Don't think this is supported
        return FeedPreferences.SkipSilence.OFF;
    }

    @Override
    public void setVolume(float volumeLeft, float volumeRight) {
        Log.d(TAG, "Setting the Stream volume on Remote Media Player");
        remoteMediaClient.setStreamVolume(volumeLeft);
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
        remoteMediaClient.unregisterCallback(remoteMediaClientCallback);
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
    public List<String> getAudioTracks() {
        return Collections.emptyList();
    }

    public void setAudioTrack(int track) {

    }

    public int getSelectedAudioTrack() {
        return -1;
    }

    @Override
    protected void endPlayback(boolean hasEnded, boolean wasSkipped, boolean shouldContinue,
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

            boolean playNextEpisode = isPlaying && nextMedia != null;
            if (playNextEpisode) {
                Log.d(TAG, "Playback of next episode will start immediately.");
            } else if (nextMedia == null) {
                Log.d(TAG, "No more episodes available to play");
            } else {
                Log.d(TAG, "Loading next episode, but not playing automatically.");
            }

            if (nextMedia != null) {
                callback.onPlaybackEnded(nextMedia.getMediaType(), !playNextEpisode);
                // setting media to null signals to playMediaObject() that we're taking care of post-playback processing
                media = null;
                playMediaObject(nextMedia, false, true, playNextEpisode, playNextEpisode);
            }
        }
        if (shouldContinue || toStoppedState) {
            if (nextMedia == null) {
                remoteMediaClient.stop();
                // Otherwise we rely on the chromecast callback to tell us the playback has stopped.
                callback.onPostPlayback(currentMedia, hasEnded, wasSkipped, false);
            } else {
                callback.onPostPlayback(currentMedia, hasEnded, wasSkipped, true);
            }
        } else if (isPlaying) {
            callback.onPlaybackPause(currentMedia,
                    currentMedia != null ? currentMedia.getPosition() : Playable.INVALID_TIME);
        }
    }

    @Override
    protected boolean shouldLockWifi() {
        return false;
    }

    @Override
    public boolean isCasting() {
        return true;
    }
}

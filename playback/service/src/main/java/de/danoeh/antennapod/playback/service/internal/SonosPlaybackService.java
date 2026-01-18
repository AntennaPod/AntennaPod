package de.danoeh.antennapod.playback.service.internal;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.github.kilianB.sonos.listener.SonosEventListener;
import com.github.kilianB.sonos.model.AVTransportEvent;
import com.github.kilianB.sonos.model.PlayMode;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.QueueEvent;
import com.github.kilianB.sonos.model.TrackInfo;
import com.github.kilianB.sonos.model.TrackMetadata;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.base.RewindAfterPauseUtils;
import de.danoeh.antennapod.playback.service.PlaybackService;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.system.utils.SonosSystem;
import de.danoeh.antennapod.ui.episodes.PlaybackSpeedUtils;

public class SonosPlaybackService extends PlaybackServiceMediaPlayer {
    private static final String TAG = "SonosPlaybackService";
    private volatile Playable media;
    private volatile boolean stream;
    private volatile MediaType mediaType;
    private boolean androidAutoConnected;
    private final AtomicBoolean startWhenPrepared;
    private final AudioManager audioManager;
    private final AudioFocusRequestCompat audioFocusRequest;
    private final Handler audioFocusCanceller;
    private boolean isShutDown = false;
    private volatile boolean pausedBecauseOfTransientAudiofocusLoss;
    private CountDownLatch seekLatch;
    private volatile PlayerStatus statusBeforeSeeking;
    private boolean registeredCallback;
    private volatile Pair<Integer, Integer> videoSize;
    private SonosDevice device;

    public SonosPlaybackService(@NonNull Context context, @NonNull PlaybackServiceMediaPlayer.PSMPCallback callback, String device_ip_address) {
        super(context, callback);
        try {
            device = new SonosDevice(device_ip_address);
        }
        catch (UnknownHostException e) {
            Log.d(TAG, "Sonos Playback Enabled and Sonos Device not Present or Assigned:\t" + e.toString());
            EventBus.getDefault().postSticky(new PlayerErrorEvent("Sonos Playback Enabled and Sonos Device not Present or Assigned"));
            throw new RuntimeException(e);
        }
        registeredCallback = false;
        audioFocusCanceller = new Handler(Looper.getMainLooper());
        startWhenPrepared = new AtomicBoolean(false);
        pausedBecauseOfTransientAudiofocusLoss = false;
        mediaType = MediaType.UNKNOWN;

        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributesCompat audioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
                .build();
        audioFocusRequest = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setWillPauseWhenDucked(true)
                .build();
    }

    public static Activity getActivity(Context context)
    {
        if (context == null)
        {
            return null;
        }
        else if (context instanceof ContextWrapper)
        {
            if (context instanceof Activity)
            {
                return (Activity) context;
            }
            else
            {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }

        return null;
    }
    public static List<SonosDevice> discover(Context context) {
        Vector<SonosDevice> finalDevices = new Vector<SonosDevice>();

        Activity activity = getActivity(context);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
                try {
                    List<SonosDevice> discovered_devices = SonosDiscovery.discover();
                    for (int i = 0; i < discovered_devices.size(); i++) {
                        SonosDevice dev = discovered_devices.get(i);
                        finalDevices.add(dev);
                    }
                } catch (IOException io) {
                    Log.d(TAG, "Sonos Playback Enabled and Sonos Device not Present or Assigned");
                    EventBus.getDefault().postSticky(new PlayerErrorEvent("Sonos Playback Enabled and Sonos Device not Present or Assigned"));
                    io.printStackTrace();
                }
            }
        });

        return finalDevices;
    }

    private void abandonAudioFocus() {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest);
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            if (isShutDown) {
                return;
            }
            if (!PlaybackService.isRunning) {
                abandonAudioFocus();
                return;
            }
            final PlayerStatus playerStatus = getPlayerStatus();
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                pause(true, false);
                callback.shouldStop();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                    && !UserPreferences.shouldPauseForFocusLoss()) {
                if (playerStatus == PlayerStatus.PLAYING) {
                    setVolume(0.25f, 0.25f);
                    pausedBecauseOfTransientAudiofocusLoss = false;
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (playerStatus == PlayerStatus.PLAYING) {
                    try {
                        device.pause();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SonosControllerException e) {
                        e.printStackTrace();
                    }
                    pausedBecauseOfTransientAudiofocusLoss = true;
                    audioFocusCanceller.removeCallbacksAndMessages(null);
                    audioFocusCanceller.postDelayed(() -> {
                        if (pausedBecauseOfTransientAudiofocusLoss) {
                            // Still did not get back the audio focus. Now actually pause.
                            pause(true, false);
                        }
                    }, 30000);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                audioFocusCanceller.removeCallbacksAndMessages(null);
                if (pausedBecauseOfTransientAudiofocusLoss) { // we paused => play now
                    try {
                        device.play();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SonosControllerException e) {
                        e.printStackTrace();
                    }
                } else { // we ducked => raise audio level back
                    setVolume(1.0f, 1.0f);
                }
                pausedBecauseOfTransientAudiofocusLoss = false;
            }
        }
    };

    public void playMediaObject(@NonNull final Playable playable, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        try {
            playMediaObject(playable, false, stream, startWhenPrepared, prepareImmediately);
        } catch (RuntimeException e) {
            Log.d(TAG, "Sonos Playback Enabled and Sonos Device not Present or Assigned");
            EventBus.getDefault().postSticky(new PlayerErrorEvent("Sonos Playback Enabled and Sonos Device not Present or Assigned"));
        }
    }

    private void playMediaObject(@NonNull final Playable playable, final boolean forceReset, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        if (media != null) {
            if (!forceReset && media.getIdentifier().equals(playable.getIdentifier())
                    && playerStatus == PlayerStatus.PLAYING) {
                return;
            }
            else {
                // stop playback of this episode
                if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.PREPARED) {
                    try {
                        device.stop();
                    } catch (IOException | SonosControllerException e) {
                        setPlayerStatus(PlayerStatus.ERROR, playable);
                        e.printStackTrace();
                    }
                }
                // set temporarily to pause in order to update list with current position
                if (playerStatus == PlayerStatus.PLAYING) {
                    callback.onPlaybackPause(media, getPosition());
                }

                if (!media.getIdentifier().equals(playable.getIdentifier())) {
                    final Playable oldMedia = media;
                    callback.onPostPlayback(oldMedia, false, false, true);
                }

                setPlayerStatus(PlayerStatus.INDETERMINATE, null);
            }
        }

        this.media = playable;
        this.stream = stream;
        this.mediaType = media.getMediaType();
        this.videoSize = null;
        SonosPlaybackService.this.startWhenPrepared.set(startWhenPrepared);
        setPlayerStatus(PlayerStatus.INITIALIZING, media);
/*
        if (!registeredCallback) {
            device.registerSonosEventListener(new SonosEventListener() {
                @Override
                public void volumeChanged(int newVolume) {
                }

                @Override
                public void playStateChanged(PlayState newPlayState) {
                    if (newPlayState == PlayState.PLAYING) {
                        setPlayerStatus(PlayerStatus.PLAYING, media);
                    }
                    if (newPlayState == PlayState.STOPPED) {
                        setPlayerStatus(PlayerStatus.STOPPED, media);
                    }
                    if (newPlayState == PlayState.PAUSED_PLAYBACK) {
                        setPlayerStatus(PlayerStatus.PAUSED, media);
                    }
                    if (newPlayState == PlayState.ERROR) {
                        setPlayerStatus(PlayerStatus.ERROR, media);
                    }
                    if (newPlayState == PlayState.TRANSITIONING) {
                        setPlayerStatus(PlayerStatus.PREPARING, media);
                    }
                }

                @Override
                public void playModeChanged(PlayMode newPlayMode) {
                }

                @Override
                public void trackChanged(TrackInfo currentTrack) {
                    List<Chapter> chapters = playable.getChapters();
                    final TrackInfo ti;
                    try {
                        ti = device.getCurrentTrackInfo();
                        final String skipTrack = ti.getUri();
                        int i = 0;
                        Chapter c = null;
                        for (Chapter chapter : chapters) {
                            if (chapter.getLink().compareTo(skipTrack) == 0) {
                                c = chapter;
                                i += 1;
                                break;
                            }
                            i += 1;
                        }
                        if (i < chapters.size() && (c != null)) {
                            final String url = c.getLink();

                            FeedItem fi = new FeedItem(
                                    i,
                                    c.getTitle(),
                                    c.getLink(),
                                    media.getPubDate(),
                                    "",
                                    c.getId(),
                                    true,
                                    c.getImageUrl(),
                                    0,
                                    c.getChapterId(),
                                    true,
                                    currentTrack.getUri(),
                                    "",
                                    "",
                                    ""
                            );
                            final Playable p = new FeedMedia(fi, url, currentTrack.getDuration(), "x-rincon-mp3radio");
                            playMediaObject(p, true, true, true);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SonosControllerException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void trebleChanged(int treble) {
                }

                @Override
                public void bassChanged(int bass) {
                }

                @Override
                public void loudenessChanged(boolean loudness) {
                }

                @Override
                public void avtTransportEvent(AVTransportEvent avtTransportEvent) {
                }

                @Override
                public void queueChanged(List<QueueEvent> queuesAffected) {
                }

                @Override
                public void sonosDeviceConnected(String deviceName) {
                }

                @Override
                public void sonosDeviceDisconnected(String deviceName) {
                }

                @Override
                public void groupChanged(ArrayList<String> allDevicesInZone) {
                }
            });

            registeredCallback = true;
        }
*/
        try {
            callback.ensureMediaInfoLoaded(media);
            callback.onMediaChanged(false);
            setPlaybackParams(PlaybackSpeedUtils.getCurrentPlaybackSpeed(media),
                    PlaybackSpeedUtils.getCurrentSkipSilencePreference(media)
                            == FeedPreferences.SkipSilence.AGGRESSIVE);
            if (stream) {
                final String url = playable.getStreamUrl();
                TrackMetadata trackMeta = new TrackMetadata(
                        playable.getEpisodeTitle(), // title
                        playable.getFeedTitle(),    // creator
                        playable.getFeedTitle(),    // album_artist
                        playable.getFeedTitle(),    // album
                        playable.getImageLocation() // album_art_uri
                );

                setPlayerStatus(PlayerStatus.INITIALIZED, playable);
                
                try {
                    device.playUri(url, trackMeta);
                    setPlayerStatus(PlayerStatus.PLAYING, playable);
                } catch (IOException | SonosControllerException e) {
                    setPlayerStatus(PlayerStatus.ERROR, playable);
                    final String errMessage = "Sonos I/O Exception";
                    Log.d(TAG, errMessage);
                    EventBus.getDefault().postSticky(new PlayerErrorEvent(errMessage));
                    throw new IOException(errMessage);
                }
            } else if (media.getLocalFileUrl() != null && new File(media.getLocalFileUrl()).canRead()) {
                final String errMessage = "Local files are not supported at this time " + media.getLocalFileUrl();
                Log.d(TAG, errMessage);
                EventBus.getDefault().postSticky(new PlayerErrorEvent(errMessage));
                throw new IOException(errMessage);

            } else {
                final String errMessage = "Unable to read local file " + media.getLocalFileUrl();
                Log.d(TAG, errMessage);
                EventBus.getDefault().postSticky(new PlayerErrorEvent(errMessage));
                throw new IOException(errMessage);
            }

            if (!androidAutoConnected) {
                setPlayerStatus(PlayerStatus.INITIALIZED, media);
            }

            if (prepareImmediately) {
                setPlayerStatus(PlayerStatus.PREPARING, media);
                onPrepared(startWhenPrepared);
            }

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            setPlayerStatus(PlayerStatus.ERROR, null);
            EventBus.getDefault().postSticky(new PlayerErrorEvent(e.getLocalizedMessage()));
        }
    }

    @Override
    public void resume() {
        final PlayerStatus playerStatus = this.getPlayerStatus();

        if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PREPARED) {
            int focusGained = AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest);

            if (focusGained == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                acquireWifiLockIfNecessary();

                setPlaybackParams(PlaybackSpeedUtils.getCurrentPlaybackSpeed(media),
                        PlaybackSpeedUtils.getCurrentSkipSilencePreference(media)
                                == FeedPreferences.SkipSilence.AGGRESSIVE);
                setVolume(1.0f, 1.0f);

                if (playerStatus == PlayerStatus.PREPARED && media.getPosition() > 0) {
                    int newPosition = RewindAfterPauseUtils.calculatePositionWithRewind(
                            media.getPosition(), media.getLastPlayedTimeStatistics());
                    seekTo(newPosition);
                }
                try {
                    device.play();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SonosControllerException e) {
                    e.printStackTrace();
                }

                setPlayerStatus(PlayerStatus.PLAYING, media);
                pausedBecauseOfTransientAudiofocusLoss = false;
            }
        }
    }

    @Override
    public void pause(boolean abandonFocus, boolean reinit) {
        releaseWifiLockIfNecessary();

        if (playerStatus == PlayerStatus.PLAYING) {
            try {
                device.pause();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SonosControllerException e) {
                e.printStackTrace();
            }
            setPlayerStatus(PlayerStatus.PAUSED, media, getPosition());

            if (abandonFocus) {
                abandonAudioFocus();
                pausedBecauseOfTransientAudiofocusLoss = false;
            }
            if (stream && reinit) {
                reinit();
            }
        }
    }

    private void onPrepared(final boolean startWhenPrepared) {
        if (playerStatus != PlayerStatus.PREPARING) {
            final String errMessage = "Player is not in PREPARING state";
            Log.d(TAG, errMessage);
            EventBus.getDefault().postSticky(new PlayerErrorEvent(errMessage));
            throw new IllegalStateException(errMessage);
        }

        // TODO this call has no effect!
        if (media.getPosition() > 0) {
            seekTo(media.getPosition());
        }

        if (media.getDuration() <= 0) {
            TrackInfo ti = null;
            try {
                ti = device.getCurrentTrackInfo();
                media.setDuration(ti.getDuration());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SonosControllerException e) {
                e.printStackTrace();
            }
        }
        setPlayerStatus(PlayerStatus.PREPARED, media);

        if (startWhenPrepared) {
            resume();
        }
    }

    @Override
    public void prepare() {
        if (playerStatus == PlayerStatus.INITIALIZED) {
            setPlayerStatus(PlayerStatus.PREPARING, media);
            onPrepared(startWhenPrepared.get());
        }
    }

    @Override
    public void reinit() {
        releaseWifiLockIfNecessary();
        SonosDevice device = SonosSystem.selectedDevice.get();

        if (media != null) {
            playMediaObject(media, true, stream, startWhenPrepared.get(), false);
        }
    }

    @Override
    public void seekTo(int t) {
        final int tValue [] = new int[1];
        tValue[0] = t;

        if (tValue[0] < 0) {
            tValue[0] = 0;
        }

        if (t >= getDuration()) {
            endPlayback(true, true, true, true);
            return;
        }

        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            if (seekLatch != null && seekLatch.getCount() > 0) {
                try {
                    seekLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            seekLatch = new CountDownLatch(1);
            statusBeforeSeeking = playerStatus;
            setPlayerStatus(PlayerStatus.SEEKING, media, getPosition());
            try {
                device.seek(tValue[0]);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SonosControllerException e) {
                e.printStackTrace();
            }
            if (statusBeforeSeeking == PlayerStatus.PREPARED) {
                media.setPosition(tValue[0]);
            }
            try {
                seekLatch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (playerStatus == PlayerStatus.INITIALIZED) {
            media.setPosition(t);
            startWhenPrepared.set(false);
            prepare();
        }
    }

    @Override
    public void seekDelta(int d) {
        int currentPosition = getPosition();
        if (currentPosition != Playable.INVALID_TIME) {
            seekTo(currentPosition + d);
        }
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getPosition() {
        final int[] retVal = new int[1];
        retVal[0] = Playable.INVALID_TIME;

        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            TrackInfo ti = null;
            try {
                ti = device.getCurrentTrackInfo();
                retVal[0] = ti.getPosition();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SonosControllerException e) {
                e.printStackTrace();
            }
        }

        if(retVal[0] <=0 && media !=null && media.getDuration()>0)
        {
            retVal[0] = media.getDuration();
        }

        return retVal[0];
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
    public void setPlaybackParams(float speed, boolean skipSilence) {
        EventBus.getDefault().post(new SpeedChangedEvent(speed));
    }

    @Override
    public float getPlaybackSpeed() {
        return 1;
    }

    @Override
    public boolean getSkipSilence() {
        return false;
    }

    @Override
    public void setVolume(float volumeLeft, float volumeRight) {
        float volLeft[] = new float[1];
        float volRight[] = new float[1];
        volLeft[0] = volumeLeft;
        volRight[0] = volumeRight;
        Playable playable = getPlayable();

        if (playable instanceof FeedMedia) {
            FeedMedia feedMedia = (FeedMedia) playable;
            FeedPreferences preferences = feedMedia.getItem().getFeed().getPreferences();
            VolumeAdaptionSetting volumeAdaptionSetting = preferences.getVolumeAdaptionSetting();
            float adaptionFactor = volumeAdaptionSetting.getAdaptionFactor();
            volLeft[0] *= adaptionFactor;
            volRight[0] *= adaptionFactor;
        }
        final int vol = (int) Math.ceil((volLeft[0] + volRight[0]) / 2.0);
        try {
            device.setVolume(vol);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SonosControllerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MediaType getCurrentMediaType() {
        return mediaType;
    }

    @Override
    public boolean isStreaming() {
        return stream;
    }

    @Override
    public void shutdown() {
        try {
            final PlayState state = device.getPlayState();
            if (state == PlayState.PLAYING) {
                device.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        playerStatus = PlayerStatus.STOPPED;

        isShutDown = true;
        abandonAudioFocus();
        releaseWifiLockIfNecessary();
    }

    @Override
    public void setVideoSurface(SurfaceHolder surface) {
    }

    @Override
    public void resetVideoSurface() {
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
        media = playable;
    }

    @Override
    public List<String> getAudioTracks() {
        Vector<String> ret = new Vector<String>();

        final int queue_track_count = 10;
        try {
            List<TrackMetadata> tml = device.getQueue(0, queue_track_count);
            for (TrackMetadata tm : tml) {
                ret.add(tm.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SonosControllerException e) {
            e.printStackTrace();
        }

        return ret;
    }

    @Override
    public void setAudioTrack(int track) {
        try {
            device.playFromQueue(track);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SonosControllerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getSelectedAudioTrack() {
        int rc[] = new int[1];
        rc[0] = -1;
        try {
            TrackInfo ti = device.getCurrentTrackInfo();
            rc[0] = ti.getQueueIndex();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SonosControllerException e) {
            e.printStackTrace();
        }

        return rc[0];
    }

    @Override
    protected void endPlayback(boolean hasEnded, boolean wasSkipped, boolean shouldContinue, boolean toStoppedState) {
        final boolean should_continue[] = new boolean[1];
        should_continue[0] = shouldContinue;

        releaseWifiLockIfNecessary();

        callback.episodeFinishedPlayback(); // notify that the current episode just finished

        boolean isPlaying = playerStatus == PlayerStatus.PLAYING;

        // we're relying on the position stored in the Playable object for post-playback processing
        if (media != null) {
            int position = getPosition();
            if (position >= 0) {
                media.setPosition(position);
            }
        }

        try {
            device.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SonosControllerException e) {
            e.printStackTrace();
        }

        abandonAudioFocus();

        final Playable currentMedia = media;
        Playable nextMedia = null;

        // we should continue to next episode if we were told to continue and we're allowed to (by sleep timer)
        should_continue[0] &= callback.shouldContinueToNextEpisode();

        if (should_continue[0]) {
            // Load next episode if previous episode was in the queue and if there
            // is an episode in the queue left.
            // Start playback immediately if continuous playback is enabled
            nextMedia = callback.getNextInQueue(currentMedia);
            if (nextMedia != null) {
                callback.onPlaybackEnded(nextMedia.getMediaType(), false);
                // setting media to null signals to playMediaObject() that
                // we're taking care of post-playback processing
                media = null;
                playMediaObject(nextMedia, false, !nextMedia.localFileAvailable(), isPlaying, isPlaying);
            } else if (wasSkipped) {
                EventBus.getDefault().post(new MessageEvent(context.getString(R.string.no_following_in_queue)));
            }
        }
        if (should_continue[0] || toStoppedState) {
            if (nextMedia == null) {
                callback.onPlaybackEnded(null, true);
                try {
                    device.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SonosControllerException e) {
                    e.printStackTrace();
                }
            }
            final boolean hasNext = nextMedia != null;

            callback.onPostPlayback(currentMedia, hasEnded, wasSkipped, hasNext);
        } else if (isPlaying) {
            callback.onPlaybackPause(currentMedia, currentMedia.getPosition());
        }
    }

    @Override
    protected boolean shouldLockWifi() {
        return stream;
    }

    @Override
    public boolean isCasting() {
        return false;
    }
}
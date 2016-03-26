package de.danoeh.antennapod.core.service.playback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;

import java.util.List;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.cast.CastConsumer;
import de.danoeh.antennapod.core.cast.CastConsumerImpl;
import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction.Action;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.IntList;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.QueueAccess;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Controls the MediaPlayer that plays a FeedMedia-file
 */
public class PlaybackService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String FORCE_WIDGET_UPDATE = "de.danoeh.antennapod.FORCE_WIDGET_UPDATE";
    public static final String STOP_WIDGET_UPDATE = "de.danoeh.antennapod.STOP_WIDGET_UPDATE";
    /**
     * Logging tag
     */
    private static final String TAG = "PlaybackService";

    /**
     * Parcelable of type Playable.
     */
    public static final String EXTRA_PLAYABLE = "PlaybackService.PlayableExtra";
    /**
     * True if media should be streamed.
     */
    public static final String EXTRA_SHOULD_STREAM = "extra.de.danoeh.antennapod.core.service.shouldStream";
    /**
     * True if playback should be started immediately after media has been
     * prepared.
     */
    public static final String EXTRA_START_WHEN_PREPARED = "extra.de.danoeh.antennapod.core.service.startWhenPrepared";

    public static final String EXTRA_PREPARE_IMMEDIATELY = "extra.de.danoeh.antennapod.core.service.prepareImmediately";

    public static final String ACTION_PLAYER_STATUS_CHANGED = "action.de.danoeh.antennapod.core.service.playerStatusChanged";
    public static final String EXTRA_NEW_PLAYER_STATUS = "extra.de.danoeh.antennapod.service.playerStatusChanged.newStatus";
    private static final String AVRCP_ACTION_PLAYER_STATUS_CHANGED = "com.android.music.playstatechanged";
    private static final String AVRCP_ACTION_META_CHANGED = "com.android.music.metachanged";

    public static final String ACTION_PLAYER_NOTIFICATION = "action.de.danoeh.antennapod.core.service.playerNotification";
    public static final String EXTRA_NOTIFICATION_CODE = "extra.de.danoeh.antennapod.core.service.notificationCode";
    public static final String EXTRA_NOTIFICATION_TYPE = "extra.de.danoeh.antennapod.core.service.notificationType";

    /**
     * If the PlaybackService receives this action, it will stop playback and
     * try to shutdown.
     */
    public static final String ACTION_SHUTDOWN_PLAYBACK_SERVICE = "action.de.danoeh.antennapod.core.service.actionShutdownPlaybackService";

    /**
     * If the PlaybackService receives this action, it will end playback of the
     * current episode and load the next episode if there is one available.
     */
    public static final String ACTION_SKIP_CURRENT_EPISODE = "action.de.danoeh.antennapod.core.service.skipCurrentEpisode";

    /**
     * If the PlaybackService receives this action, it will pause playback.
     */
    public static final String ACTION_PAUSE_PLAY_CURRENT_EPISODE = "action.de.danoeh.antennapod.core.service.pausePlayCurrentEpisode";


    /**
     * If the PlaybackService receives this action, it will resume playback.
     */
    public static final String ACTION_RESUME_PLAY_CURRENT_EPISODE = "action.de.danoeh.antennapod.core.service.resumePlayCurrentEpisode";


    /**
     * Used in NOTIFICATION_TYPE_RELOAD.
     */
    public static final int EXTRA_CODE_AUDIO = 1;
    public static final int EXTRA_CODE_VIDEO = 2;
    public static final int EXTRA_CODE_CAST = 3;

    public static final int NOTIFICATION_TYPE_ERROR = 0;
    public static final int NOTIFICATION_TYPE_INFO = 1;
    public static final int NOTIFICATION_TYPE_BUFFER_UPDATE = 2;

    /**
     * Receivers of this intent should update their information about the curently playing media
     */
    public static final int NOTIFICATION_TYPE_RELOAD = 3;
    /**
     * The state of the sleeptimer changed.
     */
    public static final int NOTIFICATION_TYPE_SLEEPTIMER_UPDATE = 4;
    public static final int NOTIFICATION_TYPE_BUFFER_START = 5;
    public static final int NOTIFICATION_TYPE_BUFFER_END = 6;
    /**
     * No more episodes are going to be played.
     */
    public static final int NOTIFICATION_TYPE_PLAYBACK_END = 7;

    /**
     * Playback speed has changed
     */
    public static final int NOTIFICATION_TYPE_PLAYBACK_SPEED_CHANGE = 8;

    /**
     * Ability to set the playback speed has changed
     */
    public static final int NOTIFICATION_TYPE_SET_SPEED_ABILITY_CHANGED = 9;

    /**
     * Returned by getPositionSafe() or getDurationSafe() if the playbackService
     * is in an invalid state.
     */
    public static final int INVALID_TIME = -1;

    /**
     * Time in seconds during which the CastManager will try to reconnect to the Cast Device after
     * the Wifi Connection is regained.
     */
    private static final int RECONNECTION_ATTEMPT_PERIOD_S = 15;

    /**
     * Is true if service is running.
     */
    public static boolean isRunning = false;
    /**
     * Is true if service has received a valid start command.
     */
    public static boolean started = false;
    /**
     * Is true if the service was running, but paused due to headphone disconnect
     */
    public static boolean transientPause = false;
    /**
     * Is true if a Cast Device is connected to the service.
     */
    private static volatile boolean isCasting = false;
    /**
     * Stores the state of the cast playback just before it disconnects.
     */
    private volatile PlaybackServiceMediaPlayer.PSMPInfo infoBeforeCastDisconnection;

    private boolean mWifiConnectivity = true;
    private BroadcastReceiver mWifiBroadcastReceiver;

    private static final int NOTIFICATION_ID = 1;

    private PlaybackServiceMediaPlayer mediaPlayer;
    private PlaybackServiceTaskManager taskManager;
    /**
     * Only used for Lollipop notifications.
     */
    private MediaSessionCompat mediaSession;

    private int startPosition;

    private static volatile MediaType currentMediaType = MediaType.UNKNOWN;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Received onUnbind event");
        return super.onUnbind(intent);
    }

    //TODO review the general intent handling and how a casting activity can be introduced
    /**
     * Returns an intent which starts an audio- or videoplayer, depending on the
     * type of media that is being played. If the playbackservice is not
     * running, the type of the last played media will be looked up.
     */
    public static Intent getPlayerActivityIntent(Context context) {
        if (isRunning) {
            return ClientConfig.playbackServiceCallbacks.getPlayerActivityIntent(context, currentMediaType);
        } else {
            if (PlaybackPreferences.getCurrentEpisodeIsVideo()) {
                return ClientConfig.playbackServiceCallbacks.getPlayerActivityIntent(context, MediaType.VIDEO);
            } else {
                return ClientConfig.playbackServiceCallbacks.getPlayerActivityIntent(context, MediaType.AUDIO);
            }
        }
    }

    /**
     * Same as getPlayerActivityIntent(context), but here the type of activity
     * depends on the FeedMedia that is provided as an argument.
     */
    public static Intent getPlayerActivityIntent(Context context, Playable media) {
        MediaType mt = media.getMediaType();
        return ClientConfig.playbackServiceCallbacks.getPlayerActivityIntent(context, mt);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created.");
        isRunning = true;

        registerReceiver(headsetDisconnected, new IntentFilter(
                Intent.ACTION_HEADSET_PLUG));
        registerReceiver(shutdownReceiver, new IntentFilter(
                ACTION_SHUTDOWN_PLAYBACK_SERVICE));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            registerReceiver(bluetoothStateUpdated, new IntentFilter(
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        }
        registerReceiver(audioBecomingNoisy, new IntentFilter(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(skipCurrentEpisodeReceiver, new IntentFilter(
                ACTION_SKIP_CURRENT_EPISODE));
        registerReceiver(pausePlayCurrentEpisodeReceiver, new IntentFilter(
                ACTION_PAUSE_PLAY_CURRENT_EPISODE));
        registerReceiver(pauseResumeCurrentEpisodeReceiver, new IntentFilter(
                ACTION_RESUME_PLAY_CURRENT_EPISODE));
        taskManager = new PlaybackServiceTaskManager(this, taskManagerCallback);
        CastManager castMgr = CastManager.getInstance();
        castMgr.addCastConsumer(castConsumer);
        isCasting = castMgr.isConnected();
        if (isCasting) {
            onCastAppConnected(true);
        } else {
            mediaPlayer = new LocalPSMP(this, mediaPlayerCallback);
        }

        ComponentName eventReceiver = new ComponentName(getApplicationContext(),
                MediaButtonReceiver.class);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG, eventReceiver, buttonReceiverIntent);

        try {
            mediaSession.setCallback(sessionCallback);
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setActive(true);
        } catch (NullPointerException npe) {
            // on some devices (Huawei) setting active can cause a NullPointerException
            // even with correct use of the api.
            // See http://stackoverflow.com/questions/31556679/android-huawei-mediassessioncompat
            // and https://plus.google.com/+IanLake/posts/YgdTkKFxz7d
            Log.e(TAG, "NullPointerException while setting up MediaSession");
            npe.printStackTrace();
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service is about to be destroyed");
        isRunning = false;
        started = false;
        currentMediaType = MediaType.UNKNOWN;

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        if (mediaSession != null) {
            mediaSession.release();
        }
        unregisterReceiver(headsetDisconnected);
        unregisterReceiver(shutdownReceiver);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            unregisterReceiver(bluetoothStateUpdated);
        }
        unregisterReceiver(audioBecomingNoisy);
        unregisterReceiver(skipCurrentEpisodeReceiver);
        unregisterReceiver(pausePlayCurrentEpisodeReceiver);
        unregisterReceiver(pauseResumeCurrentEpisodeReceiver);
        CastManager.getInstance().removeCastConsumer(castConsumer);
        unregisterWifiBroadcastReceiver();
        mediaPlayer.shutdown();
        taskManager.shutdown();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Received onBind event");
        return mBinder;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(UserPreferences.PREF_LOCKSCREEN_BACKGROUND)) {
            updateMediaSessionMetadata(getPlayable());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d(TAG, "OnStartCommand called");
        final int keycode = intent.getIntExtra(MediaButtonReceiver.EXTRA_KEYCODE, -1);
        final Playable playable = intent.getParcelableExtra(EXTRA_PLAYABLE);
        if (keycode == -1 && playable == null) {
            Log.e(TAG, "PlaybackService was started with no arguments");
            stopSelf();
            return Service.START_REDELIVER_INTENT;
        }

        if ((flags & Service.START_FLAG_REDELIVERY) != 0) {
            Log.d(TAG, "onStartCommand is a redelivered intent, calling stopForeground now.");
            stopForeground(true);
        } else {

            if (keycode != -1) {
                Log.d(TAG, "Received media button event");
                handleKeycode(keycode, intent.getIntExtra(MediaButtonReceiver.EXTRA_SOURCE,
                        InputDevice.SOURCE_CLASS_NONE));
            } else {
                started = true;
                boolean stream = intent.getBooleanExtra(EXTRA_SHOULD_STREAM,
                        true);
                boolean startWhenPrepared = intent.getBooleanExtra(EXTRA_START_WHEN_PREPARED, false);
                boolean prepareImmediately = intent.getBooleanExtra(EXTRA_PREPARE_IMMEDIATELY, false);
                sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
                //If the user asks to play External Media, the casting session, if on, should end.
                if (playable instanceof ExternalMedia) {
                    CastManager.getInstance().disconnect();
                }
                mediaPlayer.playMediaObject(playable, stream, startWhenPrepared, prepareImmediately);
            }
        }

        return Service.START_REDELIVER_INTENT;
    }

    /**
     * Handles media button events
     */
    private void handleKeycode(int keycode, int source) {
        Log.d(TAG, "Handling keycode: " + keycode);
        final PlaybackServiceMediaPlayer.PSMPInfo info = mediaPlayer.getPSMPInfo();
        final PlayerStatus status = info.playerStatus;
        switch (keycode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (status == PlayerStatus.PLAYING) {
                    if (UserPreferences.isPersistNotify()) {
                        mediaPlayer.pause(false, true);
                    } else {
                        mediaPlayer.pause(true, true);
                    }
                } else if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (status == PlayerStatus.PREPARING) {
                    mediaPlayer.setStartWhenPrepared(!mediaPlayer.isStartWhenPrepared());
                } else if (status == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (status == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(false, true);
                }
                if (UserPreferences.isPersistNotify()) {
                    mediaPlayer.pause(false, true);
                } else {
                    mediaPlayer.pause(true, true);
                }

                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if(source == InputDevice.SOURCE_CLASS_NONE ||
                        UserPreferences.shouldHardwareButtonSkip()) {
                    // assume the skip command comes from a notification or the lockscreen
                    // a >| skip button should actually skip
                    mediaPlayer.endPlayback(true, false);
                } else {
                    // assume skip command comes from a (bluetooth) media button
                    // user actually wants to fast-forward
                    seekDelta(UserPreferences.getFastFowardSecs() * 1000);
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                mediaPlayer.seekDelta(UserPreferences.getFastFowardSecs() * 1000);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                mediaPlayer.seekDelta(-UserPreferences.getRewindSecs() * 1000);
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(true, true);
                    started = false;
                }

                stopForeground(true); // gets rid of persistent notification
                break;
            default:
                Log.d(TAG, "Unhandled key code: " + keycode);
                if (info.playable != null && info.playerStatus == PlayerStatus.PLAYING) {   // only notify the user about an unknown key event if it is actually doing something
                    String message = String.format(getResources().getString(R.string.unknown_media_key), keycode);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Called by a mediaplayer Activity as soon as it has prepared its
     * mediaplayer.
     */
    public void setVideoSurface(SurfaceHolder sh) {
        Log.d(TAG, "Setting display");
        mediaPlayer.setVideoSurface(sh);
    }

    /**
     * Called when the surface holder of the mediaplayer has to be changed.
     */
    private void resetVideoSurface() {
        taskManager.cancelPositionSaver();
        mediaPlayer.resetVideoSurface();
    }

    public void notifyVideoSurfaceAbandoned() {
        stopForeground(true);
        mediaPlayer.resetVideoSurface();
    }

    private final PlaybackServiceTaskManager.PSTMCallback taskManagerCallback = new PlaybackServiceTaskManager.PSTMCallback() {
        @Override
        public void positionSaverTick() {
            saveCurrentPosition(true, PlaybackServiceTaskManager.POSITION_SAVER_WAITING_INTERVAL);
        }

        @Override
        public void onSleepTimerAlmostExpired() {
            float leftVolume = 0.1f * UserPreferences.getLeftVolume();
            float rightVolume = 0.1f * UserPreferences.getRightVolume();
            mediaPlayer.setVolume(leftVolume, rightVolume);
        }

        @Override
        public void onSleepTimerExpired() {
            mediaPlayer.pause(true, true);
            float leftVolume = UserPreferences.getLeftVolume();
            float rightVolume = UserPreferences.getRightVolume();
            mediaPlayer.setVolume(leftVolume, rightVolume);
            sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
        }

        @Override
        public void onSleepTimerReset() {
            float leftVolume = UserPreferences.getLeftVolume();
            float rightVolume = UserPreferences.getRightVolume();
            mediaPlayer.setVolume(leftVolume, rightVolume);
        }

        @Override
        public void onWidgetUpdaterTick() {
            updateWidget();
        }

        @Override
        public void onChapterLoaded(Playable media) {
            sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
        }
    };

    private final PlaybackServiceMediaPlayer.PSMPCallback mediaPlayerCallback = new PlaybackServiceMediaPlayer.PSMPCallback() {
        @Override
        public void statusChanged(PlaybackServiceMediaPlayer.PSMPInfo newInfo) {
            currentMediaType = mediaPlayer.getCurrentMediaType();
            updateMediaSession(newInfo.playerStatus);
            switch (newInfo.playerStatus) {
                case INITIALIZED:
                    writePlaybackPreferences();
                    break;

                case PREPARED:
                    taskManager.startChapterLoader(newInfo.playable);
                    break;

                case PAUSED:
                    taskManager.cancelPositionSaver();
                    saveCurrentPosition(false, 0);
                    taskManager.cancelWidgetUpdater();
                    if (UserPreferences.isPersistNotify() && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        // do not remove notification on pause based on user pref and whether android version supports expanded notifications
                        // Change [Play] button to [Pause]
                        setupNotification(newInfo);
                    } else if (!UserPreferences.isPersistNotify()) {
                        // remove notifcation on pause
                        stopForeground(true);
                    }
                    writePlayerStatusPlaybackPreferences();

                    final Playable playable = newInfo.playable;

                    // Gpodder: send play action
                    if(GpodnetPreferences.loggedIn() && playable instanceof FeedMedia) {
                        FeedMedia media = (FeedMedia) playable;
                        FeedItem item = media.getItem();
                        GpodnetEpisodeAction action = new GpodnetEpisodeAction.Builder(item, Action.PLAY)
                                .currentDeviceId()
                                .currentTimestamp()
                                .started(startPosition / 1000)
                                .position(getCurrentPosition() / 1000)
                                .total(getDuration() / 1000)
                                .build();
                        GpodnetPreferences.enqueueEpisodeAction(action);
                    }
                    break;

                case STOPPED:
                    //setCurrentlyPlayingMedia(PlaybackPreferences.NO_MEDIA_PLAYING);
                    //stopSelf();
                    break;

                case PLAYING:
                    Log.d(TAG, "Audiofocus successfully requested");
                    Log.d(TAG, "Resuming/Starting playback");

                    taskManager.startPositionSaver();
                    taskManager.startWidgetUpdater();
                    writePlayerStatusPlaybackPreferences();
                    setupNotification(newInfo);
                    started = true;
                    startPosition = mediaPlayer.getPosition();
                    break;

                case ERROR:
                    writePlaybackPreferencesNoMediaPlaying();
                    break;

            }

            Intent statusUpdate = new Intent(ACTION_PLAYER_STATUS_CHANGED);
            // statusUpdate.putExtra(EXTRA_NEW_PLAYER_STATUS, newInfo.playerStatus.ordinal());
            sendBroadcast(statusUpdate);
            updateWidget();
            bluetoothNotifyChange(newInfo, AVRCP_ACTION_PLAYER_STATUS_CHANGED);
            bluetoothNotifyChange(newInfo, AVRCP_ACTION_META_CHANGED);
        }

        @Override
        public void shouldStop() {
            stopSelf();
        }

        @Override
        public void playbackSpeedChanged(float s) {
            sendNotificationBroadcast(NOTIFICATION_TYPE_PLAYBACK_SPEED_CHANGE, 0);
        }

        public void setSpeedAbilityChanged() {
            sendNotificationBroadcast(NOTIFICATION_TYPE_SET_SPEED_ABILITY_CHANGED, 0);
        }

        @Override
        public void onBufferingUpdate(int percent) {
            sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_UPDATE, percent);
        }

        @Override
        public void updateMediaSessionMetadata(Playable p) {
            PlaybackService.this.updateMediaSessionMetadata(p);
        }

        @Override
        public boolean onMediaPlayerInfo(int code) {
            switch (code) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_START, 0);
                    return true;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_END, 0);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onMediaPlayerError(Object inObj, int what, int extra) {
            final String TAG = "PlaybackSvc.onErrorLtsn";
            Log.w(TAG, "An error has occured: " + what + " " + extra);
            if (mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING) {
                mediaPlayer.pause(true, false);
            }
            sendNotificationBroadcast(NOTIFICATION_TYPE_ERROR, what);
            writePlaybackPreferencesNoMediaPlaying();
            stopSelf();
            return true;
        }

        @Override
        public boolean endPlayback(boolean playNextEpisode, boolean wasSkipped, boolean switchingPlayers) {
            PlaybackService.this.endPlayback(playNextEpisode, wasSkipped, switchingPlayers);
            return true;
        }
    };

    private void endPlayback(boolean playNextEpisode, boolean wasSkipped, boolean switchingPlayers) {
        Log.d(TAG, "Playback ended");

        final Playable playable = mediaPlayer.getPlayable();
        if (playable == null) {
            Log.e(TAG, "Cannot end playback: media was null");
            return;
        }

        taskManager.cancelPositionSaver();

        boolean isInQueue = false;
        FeedItem nextItem = null;

        if (playable instanceof FeedMedia) {
            FeedMedia media = (FeedMedia) playable;
            FeedItem item = media.getItem();

            try {
                final List<FeedItem> queue = taskManager.getQueue();
                isInQueue = QueueAccess.ItemListAccess(queue).contains(item.getId());
                nextItem = DBTasks.getQueueSuccessorOfItem(item.getId(), queue);
            } catch (InterruptedException e) {
                e.printStackTrace();
                // isInQueue remains false
            }

            boolean shouldKeep = wasSkipped && UserPreferences.shouldSkipKeepEpisode();

            if (!shouldKeep) {
                // only mark the item as played if we're not keeping it anyways
                DBWriter.markItemPlayed(item, FeedItem.PLAYED, true);

                if (isInQueue) {
                    DBWriter.removeQueueItem(PlaybackService.this, item, true);
                }
            }

            DBWriter.addItemToPlaybackHistory(media);

            // auto-flattr if enabled
            if (isAutoFlattrable(media) && UserPreferences.getAutoFlattrPlayedDurationThreshold() == 1.0f) {
                DBTasks.flattrItemIfLoggedIn(PlaybackService.this, item);
            }

            // Delete episode if enabled
            if(item.getFeed().getPreferences().getCurrentAutoDelete() && !shouldKeep ) {
                DBWriter.deleteFeedMediaOfItem(PlaybackService.this, media.getId());
                Log.d(TAG, "Episode Deleted");
            }

            // gpodder play action
            if(GpodnetPreferences.loggedIn()) {
                GpodnetEpisodeAction action = new GpodnetEpisodeAction.Builder(item, Action.PLAY)
                        .currentDeviceId()
                        .currentTimestamp()
                        .started(startPosition / 1000)
                        .position(getDuration() / 1000)
                        .total(getDuration() / 1000)
                        .build();
                GpodnetPreferences.enqueueEpisodeAction(action);
            }
        }

        // Load next episode if previous episode was in the queue and if there
        // is an episode in the queue left.
        // Start playback immediately if continuous playback is enabled
        Playable nextMedia = null;
        boolean loadNextItem = ClientConfig.playbackServiceCallbacks.useQueue() &&
                isInQueue &&
                nextItem != null;

        playNextEpisode = playNextEpisode &&
                loadNextItem &&
                UserPreferences.isFollowQueue();

        if (loadNextItem) {
            Log.d(TAG, "Loading next item in queue");
            nextMedia = nextItem.getMedia();
        }
        final boolean prepareImmediately;
        final boolean startWhenPrepared;
        final boolean stream;

        if (playNextEpisode) {
            Log.d(TAG, "Playback of next episode will start immediately.");
            prepareImmediately = startWhenPrepared = true;
        } else {
            Log.d(TAG, "No more episodes available to play");
            prepareImmediately = startWhenPrepared = false;
            stopForeground(true);
            stopWidgetUpdater();
        }

        writePlaybackPreferencesNoMediaPlaying();
        if (nextMedia != null) {
            stream = !nextMedia.localFileAvailable();
            mediaPlayer.playMediaObject(nextMedia, stream, startWhenPrepared, prepareImmediately);
            sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD,
                    isCasting ? EXTRA_CODE_CAST :
                    (nextMedia.getMediaType() == MediaType.VIDEO) ? EXTRA_CODE_VIDEO : EXTRA_CODE_AUDIO);
        } else {
            if (!switchingPlayers) {
                sendNotificationBroadcast(NOTIFICATION_TYPE_PLAYBACK_END, 0);
            }
            mediaPlayer.stop();
            //stopSelf();
        }
    }

    public void setSleepTimer(long waitingTime, boolean shakeToReset, boolean vibrate) {
        Log.d(TAG, "Setting sleep timer to " + Long.toString(waitingTime) + " milliseconds");
        taskManager.setSleepTimer(waitingTime, shakeToReset, vibrate);
        sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
    }

    public void disableSleepTimer() {
        taskManager.disableSleepTimer();
        sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
    }

    private void writePlaybackPreferencesNoMediaPlaying() {
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA,
                PlaybackPreferences.NO_MEDIA_PLAYING);
        editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
                PlaybackPreferences.NO_MEDIA_PLAYING);
        editor.putLong(
                PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
                PlaybackPreferences.NO_MEDIA_PLAYING);
        editor.putInt(
                PlaybackPreferences.PREF_CURRENT_PLAYER_STATUS,
                PlaybackPreferences.PLAYER_STATUS_OTHER);
        editor.commit();
    }

    private int getCurrentPlayerStatusAsInt(PlayerStatus playerStatus) {
        int playerStatusAsInt;
        switch (playerStatus) {
            case PLAYING:
                playerStatusAsInt = PlaybackPreferences.PLAYER_STATUS_PLAYING;
                break;
            case PAUSED:
                playerStatusAsInt = PlaybackPreferences.PLAYER_STATUS_PAUSED;
                break;
            default:
                playerStatusAsInt = PlaybackPreferences.PLAYER_STATUS_OTHER;
        }
        return playerStatusAsInt;
    }

    private void writePlaybackPreferences() {
        Log.d(TAG, "Writing playback preferences");

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();
        PlaybackServiceMediaPlayer.PSMPInfo info = mediaPlayer.getPSMPInfo();
        MediaType mediaType = mediaPlayer.getCurrentMediaType();
        boolean stream = mediaPlayer.isStreaming();
        int playerStatus = getCurrentPlayerStatusAsInt(info.playerStatus);

        if (info.playable != null) {
            editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA,
                    info.playable.getPlayableType());
            editor.putBoolean(
                    PlaybackPreferences.PREF_CURRENT_EPISODE_IS_STREAM,
                    stream);
            editor.putBoolean(
                    PlaybackPreferences.PREF_CURRENT_EPISODE_IS_VIDEO,
                    mediaType == MediaType.VIDEO);
            if (info.playable instanceof FeedMedia) {
                FeedMedia fMedia = (FeedMedia) info.playable;
                editor.putLong(
                        PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
                        fMedia.getItem().getFeed().getId());
                editor.putLong(
                        PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
                        fMedia.getId());
            } else {
                editor.putLong(
                        PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
                        PlaybackPreferences.NO_MEDIA_PLAYING);
                editor.putLong(
                        PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
                        PlaybackPreferences.NO_MEDIA_PLAYING);
            }
            info.playable.writeToPreferences(editor);
        } else {
            editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA,
                    PlaybackPreferences.NO_MEDIA_PLAYING);
            editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
                    PlaybackPreferences.NO_MEDIA_PLAYING);
            editor.putLong(
                    PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
                    PlaybackPreferences.NO_MEDIA_PLAYING);
        }
        editor.putInt(
                PlaybackPreferences.PREF_CURRENT_PLAYER_STATUS, playerStatus);

        editor.commit();
    }

    private void writePlayerStatusPlaybackPreferences() {
        Log.d(TAG, "Writing player status playback preferences");

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();
        int playerStatus = getCurrentPlayerStatusAsInt(mediaPlayer.getPlayerStatus());

        editor.putInt(
                PlaybackPreferences.PREF_CURRENT_PLAYER_STATUS, playerStatus);

        editor.commit();
    }

    /**
     * Send ACTION_PLAYER_STATUS_CHANGED without changing the status attribute.
     */
    private void postStatusUpdateIntent() {
        sendBroadcast(new Intent(ACTION_PLAYER_STATUS_CHANGED));
    }

    private void sendNotificationBroadcast(int type, int code) {
        Intent intent = new Intent(ACTION_PLAYER_NOTIFICATION);
        intent.putExtra(EXTRA_NOTIFICATION_TYPE, type);
        intent.putExtra(EXTRA_NOTIFICATION_CODE, code);
        sendBroadcast(intent);
    }

    /**
     * Used by setupNotification to load notification data in another thread.
     */
    private Thread notificationSetupThread;

    /**
     * Updates the Media Session for the corresponding status.
     * @param playerStatus the current {@link PlayerStatus}
     */
    private void updateMediaSession(final PlayerStatus playerStatus) {
        PlaybackStateCompat.Builder sessionState = new PlaybackStateCompat.Builder();

        int state;
        if (playerStatus != null) {
            switch (playerStatus) {
                case PLAYING:
                    state = PlaybackStateCompat.STATE_PLAYING;
                    break;
                case PREPARED:
                case PAUSED:
                    state = PlaybackStateCompat.STATE_PAUSED;
                    break;
                case STOPPED:
                    state = PlaybackStateCompat.STATE_STOPPED;
                    break;
                case SEEKING:
                    state = PlaybackStateCompat.STATE_FAST_FORWARDING;
                    break;
                case PREPARING:
                case INITIALIZING:
                    state = PlaybackStateCompat.STATE_CONNECTING;
                    break;
                case INITIALIZED:
                case INDETERMINATE:
                    state = PlaybackStateCompat.STATE_NONE;
                    break;
                case ERROR:
                    state = PlaybackStateCompat.STATE_ERROR;
                    break;
                default:
                    state = PlaybackStateCompat.STATE_NONE;
                    break;
            }
        } else {
            state = PlaybackStateCompat.STATE_NONE;
        }
        sessionState.setState(state, mediaPlayer.getPosition(), mediaPlayer.getPlaybackSpeed());
        sessionState.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_FAST_FORWARD
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        mediaSession.setPlaybackState(sessionState.build());
    }

    private void updateMediaSessionMetadata(Playable p) {
        if (p == null) {
            return;
        }
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, p.getFeedTitle());
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, p.getEpisodeTitle());
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, p.getDuration());
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, p.getEpisodeTitle());
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, p.getFeedTitle());

        if (p.getImageUri() != null && UserPreferences.setLockscreenBackground()) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, p.getImageUri().toString());
            try {
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Bitmap art = Glide.with(this)
                        .load(p.getImageUri())
                        .asBitmap()
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .centerCrop()
                        .into(display.getWidth(), display.getHeight())
                        .get();
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art);
            } catch (Throwable tr) {
                Log.e(TAG, Log.getStackTraceString(tr));
            }
        }
        mediaSession.setMetadata(builder.build());
    }

    /**
     * Prepares notification and starts the service in the foreground.
     */
    private void setupNotification(final PlaybackServiceMediaPlayer.PSMPInfo info) {
        final PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                PlaybackService.getPlayerActivityIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (notificationSetupThread != null) {
            notificationSetupThread.interrupt();
        }
        Runnable notificationSetupTask = new Runnable() {
            Bitmap icon = null;

            @Override
            public void run() {
                Log.d(TAG, "Starting background work");
                if (android.os.Build.VERSION.SDK_INT >= 11) {
                    if (info.playable != null) {
                        int iconSize = getResources().getDimensionPixelSize(
                                android.R.dimen.notification_large_icon_width);
                        try {
                            icon = Glide.with(PlaybackService.this)
                                    .load(info.playable.getImageUri())
                                    .asBitmap()
                                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                                    .centerCrop()
                                    .into(iconSize, iconSize)
                                    .get();
                        } catch(Throwable tr) {
                            Log.e(TAG, Log.getStackTraceString(tr));
                        }
                    }
                }
                if (icon == null) {
                    icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                            ClientConfig.playbackServiceCallbacks.getNotificationIconResource(getApplicationContext()));
                }

                if (mediaPlayer == null) {
                    return;
                }
                PlayerStatus playerStatus = mediaPlayer.getPlayerStatus();
                final int smallIcon = ClientConfig.playbackServiceCallbacks.getNotificationIconResource(getApplicationContext());

                if (!Thread.currentThread().isInterrupted() && started && info.playable != null) {
                    String contentText = info.playable.getEpisodeTitle();
                    String contentTitle = info.playable.getFeedTitle();
                    Notification notification;

                    // Builder is v7, even if some not overwritten methods return its parent's v4 interface
                    NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(
                            PlaybackService.this)
                            .setContentTitle(contentTitle)
                            .setContentText(contentText)
                            .setOngoing(false)
                            .setContentIntent(pIntent)
                            .setLargeIcon(icon)
                            .setSmallIcon(smallIcon)
                            .setWhen(0) // we don't need the time
                            .setPriority(UserPreferences.getNotifyPriority()); // set notification priority
                    IntList compactActionList = new IntList();

                    int numActions = 0; // we start and 0 and then increment by 1 for each call to addAction

                    // always let them rewind
                    PendingIntent rewindButtonPendingIntent = getPendingIntentForMediaAction(
                            KeyEvent.KEYCODE_MEDIA_REWIND, numActions);
                    notificationBuilder.addAction(android.R.drawable.ic_media_rew,
                            getString(R.string.rewind_label),
                            rewindButtonPendingIntent);
                    if(UserPreferences.showRewindOnCompactNotification()) {
                        compactActionList.add(numActions);
                    }
                    numActions++;

                    if (playerStatus == PlayerStatus.PLAYING) {
                        PendingIntent pauseButtonPendingIntent = getPendingIntentForMediaAction(
                                KeyEvent.KEYCODE_MEDIA_PAUSE, numActions);
                        notificationBuilder.addAction(android.R.drawable.ic_media_pause, //pause action
                                getString(R.string.pause_label),
                                pauseButtonPendingIntent);
                        compactActionList.add(numActions++);
                    } else {
                        PendingIntent playButtonPendingIntent = getPendingIntentForMediaAction(
                                KeyEvent.KEYCODE_MEDIA_PLAY, numActions);
                        notificationBuilder.addAction(android.R.drawable.ic_media_play, //play action
                                getString(R.string.play_label),
                                playButtonPendingIntent);
                        compactActionList.add(numActions++);
                    }

                    // ff follows play, then we have skip (if it's present)
                    PendingIntent ffButtonPendingIntent = getPendingIntentForMediaAction(
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, numActions);
                    notificationBuilder.addAction(android.R.drawable.ic_media_ff,
                            getString(R.string.fast_forward_label),
                            ffButtonPendingIntent);
                    if(UserPreferences.showFastForwardOnCompactNotification()) {
                        compactActionList.add(numActions);
                    }
                    numActions++;

                    if (UserPreferences.isFollowQueue()) {
                        PendingIntent skipButtonPendingIntent = getPendingIntentForMediaAction(
                                KeyEvent.KEYCODE_MEDIA_NEXT, numActions);
                        notificationBuilder.addAction(android.R.drawable.ic_media_next,
                                getString(R.string.skip_episode_label),
                                skipButtonPendingIntent);
                        if(UserPreferences.showSkipOnCompactNotification()) {
                            compactActionList.add(numActions);
                        }
                        numActions++;
                    }

                    PendingIntent stopButtonPendingIntent = getPendingIntentForMediaAction(
                            KeyEvent.KEYCODE_MEDIA_STOP, numActions);
                    notificationBuilder.setStyle(new android.support.v7.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(compactActionList.toArray())
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(stopButtonPendingIntent))
                            .setVisibility(Notification.VISIBILITY_PUBLIC)
                            .setColor(Notification.COLOR_DEFAULT);

                    notification = notificationBuilder.build();

                    if (playerStatus == PlayerStatus.PLAYING ||
                            playerStatus == PlayerStatus.PREPARING ||
                            playerStatus == PlayerStatus.SEEKING) {
                        startForeground(NOTIFICATION_ID, notification);
                    } else {
                        stopForeground(false);
                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        mNotificationManager.notify(NOTIFICATION_ID, notification);
                    }
                    Log.d(TAG, "Notification set up");
                }
            }
        };
        notificationSetupThread = new Thread(notificationSetupTask);
        notificationSetupThread.start();
    }

    private PendingIntent getPendingIntentForMediaAction(int keycodeValue, int requestCode) {
        Intent intent = new Intent(
                PlaybackService.this, PlaybackService.class);
        intent.putExtra(
                MediaButtonReceiver.EXTRA_KEYCODE,
                keycodeValue);
        return PendingIntent
                .getService(PlaybackService.this, requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Persists the current position and last played time of the media file.
     *
     * @param updatePlayedDuration true if played_duration should be updated. This applies only to FeedMedia objects
     * @param deltaPlayedDuration  value by which played_duration should be increased.
     */
    private synchronized void saveCurrentPosition(boolean updatePlayedDuration, int deltaPlayedDuration) {
        int position = getCurrentPosition();
        int duration = getDuration();
        float playbackSpeed = getCurrentPlaybackSpeed();
        final Playable playable = mediaPlayer.getPlayable();
        if (position != INVALID_TIME && duration != INVALID_TIME && playable != null) {
            Log.d(TAG, "Saving current position to " + position);
            if (updatePlayedDuration && playable instanceof FeedMedia) {
                FeedMedia media = (FeedMedia) playable;
                FeedItem item = media.getItem();
                media.setPlayedDuration(media.getPlayedDuration() + ((int) (deltaPlayedDuration * playbackSpeed)));
                // Auto flattr
                if (isAutoFlattrable(media) &&
                        (media.getPlayedDuration() > UserPreferences.getAutoFlattrPlayedDurationThreshold() * duration)) {
                    Log.d(TAG, "saveCurrentPosition: performing auto flattr since played duration " + Integer.toString(media.getPlayedDuration())
                                + " is " + UserPreferences.getAutoFlattrPlayedDurationThreshold() * 100 + "% of file duration " + Integer.toString(duration));
                    DBTasks.flattrItemIfLoggedIn(this, item);
                }
            }
            playable.saveCurrentPosition(PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext()),
                    position,
                    System.currentTimeMillis()
            );
        }
    }

    private void stopWidgetUpdater() {
        taskManager.cancelWidgetUpdater();
        sendBroadcast(new Intent(STOP_WIDGET_UPDATE));
    }

    private void updateWidget() {
        PlaybackService.this.sendBroadcast(new Intent(
                FORCE_WIDGET_UPDATE));
    }

    public boolean sleepTimerActive() {
        return taskManager.isSleepTimerActive();
    }

    public long getSleepTimerTimeLeft() {
        return taskManager.getSleepTimerTimeLeft();
    }

    private void bluetoothNotifyChange(PlaybackServiceMediaPlayer.PSMPInfo info, String whatChanged) {
        boolean isPlaying = false;

        if (info.playerStatus == PlayerStatus.PLAYING) {
            isPlaying = true;
        }

        if (info.playable != null) {
            Intent i = new Intent(whatChanged);
            i.putExtra("id", 1);
            i.putExtra("artist", "");
            i.putExtra("album", info.playable.getFeedTitle());
            i.putExtra("track", info.playable.getEpisodeTitle());
            i.putExtra("playing", isPlaying);
            final List<FeedItem> queue = taskManager.getQueueIfLoaded();
            if (queue != null) {
                i.putExtra("ListSize", queue.size());
            }
            i.putExtra("duration", info.playable.getDuration());
            i.putExtra("position", info.playable.getPosition());
            sendBroadcast(i);
        }
    }

    /**
     * Pauses playback when the headset is disconnected and the preference is
     * set
     */
    private final BroadcastReceiver headsetDisconnected = new BroadcastReceiver() {
        private static final String TAG = "headsetDisconnected";
        private static final int UNPLUGGED = 0;
        private static final int PLUGGED = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                if (state != -1) {
                    Log.d(TAG, "Headset plug event. State is " + state);
                    if (state == UNPLUGGED) {
                        Log.d(TAG, "Headset was unplugged during playback.");
                        pauseIfPauseOnDisconnect();
                    } else if (state == PLUGGED) {
                        Log.d(TAG, "Headset was plugged in during playback.");
                        unpauseIfPauseOnDisconnect(false);
                    }
                } else {
                    Log.e(TAG, "Received invalid ACTION_HEADSET_PLUG intent");
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothStateUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (TextUtils.equals(intent.getAction(), BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
                    if (state == BluetoothA2dp.STATE_CONNECTED) {
                        Log.d(TAG, "Received bluetooth connection intent");
                        unpauseIfPauseOnDisconnect(true);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver audioBecomingNoisy = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // sound is about to change, eg. bluetooth -> speaker
            Log.d(TAG, "Pausing playback because audio is becoming noisy");
            pauseIfPauseOnDisconnect();
        }
        // android.media.AUDIO_BECOMING_NOISY
    };

    /**
     * Pauses playback if PREF_PAUSE_ON_HEADSET_DISCONNECT was set to true.
     */
    private void pauseIfPauseOnDisconnect() {
        if (UserPreferences.isPauseOnHeadsetDisconnect()) {
            if (mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING) {
                transientPause = true;
            }
            if (UserPreferences.isPersistNotify()) {
                mediaPlayer.pause(false, true);
            } else {
                mediaPlayer.pause(true, true);
            }
        }
    }

    /**
     * @param bluetooth true if the event for unpausing came from bluetooth
     */
    private void unpauseIfPauseOnDisconnect(boolean bluetooth) {
        if (transientPause) {
            transientPause = false;
            if (!bluetooth && UserPreferences.isUnpauseOnHeadsetReconnect()) {
                mediaPlayer.resume();
            } else if (bluetooth && UserPreferences.isUnpauseOnBluetoothReconnect()){
                // let the user know we've started playback again...
                Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                if(v != null) {
                    v.vibrate(500);
                }
                mediaPlayer.resume();
            }
        }
    }

    private final BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ACTION_SHUTDOWN_PLAYBACK_SERVICE)) {
                stopSelf();
            }
        }

    };

    private final BroadcastReceiver skipCurrentEpisodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ACTION_SKIP_CURRENT_EPISODE)) {
                Log.d(TAG, "Received SKIP_CURRENT_EPISODE intent");
                mediaPlayer.endPlayback(true, false);
            }
        }
    };

    private final BroadcastReceiver pauseResumeCurrentEpisodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ACTION_RESUME_PLAY_CURRENT_EPISODE)) {
                Log.d(TAG, "Received RESUME_PLAY_CURRENT_EPISODE intent");
                mediaPlayer.resume();
            }
        }
    };

    private final BroadcastReceiver pausePlayCurrentEpisodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ACTION_PAUSE_PLAY_CURRENT_EPISODE)) {
                Log.d(TAG, "Received PAUSE_PLAY_CURRENT_EPISODE intent");
                mediaPlayer.pause(false, false);
            }
        }
    };

    public static MediaType getCurrentMediaType() {
        return currentMediaType;
    }

    public void resume() {
        mediaPlayer.resume();
    }

    public void prepare() {
        mediaPlayer.prepare();
    }

    public void pause(boolean abandonAudioFocus, boolean reinit) {
        mediaPlayer.pause(abandonAudioFocus, reinit);
    }

    public void reinit() {
        mediaPlayer.reinit();
    }

    public PlaybackServiceMediaPlayer.PSMPInfo getPSMPInfo() {
        return mediaPlayer.getPSMPInfo();
    }

    public PlayerStatus getStatus() {
        return mediaPlayer.getPlayerStatus();
    }

    public Playable getPlayable() { return mediaPlayer.getPlayable(); }

    public boolean canSetSpeed() {
        return mediaPlayer.canSetSpeed();
    }

    public void setSpeed(float speed) {
        mediaPlayer.setSpeed(speed);
    }

    public void setVolume(float leftVolume, float rightVolume) {
        mediaPlayer.setVolume(leftVolume, rightVolume);
    }

    public float getCurrentPlaybackSpeed() {
        return mediaPlayer.getPlaybackSpeed();
    }

    public boolean canDownmix() {
        return mediaPlayer.canDownmix();
    }

    public void setDownmix(boolean enable) {
        mediaPlayer.setDownmix(enable);
    }

    public boolean isStartWhenPrepared() {
        return mediaPlayer.isStartWhenPrepared();
    }

    public void setStartWhenPrepared(boolean s) {
        mediaPlayer.setStartWhenPrepared(s);
    }


    public void seekTo(final int t) {
        if(mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING
                && GpodnetPreferences.loggedIn()) {
            final Playable playable = mediaPlayer.getPlayable();
            if (playable instanceof FeedMedia) {
                FeedMedia media = (FeedMedia) playable;
                FeedItem item = media.getItem();
                GpodnetEpisodeAction action = new GpodnetEpisodeAction.Builder(item, Action.PLAY)
                        .currentDeviceId()
                        .currentTimestamp()
                        .started(startPosition / 1000)
                        .position(getCurrentPosition() / 1000)
                        .total(getDuration() / 1000)
                        .build();
                GpodnetPreferences.enqueueEpisodeAction(action);
            }
        }
        mediaPlayer.seekTo(t);
        if(mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING ) {
            startPosition = t;
        }
    }


    public void seekDelta(final int d) {
        mediaPlayer.seekDelta(d);
    }

    /**
     * @see LocalPSMP#seekToChapter(de.danoeh.antennapod.core.feed.Chapter)
     */
    public void seekToChapter(Chapter c) {
        mediaPlayer.seekToChapter(c);
    }

    /**
     * call getDuration() on mediaplayer or return INVALID_TIME if player is in
     * an invalid state.
     */
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    /**
     * call getCurrentPosition() on mediaplayer or return INVALID_TIME if player
     * is in an invalid state.
     */
    public int getCurrentPosition() {
        return mediaPlayer.getPosition();
    }

    public boolean isStreaming() {
        return mediaPlayer.isStreaming();
    }

    public Pair<Integer, Integer> getVideoSize() {
        return mediaPlayer.getVideoSize();
    }

    private boolean isAutoFlattrable(FeedMedia media) {
        if (media != null) {
            FeedItem item = media.getItem();
            return item != null && FlattrUtils.hasToken() && UserPreferences.isAutoFlattr() && item.getPaymentLink() != null && item.getFlattrStatus().getUnflattred();
        } else {
            return false;
        }
    }

    private final MediaSessionCompat.Callback sessionCallback = new MediaSessionCompat.Callback() {

        private static final String TAG = "MediaSessionCompat";

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay()");
            PlayerStatus status = getStatus();
            if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                resume();
            } else if (status == PlayerStatus.INITIALIZED) {
                setStartWhenPrepared(true);
                prepare();
            }
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause()");
            if (getStatus() == PlayerStatus.PLAYING) {
                pause(false, true);
            }
            if (UserPreferences.isPersistNotify()) {
                pause(false, true);
            } else {
                pause(true, true);
            }
        }

        @Override
        public void onStop() {
            Log.d(TAG, "onStop()");
            mediaPlayer.stop();
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious()");
            seekDelta(-UserPreferences.getRewindSecs() * 1000);
        }

        @Override
        public void onRewind() {
            Log.d(TAG, "onRewind()");
            seekDelta(-UserPreferences.getRewindSecs() * 1000);
        }

        @Override
        public void onFastForward() {
            Log.d(TAG, "onFastForward()");
            seekDelta(UserPreferences.getFastFowardSecs() * 1000);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext()");
            if(UserPreferences.shouldHardwareButtonSkip()) {
                mediaPlayer.endPlayback(true, false);
            } else {
                seekDelta(UserPreferences.getFastFowardSecs() * 1000);
            }
        }


        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "onSeekTo()");
            seekTo((int) pos);
        }

        @Override
        public boolean onMediaButtonEvent(final Intent mediaButton) {
            Log.d(TAG, "onMediaButtonEvent(" + mediaButton + ")");
            if (mediaButton != null) {
                KeyEvent keyEvent = (KeyEvent) mediaButton.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null &&
                        keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getRepeatCount() == 0){
                    handleKeycode(keyEvent.getKeyCode(), keyEvent.getSource());
                }
            }
            return false;
        }
    };

    private CastConsumer castConsumer = new CastConsumerImpl() {
        @Override
        public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
            PlaybackService.this.onCastAppConnected(wasLaunched);
        }

        @Override
        public void onDisconnectionReason(int reason) {
            Log.d(TAG, "onDisconnectionReason() with code " + reason);
            // This is our final chance to update the underlying stream position
            // In onDisconnected(), the underlying CastPlayback#mVideoCastConsumer
            // is disconnected and hence we update our local value of stream position
            // to the latest position.
            if (mediaPlayer != null) {
                saveCurrentPosition(false, 0);
                infoBeforeCastDisconnection = mediaPlayer.getPSMPInfo();
                if (reason != BaseCastManager.DISCONNECT_REASON_EXPLICIT &&
                        infoBeforeCastDisconnection.playerStatus == PlayerStatus.PLAYING) {
                    // If it's NOT based on user action, we shouldn't automatically resume local playback
                    infoBeforeCastDisconnection.playerStatus = PlayerStatus.PAUSED;
                }
            }
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected()");
            isCasting = false;
            PlaybackServiceMediaPlayer.PSMPInfo info = infoBeforeCastDisconnection;
            infoBeforeCastDisconnection = null;
            if (info == null && mediaPlayer != null) {
                info = mediaPlayer.getPSMPInfo();
            }
            if (info == null) {
                info = new PlaybackServiceMediaPlayer.PSMPInfo(PlayerStatus.STOPPED, null);
            }
            switchMediaPlayer(new LocalPSMP(PlaybackService.this, mediaPlayerCallback),
                    info);
            if (info.playable != null) {
                sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD,
                        info.playable.getMediaType() == MediaType.AUDIO ? EXTRA_CODE_AUDIO : EXTRA_CODE_VIDEO);
            } else {
                Log.d(TAG, "Cast session disconnected, but no current media");
                sendNotificationBroadcast(NOTIFICATION_TYPE_PLAYBACK_END, 0);
            }
            unregisterWifiBroadcastReceiver();
        }
    };

    private void onCastAppConnected(boolean wasLaunched) {
        Log.d(TAG, "A cast device application was connected");
        isCasting = true;
        if (mediaPlayer != null) {
            PlaybackServiceMediaPlayer.PSMPInfo info = mediaPlayer.getPSMPInfo();
            if (info.playerStatus == PlayerStatus.PLAYING) {
                // could be pause, but this way we make sure the new player will get the correct position, since pause runs asynchronously
                saveCurrentPosition(false, 0);
            }
        }
        switchMediaPlayer(new RemotePSMP(PlaybackService.this, mediaPlayerCallback),
                (mediaPlayer != null) ? mediaPlayer.getPSMPInfo() :
                        new PlaybackServiceMediaPlayer.PSMPInfo(PlayerStatus.STOPPED, null));
        sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, EXTRA_CODE_CAST);
        registerWifiBroadcastReceiver();
    }

    private void switchMediaPlayer(@NonNull PlaybackServiceMediaPlayer newPlayer,
                                   @NonNull PlaybackServiceMediaPlayer.PSMPInfo info) {
        if (mediaPlayer != null) {
            mediaPlayer.endPlayback(true, true);
            mediaPlayer.shutdownAsync();
        }
        mediaPlayer = newPlayer;
        Log.d(TAG, "switched to " + mediaPlayer.getClass().getSimpleName());
        if (info.playable != null) {
            mediaPlayer.playMediaObject(info.playable,
                    !info.playable.localFileAvailable(),
                    info.playerStatus == PlayerStatus.PLAYING,
                    info.playerStatus.isAtLeast(PlayerStatus.PREPARING));
        }
    }

    private void registerWifiBroadcastReceiver() {
        if (mWifiBroadcastReceiver != null) {
            return;
        }
        mWifiBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    boolean isConnected = info.isConnected();
                    //apparently this method gets called twice when a change happens, but one run is enough.
                    if (isConnected && !mWifiConnectivity) {
                        mWifiConnectivity = true;
                        CastManager castMgr = CastManager.getInstance();
                        castMgr.startCastDiscovery();
                        castMgr.reconnectSessionIfPossible(RECONNECTION_ATTEMPT_PERIOD_S, NetworkUtils.getWifiSsid());
                    } else {
                        mWifiConnectivity = isConnected;
                    }
                }
            }
        };
        registerReceiver(mWifiBroadcastReceiver,
                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    private void unregisterWifiBroadcastReceiver() {
        if (mWifiBroadcastReceiver != null) {
            unregisterReceiver(mWifiBroadcastReceiver);
            mWifiBroadcastReceiver = null;
        }
    }
}

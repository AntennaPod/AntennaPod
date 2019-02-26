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
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.core.event.ServiceEvent;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.feed.SearchResult;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.service.PlayerWidgetJobService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.FeedSearcher;
import de.danoeh.antennapod.core.util.IntList;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.QueueAccess;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.greenrobot.event.EventBus;

/**
 * Controls the MediaPlayer that plays a FeedMedia-file
 */
public class PlaybackService extends MediaBrowserServiceCompat {
    /**
     * Logging tag
     */
    private static final String TAG = "PlaybackService";

    /**
     * Parcelable of type Playable.
     */
    public static final String EXTRA_PLAYABLE = "PlaybackService.PlayableExtra";
    /**
     * True if cast session should disconnect.
     */
    private static final String EXTRA_CAST_DISCONNECT = "extra.de.danoeh.antennapod.core.service.castDisconnect";
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
     * Custom action used by Android Wear
     */
    private static final String CUSTOM_ACTION_FAST_FORWARD = "action.de.danoeh.antennapod.core.service.fastForward";
    private static final String CUSTOM_ACTION_REWIND = "action.de.danoeh.antennapod.core.service.rewind";


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
     * Send a message to the user (with provided String resource id)
     */
    public static final int NOTIFICATION_TYPE_SHOW_TOAST = 10;

    /**
     * Returned by getPositionSafe() or getDurationSafe() if the playbackService
     * is in an invalid state.
     */
    public static final int INVALID_TIME = -1;

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
    private static boolean transientPause = false;
    /**
     * Is true if a Cast Device is connected to the service.
     */
    private static volatile boolean isCasting = false;

    private static final int NOTIFICATION_ID = 1;

    private PlaybackServiceMediaPlayer mediaPlayer;
    private PlaybackServiceTaskManager taskManager;
    private PlaybackServiceFlavorHelper flavorHelper;

    /**
     * Used for Lollipop notifications, Android Wear, and Android Auto.
     */
    private MediaSessionCompat mediaSession;

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

    /**
     * Returns an intent which starts an audio- or videoplayer, depending on the
     * type of media that is being played. If the playbackservice is not
     * running, the type of the last played media will be looked up.
     */
    public static Intent getPlayerActivityIntent(Context context) {
        if (isRunning) {
            return ClientConfig.playbackServiceCallbacks.getPlayerActivityIntent(context, currentMediaType, isCasting);
        } else {
            if (PlaybackPreferences.getCurrentEpisodeIsVideo()) {
                return ClientConfig.playbackServiceCallbacks.getPlayerActivityIntent(context, MediaType.VIDEO, isCasting);
            } else {
                return ClientConfig.playbackServiceCallbacks.getPlayerActivityIntent(context, MediaType.AUDIO, isCasting);
            }
        }
    }

    /**
     * Same as getPlayerActivityIntent(context), but here the type of activity
     * depends on the FeedMedia that is provided as an argument.
     */
    public static Intent getPlayerActivityIntent(Context context, Playable media) {
        MediaType mt = media.getMediaType();
        return ClientConfig.playbackServiceCallbacks.getPlayerActivityIntent(context, mt, isCasting);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created.");
        isRunning = true;

        NotificationCompat.Builder notificationBuilder = createBasicNotification();
        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        registerReceiver(autoStateUpdated, new IntentFilter("com.google.android.gms.car.media.STATUS"));
        registerReceiver(headsetDisconnected, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        registerReceiver(shutdownReceiver, new IntentFilter(ACTION_SHUTDOWN_PLAYBACK_SERVICE));
        registerReceiver(bluetoothStateUpdated, new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        registerReceiver(audioBecomingNoisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(skipCurrentEpisodeReceiver, new IntentFilter(ACTION_SKIP_CURRENT_EPISODE));
        registerReceiver(pausePlayCurrentEpisodeReceiver, new IntentFilter(ACTION_PAUSE_PLAY_CURRENT_EPISODE));
        registerReceiver(pauseResumeCurrentEpisodeReceiver, new IntentFilter(ACTION_RESUME_PLAY_CURRENT_EPISODE));
        taskManager = new PlaybackServiceTaskManager(this, taskManagerCallback);

        flavorHelper = new PlaybackServiceFlavorHelper(PlaybackService.this, flavorHelperCallback);
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(prefListener);

        ComponentName eventReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG, eventReceiver, buttonReceiverIntent);
        setSessionToken(mediaSession.getSessionToken());

        try {
            mediaSession.setCallback(sessionCallback);
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        } catch (NullPointerException npe) {
            // on some devices (Huawei) setting active can cause a NullPointerException
            // even with correct use of the api.
            // See http://stackoverflow.com/questions/31556679/android-huawei-mediassessioncompat
            // and https://plus.google.com/+IanLake/posts/YgdTkKFxz7d
            Log.e(TAG, "NullPointerException while setting up MediaSession");
            npe.printStackTrace();
        }

        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        try {
            for (FeedItem feedItem : taskManager.getQueue()) {
                if (feedItem.getMedia() != null) {
                    MediaDescriptionCompat mediaDescription = feedItem.getMedia().getMediaItem().getDescription();
                    queueItems.add(new MediaSessionCompat.QueueItem(mediaDescription, feedItem.getId()));
                }
            }
            mediaSession.setQueue(queueItems);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        flavorHelper.initializeMediaPlayer(PlaybackService.this);
        mediaSession.setActive(true);

        EventBus.getDefault().post(new ServiceEvent(ServiceEvent.Action.SERVICE_STARTED));
    }

    private NotificationCompat.Builder createBasicNotification() {
        final int smallIcon = ClientConfig.playbackServiceCallbacks.getNotificationIconResource(getApplicationContext());

        final PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                PlaybackService.getPlayerActivityIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(
                this, NotificationUtils.CHANNEL_ID_PLAYING)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running") // Just in case the notification is not updated (should not occur)
                .setOngoing(false)
                .setContentIntent(pIntent)
                .setWhen(0) // we don't need the time
                .setSmallIcon(smallIcon)
                .setPriority(NotificationCompat.PRIORITY_MIN);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service is about to be destroyed");
        stopForeground(true);
        isRunning = false;
        started = false;
        currentMediaType = MediaType.UNKNOWN;

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(prefListener);
        if (mediaSession != null) {
            mediaSession.release();
        }
        unregisterReceiver(autoStateUpdated);
        unregisterReceiver(headsetDisconnected);
        unregisterReceiver(shutdownReceiver);
        unregisterReceiver(bluetoothStateUpdated);
        unregisterReceiver(audioBecomingNoisy);
        unregisterReceiver(skipCurrentEpisodeReceiver);
        unregisterReceiver(pausePlayCurrentEpisodeReceiver);
        unregisterReceiver(pauseResumeCurrentEpisodeReceiver);
        flavorHelper.removeCastConsumer();
        flavorHelper.unregisterWifiBroadcastReceiver();
        mediaPlayer.shutdown();
        taskManager.shutdown();
    }
    
    private void stopService() {
        stopForeground(true);
        stopSelf();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        Log.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName +
                "; clientUid=" + clientUid + " ; rootHints=" + rootHints);
        return new BrowserRoot(
                getResources().getString(R.string.app_name), // Name visible in Android Auto
                null); // Bundle of optional extras
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForRoot() {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(getResources().getString(R.string.queue_label))
                .setTitle(getResources().getString(R.string.queue_label))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForFeed(Feed feed) {
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId("FeedId:" + Long.toString(feed.getId()))
                .setTitle(feed.getTitle())
                .setDescription(feed.getDescription())
                .setSubtitle(feed.getCustomTitle());
        if (feed.getImageLocation() != null) {
            builder.setIconUri(Uri.parse(feed.getImageLocation()));
        }
        if (feed.getLink() != null) {
            builder.setMediaUri(Uri.parse(feed.getLink()));
        }
        MediaDescriptionCompat description = builder.build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "OnLoadChildren: parentMediaId=" + parentId);
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        if (parentId.equals(getResources().getString(R.string.app_name))) {
            // Root List
            try {
                if (!(taskManager.getQueue().isEmpty())) {
                    mediaItems.add(createBrowsableMediaItemForRoot());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<Feed> feeds = DBReader.getFeedList();
            for (Feed feed : feeds) {
                mediaItems.add(createBrowsableMediaItemForFeed(feed));
            }
        } else if (parentId.equals(getResources().getString(R.string.queue_label))) {
            // Child List
            try {
                for (FeedItem feedItem : taskManager.getQueue()) {
                    mediaItems.add(feedItem.getMedia().getMediaItem());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (parentId.startsWith("FeedId:")) {
            Long feedId = Long.parseLong(parentId.split(":")[1]);
            List<FeedItem> feedItems = DBReader.getFeedItemList(DBReader.getFeed(feedId));
            for (FeedItem feedItem : feedItems) {
                if (feedItem.getMedia() != null && feedItem.getMedia().getMediaItem() != null) {
                    mediaItems.add(feedItem.getMedia().getMediaItem());
                }
            }
        }
        result.sendResult(mediaItems);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Received onBind event");
        if (intent.getAction() != null && TextUtils.equals(intent.getAction(), MediaBrowserServiceCompat.SERVICE_INTERFACE)) {
            return super.onBind(intent);
        } else {
            return mBinder;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d(TAG, "OnStartCommand called");
        final int keycode = intent.getIntExtra(MediaButtonReceiver.EXTRA_KEYCODE, -1);
        final boolean castDisconnect = intent.getBooleanExtra(EXTRA_CAST_DISCONNECT, false);
        Playable playable = intent.getParcelableExtra(EXTRA_PLAYABLE);
        if (keycode == -1 && playable == null && !castDisconnect) {
            Log.e(TAG, "PlaybackService was started with no arguments");
            stopService();
            return Service.START_NOT_STICKY;
        }

        if ((flags & Service.START_FLAG_REDELIVERY) != 0) {
            Log.d(TAG, "onStartCommand is a redelivered intent, calling stopForeground now.");
            stopForeground(true);
        } else {
            if (keycode != -1) {
                Log.d(TAG, "Received media button event");
                boolean handled = handleKeycode(keycode, true);
                if (!handled) {
                    stopService();
                    return Service.START_NOT_STICKY;
                }
            } else if (!flavorHelper.castDisconnect(castDisconnect) && playable != null) {
                started = true;
                boolean stream = intent.getBooleanExtra(EXTRA_SHOULD_STREAM,
                        true);
                boolean startWhenPrepared = intent.getBooleanExtra(EXTRA_START_WHEN_PREPARED, false);
                boolean prepareImmediately = intent.getBooleanExtra(EXTRA_PREPARE_IMMEDIATELY, false);
                sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
                //If the user asks to play External Media, the casting session, if on, should end.
                flavorHelper.castDisconnect(playable instanceof ExternalMedia);
                if (playable instanceof FeedMedia) {
                    playable = DBReader.getFeedMedia(((FeedMedia) playable).getId());
                }
                mediaPlayer.playMediaObject(playable, stream, startWhenPrepared, prepareImmediately);
            }
            setupNotification(playable);
        }

        return Service.START_NOT_STICKY;
    }

    /**
     * Handles media button events
     * return: keycode was handled
     */
    private boolean handleKeycode(int keycode, boolean notificationButton) {
        Log.d(TAG, "Handling keycode: " + keycode);
        final PlaybackServiceMediaPlayer.PSMPInfo info = mediaPlayer.getPSMPInfo();
        final PlayerStatus status = info.playerStatus;
        switch (keycode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(!UserPreferences.isPersistNotify(), true);
                } else if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (status == PlayerStatus.PREPARING) {
                    mediaPlayer.setStartWhenPrepared(!mediaPlayer.isStartWhenPrepared());
                } else if (status == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                } else if (mediaPlayer.getPlayable() == null) {
                    startPlayingFromPreferences();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (status == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                } else if (mediaPlayer.getPlayable() == null) {
                    startPlayingFromPreferences();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(!UserPreferences.isPersistNotify(), true);
                }

                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if (notificationButton ||
                        UserPreferences.shouldHardwareButtonSkip()) {
                    // assume the skip command comes from a notification or the lockscreen
                    // a >| skip button should actually skip
                    mediaPlayer.skip();
                } else {
                    // assume skip command comes from a (bluetooth) media button
                    // user actually wants to fast-forward
                    seekDelta(UserPreferences.getFastForwardSecs() * 1000);
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                mediaPlayer.seekDelta(UserPreferences.getFastForwardSecs() * 1000);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if (UserPreferences.shouldHardwarePreviousButtonRestart()) {
                    // user wants to restart current episode
                    mediaPlayer.seekTo(0);
                } else {
                    //  user wants to rewind current episode
                    mediaPlayer.seekDelta(-UserPreferences.getRewindSecs() * 1000);
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                mediaPlayer.seekDelta(-UserPreferences.getRewindSecs() * 1000);
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(true, true);
                    started = false;
                }

                stopForeground(true); // gets rid of persistent notification
                return true;
            default:
                Log.d(TAG, "Unhandled key code: " + keycode);
                if (info.playable != null && info.playerStatus == PlayerStatus.PLAYING) {   // only notify the user about an unknown key event if it is actually doing something
                    String message = String.format(getResources().getString(R.string.unknown_media_key), keycode);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
        }
        return false;
    }

    private void startPlayingFromPreferences() {
        Playable playable = Playable.PlayableUtils.createInstanceFromPreferences(getApplicationContext());
        if (playable != null) {
            mediaPlayer.playMediaObject(playable, false, true, true);
            started = true;
            PlaybackService.this.updateMediaSessionMetadata(playable);
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

    public void notifyVideoSurfaceAbandoned() {
        mediaPlayer.pause(true, false);
        mediaPlayer.resetVideoSurface();
        setupNotification(getPlayable());
        stopForeground(!UserPreferences.isPersistNotify());
    }

    private final PlaybackServiceTaskManager.PSTMCallback taskManagerCallback = new PlaybackServiceTaskManager.PSTMCallback() {
        @Override
        public void positionSaverTick() {
            saveCurrentPosition(true, null, PlaybackServiceMediaPlayer.INVALID_TIME);
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
            PlayerWidgetJobService.updateWidget(getBaseContext());
        }

        @Override
        public void onChapterLoaded(Playable media) {
            sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
        }
    };

    private final PlaybackServiceMediaPlayer.PSMPCallback mediaPlayerCallback = new PlaybackServiceMediaPlayer.PSMPCallback() {
        @Override
        public void statusChanged(PlaybackServiceMediaPlayer.PSMPInfo newInfo) {
            if (mediaPlayer != null) {
                currentMediaType = mediaPlayer.getCurrentMediaType();
            } else {
                currentMediaType = MediaType.UNKNOWN;
            }

            updateMediaSession(newInfo.playerStatus);
            switch (newInfo.playerStatus) {
                case INITIALIZED:
                    writePlaybackPreferences();
                    break;

                case PREPARED:
                    taskManager.startChapterLoader(newInfo.playable);
                    break;

                case PAUSED:
                    if ((UserPreferences.isPersistNotify() || isCasting) &&
                            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        // do not remove notification on pause based on user pref and whether android version supports expanded notifications
                        // Change [Play] button to [Pause]
                        setupNotification(newInfo);
                    } else if (!UserPreferences.isPersistNotify() && !isCasting) {
                        // remove notification on pause
                        stopForeground(true);
                    }
                    writePlayerStatusPlaybackPreferences();
                    break;

                case STOPPED:
                    //writePlaybackPreferencesNoMediaPlaying();
                    //stopService();
                    break;

                case PLAYING:
                    writePlayerStatusPlaybackPreferences();
                    setupNotification(newInfo);
                    started = true;
                    // set sleep timer if auto-enabled
                    if (newInfo.oldPlayerStatus != null && newInfo.oldPlayerStatus != PlayerStatus.SEEKING &&
                            SleepTimerPreferences.autoEnable() && !sleepTimerActive()) {
                        setSleepTimer(SleepTimerPreferences.timerMillis(), SleepTimerPreferences.shakeToReset(),
                                SleepTimerPreferences.vibrate());
                    }
                    break;

                case ERROR:
                    writePlaybackPreferencesNoMediaPlaying();
                    stopService();
                    break;

            }

            IntentUtils.sendLocalBroadcast(getApplicationContext(), ACTION_PLAYER_STATUS_CHANGED);
            PlayerWidgetJobService.updateWidget(getBaseContext());
            bluetoothNotifyChange(newInfo, AVRCP_ACTION_PLAYER_STATUS_CHANGED);
            bluetoothNotifyChange(newInfo, AVRCP_ACTION_META_CHANGED);
        }

        @Override
        public void shouldStop() {
            stopService();
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
        public void onMediaChanged(boolean reloadUI) {
            Log.d(TAG, "reloadUI callback reached");
            if (reloadUI) {
                sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
            }
            PlaybackService.this.updateMediaSessionMetadata(getPlayable());
        }

        @Override
        public boolean onMediaPlayerInfo(int code, @StringRes int resourceId) {
            switch (code) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_START, 0);
                    return true;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_END, 0);
                    return true;
                default:
                    return flavorHelper.onMediaPlayerInfo(PlaybackService.this, code, resourceId);
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
            stopService();
            return true;
        }

        @Override
        public void onPostPlayback(@NonNull Playable media, boolean ended, boolean skipped,
                                   boolean playingNext) {
            PlaybackService.this.onPostPlayback(media, ended, skipped, playingNext);
        }

        @Override
        public void onPlaybackStart(@NonNull Playable playable, int position) {
            taskManager.startWidgetUpdater();
            if (position != PlaybackServiceMediaPlayer.INVALID_TIME) {
                playable.setPosition(position);
            }
            playable.onPlaybackStart();
            taskManager.startPositionSaver();
        }

        @Override
        public void onPlaybackPause(Playable playable, int position) {
            taskManager.cancelPositionSaver();
            saveCurrentPosition(position == PlaybackServiceMediaPlayer.INVALID_TIME || playable == null,
                    playable, position);
            taskManager.cancelWidgetUpdater();
            if (playable != null) {
                playable.onPlaybackPause(getApplicationContext());
            }
        }

        @Override
        public Playable getNextInQueue(Playable currentMedia) {
            return PlaybackService.this.getNextInQueue(currentMedia);
        }

        @Override
        public void onPlaybackEnded(MediaType mediaType, boolean stopPlaying) {
            PlaybackService.this.onPlaybackEnded(mediaType, stopPlaying);
        }
    };

    private Playable getNextInQueue(final Playable currentMedia) {
        if (!(currentMedia instanceof FeedMedia)) {
            Log.d(TAG, "getNextInQueue(), but playable not an instance of FeedMedia, so not proceeding");
            return null;
        }
        if (!ClientConfig.playbackServiceCallbacks.useQueue()) {
            Log.d(TAG, "getNextInQueue(), but queue not in use by this app");
            return null;
        }
        Log.d(TAG, "getNextInQueue()");
        FeedMedia media = (FeedMedia) currentMedia;
        try {
            media.loadMetadata();
        } catch (Playable.PlayableException e) {
            Log.e(TAG, "Unable to load metadata to get next in queue", e);
            return null;
        }
        FeedItem item = media.getItem();
        if (item == null) {
            Log.w(TAG, "getNextInQueue() with FeedMedia object whose FeedItem is null");
            return null;
        }
        FeedItem nextItem;
        try {
            final List<FeedItem> queue = taskManager.getQueue();
            nextItem = DBTasks.getQueueSuccessorOfItem(item.getId(), queue);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error handling the queue in order to retrieve the next item", e);
            return null;
        }
        return (nextItem != null) ? nextItem.getMedia() : null;

    }

    /**
     * Set of instructions to be performed when playback ends.
     */
    private void onPlaybackEnded(MediaType mediaType, boolean stopPlaying) {
        Log.d(TAG, "Playback ended");
        if (stopPlaying) {
            taskManager.cancelPositionSaver();
            writePlaybackPreferencesNoMediaPlaying();
            if (!isCasting) {
                stopForeground(true);
            }
        }
        if (mediaType == null) {
            sendNotificationBroadcast(NOTIFICATION_TYPE_PLAYBACK_END, 0);
        } else {
            sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD,
                    isCasting ? EXTRA_CODE_CAST :
                            (mediaType == MediaType.VIDEO) ? EXTRA_CODE_VIDEO : EXTRA_CODE_AUDIO);
        }
    }

    /**
     * This method processes the media object after its playback ended, either because it completed
     * or because a different media object was selected for playback.
     * <p>
     * Even though these tasks aren't supposed to be resource intensive, a good practice is to
     * usually call this method on a background thread.
     *
     * @param playable    the media object that was playing. It is assumed that its position
     *                    property was updated before this method was called.
     * @param ended       if true, it signals that {@param playable} was played until its end.
     *                    In such case, the position property of the media becomes irrelevant for
     *                    most of the tasks (although it's still a good practice to keep it
     *                    accurate).
     * @param skipped     if the user pressed a skip >| button.
     * @param playingNext if true, it means another media object is being loaded in place of this
     *                    one.
     *                    Instances when we'd set it to false would be when we're not following the
     *                    queue or when the queue has ended.
     */
    private void onPostPlayback(final Playable playable, boolean ended, boolean skipped,
                                boolean playingNext) {
        if (playable == null) {
            Log.e(TAG, "Cannot do post-playback processing: media was null");
            return;
        }
        Log.d(TAG, "onPostPlayback(): media=" + playable.getEpisodeTitle());

        if (!(playable instanceof FeedMedia)) {
            Log.d(TAG, "Not doing post-playback processing: media not of type FeedMedia");
            if (ended) {
                playable.onPlaybackCompleted(getApplicationContext());
            } else {
                playable.onPlaybackPause(getApplicationContext());
            }
            return;
        }
        FeedMedia media = (FeedMedia) playable;
        FeedItem item = media.getItem();
        boolean smartMarkAsPlayed = playingNext && media.hasAlmostEnded();
        if (!ended && smartMarkAsPlayed) {
            Log.d(TAG, "smart mark as played");
        }

        if (ended || smartMarkAsPlayed) {
            media.onPlaybackCompleted(getApplicationContext());
        } else {
            media.onPlaybackPause(getApplicationContext());
        }

        if (item != null) {
            if (ended || smartMarkAsPlayed ||
                    (skipped && !UserPreferences.shouldSkipKeepEpisode())) {
                // only mark the item as played if we're not keeping it anyways
                DBWriter.markItemPlayed(item, FeedItem.PLAYED, ended);
                try {
                    final List<FeedItem> queue = taskManager.getQueue();
                    if (QueueAccess.ItemListAccess(queue).contains(item.getId())) {
                        // don't know if it actually matters to not autodownload when smart mark as played is triggered
                        DBWriter.removeQueueItem(PlaybackService.this, item, ended);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // isInQueue remains false
                }
                // Delete episode if enabled
                if (item.getFeed().getPreferences().getCurrentAutoDelete() &&
                        (!item.isTagged(FeedItem.TAG_FAVORITE) || !UserPreferences.shouldFavoriteKeepEpisode())) {
                    DBWriter.deleteFeedMediaOfItem(PlaybackService.this, media.getId());
                    Log.d(TAG, "Episode Deleted");
                }
            }
        }

        if (ended || skipped || playingNext) {
            DBWriter.addItemToPlaybackHistory(media);
        }
    }

    public void setSleepTimer(long waitingTime, boolean shakeToReset, boolean vibrate) {
        Log.d(TAG, "Setting sleep timer to " + Long.toString(waitingTime) + " milliseconds");
        taskManager.setSleepTimer(waitingTime, shakeToReset, vibrate);
        sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
        EventBus.getDefault().post(new MessageEvent(getString(R.string.sleep_timer_enabled_label),
                this::disableSleepTimer));
    }

    public void disableSleepTimer() {
        taskManager.disableSleepTimer();
        sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
        EventBus.getDefault().post(new MessageEvent(getString(R.string.sleep_timer_disabled_label)));
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

    private void sendNotificationBroadcast(int type, int code) {
        Intent intent = new Intent(ACTION_PLAYER_NOTIFICATION);
        intent.putExtra(EXTRA_NOTIFICATION_TYPE, type);
        intent.putExtra(EXTRA_NOTIFICATION_CODE, code);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    /**
     * Updates the Media Session for the corresponding status.
     *
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
        sessionState.setState(state, getCurrentPosition(), getCurrentPlaybackSpeed());
        long capabilities = PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_FAST_FORWARD
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;

        if (useSkipToPreviousForRewindInLockscreen()) {
            // Workaround to fool Android so that Lockscreen will expose a skip-to-previous button,
            // which will be used for rewind.
            // The workaround is used for pre Lollipop (Androidv5) devices.
            // For Androidv5+, lockscreen widges are really notifications (compact),
            // with an independent codepath
            //
            // @see #sessionCallback in the backing callback, skipToPrevious implementation
            //   is actually the same as rewind. So no new inconsistency is created.
            // @see #setupNotification() for the method to create Androidv5+ lockscreen UI
            //   with notification (compact)
            capabilities = capabilities | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }

        sessionState.setActions(capabilities);

        flavorHelper.sessionStateAddActionForWear(sessionState,
                CUSTOM_ACTION_REWIND, getString(R.string.rewind_label), android.R.drawable.ic_media_rew);
        flavorHelper.sessionStateAddActionForWear(sessionState,
                CUSTOM_ACTION_FAST_FORWARD, getString(R.string.fast_forward_label), android.R.drawable.ic_media_ff);

        flavorHelper.mediaSessionSetExtraForWear(mediaSession);

        mediaSession.setPlaybackState(sessionState.build());
    }

    private static boolean useSkipToPreviousForRewindInLockscreen() {
        // showRewindOnCompactNotification() corresponds to the "Set Lockscreen Buttons"
        // Settings in UI.
        // Hence, from user perspective, he/she is setting the buttons for Lockscreen
        return (UserPreferences.showRewindOnCompactNotification() &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP));
    }

    /**
     * Used by updateMediaSessionMetadata to load notification data in another thread.
     */
    private Thread mediaSessionSetupThread;

    private void updateMediaSessionMetadata(final Playable p) {
        if (p == null || mediaSession == null) {
            return;
        }
        if (mediaSessionSetupThread != null) {
            mediaSessionSetupThread.interrupt();
        }

        Runnable mediaSessionSetupTask = () -> {
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, p.getFeedTitle());
            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, p.getEpisodeTitle());
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, p.getFeedTitle());
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, p.getDuration());
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, p.getEpisodeTitle());
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, p.getFeedTitle());

            String imageLocation = p.getImageLocation();

            if (!TextUtils.isEmpty(imageLocation)) {
                if (UserPreferences.setLockscreenBackground()) {
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, imageLocation);
                    try {
                        Bitmap art = Glide.with(this)
                                .asBitmap()
                                .load(imageLocation)
                                .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .get();
                        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art);
                    } catch (Throwable tr) {
                        Log.e(TAG, Log.getStackTraceString(tr));
                    }
                } else if (isCasting) {
                    // In the absence of metadata art, the controller dialog takes care of creating it.
                    builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, imageLocation);
                }
            }
            if (!Thread.currentThread().isInterrupted() && started) {
                mediaSession.setSessionActivity(PendingIntent.getActivity(this, 0,
                        PlaybackService.getPlayerActivityIntent(this),
                        PendingIntent.FLAG_UPDATE_CURRENT));
                try {
                    mediaSession.setMetadata(builder.build());
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Setting media session metadata", e);
                    builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null);
                    mediaSession.setMetadata(builder.build());
                }
            }
        };

        mediaSessionSetupThread = new Thread(mediaSessionSetupTask);
        mediaSessionSetupThread.start();
    }

    /**
     * Used by setupNotification to load notification data in another thread.
     */
    private Thread notificationSetupThread;

    /**
     * Prepares notification and starts the service in the foreground.
     */
    private void setupNotification(final PlaybackServiceMediaPlayer.PSMPInfo info) {
        setupNotification(info.playable);
    }

    private synchronized void setupNotification(final Playable playable) {
        if (notificationSetupThread != null) {
            notificationSetupThread.interrupt();
        }
        if (playable == null) {
            Log.d(TAG, "setupNotification: playable is null");
            if (!started) {
                stopService();
            }
            return;
        }
        Runnable notificationSetupTask = new Runnable() {
            Bitmap icon = null;

            @Override
            public void run() {
                Log.d(TAG, "Starting background work");

                if (mediaPlayer == null) {
                    Log.d(TAG, "notificationSetupTask: mediaPlayer is null");
                    if (!started) {
                        stopService();
                    }
                    return;
                }

                int iconSize = getResources().getDimensionPixelSize(
                        android.R.dimen.notification_large_icon_width);
                try {
                    icon = Glide.with(PlaybackService.this)
                            .asBitmap()
                            .load(playable.getImageLocation())
                            .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                            .apply(new RequestOptions().centerCrop())
                            .submit(iconSize, iconSize)
                            .get();
                } catch (Throwable tr) {
                    Log.e(TAG, "Error loading the media icon for the notification", tr);
                }

                if (icon == null) {
                    icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                            ClientConfig.playbackServiceCallbacks.getNotificationIconResource(getApplicationContext()));
                }

                PlayerStatus playerStatus = mediaPlayer.getPlayerStatus();

                if (!Thread.currentThread().isInterrupted() && started) {
                    String contentText = playable.getEpisodeTitle();
                    String contentTitle = playable.getFeedTitle();
                    Notification notification;

                    // Builder is v7, even if some not overwritten methods return its parent's v4 interface
                    NotificationCompat.Builder notificationBuilder = createBasicNotification();
                    notificationBuilder.setContentTitle(contentTitle)
                            .setContentText(contentText)
                            .setPriority(UserPreferences.getNotifyPriority())
                            .setLargeIcon(icon); // set notification priority
                    IntList compactActionList = new IntList();

                    int numActions = 0; // we start and 0 and then increment by 1 for each call to addAction

                    if (isCasting) {
                        Intent stopCastingIntent = new Intent(PlaybackService.this, PlaybackService.class);
                        stopCastingIntent.putExtra(EXTRA_CAST_DISCONNECT, true);
                        PendingIntent stopCastingPendingIntent = PendingIntent.getService(PlaybackService.this,
                                numActions, stopCastingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        notificationBuilder.addAction(R.drawable.ic_media_cast_disconnect,
                                getString(R.string.cast_disconnect_label),
                                stopCastingPendingIntent);
                        numActions++;
                    }

                    // always let them rewind
                    PendingIntent rewindButtonPendingIntent = getPendingIntentForMediaAction(
                            KeyEvent.KEYCODE_MEDIA_REWIND, numActions);
                    notificationBuilder.addAction(android.R.drawable.ic_media_rew,
                            getString(R.string.rewind_label),
                            rewindButtonPendingIntent);
                    if (UserPreferences.showRewindOnCompactNotification()) {
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
                    if (UserPreferences.showFastForwardOnCompactNotification()) {
                        compactActionList.add(numActions);
                    }
                    numActions++;

                    if (UserPreferences.isFollowQueue()) {
                        PendingIntent skipButtonPendingIntent = getPendingIntentForMediaAction(
                                KeyEvent.KEYCODE_MEDIA_NEXT, numActions);
                        notificationBuilder.addAction(android.R.drawable.ic_media_next,
                                getString(R.string.skip_episode_label),
                                skipButtonPendingIntent);
                        if (UserPreferences.showSkipOnCompactNotification()) {
                            compactActionList.add(numActions);
                        }
                        numActions++;
                    }

                    PendingIntent stopButtonPendingIntent = getPendingIntentForMediaAction(
                            KeyEvent.KEYCODE_MEDIA_STOP, numActions);
                    notificationBuilder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(compactActionList.toArray())
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(stopButtonPendingIntent))
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setColor(NotificationCompat.COLOR_DEFAULT);

                    notification = notificationBuilder.build();

                    if (playerStatus == PlayerStatus.PLAYING ||
                            playerStatus == PlayerStatus.PREPARING ||
                            playerStatus == PlayerStatus.SEEKING ||
                            isCasting) {
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
     * @param fromMediaPlayer if true, the information is gathered from the current Media Player
     *                        and {@param playable} and {@param position} become irrelevant.
     * @param playable        the playable for which the current position should be saved, unless
     *                        {@param fromMediaPlayer} is true.
     * @param position        the position that should be saved, unless {@param fromMediaPlayer} is true.
     */
    private synchronized void saveCurrentPosition(boolean fromMediaPlayer, Playable playable, int position) {
        int duration;
        if (fromMediaPlayer) {
            position = getCurrentPosition();
            duration = getDuration();
            playable = mediaPlayer.getPlayable();
        } else {
            duration = playable.getDuration();
        }
        if (position != INVALID_TIME && duration != INVALID_TIME && playable != null) {
            Log.d(TAG, "Saving current position to " + position);
            playable.saveCurrentPosition(
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()),
                    position,
                    System.currentTimeMillis());
        }
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
            i.putExtra("id", 1L);
            i.putExtra("artist", "");
            i.putExtra("album", info.playable.getFeedTitle());
            i.putExtra("track", info.playable.getEpisodeTitle());
            i.putExtra("playing", isPlaying);
            final List<FeedItem> queue = taskManager.getQueueIfLoaded();
            if (queue != null) {
                i.putExtra("ListSize", queue.size());
            }
            i.putExtra("duration", (long) info.playable.getDuration());
            i.putExtra("position", (long) info.playable.getPosition());
            sendBroadcast(i);
        }
    }

    private final BroadcastReceiver autoStateUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("media_connection_status");
            boolean isConnectedToCar = "media_connected".equals(status);
            Log.d(TAG, "Received Auto Connection update: " + status);
            if (!isConnectedToCar) {
                Log.d(TAG, "Car was unplugged during playback.");
                pauseIfPauseOnDisconnect();
            } else {
                PlayerStatus playerStatus = mediaPlayer.getPlayerStatus();
                if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (playerStatus == PlayerStatus.PREPARING) {
                    mediaPlayer.setStartWhenPrepared(!mediaPlayer.isStartWhenPrepared());
                } else if (playerStatus == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                }
            }
        }
    };

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
            if (isInitialStickyBroadcast ()) {
                // Don't pause playback after we just started, just because the receiver
                // delivers the current headset state (instead of a change)
                return;
            }

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
            if (TextUtils.equals(intent.getAction(), BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
                if (state == BluetoothA2dp.STATE_CONNECTED) {
                    Log.d(TAG, "Received bluetooth connection intent");
                    unpauseIfPauseOnDisconnect(true);
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
        if (UserPreferences.isPauseOnHeadsetDisconnect() && !isCasting()) {
            if (mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING) {
                transientPause = true;
            }
            mediaPlayer.pause(!UserPreferences.isPersistNotify(), true);
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
            } else if (bluetooth && UserPreferences.isUnpauseOnBluetoothReconnect()) {
                // let the user know we've started playback again...
                Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
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
                stopService();
            }
        }

    };

    private final BroadcastReceiver skipCurrentEpisodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ACTION_SKIP_CURRENT_EPISODE)) {
                Log.d(TAG, "Received SKIP_CURRENT_EPISODE intent");
                mediaPlayer.skip();
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

    public static boolean isCasting() {
        return isCasting;
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

    public Playable getPlayable() {
        return mediaPlayer.getPlayable();
    }

    public boolean canSetSpeed() {
        return mediaPlayer.canSetSpeed();
    }

    public void setSpeed(float speed) {
        mediaPlayer.setPlaybackParams(speed, UserPreferences.isSkipSilence());
    }

    public void skipSilence(boolean skipSilence) {
        mediaPlayer.setPlaybackParams(getCurrentPlaybackSpeed(), skipSilence);
    }

    public void setVolume(float leftVolume, float rightVolume) {
        mediaPlayer.setVolume(leftVolume, rightVolume);
    }

    public float getCurrentPlaybackSpeed() {
        if(mediaPlayer == null) {
            return 1.0f;
        }
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
        mediaPlayer.seekTo(t);
    }


    private void seekDelta(final int d) {
        mediaPlayer.seekDelta(d);
    }

    /**
     * Seek to the start of the specified chapter.
     */
    public void seekToChapter(Chapter c) {
        seekTo((int) c.getStart());
    }

    /**
     * call getDuration() on mediaplayer or return INVALID_TIME if player is in
     * an invalid state.
     */
    public int getDuration() {
        if (mediaPlayer == null) {
            return INVALID_TIME;
        }
        return mediaPlayer.getDuration();
    }

    /**
     * call getCurrentPosition() on mediaplayer or return INVALID_TIME if player
     * is in an invalid state.
     */
    public int getCurrentPosition() {
        if (mediaPlayer == null) {
            return INVALID_TIME;
        }
        return mediaPlayer.getPosition();
    }

    public boolean isStreaming() {
        return mediaPlayer.isStreaming();
    }

    public Pair<Integer, Integer> getVideoSize() {
        return mediaPlayer.getVideoSize();
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
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId: mediaId: " + mediaId + " extras: " + extras.toString());
            FeedMedia p = DBReader.getFeedMedia(Long.parseLong(mediaId));
            if (p != null) {
                mediaPlayer.playMediaObject(p, !p.localFileAvailable(), true, true);
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Log.d(TAG, "onPlayFromSearch  query=" + query + " extras=" + extras.toString());

            List<SearchResult> results = FeedSearcher.performSearch(getBaseContext(), query, 0);
            for (SearchResult result : results) {
                try {
                    FeedMedia p = ((FeedItem) (result.getComponent())).getMedia();
                    mediaPlayer.playMediaObject(p, !p.localFileAvailable(), true, true);
                    return;
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                    e.printStackTrace();
                    continue;
                }
            }
            onPlay();
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause()");
            if (getStatus() == PlayerStatus.PLAYING) {
                pause(!UserPreferences.isPersistNotify(), true);
            }
        }

        @Override
        public void onStop() {
            Log.d(TAG, "onStop()");
            mediaPlayer.stopPlayback(true);
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
            seekDelta(UserPreferences.getFastForwardSecs() * 1000);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext()");
            if (UserPreferences.shouldHardwareButtonSkip()) {
                mediaPlayer.skip();
            } else {
                seekDelta(UserPreferences.getFastForwardSecs() * 1000);
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
                KeyEvent keyEvent = mediaButton.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null &&
                        keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getRepeatCount() == 0) {
                    return handleKeycode(keyEvent.getKeyCode(), false);
                }
            }
            return false;
        }

        @Override
        public void onCustomAction(String action, Bundle extra) {
            Log.d(TAG, "onCustomAction(" + action + ")");
            if (CUSTOM_ACTION_FAST_FORWARD.equals(action)) {
                onFastForward();
            } else if (CUSTOM_ACTION_REWIND.equals(action)) {
                onRewind();
            }
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            (sharedPreferences, key) -> {
                if (UserPreferences.PREF_LOCKSCREEN_BACKGROUND.equals(key)) {
                    updateMediaSessionMetadata(getPlayable());
                } else {
                    flavorHelper.onSharedPreference(key);
                }
            };

    interface FlavorHelperCallback {
        PlaybackServiceMediaPlayer.PSMPCallback getMediaPlayerCallback();

        void setMediaPlayer(PlaybackServiceMediaPlayer mediaPlayer);

        PlaybackServiceMediaPlayer getMediaPlayer();

        void setIsCasting(boolean isCasting);

        void sendNotificationBroadcast(int type, int code);

        void saveCurrentPosition(boolean fromMediaPlayer, Playable playable, int position);

        void setupNotification(boolean connected, PlaybackServiceMediaPlayer.PSMPInfo info);

        MediaSessionCompat getMediaSession();

        Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter);

        void unregisterReceiver(BroadcastReceiver receiver);
    }

    private final FlavorHelperCallback flavorHelperCallback = new FlavorHelperCallback() {
        @Override
        public PlaybackServiceMediaPlayer.PSMPCallback getMediaPlayerCallback() {
            return PlaybackService.this.mediaPlayerCallback;
        }

        @Override
        public void setMediaPlayer(PlaybackServiceMediaPlayer mediaPlayer) {
            PlaybackService.this.mediaPlayer = mediaPlayer;
        }

        @Override
        public PlaybackServiceMediaPlayer getMediaPlayer() {
            return PlaybackService.this.mediaPlayer;
        }

        @Override
        public void setIsCasting(boolean isCasting) {
            PlaybackService.isCasting = isCasting;
        }

        @Override
        public void sendNotificationBroadcast(int type, int code) {
            PlaybackService.this.sendNotificationBroadcast(type, code);
        }

        @Override
        public void saveCurrentPosition(boolean fromMediaPlayer, Playable playable, int position) {
            PlaybackService.this.saveCurrentPosition(fromMediaPlayer, playable, position);
        }

        @Override
        public void setupNotification(boolean connected, PlaybackServiceMediaPlayer.PSMPInfo info) {
            if (connected) {
                PlaybackService.this.setupNotification(info);
            } else {
                PlayerStatus status = info.playerStatus;
                if ((status == PlayerStatus.PLAYING ||
                        status == PlayerStatus.SEEKING ||
                        status == PlayerStatus.PREPARING ||
                        UserPreferences.isPersistNotify()) &&
                        android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    PlaybackService.this.setupNotification(info);
                } else if (!UserPreferences.isPersistNotify()) {
                    PlaybackService.this.stopForeground(true);
                }
            }
        }

        @Override
        public MediaSessionCompat getMediaSession() {
            return PlaybackService.this.mediaSession;
        }

        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
            return PlaybackService.this.registerReceiver(receiver, filter);
        }

        @Override
        public void unregisterReceiver(BroadcastReceiver receiver) {
            PlaybackService.this.unregisterReceiver(receiver);
        }
    };
}

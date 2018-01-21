package de.danoeh.antennapod.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RemoteViews;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.receiver.PlayerWidget;

/**
 * Updates the state of the player widget
 */
public class PlayerWidgetService extends Service {
    private static final String TAG = "PlayerWidgetService";

    private PlaybackService playbackService;

    /**
     * Controls write access to playbackservice reference
     */
    private final Object psLock = new Object();

    /**
     * True while service is updating the widget
     */
    private volatile boolean isUpdating;

    public PlayerWidgetService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        isUpdating = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service is about to be destroyed");
        if (playbackService != null) {
            Playable playable = playbackService.getPlayable();
            if (playable != null && playable instanceof FeedMedia) {
                FeedMedia media = (FeedMedia) playable;
                if (media.hasAlmostEnded()) {
                    Log.d(TAG, "smart mark as read");
                    FeedItem item = media.getItem();
                    DBWriter.markItemPlayed(item, FeedItem.PLAYED, false);
                    DBWriter.removeQueueItem(this, item, false);
                    DBWriter.addItemToPlaybackHistory(media);
                    if (item.getFeed().getPreferences().getCurrentAutoDelete() &&
                            (!item.isTagged(FeedItem.TAG_FAVORITE) || !UserPreferences.shouldFavoriteKeepEpisode())) {
                        Log.d(TAG, "Delete " + media.toString());
                        DBWriter.deleteFeedMediaOfItem(this, media.getId());
                    }
                }
            }
        }

        try {
            unbindService(mConnection);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "IllegalArgumentException when trying to unbind service");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isUpdating) {
            if (playbackService == null && PlaybackService.isRunning) {
                bindService(new Intent(this, PlaybackService.class),
                        mConnection, 0);
            } else {
                startViewUpdaterIfNotRunning();
            }
        } else {
            Log.d(TAG, "Service was called while updating. Ignoring update request");
        }
        return Service.START_NOT_STICKY;
    }

    private void updateViews() {
        isUpdating = true;

        ComponentName playerWidget = new ComponentName(this, PlayerWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(),
                R.layout.player_widget);
        PendingIntent startMediaplayer = PendingIntent.getActivity(this, 0,
                PlaybackService.getPlayerActivityIntent(this), 0);

        Intent startApp = new Intent(getBaseContext(), MainActivity.class);
        startApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startApp.putExtra(MainActivity.EXTRA_FRAGMENT_TAG, QueueFragment.TAG);
        PendingIntent startAppPending = PendingIntent.getActivity(getBaseContext(), 0, startApp, PendingIntent.FLAG_UPDATE_CURRENT);

        boolean nothingPlaying = false;
        if (playbackService != null) {
            final Playable media = playbackService.getPlayable();
            if (media != null) {
                PlayerStatus status = playbackService.getStatus();
                views.setOnClickPendingIntent(R.id.layout_left, startMediaplayer);

                views.setTextViewText(R.id.txtvTitle, media.getEpisodeTitle());

                String progressString = getProgressString();
                if (progressString != null) {
                    views.setViewVisibility(R.id.txtvProgress, View.VISIBLE);
                    views.setTextViewText(R.id.txtvProgress, progressString);
                }

                if (status == PlayerStatus.PLAYING) {
                    views.setImageViewResource(R.id.butPlay, R.drawable.ic_pause_white_24dp);
                    if (Build.VERSION.SDK_INT >= 15) {
                        views.setContentDescription(R.id.butPlay, getString(R.string.pause_label));
                    }
                } else {
                    views.setImageViewResource(R.id.butPlay, R.drawable.ic_play_arrow_white_24dp);
                    if (Build.VERSION.SDK_INT >= 15) {
                        views.setContentDescription(R.id.butPlay, getString(R.string.play_label));
                    }
                }
                views.setOnClickPendingIntent(R.id.butPlay,
                        createMediaButtonIntent());
            } else {
                nothingPlaying = true;
            }
        } else {
            nothingPlaying = true;
        }

        if (nothingPlaying) {
            // start the app if they click anything
            views.setOnClickPendingIntent(R.id.layout_left, startAppPending);
            views.setOnClickPendingIntent(R.id.butPlay, startAppPending);
            views.setViewVisibility(R.id.txtvProgress, View.INVISIBLE);
            views.setTextViewText(R.id.txtvTitle,
                    this.getString(R.string.no_media_playing_label));
            views.setImageViewResource(R.id.butPlay, R.drawable.ic_play_arrow_white_24dp);
        }

        manager.updateAppWidget(playerWidget, views);
        isUpdating = false;
    }

    /**
     * Creates an intent which fakes a mediabutton press
     */
    private PendingIntent createMediaButtonIntent() {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        Intent startingIntent = new Intent(
                MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);

        return PendingIntent.getBroadcast(this, 0, startingIntent, 0);
    }

    private String getProgressString() {
        int position = playbackService.getCurrentPosition();
        int duration = playbackService.getDuration();
        if (position > 0 && duration > 0) {
            return Converter.getDurationStringLong(position) + " / "
                    + Converter.getDurationStringLong(duration);
        } else {
            return null;
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connection to service established");
            synchronized (psLock) {
                if(service instanceof PlaybackService.LocalBinder) {
                    playbackService = ((PlaybackService.LocalBinder) service).getService();
                    startViewUpdaterIfNotRunning();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (psLock) {
                playbackService = null;
                Log.d(TAG, "Disconnected from service");
            }
        }

    };

    private void startViewUpdaterIfNotRunning() {
        if (!isUpdating) {
            ViewUpdater updateThread = new ViewUpdater(this);
            updateThread.start();
        }
    }

    class ViewUpdater extends Thread {
        private static final String THREAD_NAME = "ViewUpdater";
        private final PlayerWidgetService service;

        public ViewUpdater(PlayerWidgetService service) {
            super();
            setName(THREAD_NAME);
            this.service = service;

        }

        @Override
        public void run() {
            synchronized (psLock) {
                service.updateViews();
            }
        }

    }

}

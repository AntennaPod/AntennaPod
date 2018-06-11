package de.danoeh.antennapod.core.service;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RemoteViews;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.receiver.PlayerWidget;

/**
 * Updates the state of the player widget
 */
public class PlayerWidgetJobService extends JobIntentService {
    private static final String TAG = "PlayerWidgetJobService";

    private PlaybackService playbackService;
    private final Object waitForService = new Object();

    public static void updateWidget(Context context) {
        enqueueWork(context, PlayerWidgetJobService.class, 0, new Intent(context, PlayerWidgetJobService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (!PlayerWidget.isEnabled(getApplicationContext())) {
            return;
        }

        if (PlaybackService.isRunning && playbackService == null) {
            synchronized (waitForService) {
                bindService(new Intent(this, PlaybackService.class), mConnection, 0);
                while (playbackService == null) {
                    try {
                        waitForService.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }

        updateViews();

        if (playbackService != null) {
            try {
                unbindService(mConnection);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "IllegalArgumentException when trying to unbind service");
            }
        }
    }

    private void updateViews() {

        ComponentName playerWidget = new ComponentName(this, PlayerWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.player_widget);
        PendingIntent startMediaplayer = PendingIntent.getActivity(this, 0,
                PlaybackService.getPlayerActivityIntent(this), 0);

        final PendingIntent startAppPending = PendingIntent.getActivity(this, 0,
                PlaybackService.getPlayerActivityIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT);

        boolean nothingPlaying = false;
        Playable media;
        PlayerStatus status;
        if (playbackService != null) {
            media = playbackService.getPlayable();
            status = playbackService.getStatus();
        } else {
            media = Playable.PlayableUtils.createInstanceFromPreferences(getApplicationContext());
            status = PlayerStatus.STOPPED;
        }

        if (media != null) {
            views.setOnClickPendingIntent(R.id.layout_left, startMediaplayer);

            views.setTextViewText(R.id.txtvTitle, media.getEpisodeTitle());

            String progressString;
            if (playbackService != null) {
                progressString = getProgressString(playbackService.getCurrentPosition(), playbackService.getDuration());
            } else {
                progressString = getProgressString(media.getPosition(), media.getDuration());
            }

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
            views.setOnClickPendingIntent(R.id.butPlay, createMediaButtonIntent());
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
    }

    /**
     * Creates an intent which fakes a mediabutton press
     */
    private PendingIntent createMediaButtonIntent() {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        Intent startingIntent = new Intent(getBaseContext(), MediaButtonReceiver.class);
        startingIntent.setAction(MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);

        return PendingIntent.getBroadcast(this, 0, startingIntent, 0);
    }

    private String getProgressString(int position, int duration) {
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
            if (service instanceof PlaybackService.LocalBinder) {
                synchronized (waitForService) {
                    playbackService = ((PlaybackService.LocalBinder) service).getService();
                    waitForService.notifyAll();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            Log.d(TAG, "Disconnected from service");
        }

    };
}

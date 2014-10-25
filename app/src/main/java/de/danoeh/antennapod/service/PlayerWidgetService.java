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
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.receiver.PlayerWidget;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.Playable;

/** Updates the state of the player widget */
public class PlayerWidgetService extends Service {
	private static final String TAG = "PlayerWidgetService";

	private PlaybackService playbackService;

    /** Controls write access to playbackservice reference */
    private Object psLock;

	/** True while service is updating the widget */
	private volatile boolean isUpdating;

	public PlayerWidgetService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Service created");
		isUpdating = false;
        psLock = new Object();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Service is about to be destroyed");
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
			if (BuildConfig.DEBUG)
				Log.d(TAG,
						"Service was called while updating. Ignoring update request");
		}
		return Service.START_NOT_STICKY;
	}

	private void updateViews() {
        if (playbackService == null) {
            return;
        }
		isUpdating = true;

		ComponentName playerWidget = new ComponentName(this, PlayerWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.player_widget);
		PendingIntent startMediaplayer = PendingIntent.getActivity(this, 0,
				PlaybackService.getPlayerActivityIntent(this), 0);

		views.setOnClickPendingIntent(R.id.layout_left, startMediaplayer);
        final Playable media = playbackService.getPlayable();
        if (playbackService != null && media != null) {
			PlayerStatus status = playbackService.getStatus();

			views.setTextViewText(R.id.txtvTitle, media.getEpisodeTitle());

			if (status == PlayerStatus.PLAYING) {
				String progressString = getProgressString(playbackService);
				if (progressString != null) {
					views.setTextViewText(R.id.txtvProgress, progressString);
				}
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
			views.setViewVisibility(R.id.txtvProgress, View.INVISIBLE);
			views.setTextViewText(R.id.txtvTitle,
					this.getString(R.string.no_media_playing_label));
			views.setImageViewResource(R.id.butPlay, R.drawable.ic_play_arrow_white_24dp);

		}

		manager.updateAppWidget(playerWidget, views);
		isUpdating = false;
	}

	/** Creates an intent which fakes a mediabutton press */
	private PendingIntent createMediaButtonIntent() {
		KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN,
				KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
		Intent startingIntent = new Intent(
				MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
		startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);

		return PendingIntent.getBroadcast(this, 0, startingIntent, 0);
	}

	private String getProgressString(PlaybackService ps) {
		int position = ps.getCurrentPosition();
		int duration = ps.getDuration();
		if (position != PlaybackService.INVALID_TIME
				&& duration != PlaybackService.INVALID_TIME) {
			return Converter.getDurationStringLong(position) + " / "
					+ Converter.getDurationStringLong(duration);
		} else {
			return null;
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			if (BuildConfig.DEBUG)
				Log.d(TAG, "Connection to service established");
            synchronized (psLock) {
                playbackService = ((PlaybackService.LocalBinder) service)
                        .getService();
                startViewUpdaterIfNotRunning();
            }
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
            synchronized (psLock) {
                playbackService = null;
                if (BuildConfig.DEBUG)
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
		private PlayerWidgetService service;

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

package de.danoeh.antennapod.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RemoteViews;
import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.receiver.PlayerWidget;
import de.danoeh.antennapod.util.Converter;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;

/** Updates the state of the player widget */
public class PlayerWidgetService extends Service {
	private static final String TAG = "PlayerWidgetService";

	private PlaybackService playbackService;
	/** True while service is updating the widget */
	private boolean isUpdating;

	public PlayerWidgetService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (AppConfig.DEBUG) Log.d(TAG, "Service created");
		isUpdating = false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!isUpdating) {
			isUpdating = true;
			if (playbackService == null && PlaybackService.isRunning) {
				bindService(new Intent(this, PlaybackService.class),
						mConnection, 0);
			} else {
				updateViews();
				isUpdating = false;
			}
		} else {
			if (AppConfig.DEBUG) Log.d(TAG,
					"Service was called while updating. Ignoring update request");
		}
		return Service.START_NOT_STICKY;
	}

	private void updateViews() {
		if (AppConfig.DEBUG) Log.d(TAG, "Updating widget views");
		ComponentName playerWidget = new ComponentName(this, PlayerWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.player_widget);
		PendingIntent startMediaplayer = PendingIntent.getActivity(this, 0,
				PlaybackService.getPlayerActivityIntent(this), 0);

		views.setOnClickPendingIntent(R.id.layout_left, startMediaplayer);
		if (playbackService != null) {
			FeedMedia media = playbackService.getMedia();
			MediaPlayer player = playbackService.getPlayer();
			PlayerStatus status = playbackService.getStatus();

			views.setTextViewText(R.id.txtvTitle, media.getItem().getTitle());

			if (status == PlayerStatus.PLAYING) {
				views.setTextViewText(R.id.txtvProgress,
						getProgressString(player));
				views.setImageViewResource(R.id.butPlay, R.drawable.av_pause);
			} else {
				views.setImageViewResource(R.id.butPlay, R.drawable.av_play);
			}
			views.setOnClickPendingIntent(R.id.butPlay,
					createMediaButtonIntent());
		} else {
			if (AppConfig.DEBUG) Log.d(TAG, "No media playing. Displaying defaultt views");
			views.setViewVisibility(R.id.txtvProgress, View.INVISIBLE);
			views.setTextViewText(R.id.txtvTitle,
					this.getString(R.string.no_media_playing_label));
			views.setImageViewResource(R.id.butPlay, R.drawable.av_play);

		}

		manager.updateAppWidget(playerWidget, views);
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

	private String getProgressString(MediaPlayer player) {

		return Converter.getDurationStringLong(player.getCurrentPosition())
				+ " / " + Converter.getDurationStringLong(player.getDuration());
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			if (AppConfig.DEBUG) Log.d(TAG, "Connection to service established");
			playbackService = ((PlaybackService.LocalBinder) service)
					.getService();
			updateViews();
			isUpdating = false;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			playbackService = null;
			if (AppConfig.DEBUG) Log.d(TAG, "Disconnected from service");
		}

	};

}

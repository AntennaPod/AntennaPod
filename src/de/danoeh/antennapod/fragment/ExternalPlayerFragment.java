package de.danoeh.antennapod.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.receiver.PlayerWidget;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.PlayerStatus;
import de.danoeh.antennapod.util.Converter;

/**
 * Fragment which is supposed to be displayed outside of the MediaplayerActivity
 * if the PlaybackService is running
 */
public class ExternalPlayerFragment extends SherlockFragment {
	private static final String TAG = "ExternalPlayerFragment";

	private ViewGroup fragmentLayout;
	private ImageView imgvCover;
	private ViewGroup layoutInfo;
	private TextView txtvTitle;
	private TextView txtvPosition;
	private ImageButton butPlay;

	private PlaybackService playbackService;
	private BroadcastReceiver playbackServiceNotificationReceiver;

	private boolean mediaInfoLoaded = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.external_player_fragment,
				container, false);
		fragmentLayout = (ViewGroup) root.findViewById(R.id.fragmentLayout);
		imgvCover = (ImageView) root.findViewById(R.id.imgvCover);
		layoutInfo = (ViewGroup) root.findViewById(R.id.layoutInfo);
		txtvTitle = (TextView) root.findViewById(R.id.txtvTitle);
		txtvPosition = (TextView) root.findViewById(R.id.txtvPosition);
		butPlay = (ImageButton) root.findViewById(R.id.butPlay);

		layoutInfo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "layoutInfo was clicked");

				if (playbackService != null
						&& playbackService.getMedia() != null) {
					startActivity(PlaybackService.getPlayerActivityIntent(
							getActivity(), playbackService.getMedia()));
				}
			}
		});

		butPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "butPlay was clicked");
				if (playbackService != null) {
					PlayerStatus status = playbackService.getStatus();
					if (status == PlayerStatus.PLAYING) {
						playbackService.pause(true);
					} else if (status == PlayerStatus.PAUSED){
						playbackService.play();
					}
				}
			}
		});
		return root;
	}

	private void setupReceiver() {
		playbackServiceNotificationReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction() != null) {
					if (intent.getAction().equals(
							PlaybackService.ACTION_PLAYER_STATUS_CHANGED)) {
						refreshFragmentState();
					} else if (intent.getAction().equals(
							PlayerWidget.FORCE_WIDGET_UPDATE)) {
						refreshFragmentState();
					} else if (intent.getAction().equals(
							PlaybackService.ACTION_PLAYER_NOTIFICATION)) {
						int type = intent.getIntExtra(
								PlaybackService.EXTRA_NOTIFICATION_TYPE, -1);
						if (type == PlaybackService.NOTIFICATION_TYPE_RELOAD) {
							mediaInfoLoaded = false;
							refreshFragmentState();
						}
					}
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(PlaybackService.ACTION_PLAYER_STATUS_CHANGED);
		filter.addAction(PlayerWidget.FORCE_WIDGET_UPDATE);
		filter.addAction(PlaybackService.ACTION_PLAYER_NOTIFICATION);
		getActivity().registerReceiver(playbackServiceNotificationReceiver,
				filter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setupReceiver();
		refreshFragmentState();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unbindService(mConnection);
		try {
			getActivity().unregisterReceiver(
					playbackServiceNotificationReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, "Fragment is about to be destroyed");
	}

	private void refreshPlayButtonAppearance() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Refreshing playbutton appearance");
		if (playbackService != null) {
			if (!PlaybackService.isPlayingVideo()) {
				PlayerStatus status = playbackService.getStatus();
				switch (status) {
				case PLAYING:
					butPlay.setImageResource(R.drawable.av_pause);
					butPlay.setVisibility(View.VISIBLE);
					break;
				case PAUSED:
					butPlay.setImageResource(R.drawable.av_play);
					butPlay.setVisibility(View.VISIBLE);
					break;
				default:
					butPlay.setVisibility(View.GONE);
				}
			} else {
				butPlay.setVisibility(View.GONE);
			}
		} else {
			Log.w(TAG,
					"refreshPlayButtonAppearance was called while playbackService was null!");
		}
	}

	private void loadMediaInfo() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Loading media info");
		if (playbackService != null) {
			FeedMedia media = playbackService.getMedia();
			if (media != null) {
				if (!mediaInfoLoaded) {
					txtvTitle.setText(media.getItem().getTitle());
					FeedImageLoader.getInstance().loadThumbnailBitmap(
							media.getItem().getFeed().getImage(), imgvCover);
					mediaInfoLoaded = true;
				}
				PlayerStatus status = playbackService.getStatus();
				refreshPlayButtonAppearance();
				if (status == PlayerStatus.PLAYING
						|| status == PlayerStatus.PAUSED) {

					txtvPosition.setText(Converter
							.getDurationStringLong(playbackService.getPlayer()
									.getCurrentPosition())
							+ " / "
							+ Converter.getDurationStringLong(playbackService
									.getPlayer().getDuration()));
				}

			} else {
				Log.w(TAG,
						"loadMediaInfo was called while the media object of playbackService was null!");
			}
		} else {
			Log.w(TAG,
					"loadMediaInfo was called while playbackService was null!");
		}
	}

	/**
	 * Creates a connection to the playbackService if necessary and refreshes
	 * the fragment's state.
	 */
	private void refreshFragmentState() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Refreshing fragment state");
		if (playbackService == null) {
			getActivity().bindService(
					new Intent(getActivity(), PlaybackService.class),
					mConnection, 0);
		} else {
			PlayerStatus status = playbackService.getStatus();
			if ((status == PlayerStatus.PAUSED || status == PlayerStatus.PLAYING)) {
				if (fragmentLayout.getVisibility() != View.VISIBLE) {
					fragmentLayout.setVisibility(View.VISIBLE);
				}
				loadMediaInfo();
			} else if (fragmentLayout.getVisibility() != View.GONE) {
				fragmentLayout.setVisibility(View.GONE);
			}
		}

	}

	protected ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			playbackService = ((PlaybackService.LocalBinder) service)
					.getService();
			refreshFragmentState();
			if (AppConfig.DEBUG)
				Log.d(TAG, "Connection to Service established");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			playbackService = null;
			if (AppConfig.DEBUG)
				Log.d(TAG, "Disconnected from Service");
		}
	};

}

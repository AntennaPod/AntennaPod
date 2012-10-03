package de.danoeh.antennapod.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.FeedManager;

public class PlaybackHistoryFragment extends ItemlistFragment {
	private static final String TAG = "PlaybackHistoryFragment";

	public PlaybackHistoryFragment() {
		super(FeedManager.getInstance().getPlaybackHistory(), true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().registerReceiver(historyUpdate,
				new IntentFilter(FeedManager.ACTION_PLAYBACK_HISTORY_UPDATE));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			getActivity().unregisterReceiver(historyUpdate);
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}

	private BroadcastReceiver historyUpdate = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Received content update");
			fila.notifyDataSetChanged();
		}

	};

}

package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.adapter.DefaultFeedItemlistAdapter;
import de.danoeh.antennapod.adapter.InternalFeedItemlistAdapter;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;

public class PlaybackHistoryFragment extends ItemlistFragment {
	private static final String TAG = "PlaybackHistoryFragment";

	public PlaybackHistoryFragment() {
		super(new DefaultFeedItemlistAdapter.ItemAccess() {

			@Override
			public FeedItem getItem(int position) {
				return FeedManager.getInstance().getPlaybackHistoryItemIndex(
						position);
			}

			@Override
			public int getCount() {
				return FeedManager.getInstance().getPlaybackHistorySize();
			}
		}, true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EventDistributor.getInstance().register(historyUpdate);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		EventDistributor.getInstance().unregister(historyUpdate);
	}

	private EventDistributor.EventListener historyUpdate = new EventDistributor.EventListener() {

		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if ((EventDistributor.PLAYBACK_HISTORY_UPDATE & arg) != 0) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Received content update");
				fila.notifyDataSetChanged();
			}

		}
	};

}

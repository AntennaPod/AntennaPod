package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.adapter.InternalFeedItemlistAdapter;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.storage.DBReader;

import java.util.Iterator;
import java.util.List;

public class PlaybackHistoryFragment extends ItemlistFragment {
	private static final String TAG = "PlaybackHistoryFragment";

    private List<FeedItem> playbackHistory;

	public PlaybackHistoryFragment() {
		super(true);
	}

    InternalFeedItemlistAdapter.ItemAccess itemAccessRef;
    @Override
    protected InternalFeedItemlistAdapter.ItemAccess itemAccess() {
        if (itemAccessRef == null) {
            itemAccessRef = new InternalFeedItemlistAdapter.ItemAccess() {

                @Override
                public FeedItem getItem(int position) {
                    return (playbackHistory != null) ? playbackHistory.get(position) : null;
                }

                @Override
                public int getCount() {
                    return (playbackHistory != null) ? playbackHistory.size() : 0;
                }

                @Override
                public boolean isInQueue(FeedItem item) {
                    return (queue != null) ? queue.contains(item.getId()) : false;
                }
            };
        }
        return itemAccessRef;
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
				loadData();
			}

		}
	};

    @Override
    protected void loadData() {
        AsyncTask<Void, Void, Void> loadTask = new AsyncTask<Void, Void, Void>() {
            private volatile List<FeedItem> phRef;
            private volatile List<Long> queueRef;

            @Override
            protected Void doInBackground(Void... voids) {
                Context context = PlaybackHistoryFragment.this.getActivity();
                if (context != null) {
                    queueRef = DBReader.getQueueIDList(context);
                    phRef = DBReader.getPlaybackHistory(context);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (queueRef != null && phRef != null) {
                    queue = queueRef;
                    playbackHistory = phRef;
                    Log.i(TAG, "Number of items in playback history: " + playbackHistory.size());
                    if (fila != null) {
                        fila.notifyDataSetChanged();
                    }
                } else {
                    if (queueRef == null) {
                        Log.e(TAG, "Could not load queue");
                    }
                    if (phRef == null) {
                        Log.e(TAG, "Could not load playback history");
                    }
                }
            }
        };
        loadTask.execute();
    }
}

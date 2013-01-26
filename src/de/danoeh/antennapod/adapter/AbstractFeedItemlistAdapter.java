package de.danoeh.antennapod.adapter;

import java.util.List;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.util.EpisodeFilter;
import android.content.Context;
import android.widget.ArrayAdapter;

public abstract class AbstractFeedItemlistAdapter extends
		ArrayAdapter<FeedItem> {

	private List<FeedItem> objects;
	private boolean isExpanded = true;

	public AbstractFeedItemlistAdapter(Context context, int textViewResourceId,
			List<FeedItem> objects) {
		super(context, textViewResourceId, objects);
		this.objects = objects;
	}

	@Override
	public int getCount() {
		if (isExpanded) {
			if (PodcastApp.getInstance().displayOnlyEpisodes()) {
				return EpisodeFilter.countItemsWithEpisodes(objects);
			} else {
				return super.getCount();
			}
		} else {
			return 0;
		}
	}

	@Override
	public FeedItem getItem(int position) {
		if (PodcastApp.getInstance().displayOnlyEpisodes()) {
			return EpisodeFilter.accessEpisodeByIndex(objects, position);
		} else {
			return super.getItem(position);
		}
	}

	public void toggleExpandedState() {
		isExpanded = !isExpanded;
		notifyDataSetChanged();
	}
}

package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.util.EpisodeFilter;

public abstract class AbstractFeedItemlistAdapter extends
		ArrayAdapter<FeedItem> {

	private List<FeedItem> objects;

	public AbstractFeedItemlistAdapter(Context context, int textViewResourceId,
			List<FeedItem> objects) {
		super(context, textViewResourceId, objects);
		this.objects = objects;
	}

	@Override
	public int getCount() {
			if (UserPreferences.isDisplayOnlyEpisodes()) {
				return EpisodeFilter.countItemsWithEpisodes(objects);
			} else {
				return super.getCount();
			}
		
	}

	@Override
	public FeedItem getItem(int position) {
		if (UserPreferences.isDisplayOnlyEpisodes()) {
			return EpisodeFilter.accessEpisodeByIndex(objects, position);
		} else {
			return super.getItem(position);
		}
	}
}

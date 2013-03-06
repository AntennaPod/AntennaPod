package de.danoeh.antennapod.adapter;

import android.widget.BaseAdapter;
import de.danoeh.antennapod.feed.FeedItem;

public abstract class AbstractFeedItemlistAdapter extends BaseAdapter {

	ItemAccess itemAccess;

	public AbstractFeedItemlistAdapter(ItemAccess itemAccess) {
		super();
		if (itemAccess == null) {
			throw new IllegalArgumentException("itemAccess must not be null");
		}
		this.itemAccess = itemAccess;
	}

	@Override
	public int getCount() {
		return itemAccess.getCount();

	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public FeedItem getItem(int position) {
		return itemAccess.getItem(position);
	}

	public static interface ItemAccess {
		int getCount();

		FeedItem getItem(int position);
	}
}

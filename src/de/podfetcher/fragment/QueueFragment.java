package de.podfetcher.fragment;

import de.podfetcher.feed.FeedManager;

public class QueueFragment extends ItemlistFragment {

	public QueueFragment() {
		super(FeedManager.getInstance().getQueue(), true);
	}

}

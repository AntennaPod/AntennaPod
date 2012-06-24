package de.podfetcher.fragment;

import de.podfetcher.feed.FeedManager;

public class QueueFragment extends FeedItemlistFragment {

	public QueueFragment() {
		super(FeedManager.getInstance().getQueue());
	}

}

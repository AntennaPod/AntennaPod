package de.podfetcher.fragment;

import de.podfetcher.feed.FeedManager;

/** Contains all unread items. */
public class UnreadItemlistFragment extends FeedItemlistFragment {

	public UnreadItemlistFragment() {
		super(FeedManager.getInstance().getUnreadItems());
		
	}

}

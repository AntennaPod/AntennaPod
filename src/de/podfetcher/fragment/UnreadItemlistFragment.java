package de.podfetcher.fragment;

import de.podfetcher.feed.FeedManager;

/** Contains all unread items. */
public class UnreadItemlistFragment extends ItemlistFragment {

	public UnreadItemlistFragment() {
		super(FeedManager.getInstance().getUnreadItems());
		
	}

}

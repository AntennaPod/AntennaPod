package de.danoeh.antennapod.adapter;

import de.danoeh.antennapod.feed.FeedItem;

public interface ActionButtonCallback {
	/** Is called when the action button of a list item has been pressed. */
	abstract void onActionButtonPressed(FeedItem item);
}

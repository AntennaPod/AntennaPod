package de.danoeh.antennapod.adapter;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.LongList;

interface ActionButtonCallback {
	/** Is called when the action button of a list item has been pressed. */
	void onActionButtonPressed(FeedItem item, LongList queueIds);
}

package de.danoeh.antennapod.util.flattr;

import de.danoeh.antennapod.R;

/** A Flattr thing for storing in Flattr queue */
public final class FlattrThing {
	private long feedId = 0;     // id column in Feeds table
	private long feedItemId = 0; // id column in FeedItems table, 0 if entire feed is to be flattred
	private String title = "AntennaPod";    // title column content from Feeds/FeedItems table
	private String paymentLink = FlattrUtils.APP_URL; // payment_link column content from Feeds/FeedItems table

	// flattr AntennaPod
	public FlattrThing() {
	}
	
	public FlattrThing(long feedId, long feedItemId, String title, String paymentLink) {
		if (feedId != 0 || feedItemId != 0) {
			this.feedItemId = feedItemId;
			this.feedId = feedId;
			this.title = title;
			this.paymentLink = paymentLink;
		}
	}
	
	public long getFeedItemId() {
		return feedItemId;
	}
	
	public long getFeedId() {
		return feedId;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getPaymentLink() {
		return paymentLink;
	}
}

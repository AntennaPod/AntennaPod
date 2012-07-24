package de.danoeh.antennapod.feed;

public class SearchResult {
	private FeedComponent component;
	/** Additional information (e.g. where it was found) */
	private String subtitle;

	public SearchResult(FeedComponent component, String subtitle) {
		super();
		this.component = component;
		this.subtitle = subtitle;
	}

	public FeedComponent getComponent() {
		return component;
	}

	public String getSubtitle() {
		return subtitle;
	}

}

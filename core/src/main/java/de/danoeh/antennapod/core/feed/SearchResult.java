package de.danoeh.antennapod.core.feed;

public class SearchResult {
	private final FeedComponent component;
	/** Additional information (e.g. where it was found) */
	private String subtitle;
	/** Higher value means more importance */
	private final int value;

	public SearchResult(FeedComponent component, int value, String subtitle) {
		super();
		this.component = component;
		this.value = value;
        this.subtitle = subtitle;
	}

	public FeedComponent getComponent() {
		return component;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public int getValue() {
		return value;
	}
	

}

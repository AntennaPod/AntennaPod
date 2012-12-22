package de.danoeh.antennapod.feed;

/**
 * Represents every possible component of a feed
 * @author daniel
 *
 */
public class FeedComponent {

	protected long id;

	public FeedComponent() {
		super();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	/**
	 * Update this FeedComponent's attributes with the attributes from another
	 * FeedComponent. This method should only update attributes which where read from
	 * the feed.
	 */
	public void updateFromOther(FeedComponent other) {
	}

	/**
	 * Compare's this FeedComponent's attribute values with another FeedComponent's
	 * attribute values. This method will only compare attributes which were
	 * read from the feed.
	 * 
	 * @return true if attribute values are different, false otherwise
	 */
	public boolean compareWithOther(FeedComponent other) {
		return false;
	}
	
	

}
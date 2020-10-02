package de.danoeh.antennapod.core.feed;

/**
 * Represents every possible component of a feed
 *
 * @author daniel
 */
public abstract class FeedComponent {

    long id;

    FeedComponent() {
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
    void updateFromOther(FeedComponent other) {
    }

    /**
     * Compare's this FeedComponent's attribute values with another FeedComponent's
     * attribute values. This method will only compare attributes which were
     * read from the feed.
     *
     * @return true if attribute values are different, false otherwise
     */
    boolean compareWithOther(FeedComponent other) {
        return false;
    }


    /**
     * Should return a non-null, human-readable String so that the item can be
     * identified by the user. Can be title, download-url, etc.
     */
    public abstract String getHumanReadableIdentifier();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeedComponent)) return false;

        FeedComponent that = (FeedComponent) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}

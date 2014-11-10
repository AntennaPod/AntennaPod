package de.danoeh.antennapod.core.feed;

import android.content.Context;
import android.net.Uri;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.asynctask.PicassoImageResource;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.EpisodeFilter;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;
import de.danoeh.antennapod.core.util.flattr.FlattrThing;

/**
 * Data Object for a whole feed
 *
 * @author daniel
 */
public class Feed extends FeedFile implements FlattrThing, PicassoImageResource {
    public static final int FEEDFILETYPE_FEED = 0;
    public static final String TYPE_RSS2 = "rss";
    public static final String TYPE_RSS091 = "rss";
    public static final String TYPE_ATOM1 = "atom";

    private String title;
    /**
     * Contains 'id'-element in Atom feed.
     */
    private String feedIdentifier;
    /**
     * Link to the website.
     */
    private String link;
    private String description;
    private String language;
    /**
     * Name of the author
     */
    private String author;
    private FeedImage image;
    private List<FeedItem> items;
    /**
     * Date of last refresh.
     */
    private Date lastUpdate;
    private FlattrStatus flattrStatus;
    private String paymentLink;
    /**
     * Feed type, for example RSS 2 or Atom
     */
    private String type;

    /**
     * Feed preferences
     */
    private FeedPreferences preferences;

    /**
     * The page number that this feed is on. Only feeds with page number "0" should be stored in the
     * database, feed objects with a higher page number only exist temporarily and should be merged
     * into feeds with page number "0".
     * <p/>
     * This attribute's value is not saved in the database
     */
    private int pageNr;

    /**
     * True if this is a "paged feed", i.e. there exist other feed files that belong to the same
     * logical feed.
     */
    private boolean paged;

    /**
     * Link to the next page of this feed. If this feed object represents a logical feed (i.e. a feed
     * that is saved in the database) this might be null while still being a paged feed.
     */
    private String nextPageLink;

    /**
     * This constructor is used for restoring a feed from the database.
     */
    public Feed(long id, Date lastUpdate, String title, String link, String description, String paymentLink,
                String author, String language, String type, String feedIdentifier, FeedImage image, String fileUrl,
                String downloadUrl, boolean downloaded, FlattrStatus status, boolean paged, String nextPageLink) {
        super(fileUrl, downloadUrl, downloaded);
        this.id = id;
        this.title = title;
        if (lastUpdate != null) {
            this.lastUpdate = (Date) lastUpdate.clone();
        } else {
            this.lastUpdate = null;
        }
        this.link = link;
        this.description = description;
        this.paymentLink = paymentLink;
        this.author = author;
        this.language = language;
        this.type = type;
        this.feedIdentifier = feedIdentifier;
        this.image = image;
        this.flattrStatus = status;
        this.paged = paged;
        this.nextPageLink = nextPageLink;

        items = new ArrayList<FeedItem>();
    }

    /**
     * This constructor is used for test purposes and uses a default flattr status object.
     */
    public Feed(long id, Date lastUpdate, String title, String link, String description, String paymentLink,
                String author, String language, String type, String feedIdentifier, FeedImage image, String fileUrl,
                String downloadUrl, boolean downloaded) {
        this(id, lastUpdate, title, link, description, paymentLink, author, language, type, feedIdentifier, image,
                fileUrl, downloadUrl, downloaded, new FlattrStatus(), false, null);
    }

    /**
     * This constructor can be used when parsing feed data. Only the 'lastUpdate' and 'items' field are initialized.
     */
    public Feed() {
        super();
        items = new ArrayList<FeedItem>();
        lastUpdate = new Date();
        this.flattrStatus = new FlattrStatus();
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should NOT be
     * used if the title of the feed is already known.
     */
    public Feed(String url, Date lastUpdate) {
        super(null, url, false);
        this.lastUpdate = (lastUpdate != null) ? (Date) lastUpdate.clone() : null;
        this.flattrStatus = new FlattrStatus();
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should be
     * used if the title of the feed is already known.
     */
    public Feed(String url, Date lastUpdate, String title) {
        this(url, lastUpdate);
        this.title = title;
        this.flattrStatus = new FlattrStatus();
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should be
     * used if the title of the feed is already known.
     */
    public Feed(String url, Date lastUpdate, String title, String username, String password) {
        this(url, lastUpdate, title);
        preferences = new FeedPreferences(0, true, username, password);
    }

    /**
     * Returns the number of FeedItems where 'read' is false. If the 'display
     * only episodes' - preference is set to true, this method will only count
     * items with episodes.
     */
    public int getNumOfNewItems() {
        int count = 0;
        for (FeedItem item : items) {
            if (item.getState() == FeedItem.State.NEW) {
                if (!UserPreferences.isDisplayOnlyEpisodes()
                        || item.getMedia() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns the number of FeedItems where the media started to play but
     * wasn't finished yet.
     */
    public int getNumOfStartedItems() {
        int count = 0;

        for (FeedItem item : items) {
            FeedItem.State state = item.getState();
            if (state == FeedItem.State.IN_PROGRESS
                    || state == FeedItem.State.PLAYING) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns true if at least one item in the itemlist is unread.
     *
     * @param enableEpisodeFilter true if this method should only count items with episodes if
     *                            the 'display only episodes' - preference is set to true by the
     *                            user.
     */
    public boolean hasNewItems(boolean enableEpisodeFilter) {
        for (FeedItem item : items) {
            if (item.getState() == FeedItem.State.NEW) {
                if (!(enableEpisodeFilter && UserPreferences
                        .isDisplayOnlyEpisodes()) || item.getMedia() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the number of FeedItems.
     *
     * @param enableEpisodeFilter true if this method should only count items with episodes if
     *                            the 'display only episodes' - preference is set to true by the
     *                            user.
     */
    public int getNumOfItems(boolean enableEpisodeFilter) {
        if (enableEpisodeFilter && UserPreferences.isDisplayOnlyEpisodes()) {
            return EpisodeFilter.countItemsWithEpisodes(items);
        } else {
            return items.size();
        }
    }

    /**
     * Returns the item at the specified index.
     *
     * @param enableEpisodeFilter true if this method should ignore items without episdodes if
     *                            the episodes filter has been enabled by the user.
     */
    public FeedItem getItemAtIndex(boolean enableEpisodeFilter, int position) {
        if (enableEpisodeFilter && UserPreferences.isDisplayOnlyEpisodes()) {
            return EpisodeFilter.accessEpisodeByIndex(items, position);
        } else {
            return items.get(position);
        }
    }

    /**
     * Returns the value that uniquely identifies this Feed. If the
     * feedIdentifier attribute is not null, it will be returned. Else it will
     * try to return the title. If the title is not given, it will use the link
     * of the feed.
     */
    public String getIdentifyingValue() {
        if (feedIdentifier != null && !feedIdentifier.isEmpty()) {
            return feedIdentifier;
        } else if (download_url != null && !download_url.isEmpty()) {
            return download_url;
        } else if (title != null && !title.isEmpty()) {
            return title;
        } else {
            return link;
        }
    }

    @Override
    public String getHumanReadableIdentifier() {
        if (title != null) {
            return title;
        } else {
            return download_url;
        }
    }

    public void updateFromOther(Feed other) {
        super.updateFromOther(other);
        if (other.title != null) {
            title = other.title;
        }
        if (other.feedIdentifier != null) {
            feedIdentifier = other.feedIdentifier;
        }
        if (other.link != null) {
            link = other.link;
        }
        if (other.description != null) {
            description = other.description;
        }
        if (other.language != null) {
            language = other.language;
        }
        if (other.author != null) {
            author = other.author;
        }
        if (other.paymentLink != null) {
            paymentLink = other.paymentLink;
        }
        if (other.flattrStatus != null) {
            flattrStatus = other.flattrStatus;
        }
        // this feed's nextPage might already point to a higher page, so we only update the nextPage value
        // if this feed is not paged and the other feed is.
        if (!this.paged && other.paged) {
            this.paged = other.paged;
            this.nextPageLink = other.nextPageLink;
        }
    }

    public boolean compareWithOther(Feed other) {
        if (super.compareWithOther(other)) {
            return true;
        }
        if (!title.equals(other.title)) {
            return true;
        }
        if (other.feedIdentifier != null) {
            if (feedIdentifier == null
                    || !feedIdentifier.equals(other.feedIdentifier)) {
                return true;
            }
        }
        if (other.link != null) {
            if (link == null || !link.equals(other.link)) {
                return true;
            }
        }
        if (other.description != null) {
            if (description == null || !description.equals(other.description)) {
                return true;
            }
        }
        if (other.language != null) {
            if (language == null || !language.equals(other.language)) {
                return true;
            }
        }
        if (other.author != null) {
            if (author == null || !author.equals(other.author)) {
                return true;
            }
        }
        if (other.paymentLink != null) {
            if (paymentLink == null || !paymentLink.equals(other.paymentLink)) {
                return true;
            }
        }
        if (other.isPaged() && !this.isPaged()) {
            return true;
        }
        if (!StringUtils.equals(other.getNextPageLink(), this.getNextPageLink())) {
            return true;
        }
        return false;
    }

    @Override
    public int getTypeAsInt() {
        return FEEDFILETYPE_FEED;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FeedImage getImage() {
        return image;
    }

    public void setImage(FeedImage image) {
        this.image = image;
    }

    public List<FeedItem> getItems() {
        return items;
    }

    public void setItems(List<FeedItem> list) {
        this.items = list;
    }

    public Date getLastUpdate() {
        return (lastUpdate != null) ? (Date) lastUpdate.clone() : null;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = (lastUpdate != null) ? (Date) lastUpdate.clone() : null;
    }

    public String getFeedIdentifier() {
        return feedIdentifier;
    }

    public void setFeedIdentifier(String feedIdentifier) {
        this.feedIdentifier = feedIdentifier;
    }

    public void setFlattrStatus(FlattrStatus status) {
        this.flattrStatus = status;
    }

    public FlattrStatus getFlattrStatus() {
        return flattrStatus;
    }

    public String getPaymentLink() {
        return paymentLink;
    }

    public void setPaymentLink(String paymentLink) {
        this.paymentLink = paymentLink;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPreferences(FeedPreferences preferences) {
        this.preferences = preferences;
    }

    public FeedPreferences getPreferences() {
        return preferences;
    }

    public void savePreferences(Context context) {
        DBWriter.setFeedPreferences(context, preferences);
    }

    @Override
    public void setId(long id) {
        super.setId(id);
        if (preferences != null) {
            preferences.setFeedID(id);
        }
    }

    @Override
    public Uri getImageUri() {
        if (image != null) {
            return image.getImageUri();
        } else {
            return null;
        }
    }

    public int getPageNr() {
        return pageNr;
    }

    public void setPageNr(int pageNr) {
        this.pageNr = pageNr;
    }

    public boolean isPaged() {
        return paged;
    }

    public void setPaged(boolean paged) {
        this.paged = paged;
    }

    public String getNextPageLink() {
        return nextPageLink;
    }

    public void setNextPageLink(String nextPageLink) {
        this.nextPageLink = nextPageLink;
    }
}

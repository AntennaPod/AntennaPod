package de.danoeh.antennapod.model.feed;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Data Object for a whole feed.
 *
 * @author daniel
 */
public class Feed {

    public static final int FEEDFILETYPE_FEED = 0;
    public static final int STATE_SUBSCRIBED = 0;
    public static final int STATE_NOT_SUBSCRIBED = 1;
    public static final String TYPE_RSS2 = "rss";
    public static final String TYPE_ATOM1 = "atom";
    public static final String PREFIX_LOCAL_FOLDER = "antennapod_local:";
    public static final String PREFIX_GENERATIVE_COVER = "antennapod_generative_cover:";

    private long id;
    private String localFileUrl;
    private String downloadUrl;
    /**
     * title as defined by the feed.
     */
    private String feedTitle;

    /**
     * custom title set by the user.
     */
    private String customTitle;

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
     * Name of the author.
     */
    private String author;
    private String imageUrl;
    private List<FeedItem> items;

    /**
     * String that identifies the last update (adopted from Last-Modified or ETag header).
     */
    private String lastModified;
    private long lastRefreshAttempt;

    private ArrayList<FeedFunding> fundingList;
    /**
     * Feed type, for example RSS 2 or Atom.
     */
    private String type;

    /**
     * Feed preferences.
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

    private boolean lastUpdateFailed;

    /**
     * Contains property strings. If such a property applies to a feed item, it is not shown in the feed list
     */
    private FeedItemFilter itemfilter;

    /**
     * User-preferred sortOrder for display.
     * Only those of scope {@link SortOrder.Scope#INTRA_FEED} is allowed.
     */
    @Nullable
    private SortOrder sortOrder;
    private int state;

    /**
     * This constructor is used for restoring a feed from the database.
     */
    public Feed(long id, String lastModified, String title, String customTitle, String link,
                String description, String paymentLinks, String author, String language,
                String type, String feedIdentifier, String imageUrl, String fileUrl,
                String downloadUrl, long lastRefreshAttempt, boolean paged, String nextPageLink,
                String filter, @Nullable SortOrder sortOrder, boolean lastUpdateFailed, int state) {
        this.localFileUrl = fileUrl;
        this.downloadUrl = downloadUrl;
        this.lastRefreshAttempt = lastRefreshAttempt;
        this.id = id;
        this.feedTitle = title;
        this.customTitle = customTitle;
        this.lastModified = lastModified;
        this.link = link;
        this.description = description;
        this.fundingList = FeedFunding.extractPaymentLinks(paymentLinks);
        this.author = author;
        this.language = language;
        this.type = type;
        this.feedIdentifier = feedIdentifier;
        this.imageUrl = imageUrl;
        this.paged = paged;
        this.nextPageLink = nextPageLink;
        this.items = new ArrayList<>();
        if (filter != null) {
            this.itemfilter = new FeedItemFilter(filter);
        } else {
            this.itemfilter = new FeedItemFilter();
        }
        setSortOrder(sortOrder);
        this.lastUpdateFailed = lastUpdateFailed;
        this.state = state;
    }

    /**
     * This constructor is used for test purposes.
     */
    public Feed(long id, String lastModified, String title, String link, String description, String paymentLink,
                String author, String language, String type, String feedIdentifier, String imageUrl, String fileUrl,
                String downloadUrl, long lastRefreshAttempt) {
        this(id, lastModified, title, null, link, description, paymentLink, author, language, type, feedIdentifier,
                imageUrl, fileUrl, downloadUrl, lastRefreshAttempt, false, null, null, null, false, STATE_SUBSCRIBED);
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should NOT be
     * used if the title of the feed is already known.
     */
    public Feed(String url, String lastModified) {
        this.localFileUrl = null;
        this.downloadUrl = url;
        this.lastRefreshAttempt = 0;
        this.lastModified = lastModified;
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should be
     * used if the title of the feed is already known.
     */
    public Feed(String url, String lastModified, String title) {
        this(url, lastModified);
        this.feedTitle = title;
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should be
     * used if the title of the feed is already known.
     */
    public Feed(String url, String lastModified, String title, String username, String password) {
        this(url, lastModified, title);
        preferences = new FeedPreferences(0, FeedPreferences.AutoDownloadSetting.GLOBAL, FeedPreferences.AutoDeleteAction.GLOBAL, VolumeAdaptionSetting.OFF,
            FeedPreferences.NewEpisodesAction.GLOBAL, username, password);
    }

    /**
     * Returns the item at the specified index.
     *
     */
    public FeedItem getItemAtIndex(int position) {
        return items.get(position);
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
        } else if (downloadUrl != null && !downloadUrl.isEmpty()) {
            return downloadUrl;
        } else if (feedTitle != null && !feedTitle.isEmpty()) {
            return feedTitle;
        } else {
            return link;
        }
    }

    public String getHumanReadableIdentifier() {
        if (!StringUtils.isEmpty(customTitle)) {
            return customTitle;
        } else if (!StringUtils.isEmpty(feedTitle)) {
            return feedTitle;
        } else {
            return downloadUrl;
        }
    }

    public void updateFromOther(Feed other) {
        // don't update feed's download_url, we do that manually if redirected
        // see AntennapodHttpClient
        if (other.imageUrl != null) {
            this.imageUrl = other.imageUrl;
        }
        if (other.feedTitle != null) {
            feedTitle = other.feedTitle;
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
        if (other.fundingList != null) {
            fundingList = other.fundingList;
        }
        if (other.lastRefreshAttempt > lastRefreshAttempt) {
            lastRefreshAttempt = other.lastRefreshAttempt;
        }
        // this feed's nextPage might already point to a higher page, so we only update the nextPage value
        // if this feed is not paged and the other feed is.
        if (!this.paged && other.paged) {
            this.paged = other.paged;
            this.nextPageLink = other.nextPageLink;
        }
    }

    public FeedItem getMostRecentItem() {
        // we could sort, but we don't need to, a simple search is fine...
        Date mostRecentDate = new Date(0);
        FeedItem mostRecentItem = null;
        for (FeedItem item : items) {
            if (item.getPubDate() != null && item.getPubDate().after(mostRecentDate)) {
                mostRecentDate = item.getPubDate();
                mostRecentItem = item;
            }
        }
        return mostRecentItem;
    }

    public String getTitle() {
        return !StringUtils.isEmpty(customTitle) ? customTitle : feedTitle;
    }

    public void setTitle(String title) {
        this.feedTitle = title;
    }

    public String getFeedTitle() {
        return this.feedTitle;
    }

    @Nullable
    public String getCustomTitle() {
        return this.customTitle;
    }

    public void setCustomTitle(String customTitle) {
        if (customTitle == null || customTitle.equals(feedTitle)) {
            this.customTitle = null;
        } else {
            this.customTitle = customTitle;
        }
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<FeedItem> getItems() {
        return items;
    }

    public void setItems(List<FeedItem> list) {
        this.items = list;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getFeedIdentifier() {
        return feedIdentifier;
    }

    public void setFeedIdentifier(String feedIdentifier) {
        this.feedIdentifier = feedIdentifier;
    }

    public void addPayment(FeedFunding funding) {
        if (fundingList == null) {
            fundingList = new ArrayList<FeedFunding>();
        }
        fundingList.add(funding);
    }

    public ArrayList<FeedFunding> getPaymentLinks() {
        return fundingList;
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

    public void setId(long id) {
        this.id = id;
        if (preferences != null) {
            preferences.setFeedID(id);
        }
    }

    public long getId() {
        return id;
    }

    public String getLocalFileUrl() {
        return localFileUrl;
    }

    public void setLocalFileUrl(String fileUrl) {
        this.localFileUrl = fileUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public long getLastRefreshAttempt() {
        return lastRefreshAttempt;
    }

    public void setLastRefreshAttempt(long lastRefreshAttempt) {
        this.lastRefreshAttempt = lastRefreshAttempt;
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

    @Nullable
    public FeedItemFilter getItemFilter() {
        return itemfilter;
    }

    @Nullable
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(@Nullable SortOrder sortOrder) {
        if (sortOrder != null && sortOrder.scope != SortOrder.Scope.INTRA_FEED) {
            throw new IllegalArgumentException("The specified sortOrder " + sortOrder
                    + " is invalid. Only those with INTRA_FEED scope are allowed.");
        }
        this.sortOrder = sortOrder;
    }

    public boolean hasLastUpdateFailed() {
        return this.lastUpdateFailed;
    }

    public void setLastUpdateFailed(boolean lastUpdateFailed) {
        this.lastUpdateFailed = lastUpdateFailed;
    }

    public boolean isLocalFeed() {
        return downloadUrl.startsWith(PREFIX_LOCAL_FOLDER);
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean hasEpisodeInApp() {
        if (items == null) {
            return false;
        }
        for (FeedItem item : items) {
            if (item.isTagged(FeedItem.TAG_FAVORITE)
                    || item.isTagged(FeedItem.TAG_QUEUE)
                    || item.isDownloaded()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInteractedWithEpisode() {
        if (items == null) {
            return false;
        }
        for (FeedItem item : items) {
            if (item.isTagged(FeedItem.TAG_FAVORITE)
                    || item.isTagged(FeedItem.TAG_QUEUE)
                    || item.isDownloaded()
                    || item.isPlayed()) {
                return true;
            }
            if (item.getMedia() != null && item.getMedia().getPosition() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Feed feed = (Feed) o;
        return id == feed.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

package de.danoeh.antennapod.core.feed;

import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.asynctask.ImageResource;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.SortOrder;

/**
 * Data Object for a whole feed
 *
 * @author daniel
 */
public class Feed extends FeedFile implements ImageResource {

    public static final int FEEDFILETYPE_FEED = 0;
    public static final String TYPE_RSS2 = "rss";
    public static final String TYPE_ATOM1 = "atom";

    /* title as defined by the feed */
    private String feedTitle;
    /* custom title set by the user */
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
     * Name of the author
     */
    private String author;
    private String imageUrl;
    private List<FeedItem> items;

    /**
     * String that identifies the last update (adopted from Last-Modified or ETag header)
     */
    private String lastUpdate;

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

    /**
     * This constructor is used for restoring a feed from the database.
     */
    public Feed(long id, String lastUpdate, String title, String customTitle, String link, String description, String paymentLink,
                String author, String language, String type, String feedIdentifier, String imageUrl, String fileUrl,
                String downloadUrl, boolean downloaded, boolean paged, String nextPageLink,
                String filter, @Nullable SortOrder sortOrder, boolean lastUpdateFailed) {
        super(fileUrl, downloadUrl, downloaded);
        this.id = id;
        this.feedTitle = title;
        this.customTitle = customTitle;
        this.lastUpdate = lastUpdate;
        this.link = link;
        this.description = description;
        this.paymentLink = paymentLink;
        this.author = author;
        this.language = language;
        this.type = type;
        this.feedIdentifier = feedIdentifier;
        this.imageUrl = imageUrl;
        this.paged = paged;
        this.nextPageLink = nextPageLink;
        this.items = new ArrayList<>();
        if(filter != null) {
            this.itemfilter = new FeedItemFilter(filter);
        } else {
            this.itemfilter = new FeedItemFilter(new String[0]);
        }
        setSortOrder(sortOrder);
        this.lastUpdateFailed = lastUpdateFailed;
    }

    /**
     * This constructor is used for test purposes
     */
    public Feed(long id, String lastUpdate, String title, String link, String description, String paymentLink,
                String author, String language, String type, String feedIdentifier, String imageUrl, String fileUrl,
                String downloadUrl, boolean downloaded) {
        this(id, lastUpdate, title, null, link, description, paymentLink, author, language, type, feedIdentifier, imageUrl,
                fileUrl, downloadUrl, downloaded, false, null, null, null, false);
    }

    /**
     * This constructor can be used when parsing feed data. Only the 'lastUpdate' and 'items' field are initialized.
     */
    public Feed() {
        super();
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should NOT be
     * used if the title of the feed is already known.
     */
    public Feed(String url, String lastUpdate) {
        super(null, url, false);
        this.lastUpdate = lastUpdate;
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should be
     * used if the title of the feed is already known.
     */
    public Feed(String url, String lastUpdate, String title) {
        this(url, lastUpdate);
        this.feedTitle = title;
    }

    /**
     * This constructor is used for requesting a feed download (it must not be used for anything else!). It should be
     * used if the title of the feed is already known.
     */
    public Feed(String url, String lastUpdate, String title, String username, String password) {
        this(url, lastUpdate, title);
        preferences = new FeedPreferences(0, true, FeedPreferences.AutoDeleteAction.GLOBAL, VolumeAdaptionSetting.OFF, username, password);
    }

    public static Feed fromCursor(Cursor cursor) {
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexLastUpdate = cursor.getColumnIndex(PodDBAdapter.KEY_LASTUPDATE);
        int indexTitle = cursor.getColumnIndex(PodDBAdapter.KEY_TITLE);
        int indexCustomTitle = cursor.getColumnIndex(PodDBAdapter.KEY_CUSTOM_TITLE);
        int indexLink = cursor.getColumnIndex(PodDBAdapter.KEY_LINK);
        int indexDescription = cursor.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION);
        int indexPaymentLink = cursor.getColumnIndex(PodDBAdapter.KEY_PAYMENT_LINK);
        int indexAuthor = cursor.getColumnIndex(PodDBAdapter.KEY_AUTHOR);
        int indexLanguage = cursor.getColumnIndex(PodDBAdapter.KEY_LANGUAGE);
        int indexType = cursor.getColumnIndex(PodDBAdapter.KEY_TYPE);
        int indexFeedIdentifier = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_IDENTIFIER);
        int indexFileUrl = cursor.getColumnIndex(PodDBAdapter.KEY_FILE_URL);
        int indexDownloadUrl = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL);
        int indexDownloaded = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOADED);
        int indexIsPaged = cursor.getColumnIndex(PodDBAdapter.KEY_IS_PAGED);
        int indexNextPageLink = cursor.getColumnIndex(PodDBAdapter.KEY_NEXT_PAGE_LINK);
        int indexHide = cursor.getColumnIndex(PodDBAdapter.KEY_HIDE);
        int indexSortOrder = cursor.getColumnIndex(PodDBAdapter.KEY_SORT_ORDER);
        int indexLastUpdateFailed = cursor.getColumnIndex(PodDBAdapter.KEY_LAST_UPDATE_FAILED);
        int indexImageUrl = cursor.getColumnIndex(PodDBAdapter.KEY_IMAGE_URL);

        Feed feed = new Feed(
                cursor.getLong(indexId),
                cursor.getString(indexLastUpdate),
                cursor.getString(indexTitle),
                cursor.getString(indexCustomTitle),
                cursor.getString(indexLink),
                cursor.getString(indexDescription),
                cursor.getString(indexPaymentLink),
                cursor.getString(indexAuthor),
                cursor.getString(indexLanguage),
                cursor.getString(indexType),
                cursor.getString(indexFeedIdentifier),
                cursor.getString(indexImageUrl),
                cursor.getString(indexFileUrl),
                cursor.getString(indexDownloadUrl),
                cursor.getInt(indexDownloaded) > 0,
                cursor.getInt(indexIsPaged) > 0,
                cursor.getString(indexNextPageLink),
                cursor.getString(indexHide),
                SortOrder.fromCodeString(cursor.getString(indexSortOrder)),
                cursor.getInt(indexLastUpdateFailed) > 0
        );

        FeedPreferences preferences = FeedPreferences.fromCursor(cursor);
        feed.setPreferences(preferences);
        return feed;
    }

    /**
     * Returns the number of FeedItems.
     *
     */
    public int getNumOfItems() {
        return items.size();
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
        } else if (download_url != null && !download_url.isEmpty()) {
            return download_url;
        } else if (feedTitle != null && !feedTitle.isEmpty()) {
            return feedTitle;
        } else {
            return link;
        }
    }

    @Override
    public String getHumanReadableIdentifier() {
        if (!TextUtils.isEmpty(customTitle)) {
            return customTitle;
        } else if (!TextUtils.isEmpty(feedTitle)) {
            return feedTitle;
        } else {
            return download_url;
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
        if (other.paymentLink != null) {
            paymentLink = other.paymentLink;
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
        if (other.imageUrl != null) {
            if (imageUrl == null || !TextUtils.equals(imageUrl, other.imageUrl)) {
                return true;
            }
        }
        if (!TextUtils.equals(feedTitle, other.feedTitle)) {
            return true;
        }
        if (other.feedIdentifier != null) {
            if (feedIdentifier == null || !feedIdentifier.equals(other.feedIdentifier)) {
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
        if (!TextUtils.equals(other.getNextPageLink(), this.getNextPageLink())) {
            return true;
        }
        return false;
    }

    public FeedItem getMostRecentItem() {
        // we could sort, but we don't need to, a simple search is fine...
        Date mostRecentDate = new Date(0);
        FeedItem mostRecentItem = null;
        for (FeedItem item : items) {
            if (item.getPubDate().after(mostRecentDate)) {
                mostRecentDate = item.getPubDate();
                mostRecentItem = item;
            }
        }
        return mostRecentItem;
    }

    @Override
    public int getTypeAsInt() {
        return FEEDFILETYPE_FEED;
    }

    public String getTitle() {
        return !TextUtils.isEmpty(customTitle) ? customTitle : feedTitle;
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
        if(customTitle == null || customTitle.equals(feedTitle)) {
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

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastModified) {
        this.lastUpdate = lastModified;
    }

    public String getFeedIdentifier() {
        return feedIdentifier;
    }

    public void setFeedIdentifier(String feedIdentifier) {
        this.feedIdentifier = feedIdentifier;
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

    public void savePreferences() {
        DBWriter.setFeedPreferences(preferences);
    }

    @Override
    public void setId(long id) {
        super.setId(id);
        if (preferences != null) {
            preferences.setFeedID(id);
        }
    }

    @Override
    public String getImageLocation() {
        return imageUrl;
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

    public void setItemFilter(String[] properties) {
        if (properties != null) {
            this.itemfilter = new FeedItemFilter(properties);
        }
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

}

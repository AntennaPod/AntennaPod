package de.danoeh.antennapod.core.feed;

import android.net.Uri;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.asynctask.ImageResource;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.ShownotesProvider;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;
import de.danoeh.antennapod.core.util.flattr.FlattrThing;

/**
 * Data Object for a XML message
 *
 * @author daniel
 */
public class FeedItem extends FeedComponent implements ShownotesProvider, FlattrThing, ImageResource {

    /**
     * The id/guid that can be found in the rss/atom feed. Might not be set.
     */
    private String itemIdentifier;
    private String title;
    /**
     * The description of a feeditem.
     */
    private String description;
    /**
     * The content of the content-encoded tag of a feeditem.
     */
    private String contentEncoded;

    private String link;
    private Date pubDate;
    private FeedMedia media;

    private Feed feed;
    private long feedId;

    private int state;
    public final static int NEW = -1;
    public final static int UNPLAYED = 0;
    public final static int PLAYED = 1;

    private String paymentLink;
    private FlattrStatus flattrStatus;

    /**
     * Is true if the database contains any chapters that belong to this item. This attribute is only
     * written once by DBReader on initialization.
     * The FeedItem might still have a non-null chapters value. In this case, the list of chapters
     * has not been saved in the database yet.
     * */
    private final boolean hasChapters;

    /**
     * The list of chapters of this item. This might be null even if there are chapters of this item
     * in the database. The 'hasChapters' attribute should be used to check if this item has any chapters.
     * */
    private List<Chapter> chapters;
    private FeedImage image;

    private boolean autoDownload = true;

    public FeedItem() {
        this.state = UNPLAYED;
        this.flattrStatus = new FlattrStatus();
        this.hasChapters = false;
    }

    /**
     * This constructor is used by DBReader.
     * */
    public FeedItem(long id, String title, String link, Date pubDate, String paymentLink, long feedId,
                    FlattrStatus flattrStatus, boolean hasChapters, FeedImage image, int state,
                    String itemIdentifier, boolean autoDownload) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.paymentLink = paymentLink;
        this.feedId = feedId;
        this.flattrStatus = flattrStatus;
        this.hasChapters = hasChapters;
        this.image = image;
        this.state = state;
        this.itemIdentifier = itemIdentifier;
        this.autoDownload = autoDownload;
    }

    /**
     * This constructor should be used for creating test objects.
     */
    public FeedItem(long id, String title, String itemIdentifier, String link, Date pubDate, int state, Feed feed) {
        this.id = id;
        this.title = title;
        this.itemIdentifier = itemIdentifier;
        this.link = link;
        this.pubDate = (pubDate != null) ? (Date) pubDate.clone() : null;
        this.state = state;
        this.feed = feed;
        this.flattrStatus = new FlattrStatus();
        this.hasChapters = false;
    }

    /**
     * This constructor should be used for creating test objects involving chapter marks.
     */
    public FeedItem(long id, String title, String itemIdentifier, String link, Date pubDate, int state, Feed feed, boolean hasChapters) {
        this.id = id;
        this.title = title;
        this.itemIdentifier = itemIdentifier;
        this.link = link;
        this.pubDate = (pubDate != null) ? (Date) pubDate.clone() : null;
        this.state = state;
        this.feed = feed;
        this.flattrStatus = new FlattrStatus();
        this.hasChapters = hasChapters;
    }

    public void updateFromOther(FeedItem other) {
        super.updateFromOther(other);
        if (other.title != null) {
            title = other.title;
        }
        if (other.getDescription() != null) {
            description = other.getDescription();
        }
        if (other.getContentEncoded() != null) {
            contentEncoded = other.contentEncoded;
        }
        if (other.link != null) {
            link = other.link;
        }
        if (other.pubDate != null && other.pubDate != pubDate) {
            pubDate = other.pubDate;
        }
        if (other.media != null) {
            if (media == null) {
                setMedia(other.media);
            } else if (media.compareWithOther(other.media)) {
                media.updateFromOther(other.media);
            }
        }
        if (other.paymentLink != null) {
            paymentLink = other.paymentLink;
        }
        if (other.chapters != null) {
            if (!hasChapters) {
                chapters = other.chapters;
            }
        }
        if (image == null) {
            image = other.image;
        }
    }

    /**
     * Returns the value that uniquely identifies this FeedItem. If the
     * itemIdentifier attribute is not null, it will be returned. Else it will
     * try to return the title. If the title is not given, it will use the link
     * of the entry.
     */
    public String getIdentifyingValue() {
        if (itemIdentifier != null && !itemIdentifier.isEmpty()) {
            return itemIdentifier;
        } else if (title != null && !title.isEmpty()) {
            return title;
        } else {
            return link;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Date getPubDate() {
        if (pubDate != null) {
            return (Date) pubDate.clone();
        } else {
            return null;
        }
    }

    public void setPubDate(Date pubDate) {
        if (pubDate != null) {
            this.pubDate = (Date) pubDate.clone();
        } else {
            this.pubDate = null;
        }
    }

    public FeedMedia getMedia() {
        return media;
    }

    /**
     * Sets the media object of this FeedItem. If the given
     * FeedMedia object is not null, it's 'item'-attribute value
     * will also be set to this item.
     */
    public void setMedia(FeedMedia media) {
        this.media = media;
        if (media != null && media.getItem() != this) {
            media.setItem(this);
        }
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public boolean isNew() {
        return state == NEW;
    }


    public void setNew() {
        state = NEW;
    }

    public boolean isPlayed() {
        return state == PLAYED;
    }

    public void setPlayed(boolean played) {
        if(played) {
            state = PLAYED;
        } else {
            state = UNPLAYED;
        }
    }

    private boolean isInProgress() {
        return (media != null && media.isInProgress());
    }

    public String getContentEncoded() {
        return contentEncoded;
    }

    public void setContentEncoded(String contentEncoded) {
        this.contentEncoded = contentEncoded;
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

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    public String getItemIdentifier() {
        return itemIdentifier;
    }

    public void setItemIdentifier(String itemIdentifier) {
        this.itemIdentifier = itemIdentifier;
    }

    public boolean hasMedia() {
        return media != null;
    }

    private boolean isPlaying() {
        if (media != null) {
            return media.isPlaying();
        }
        return false;
    }

    @Override
    public Callable<String> loadShownotes() {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {

                if (contentEncoded == null || description == null) {
                    DBReader.loadExtraInformationOfFeedItem(ClientConfig.applicationCallbacks.getApplicationInstance(), FeedItem.this);

                }
                return (contentEncoded != null) ? contentEncoded : description;
            }
        };
    }

    @Override
    public Uri getImageUri() {
        if(media != null && media.hasEmbeddedPicture()) {
            return media.getImageUri();
        } else if (image != null) {
           return image.getImageUri();
        } else if (feed != null) {
            return feed.getImageUri();
        } else {
            return null;
        }
    }

    public enum State {
        UNREAD, IN_PROGRESS, READ, PLAYING
    }

    public State getState() {
        if (hasMedia()) {
            if (isPlaying()) {
                return State.PLAYING;
            }
            if (isInProgress()) {
                return State.IN_PROGRESS;
            }
        }
        return (isPlayed() ? State.READ : State.UNREAD);
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    /**
     * Returns the image of this item or the image of the feed if this item does
     * not have its own image.
     */
    public FeedImage getImage() {
        return (hasItemImage()) ? image : feed.getImage();
    }

    public void setImage(FeedImage image) {
        this.image = image;
        if (image != null) {
            image.setOwner(this);
        }
    }

    /**
     * Returns true if this FeedItem has its own image, false otherwise.
     */
    public boolean hasItemImage() {
        return image != null;
    }

    /**
     * Returns true if this FeedItem has its own image and the image has been downloaded.
     */
    public boolean hasItemImageDownloaded() {
        return image != null && image.isDownloaded();
    }

    @Override
    public String getHumanReadableIdentifier() {
        return title;
    }

    public boolean hasChapters() {
        return hasChapters;
    }

    public void setAutoDownload(boolean autoDownload) {
        this.autoDownload = autoDownload;
    }

    public boolean getAutoDownload() {
        return this.autoDownload;
    }

    public boolean isAutoDownloadable() {
        return this.hasMedia() &&
                false == this.getMedia().isPlaying() &&
                false == this.getMedia().isDownloaded() &&
                this.getAutoDownload();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

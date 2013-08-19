package de.danoeh.antennapod.feed;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.util.ShownotesProvider;

/**
 * Data Object for a XML message
 *
 * @author daniel
 */
public class FeedItem extends FeedComponent implements
        ImageLoader.ImageWorkerTaskResource, ShownotesProvider {

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

    private boolean read;
    private String paymentLink;
    private List<Chapter> chapters;

    public FeedItem() {
        this.read = true;
    }

    /**
     * This constructor should be used for creating test objects.
     * */
    public FeedItem(long id, String title, String itemIdentifier, String link, Date pubDate, boolean read, Feed feed) {
        this.id = id;
        this.title = title;
        this.itemIdentifier = itemIdentifier;
        this.link = link;
        this.pubDate = (pubDate != null) ? (Date) pubDate.clone() : null;
        this.read = read;
        this.feed = feed;
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
                media = other.media;
            } else if (media.compareWithOther(other)) {
                media.updateFromOther(other);
            }
        }
        if (other.paymentLink != null) {
            paymentLink = other.paymentLink;
        }
        if (other.chapters != null) {
            if (chapters == null) {
                chapters = other.chapters;
            }
        }
    }

    /**
     * Returns the value that uniquely identifies this FeedItem. If the
     * itemIdentifier attribute is not null, it will be returned. Else it will
     * try to return the title. If the title is not given, it will use the link
     * of the entry.
     */
    public String getIdentifyingValue() {
        if (itemIdentifier != null) {
            return itemIdentifier;
        } else if (title != null) {
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
     * */
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

    public boolean isRead() {
        return read || isInProgress();
    }

    public void setRead(boolean read) {
        this.read = read;
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
                    DBReader.loadExtraInformationOfFeedItem(PodcastApp.getInstance(), FeedItem.this);

                }
                return (contentEncoded != null) ? contentEncoded : description;
            }
        };
    }

    public enum State {
        NEW, IN_PROGRESS, READ, PLAYING
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
        return (isRead() ? State.READ : State.NEW);
    }

    @Override
    public InputStream openImageInputStream() {
        InputStream out = null;
        if (hasMedia()) {
            out = media.openImageInputStream();
        }
        if (out == null && feed.getImage() != null) {
            out = feed.getImage().openImageInputStream();
        }
        return out;
    }

    @Override
    public InputStream reopenImageInputStream(InputStream input) {
        InputStream out = null;
        if (hasMedia()) {
            out = media.reopenImageInputStream(input);
        }
        if (out == null && feed.getImage() != null) {
            out = feed.getImage().reopenImageInputStream(input);
        }
        return out;
    }

    @Override
    public String getImageLoaderCacheKey() {
        String out = null;
        if (hasMedia()) {
            out = media.getImageLoaderCacheKey();
        }
        if (out == null && feed.getImage() != null) {
            out = feed.getImage().getImageLoaderCacheKey();
        }
        return out;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

}

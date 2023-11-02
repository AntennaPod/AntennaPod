package de.danoeh.antennapod.model.feed;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Item (episode) within a feed.
 *
 * @author daniel
 */
public class FeedItem extends FeedComponent implements Serializable {

    /** tag that indicates this item is in the queue */
    public static final String TAG_QUEUE = "Queue";
    /** tag that indicates this item is in favorites */
    public static final String TAG_FAVORITE = "Favorite";

    /**
     * The id/guid that can be found in the rss/atom feed. Might not be set.
     */
    private String itemIdentifier;
    private String title;
    /**
     * The description of a feeditem.
     */
    private String description;

    private String link;
    private Date pubDate;
    private FeedMedia media;

    private transient Feed feed;
    private long feedId;
    private String podcastIndexChapterUrl;
    private String podcastIndexTranscriptText;
    private Hashtable<String, String> podcastIndexTranscriptUrls;

    private int state;
    public static final int NEW = -1;
    public static final int UNPLAYED = 0;
    public static final int PLAYED = 1;

    private String paymentLink;

    /**
     * Is true if the database contains any chapters that belong to this item. This attribute is only
     * written once by DBReader on initialization.
     * The FeedItem might still have a non-null chapters value. In this case, the list of chapters
     * has not been saved in the database yet.
     * */
    private final boolean hasChapters;

    /**
     * Is true if database or feeditem has podcast:transcript
     */
    private boolean hasTranscript;

    /**
     * The list of chapters of this item. This might be null even if there are chapters of this item
     * in the database. The 'hasChapters' attribute should be used to check if this item has any chapters.
     * */
    private transient List<Chapter> chapters;
    private String imageUrl;

    private long autoDownload = 1;

    /**
     * Any tags assigned to this item
     */
    private final Set<String> tags = new HashSet<>();

    public FeedItem() {
        this.state = UNPLAYED;
        this.hasChapters = false;
        this.hasTranscript = false;
    }

    /**
     * This constructor is used by DBReader.
     * */
    public FeedItem(long id, String title, String link, Date pubDate, String paymentLink, long feedId,
                    boolean hasChapters, String imageUrl, int state,
                    String itemIdentifier, long autoDownload, String podcastIndexChapterUrl,
                    String transcriptType, String transcriptUrl, String transcriptText) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.paymentLink = paymentLink;
        this.feedId = feedId;
        this.hasChapters = hasChapters;
        this.imageUrl = imageUrl;
        this.state = state;
        this.itemIdentifier = itemIdentifier;
        this.autoDownload = autoDownload;
        this.podcastIndexChapterUrl = podcastIndexChapterUrl;
        if (transcriptUrl != null) {
            this.podcastIndexTranscriptUrls = new Hashtable<String, String>();
            // TT TODO, how many urls do we store?
            this.podcastIndexTranscriptUrls.put(transcriptType, transcriptUrl);
            this.hasTranscript = true;
        }
        this.podcastIndexTranscriptText = transcriptText;
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
        this.hasChapters = hasChapters;
    }

    public void updateFromOther(FeedItem other) {
        super.updateFromOther(other);
        if (other.imageUrl != null) {
            this.imageUrl = other.imageUrl;
        }
        if (other.title != null) {
            title = other.title;
        }
        if (other.getDescription() != null) {
            description = other.getDescription();
        }
        if (other.link != null) {
            link = other.link;
        }
        if (other.pubDate != null && !other.pubDate.equals(pubDate)) {
            pubDate = other.pubDate;
        }
        if (other.media != null) {
            if (media == null) {
                setMedia(other.media);
                // reset to new if feed item did link to a file before
                setNew();
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
        if (other.podcastIndexChapterUrl != null) {
            podcastIndexChapterUrl = other.podcastIndexChapterUrl;
        }
        if (other.getPodcastIndexTranscriptUrls() != null) {
            podcastIndexTranscriptUrls = other.podcastIndexTranscriptUrls;
            hasTranscript = true;
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
        } else if (hasMedia() && media.getDownload_url() != null) {
            return media.getDownload_url();
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

    @Nullable
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

    public int getPlayState() {
        return state;
    }

    public void setNew() {
        state = NEW;
    }

    public boolean isPlayed() {
        return state == PLAYED;
    }

    public void setPlayed(boolean played) {
        if (played) {
            state = PLAYED;
        } else {
            state = UNPLAYED;
        }
    }

    public boolean isInProgress() {
        return (media != null && media.isInProgress());
    }

    /**
     * Updates this item's description property if the given argument is longer than the already stored description
     * @param newDescription The new item description, content:encoded, itunes:description, etc.
     */
    public void setDescriptionIfLonger(String newDescription) {
        if (newDescription == null) {
            return;
        }
        if (this.description == null) {
            this.description = newDescription;
        } else if (this.description.length() < newDescription.length()) {
            this.description = newDescription;
        }
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

    public String getImageLocation() {
        if (imageUrl != null) {
            return imageUrl;
        } else if (media != null && media.hasEmbeddedPicture()) {
            return FeedMedia.FILENAME_PREFIX_EMBEDDED_COVER + media.getLocalMediaUrl();
        } else if (feed != null) {
            return feed.getImageUrl();
        } else {
            return null;
        }
    }

    public enum State {
        UNREAD, IN_PROGRESS, READ, PLAYING
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    /**
     * Returns the image of this item, as specified in the feed.
     * To load the image that can be displayed to the user, use {@link #getImageLocation},
     * which also considers embedded pictures or the feed picture if no other picture is present.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String getHumanReadableIdentifier() {
        return title;
    }

    public boolean hasChapters() {
        return hasChapters;
    }

    public void disableAutoDownload() {
        this.autoDownload = 0;
    }

    public long getAutoDownloadAttemptsAndTime() {
        return autoDownload;
    }

    public int getFailedAutoDownloadAttempts() {
        // 0: auto download disabled
        // 1: auto download enabled (default)
        // > 1: auto download enabled, timestamp of last failed attempt, last digit denotes number of failed attempts
        if (autoDownload <= 1) {
            return 0;
        }
        int failedAttempts = (int)(autoDownload % 10);
        if (failedAttempts == 0) {
            failedAttempts = 10;
        }
        return failedAttempts;
    }

    public void increaseFailedAutoDownloadAttempts(long now) {
        if (autoDownload == 0) {
            return; // Don't re-enable
        }
        int failedAttempts = getFailedAutoDownloadAttempts() + 1;
        if (failedAttempts >= 5) {
            disableAutoDownload(); // giving up
        } else {
            autoDownload = (now / 10) * 10 + failedAttempts;
        }
    }

    public boolean isAutoDownloadable(long now) {
        if (media == null || media.isDownloaded() || autoDownload == 0) {
            return false;
        }
        if (autoDownload == 1) {
            return true; // Never failed
        }
        int failedAttempts = getFailedAutoDownloadAttempts();
        long waitingTime = TimeUnit.HOURS.toMillis((long) Math.pow(2, failedAttempts - 1));
        long lastAttempt = (autoDownload / 10) * 10;
        return now >= (lastAttempt + waitingTime);
    }

    public boolean isDownloaded() {
        return media != null && media.isDownloaded();
    }

    /**
     * @return true if the item has this tag
     */
    public boolean isTagged(String tag) {
        return tags.contains(tag);
    }

    /**
     * @param tag adds this tag to the item. NOTE: does NOT persist to the database
     */
    public void addTag(String tag) {
        tags.add(tag);
    }

    /**
     * @param tag the to remove
     */
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public String getPodcastIndexChapterUrl() {
        return podcastIndexChapterUrl;
    }

    public void setPodcastIndexChapterUrl(String url) {
        podcastIndexChapterUrl = url;
    }

    public void setPodcastIndexTranscriptUrl(String t, String url) {
        if (podcastIndexTranscriptUrls == null) {
            // TT TODO: Should not crate it here, instead load it from
            //  DBReader or make sure to free up in the destructor?
            podcastIndexTranscriptUrls = new Hashtable<String, String>();
        }
        podcastIndexTranscriptUrls.put(t, url);
        hasTranscript = true;
    }


    public String getPodcastIndexTranscriptUrls(String t) {
        if (podcastIndexTranscriptUrls == null) {
            return null;
        }
        return podcastIndexTranscriptUrls.get(t);
    }

    public Hashtable<String, String> getPodcastIndexTranscriptUrls() {
        if (podcastIndexTranscriptUrls == null) {
            return null;
        }
        return podcastIndexTranscriptUrls;
    }

    public Pair<String, String> getPodcastIndexTranscriptUrlPreferred() {
        if (podcastIndexTranscriptUrls == null) {
            return null;
        }
        // TT TODO - ranked order of preference, a better way to get this list?
        if (podcastIndexTranscriptUrls.get("application/json") != null) {
           return new Pair("application/json", podcastIndexTranscriptUrls.get("application/json"));
        }
        if (podcastIndexTranscriptUrls.get("application/srr") != null) {
            return new Pair("application/srr", podcastIndexTranscriptUrls.get("application/srr"));
        }
        if (podcastIndexTranscriptUrls.get("application/srt") != null) {
            return new Pair("application/srt", podcastIndexTranscriptUrls.get("application/srt"));
        }
        if (podcastIndexTranscriptUrls.get("application/x-subrip") != null) {
            return new Pair("application/srr", podcastIndexTranscriptUrls.get("application/x-subrip"));
        }
        return null;
    }

    public String getPodcastIndexTranscriptText() {
        return podcastIndexTranscriptText;
    }

    public String setPodcastIndexTranscriptText(String str) {
        return podcastIndexTranscriptText = str;
    }

    public boolean hasTranscript() {
        return hasTranscript;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

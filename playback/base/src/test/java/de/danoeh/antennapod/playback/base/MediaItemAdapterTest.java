package de.danoeh.antennapod.playback.base;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.preference.PreferenceManager;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class MediaItemAdapterTest {
    private static final String EPISODE_COVER = "https://example.com/episode.png";
    private static final String PODCAST_COVER = "https://example.com/podcast.png";
    private Context context;
    private FeedMedia media;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        UserPreferences.init(context);
        Feed feed = new Feed("https://example.com/feed.xml", null, "Podcast");
        feed.setImageUrl(PODCAST_COVER);
        FeedItem item = new FeedItem();
        item.setImageUrl(EPISODE_COVER);
        item.setFeed(feed);
        media = new FeedMedia(item, "https://example.com/episode.mp3", 2, "audio/mpeg");
        item.setMedia(media);
    }

    @Test
    public void usesEpisodeCoverWhenEnabled() {
        setUseEpisodeCover(true);
        MediaItem mediaItem = MediaItemAdapter.fromPlayable(context, media, true);
        assertEquals(Uri.parse(EPISODE_COVER), mediaItem.mediaMetadata.artworkUri);
    }

    @Test
    public void usesPodcastCoverWhenEpisodeCoverDisabled() {
        setUseEpisodeCover(false);
        MediaItem mediaItem = MediaItemAdapter.fromPlayable(context, media, true);
        assertEquals(Uri.parse(PODCAST_COVER), mediaItem.mediaMetadata.artworkUri);
    }

    private void setUseEpisodeCover(boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(UserPreferences.PREF_USE_EPISODE_COVER, value).commit();
    }
}

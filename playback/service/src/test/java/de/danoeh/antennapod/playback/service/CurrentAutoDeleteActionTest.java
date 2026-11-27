package de.danoeh.antennapod.playback.service;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class CurrentAutoDeleteActionTest {
    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
        DBWriter.tearDownTests();
    }

    @Test
    public void usesPreferenceSavedAfterItemWasLoaded() throws Exception {
        Feed feed = createFeed(FeedPreferences.AutoDeleteAction.GLOBAL);
        saveFeed(feed);

        // Episode loaded into the player while the podcast still used the global setting
        FeedItem loadedItem = DBReader.getFeedItem(feed.getItems().get(0).getId());

        FeedPreferences preferences = DBReader.getFeed(feed.getId(), false, 0, 0).getPreferences();
        preferences.setAutoDeleteAction(FeedPreferences.AutoDeleteAction.NEVER);
        DBWriter.setFeedPreferences(preferences).get();

        assertEquals(FeedPreferences.AutoDeleteAction.GLOBAL,
                loadedItem.getFeed().getPreferences().getCurrentAutoDelete());
        assertEquals(FeedPreferences.AutoDeleteAction.NEVER,
                Media3PlaybackService.getCurrentAutoDeleteAction(loadedItem));
    }

    @Test
    public void keepsLoadedPreferenceWhenFeedIsNotInDatabase() {
        Feed feed = createFeed(FeedPreferences.AutoDeleteAction.ALWAYS);
        feed.setId(12345);
        FeedItem item = feed.getItems().get(0);
        item.setFeedId(feed.getId());

        assertEquals(FeedPreferences.AutoDeleteAction.ALWAYS,
                Media3PlaybackService.getCurrentAutoDeleteAction(item));
    }

    private static Feed createFeed(FeedPreferences.AutoDeleteAction autoDeleteAction) {
        Feed feed = new Feed("https://example.com/feed.xml", null, "Feed");
        feed.setItems(new ArrayList<>());
        feed.setPreferences(new FeedPreferences(0, FeedPreferences.AutoDownloadSetting.GLOBAL,
                autoDeleteAction, VolumeAdaptionSetting.OFF, FeedPreferences.NewEpisodesAction.GLOBAL, null, null));
        FeedItem item = new FeedItem(0, "Episode", "episode-1", "https://example.com/1", new Date(),
                FeedItem.UNPLAYED, feed);
        feed.getItems().add(item);
        return feed;
    }

    private static void saveFeed(Feed feed) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
    }
}

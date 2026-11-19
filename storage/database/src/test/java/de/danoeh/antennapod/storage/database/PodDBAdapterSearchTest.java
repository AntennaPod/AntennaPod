package de.danoeh.antennapod.storage.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PodDBAdapterSearchTest {
    private static final long FEED_ID = 42;
    private PodDBAdapter adapter;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        PodDBAdapter.init(context);
        adapter = PodDBAdapter.getInstance();
        PodDBAdapter.deleteDatabase();

        ContentValues feed = new ContentValues();
        feed.put(PodDBAdapter.KEY_ID, FEED_ID);
        feed.put(PodDBAdapter.KEY_TITLE, "Search test podcast");
        feed.put(PodDBAdapter.KEY_STATE, Feed.STATE_SUBSCRIBED);
        adapter.insertTestData(PodDBAdapter.TABLE_NAME_FEEDS, feed);
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void searchItemsRanksMatchesBeforePublicationDate() {
        addEpisode("ordered-new", "Power and Broker", "Other topic", 300);
        addEpisode("ordered-old", "The Power Broker", "Other topic", 100);
        addEpisode("reverse-title", "Broker on Power", "Other topic", 900);
        addEpisode("ordered-description", "Other topic", "About the power broker", 800);
        addEpisode("mixed", "Power and politics", "A broker interview", 700);
        addEpisode("reverse-description", "Other topic", "Broker on power", 600);
        addEpisode("missing-term", "Power only", "Other topic", 1000);

        assertEquals(Arrays.asList("ordered-new", "ordered-old", "reverse-title", "ordered-description",
                "mixed", "reverse-description"), searchTitles(0, "power broker"));
    }

    @Test
    public void searchItemsRetainsMatchesAndDatesForSingleWord() {
        addEpisode("new-title", "Power from today", null, 500);
        addEpisode("old-title", "Earlier power", null, 100);
        addEpisode("new-description", "Other topic", "The power is back", 900);
        addEpisode("unrelated", "Other topic", "Broker interview", 1000);

        assertEquals(Arrays.asList("new-title", "old-title", "new-description"), searchTitles(0, "POWER"));
    }

    @Test
    public void searchItemsPrioritizesRelevantResultsBeforeLimit() {
        addEpisode("old-title", "The Power Broker", "Other topic", 1);
        for (int i = 0; i < 300; i++) {
            addEpisode("description-" + i, "Other topic", "The broker has power", 1000 + i);
        }

        List<String> titles = searchTitles(0, "power broker");
        assertEquals(300, titles.size());
        assertEquals("old-title", titles.get(0));
        assertEquals("description-299", titles.get(1));
    }

    private void addEpisode(String title, String episodeTitle, String description, long pubDate) {
        ContentValues episode = new ContentValues();
        episode.put(PodDBAdapter.KEY_TITLE, episodeTitle);
        episode.put(PodDBAdapter.KEY_DESCRIPTION, description);
        episode.put(PodDBAdapter.KEY_PUBDATE, pubDate);
        episode.put(PodDBAdapter.KEY_FEED, FEED_ID);
        episode.put(PodDBAdapter.KEY_ITEM_IDENTIFIER, title);
        adapter.insertTestData(PodDBAdapter.TABLE_NAME_FEED_ITEMS, episode);
    }

    private List<String> searchTitles(long feedId, String query) {
        List<String> titles = new ArrayList<>();
        try (Cursor cursor = adapter.searchItems(feedId, query, FeedItemFilter.unfiltered())) {
            int identifierColumn = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ITEM_IDENTIFIER);
            while (cursor.moveToNext()) {
                titles.add(cursor.getString(identifierColumn));
            }
        }
        return titles;
    }
}

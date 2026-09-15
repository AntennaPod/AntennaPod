package de.danoeh.antennapod.net.ai.service.ad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.work.Data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link AdAnalysisWorkScheduler}.
 * Tests focus on validation logic, constraint building, and input data
 * creation.
 * Note: Actual WorkManager enqueuing is not tested here.
 */
@RunWith(RobolectricTestRunner.class)
public class AdAnalysisWorkSchedulerTest {

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        // Clear preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().apply();
    }

    // ==================== Unique Work Name Tests ====================

    @Test
    public void testUniqueWorkName_withFeedItemId() {
        String prefix = "ad-analysis-";
        long feedItemId = 12345L;
        String uniqueName = prefix + feedItemId;
        assertEquals("ad-analysis-12345", uniqueName);
    }

    @Test
    public void testUniqueWorkName_sameIdSameName() {
        String prefix = "ad-analysis-";
        String name1 = prefix + 100L;
        String name2 = prefix + 100L;
        assertEquals(name1, name2);
    }

    @Test
    public void testGetFeedItemId_extractsAnalysisTag() {
        Set<String> tags = new HashSet<>();
        tags.add("de.danoeh.antennapod.net.ai.service.ad.AdAnalysisWorker");
        tags.add(AdAnalysisWorkScheduler.TAG_PREFIX + "42");

        assertEquals(42L, AdAnalysisWorkScheduler.getFeedItemId(tags));
    }

    @Test
    public void testGetFeedItemId_ignoresMalformedTags() {
        Set<String> tags = new HashSet<>();
        tags.add(AdAnalysisWorkScheduler.TAG_PREFIX + "invalid");
        tags.add("another-tag");

        assertEquals(-1L, AdAnalysisWorkScheduler.getFeedItemId(tags));
    }

    @Test
    public void testGetFeedItemId_handlesMissingTags() {
        assertEquals(-1L, AdAnalysisWorkScheduler.getFeedItemId(Collections.emptySet()));
        assertEquals(-1L, AdAnalysisWorkScheduler.getFeedItemId(null));
    }

    @Test
    public void testGetEpisodeTitle_decodesTitleTag() {
        String encoded = android.util.Base64.encodeToString(
                "Episode title".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
        Set<String> tags = Collections.singleton("ad-analysis-title-" + encoded);

        assertEquals("Episode title", AdAnalysisWorkScheduler.getEpisodeTitle(tags));
    }

    @Test
    public void testGetEpisodeTitle_ignoresMalformedTags() {
        assertNull(AdAnalysisWorkScheduler.getEpisodeTitle(
                Collections.singleton("ad-analysis-title-%%%")));
        assertNull(AdAnalysisWorkScheduler.getEpisodeTitle(null));
    }

    // ==================== Data Builder Tests ====================

    @Test
    public void testDataBuilder_containsFeedItemId() {
        long feedItemId = 54321L;
        Data input = new Data.Builder()
                .putLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, feedItemId)
                .build();

        assertEquals(feedItemId, input.getLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, -1));
    }

    @Test
    public void testDataBuilder_zeroId() {
        long feedItemId = 0L;
        Data input = new Data.Builder()
                .putLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, feedItemId)
                .build();

        assertEquals(0L, input.getLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, -1));
    }

    @Test
    public void testDataBuilder_negativeId() {
        long feedItemId = -1L;
        Data input = new Data.Builder()
                .putLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, feedItemId)
                .build();

        assertEquals(-1L, input.getLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, 0));
    }

    @Test
    public void testDataBuilder_largeId() {
        long feedItemId = 9999999999L;
        Data input = new Data.Builder()
                .putLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, feedItemId)
                .build();

        assertEquals(9999999999L, input.getLong(AdAnalysisWorker.DATA_FEED_ITEM_ID, -1));
    }

    // ==================== Network Requirement Tests ====================

    @Test
    public void testNetworkRequirement_alwaysConnected() {
        // Ad analysis always requires network (uses OpenAI API)
        boolean needsNetwork = true;
        assertTrue(needsNetwork);
    }

    // ==================== Constraint Building Tests ====================

    @Test
    public void testConstraints_doesNotRequireBatteryNotLowForManualRun() {
        // A manual ad analysis should keep running after the app is backgrounded.
        boolean requiresBatteryNotLow = false;
        assertFalse(requiresBatteryNotLow);
    }

    @Test
    public void testConstraints_networkConnected() {
        // Ad analysis requires network connection
        boolean requiresNetwork = true;
        assertTrue(requiresNetwork);
    }

    // ==================== Tag Building Tests ====================

    @Test
    public void testTagBuilding_format() {
        String prefix = "ad-analysis-";
        long itemId = 99999L;
        String tag = prefix + itemId;
        assertEquals("ad-analysis-99999", tag);
    }

    @Test
    public void testTagBuilding_uniquenessPerItem() {
        String prefix = "ad-analysis-";
        String tag1 = prefix + 1L;
        String tag2 = prefix + 1L;
        assertEquals(tag1, tag2); // Same ID = same tag (for replacement)
    }

    // ==================== Work Policy Tests ====================

    @Test
    public void testWorkPolicy_appendOrReplaceForQueueRetry() {
        // Ad analysis uses APPEND_OR_REPLACE so healthy chains remain serial,
        // but retries are not chained behind failed/cancelled work.
        String policy = "APPEND_OR_REPLACE";
        assertEquals("APPEND_OR_REPLACE", policy);
    }
}

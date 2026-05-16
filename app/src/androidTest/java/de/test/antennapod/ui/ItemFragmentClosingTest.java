package de.test.antennapod.ui;

import android.content.Intent;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.test.antennapod.EspressoTestUtils;

import org.greenrobot.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static de.test.antennapod.EspressoTestUtils.waitForViewGlobally;

@RunWith(AndroidJUnit4.class)
public class ItemFragmentClosingTest {

    private UITestUtils uiTestUtils;
    private long itemId;
    private long feedId;

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws Exception {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();

        uiTestUtils = new UITestUtils(InstrumentationRegistry.getInstrumentation().getTargetContext());
        uiTestUtils.setup();
        uiTestUtils.addLocalFeedData(false);

        FeedItem queueItem = DBReader.getQueue().get(0);
        itemId = queueItem.getId();
        feedId = queueItem.getFeedId();

        Intent intent = new Intent();
        intent.putExtra(MainActivityStarter.EXTRA_EPISODE_ID, itemId);
        activityRule.launchActivity(intent);
    }

    @After
    public void tearDown() throws IOException {
        uiTestUtils.tearDown();
    }

    @Test
    public void testClosesWhenFeedRefreshRemovesItem() {
        waitForViewGlobally(withId(R.id.pager), 15000);

        FeedItem existingItem = DBReader.getFeedItem(itemId);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.removeFeedItems(Collections.singletonList(existingItem));
        adapter.close();

        EventBus.getDefault().post(new FeedListUpdateEvent(feedId));

        EspressoTestUtils.waitForViewToDisappear(withId(R.id.pager), 15000);
    }
}

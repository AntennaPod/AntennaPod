package de.test.antennapod.ui;

import android.content.Context;
import android.content.Intent;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.screen.download.CompletedDownloadsFragment;
import de.test.antennapod.EspressoTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static de.test.antennapod.EspressoTestUtils.waitForViewGlobally;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class DownloadLogTest {
    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);
    private Context context;
    private Intent completedDownloadsIntent;
    private Feed feed;
    private FeedMedia media;

    @Before
    public void setUp() throws Exception {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        completedDownloadsIntent = new Intent(context, MainActivity.class);
        completedDownloadsIntent.putExtra(MainActivityStarter.EXTRA_FRAGMENT_TAG, CompletedDownloadsFragment.TAG);

        feed = new Feed(0, "last modified", "@@Feed title@@", "link", "description", "payment link",
                "@author@", "language", "type", "feedIdentifier", "http://localhost/cover.png",
                "/sdcard/abc", "http://localhost/feed.xml", 0);
        FeedItem item = new FeedItem(0, "title", "identifier", "link", new Date(), FeedItem.UNPLAYED, feed);
        media = new FeedMedia(item, "http://localhost/media.mp3", 10000, "mime type");
        item.setMedia(media);
        feed.setItems(Collections.singletonList(item));
        feed = FeedDatabaseWriter.updateFeed(context, feed, false);
    }

    @Test
    public void testExistingSubscribedFeed() {
        DownloadResult result = new DownloadResult("@@Title@@", feed.getId(),
                Feed.FEEDFILETYPE_FEED, false, DownloadError.ERROR_IO_ERROR, "@@reason@@");
        openDialog(result);
        // Open feed
        onView(withText(R.string.download_log_open_feed)).perform(click());
        waitForViewGlobally(withText(feed.getAuthor()), 2000);
    }

    @Test
    public void testExistingNonSubscribedFeed() throws InterruptedException, ExecutionException {
        DBWriter.setFeedState(context, feed, Feed.STATE_NOT_SUBSCRIBED).get();
        DownloadResult result = new DownloadResult("@@Title@@", feed.getId(),
                Feed.FEEDFILETYPE_FEED, false, DownloadError.ERROR_IO_ERROR, "@@reason@@");
        openDialog(result);
        // Opens online feed view
        onView(withText(R.string.download_log_open_feed)).perform(click());
        waitForViewGlobally(withText(feed.getAuthor()), 2000);
        onView(isRoot()).perform(waitForView(allOf(withText(R.string.subscribe_label), isDisplayed()), 2000));
    }

    @Test
    public void testNonExistingFeed() {
        DownloadResult result = new DownloadResult("@@Title@@", feed.getId() + 1,
                Feed.FEEDFILETYPE_FEED, false, DownloadError.ERROR_IO_ERROR, "@@reason@@");
        openDialog(result);
        // Does not have button
        onView(withText(R.string.download_log_open_feed)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testExistingMedia() {
        DownloadResult result = new DownloadResult("@@Title@@", media.getId(),
                FeedMedia.FEEDFILETYPE_FEEDMEDIA, false, DownloadError.ERROR_IO_ERROR, "@@reason@@");
        openDialog(result);
        // Opens feed
        onView(withText(R.string.download_log_open_feed)).perform(click());
        waitForViewGlobally(withText(feed.getAuthor()), 2000);
    }

    @Test
    public void testNonExistingMedia() {
        DownloadResult result = new DownloadResult("@@Title@@", media.getId() + 1,
                FeedMedia.FEEDFILETYPE_FEEDMEDIA, false, DownloadError.ERROR_IO_ERROR, "@@reason@@");
        openDialog(result);
        // Does not have button
        onView(withText(R.string.download_log_open_feed)).check(matches(not(isDisplayed())));
    }

    void openDialog(DownloadResult result) {
        DBWriter.addDownloadStatus(result);
        activityRule.launchActivity(completedDownloadsIntent);
        onView(withContentDescription(R.string.downloads_log_label)).perform(click());
        onView(isRoot()).perform(waitForView(allOf(withText(result.getTitle()), isDisplayed()), 1000));
        onView(withText(result.getTitle())).perform(click());
        onView(isRoot()).perform(waitForView(allOf(withText(result.getReasonDetailed()), isDisplayed()), 1000));
    }
}

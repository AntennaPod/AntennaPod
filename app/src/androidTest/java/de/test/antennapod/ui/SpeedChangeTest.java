package de.test.antennapod.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.test.antennapod.EspressoTestUtils;
import de.test.antennapod.IgnoreOnCi;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static de.test.antennapod.EspressoTestUtils.waitForView;

/**
 * User interface tests for changing the playback speed.
 */
@RunWith(AndroidJUnit4.class)
@IgnoreOnCi
public class SpeedChangeTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, false, false);
    private UITestUtils uiTestUtils;
    private String[] availableSpeeds;
    private PlaybackController controller;

    @Before
    public void setUp() throws Exception {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();
        EspressoTestUtils.setLastNavFragment(QueueFragment.TAG);

        Context context = getInstrumentation().getTargetContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(UserPreferences.PREF_FOLLOW_QUEUE, true).commit();

        uiTestUtils = new UITestUtils(context);
        uiTestUtils.setup();
        uiTestUtils.setMediaFileName("30sec.mp3");
        uiTestUtils.addLocalFeedData(true);

        List<FeedItem> queue = DBReader.getQueue();
        PlaybackPreferences.writeMediaPlaying(queue.get(0).getMedia(), PlayerStatus.PAUSED, false);
        availableSpeeds = new String[] {"1.00", "2.00", "3.00"};
        UserPreferences.setPlaybackSpeedArray(Arrays.asList(1.0f, 2.0f, 3.0f));

        EspressoTestUtils.tryKillPlaybackService();
        activityRule.launchActivity(new Intent().putExtra(MainActivity.EXTRA_OPEN_PLAYER, true));
        controller = new PlaybackController(activityRule.getActivity());
        controller.init();
        controller.getMedia(); // To load media
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
        controller.release();
    }

    @Test
    public void testChangeSpeedServiceNotRunning() {
        clickThroughSpeeds();
    }

    @Test
    public void testChangeSpeedPlaying() {
        onView(isRoot()).perform(waitForView(withId(R.id.butPlay), 1000));
        controller.playPause();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()
                -> controller.getStatus() == PlayerStatus.PLAYING);
        clickThroughSpeeds();
    }

    @Test
    public void testChangeSpeedPaused() {
        onView(isRoot()).perform(waitForView(withId(R.id.butPlay), 1000));
        controller.playPause();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()
                -> controller.getStatus() == PlayerStatus.PLAYING);
        controller.playPause();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()
                -> controller.getStatus() == PlayerStatus.PAUSED);
        clickThroughSpeeds();
    }

    private void clickThroughSpeeds() {
        onView(isRoot()).perform(waitForView(withText(availableSpeeds[0]), 1000));
        onView(withId(R.id.txtvPlaybackSpeed)).check(matches(withText(availableSpeeds[0])));
        onView(withId(R.id.butPlaybackSpeed)).perform(click());
        onView(isRoot()).perform(waitForView(withText(availableSpeeds[1]), 1000));
        onView(withId(R.id.txtvPlaybackSpeed)).check(matches(withText(availableSpeeds[1])));
        onView(withId(R.id.butPlaybackSpeed)).perform(click());
        onView(isRoot()).perform(waitForView(withText(availableSpeeds[2]), 1000));
        onView(withId(R.id.txtvPlaybackSpeed)).check(matches(withText(availableSpeeds[2])));
        onView(withId(R.id.butPlaybackSpeed)).perform(click());
        onView(isRoot()).perform(waitForView(withText(availableSpeeds[0]), 1000));
        onView(withId(R.id.txtvPlaybackSpeed)).check(matches(withText(availableSpeeds[0])));
    }
}

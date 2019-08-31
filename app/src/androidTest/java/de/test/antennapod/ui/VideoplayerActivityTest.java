package de.test.antennapod.ui;

import android.content.Intent;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.VideoplayerActivity;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Test class for VideoplayerActivity
 */
@MediumTest
@Ignore
public class VideoplayerActivityTest {

    @Rule
    public ActivityTestRule<VideoplayerActivity> activityTestRule = new ActivityTestRule<>(VideoplayerActivity.class, false, false);

    /**
     * Test if activity can be started.
     */
    @Test
    public void testStartActivity() throws Exception {
        activityTestRule.launchActivity(new Intent());
        onView(withId(R.id.videoframe)).check(matches(isDisplayed()));
    }
}

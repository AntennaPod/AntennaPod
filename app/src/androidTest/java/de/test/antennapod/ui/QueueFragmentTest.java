package de.test.antennapod.ui;

import android.content.Intent;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.test.antennapod.EspressoTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.NthMatcher.first;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;

/**
 * User interface tests for queue fragment.
 */
@RunWith(AndroidJUnit4.class)
public class QueueFragmentTest {

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();
        EspressoTestUtils.setLaunchScreen(QueueFragment.TAG);
        activityRule.launchActivity(new Intent());
    }

    @Test
    public void testLockEmptyQueue() {
        onView(first(EspressoTestUtils.actionBarOverflow())).perform(click());
        onView(withText(R.string.lock_queue)).perform(click());
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.lock_queue))).perform(click());
        onView(first(EspressoTestUtils.actionBarOverflow())).perform(click());
        onView(withText(R.string.lock_queue)).perform(click());
    }

    @Test
    public void testSortEmptyQueue() {
        onView(first(EspressoTestUtils.actionBarOverflow())).perform(click());
        onView(withText(R.string.sort)).perform(click());
        onView(withText(R.string.random)).perform(click());
    }

    @Test
    public void testKeepEmptyQueueSorted() {
        onView(first(EspressoTestUtils.actionBarOverflow())).perform(click());
        onView(withText(R.string.sort)).perform(click());
        onView(withText(R.string.keep_sorted)).perform(click());
    }
}

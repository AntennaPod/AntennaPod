package de.test.antennapod.ui;

import android.content.Intent;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.test.antennapod.EspressoTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * User interface tests for queue fragment
 */
@RunWith(AndroidJUnit4.class)
public class QueueFragmentTest {

    @Rule
    public IntentsTestRule<MainActivity> mActivityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.makeNotFirstRun();
        EspressoTestUtils.clearDatabase();
        EspressoTestUtils.setLastNavFragment(QueueFragment.TAG);
        mActivityRule.launchActivity(new Intent());
    }

    @Test
    public void testLockEmptyQueue() {
        onView(withContentDescription(R.string.lock_queue)).perform(click());
        onView(withContentDescription(R.string.unlock_queue)).perform(click());
    }

    @Test
    public void testSortEmptyQueue() {
        Espresso.openContextualActionModeOverflowMenu();
        onView(withText(R.string.sort)).perform(click());
        onView(withText(R.string.random)).perform(click());
    }

    @Test
    public void testKeepEmptyQueueSorted() {
        Espresso.openContextualActionModeOverflowMenu();
        onView(withText(R.string.sort)).perform(click());
        onView(withText(R.string.keep_sorted)).perform(click());
    }
}

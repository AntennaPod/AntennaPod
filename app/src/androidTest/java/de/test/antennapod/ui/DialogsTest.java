package de.test.antennapod.ui;

import android.content.Intent;
import android.view.View;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.runner.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.test.antennapod.EspressoTestUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static de.test.antennapod.NthMatcher.first;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;

/**
 * User interface tests for queue fragment.
 */
@RunWith(AndroidJUnit4.class)
public class DialogsTest {

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws InterruptedException {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();
        EspressoTestUtils.setLastNavFragment(AddFeedFragment.TAG);
        activityRule.launchActivity(new Intent());

        String url = "https://omny.fm/shows/silence-is-not-an-option/why-not-being-racist-is-not-enough";

        onView(withId(R.id.btn_add_via_url)).perform(scrollTo()).perform(click());
        onView(withId(R.id.text)).perform(clearText(), typeText(url));
        onView(withText(R.string.confirm_label)).inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(closeSoftKeyboard())
                .perform(scrollTo())
                .perform(click());
        Thread.sleep(5000);
        onView(withId(R.id.butSubscribe)).perform(click());
        Thread.sleep(5000);
        onView(withId(R.id.recyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        Thread.sleep(3000);
        onView(first(EspressoTestUtils.actionBarOverflow())).perform(click());
    }

    @Test
    public void testShareDialogDisplayed() {
        onView(withText(R.string.share_label)).perform(scrollTo()).perform(click());
        onView(allOf(isDisplayed(), withText(R.string.share_episode_label)));
    }

    @Test
    public void testShareDialogShareButton() throws InterruptedException {
        onView(withText(R.string.share_label)).perform(scrollTo()).perform(click());
        onView(allOf(isDisplayed(), withText(R.string.share_label)));
        Thread.sleep(1000);
        onView(withText(R.string.share_episode_positive_label_button)).perform(scrollTo()).perform(click());
        Thread.sleep(2000);
    }

    @Test
    public void testShareDialogCancelButton() {
        onView(withText(R.string.share_label)).perform(scrollTo()).perform(click());
        onView(withText(R.string.cancel_label)).check(matches(isDisplayed()));
    }

}

package de.danoeh.antennapodSA.activity;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapodSA.R;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CheckUninstallOfDyanmicFeature1 {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void checkUninstallOfDyanmicFeature1() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DataInteraction relativeLayout = onData(anything())
                .inAdapterView(allOf(withId(R.id.nav_list),
                        childAtPosition(
                                withId(R.id.nav_layout),
                                2)))
                .atPosition(3);
        relativeLayout.perform(click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Open menu"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withId(R.id.content),
                                                1)),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        DataInteraction relativeLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.nav_list),
                        childAtPosition(
                                withId(R.id.nav_layout),
                                2)))
                .atPosition(4);
        relativeLayout2.perform(click());

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withContentDescription("Open menu"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withId(R.id.content),
                                                1)),
                                1),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

        DataInteraction relativeLayout3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.nav_list),
                        childAtPosition(
                                withId(R.id.nav_layout),
                                2)))
                .atPosition(3);
        relativeLayout3.perform(click());
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals((( ViewGroup ) parent).getChildAt(position));
            }
        };
    }
}

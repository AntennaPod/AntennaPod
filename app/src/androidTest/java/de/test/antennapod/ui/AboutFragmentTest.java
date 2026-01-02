package de.test.antennapod.ui;

import android.content.Intent;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.screen.preferences.PreferenceActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickPreference;

@RunWith(AndroidJUnit4.class)
public class AboutFragmentTest {

    @Rule
    public ActivityTestRule<PreferenceActivity> activityTestRule = new ActivityTestRule<>(PreferenceActivity.class,
            false, false);

    @Test
    public void testAboutNavigation() {
        activityTestRule.launchActivity(new Intent());
        clickPreference(R.string.about_pref);
        onView(withText(R.string.about_pref)).check(matches(isDisplayed()));
        onView(withText(R.string.contributors)).check(matches(isDisplayed()));
        onView(withText(R.string.licenses)).check(matches(isDisplayed()));
    }

    @Test
    public void testContributors() {
        activityTestRule.launchActivity(new Intent());
        clickPreference(R.string.about_pref);
        clickPreference(R.string.contributors);
        onView(withText(R.string.contributors)).check(matches(isDisplayed()));
    }

    @Test
    public void testLicenses() {
        activityTestRule.launchActivity(new Intent());
        clickPreference(R.string.about_pref);
        clickPreference(R.string.licenses);
        onView(withText(R.string.licenses)).check(matches(isDisplayed()));
    }

    @Test
    public void testPrivacyPolicy() {
        activityTestRule.launchActivity(new Intent());
        clickPreference(R.string.about_pref);
        onView(withText(R.string.privacy_policy)).check(matches(isDisplayed()));
    }
}

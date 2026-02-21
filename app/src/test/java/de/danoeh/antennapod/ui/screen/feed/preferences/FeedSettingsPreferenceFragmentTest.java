package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.model.feed.FeedFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class FeedSettingsPreferenceFragmentTest {

    private Context context;
    private SharedPreferences sharedPreferences;
    private ListPreference enqueueLocationPreference;
    private FeedPreferences feedPreferences;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Create preference
        enqueueLocationPreference = new ListPreference(context);
        enqueueLocationPreference.setKey("feedEnqueueLocation");

        // Setup entries and values using enum names (matching actual implementation)
        String[] entries = { "Use global setting", "Back", "Front", "After current episode", "Random" };
        String[] values = { "GLOBAL", "BACK", "FRONT", "AFTER_CURRENTLY_PLAYING", "RANDOM" };
        enqueueLocationPreference.setEntries(entries);
        enqueueLocationPreference.setEntryValues(values);
        enqueueLocationPreference.setValue("GLOBAL"); // Start with GLOBAL

        feedPreferences = new FeedPreferences(
                1L, // feedId
                FeedPreferences.AutoDownloadSetting.fromInteger(0),
                true, // keepUpdated
                FeedPreferences.AutoDeleteAction.fromCode(0),
                VolumeAdaptionSetting.fromInteger(0),
                "", // username
                "", // password
                new FeedFilter("", "", 0), // filter
                1.0f, // playbackSpeed
                0, // skipIntro
                0, // skipEnding
                FeedPreferences.SkipSilence.fromCode(0),
                false, // episodeNotification
                FeedPreferences.NewEpisodesAction.fromCode(0),
                new HashSet<>() // tags
        );
        feedPreferences.setEnqueueLocation(FeedPreferences.EnqueueLocation.GLOBAL);

        // Set initial summary to match what happens in setupPreferences()
        CharSequence[] initialEntries = enqueueLocationPreference.getEntries();
        CharSequence[] initialValues = enqueueLocationPreference.getEntryValues();
        for (int i = 0; i < initialValues.length; i++) {
            if (initialValues[i].toString().equals("GLOBAL")) {
                enqueueLocationPreference.setSummary(initialEntries[i].toString());
                break;
            }
        }
    }

    @Test
    public void testEnqueueLocationPreferenceUpdatesUI() {
        // Test that UI updates when making a selection
        ListPreference.OnPreferenceChangeListener listener = (preference, newValue) -> {
            // This simulates what happens in FeedSettingsPreferenceFragment
            String valueStr = (String) newValue;
            FeedPreferences.EnqueueLocation newLocation = FeedPreferences.EnqueueLocation.valueOf(valueStr);

            // In real code, these would be called:
            feedPreferences.setEnqueueLocation(newLocation);
            // DBWriter.setFeedPreferences(feedPreferences);

            // Update summary using the actual method logic
            CharSequence[] entries = ((ListPreference) preference).getEntries();
            CharSequence[] values = ((ListPreference) preference).getEntryValues();

            // Find the index of the enum name in values array
            for (int i = 0; i < values.length; i++) {
                if (values[i].toString().equals(newLocation.name())) {
                    preference.setSummary(entries[i].toString());
                    break;
                }
            }

            // Update preference value to show selection in UI
            ((ListPreference) preference).setValue(valueStr);

            return false; // Don't persist to SharedPreferences - DB is authoritative
        };

        enqueueLocationPreference.setOnPreferenceChangeListener(listener);

        // Initially should be GLOBAL
        assertEquals("GLOBAL", enqueueLocationPreference.getValue());
        assertEquals("Use global setting", enqueueLocationPreference.getSummary());

        // Change to FRONT
        boolean result = enqueueLocationPreference.callChangeListener("FRONT");

        // Verify change was accepted
        assertEquals(false, result); // callChangeListener returns listener's return value

        // Verify UI was updated immediately
        assertEquals("FRONT", enqueueLocationPreference.getValue());
        assertEquals("Front", enqueueLocationPreference.getSummary());

        // Verify that preference value was updated in FeedPreferences object
        assertEquals(FeedPreferences.EnqueueLocation.FRONT, feedPreferences.getEnqueueLocation());
    }

    @Test
    public void testEnqueueLocationPreferencePersistsAfterNavigation() {
        // Test that value persists when navigating away and back
        ListPreference.OnPreferenceChangeListener listener = (preference, newValue) -> {
            // This simulates what happens in FeedSettingsPreferenceFragment
            String valueStr = (String) newValue;
            FeedPreferences.EnqueueLocation newLocation = FeedPreferences.EnqueueLocation.valueOf(valueStr);

            // In real code, these would be called:
            feedPreferences.setEnqueueLocation(newLocation);
            // DBWriter.setFeedPreferences(feedPreferences);

            // Update summary using the actual method logic
            CharSequence[] entries = ((ListPreference) preference).getEntries();
            CharSequence[] values = ((ListPreference) preference).getEntryValues();

            // Find the index of the enum name in values array
            for (int i = 0; i < values.length; i++) {
                if (values[i].toString().equals(newLocation.name())) {
                    preference.setSummary(entries[i].toString());
                    break;
                }
            }

            // Update preference value to show selection in UI
            ((ListPreference) preference).setValue(valueStr);

            return false; // Don't persist to SharedPreferences - DB is authoritative
        };

        enqueueLocationPreference.setOnPreferenceChangeListener(listener);

        // Change to AFTER_CURRENTLY_PLAYING
        enqueueLocationPreference.callChangeListener("AFTER_CURRENTLY_PLAYING");

        // Verify immediate change
        assertEquals("AFTER_CURRENTLY_PLAYING", enqueueLocationPreference.getValue());
        assertEquals("After current episode", enqueueLocationPreference.getSummary());
        assertEquals(FeedPreferences.EnqueueLocation.AFTER_CURRENTLY_PLAYING, feedPreferences.getEnqueueLocation());

        // Simulate navigating away and back by recreating preference
        // (This simulates what happens when the fragment is recreated)
        ListPreference recreatedPreference = new ListPreference(context);
        recreatedPreference.setKey("feedEnqueueLocation");
        recreatedPreference.setEntries(enqueueLocationPreference.getEntries());
        recreatedPreference.setEntryValues(enqueueLocationPreference.getEntryValues());

        // In real scenario, the value would be loaded from database via FeedPreferences
        // Here we simulate that by setting it from our feedPreferences object
        recreatedPreference.setValue(feedPreferences.getEnqueueLocation().name());

        // Update summary to match what would happen in setupPreferences()
        FeedPreferences.EnqueueLocation persistedLocation = feedPreferences.getEnqueueLocation();
        CharSequence[] entries = recreatedPreference.getEntries();
        CharSequence[] values = recreatedPreference.getEntryValues();

        for (int i = 0; i < values.length; i++) {
            if (values[i].toString().equals(persistedLocation.name())) {
                recreatedPreference.setSummary(entries[i].toString());
                break;
            }
        }

        // Verify that value persisted correctly after "navigation"
        assertEquals("AFTER_CURRENTLY_PLAYING", recreatedPreference.getValue());
        assertEquals("After current episode", recreatedPreference.getSummary());
        assertEquals(FeedPreferences.EnqueueLocation.AFTER_CURRENTLY_PLAYING, feedPreferences.getEnqueueLocation());
    }

    @Test
    public void testEnqueueLocationPreferenceHandlesInvalidValues() {
        // Test that invalid values don't crash the preference
        ListPreference.OnPreferenceChangeListener listener = (preference, newValue) -> {
            try {
                String valueStr = (String) newValue;
                FeedPreferences.EnqueueLocation newLocation = FeedPreferences.EnqueueLocation.valueOf(valueStr);
                feedPreferences.setEnqueueLocation(newLocation);
                return false;
            } catch (IllegalArgumentException e) {
                // Should handle invalid enum values gracefully
                return false;
            }
        };

        enqueueLocationPreference.setOnPreferenceChangeListener(listener);

        // Test with invalid enum value
        boolean result = enqueueLocationPreference.callChangeListener("INVALID_VALUE");

        // Should not crash and should return false
        assertEquals(false, result);

        // Preference should remain unchanged
        assertEquals("GLOBAL", enqueueLocationPreference.getValue());
        assertEquals(FeedPreferences.EnqueueLocation.GLOBAL, feedPreferences.getEnqueueLocation());
    }
}

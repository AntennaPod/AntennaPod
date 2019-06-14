package de.danoeh.antennapod.preferences;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.fragment.preferences.PlaybackPreferencesFragment;

/**
 * Implements functions from PreferenceController that are flavor dependent.
 */
public class PreferenceControllerFlavorHelper {

    public static void setupFlavoredUI(PlaybackPreferencesFragment ui) {
        ui.findPreference(UserPreferences.PREF_CAST_ENABLED).setEnabled(false);
    }
}

package de.danoeh.antennapodSA.preferences;

import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.fragment.preferences.PlaybackPreferencesFragment;

/**
 * Implements functions from PreferenceController that are flavor dependent.
 */
public class PreferenceControllerFlavorHelper {

    public static void setupFlavoredUI(PlaybackPreferencesFragment ui) {
        ui.findPreference(UserPreferences.PREF_CAST_ENABLED).setEnabled(false);
    }
}

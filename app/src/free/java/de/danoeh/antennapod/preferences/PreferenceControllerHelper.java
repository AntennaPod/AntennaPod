package de.danoeh.antennapod.preferences;

import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Implements functions from PreferenceController that are flavor dependent.
 */
public class PreferenceControllerHelper {

    static void setupFlavoredUI(PreferenceController.PreferenceUI ui) {
        ui.findPreference(UserPreferences.PREF_CAST_ENABLED).setEnabled(false);
    }
}

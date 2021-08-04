package de.danoeh.antennapod.fragment.preferences.dialog;

import de.danoeh.antennapod.core.preferences.UserPreferences;

class PreferenceDialog {
    private UserPreferences userPreferences;


    interface OnPreferenceChanged {
        /**
         * Notified when user confirms preference
         * @param object The preference
         */
        void preferenceChanged(Object object);
    }
}

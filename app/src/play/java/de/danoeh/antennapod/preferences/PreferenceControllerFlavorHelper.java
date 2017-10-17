package de.danoeh.antennapod.preferences;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Implements functions from PreferenceController that are flavor dependent.
 */
public class PreferenceControllerFlavorHelper {

    static void setupFlavoredUI(PreferenceController.PreferenceUI ui) {
        //checks whether Google Play Services is installed on the device (condition necessary for Cast support)
        ui.findPreference(UserPreferences.PREF_CAST_ENABLED).setOnPreferenceChangeListener((preference, o) -> {
            if (o instanceof Boolean && ((Boolean) o)) {
                final int googlePlayServicesCheck = GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(ui.getActivity());
                if (googlePlayServicesCheck == ConnectionResult.SUCCESS) {
                    return true;
                } else {
                    GoogleApiAvailability.getInstance()
                            .getErrorDialog(ui.getActivity(), googlePlayServicesCheck, 0)
                            .show();
                    return false;
                }
            }
            return true;
        });
    }
}

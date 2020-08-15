package de.danoeh.antennapod.preferences;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.fragment.preferences.PlaybackPreferencesFragment;

/**
 * Implements functions from PreferenceController that are flavor dependent.
 */
public class PreferenceControllerFlavorHelper {

    public static void setupFlavoredUI(PlaybackPreferencesFragment ui) {
        //checks whether Google Play Services is installed on the device (condition necessary for Cast support)
        ui.findPreference(UserPreferences.PREF_CAST_ENABLED).setOnPreferenceChangeListener((preference, o) -> {
            if (o instanceof Boolean && ((Boolean) o)) {
                final int googlePlayServicesCheck = GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(ui.getActivity());
                if (googlePlayServicesCheck == ConnectionResult.SUCCESS) {
                    displayRestartRequiredDialog(ui.requireContext());
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

    private static void displayRestartRequiredDialog(@NonNull Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(android.R.string.dialog_alert_title);
        dialog.setMessage(R.string.pref_restart_required);
        dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> PodcastApp.forceRestart());
        dialog.setCancelable(false);
        dialog.show();
    }
}

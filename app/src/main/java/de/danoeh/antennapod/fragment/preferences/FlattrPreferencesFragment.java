package de.danoeh.antennapod.fragment.preferences;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;
import de.danoeh.antennapod.dialog.AutoFlattrPreferenceDialog;

public class FlattrPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_FLATTR_AUTH = "pref_flattr_authenticate";
    private static final String PREF_FLATTR_REVOKE = "prefRevokeAccess";
    private static final String PREF_AUTO_FLATTR_PREFS = "prefAutoFlattrPrefs";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_flattr);
        setupFlattrScreen();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkFlattrItemVisibility();
    }

    private void setupFlattrScreen() {
        findPreference(PREF_FLATTR_REVOKE).setOnPreferenceClickListener(
                preference -> {
                    FlattrUtils.revokeAccessToken(getActivity());
                    checkFlattrItemVisibility();
                    return true;
                }
        );

        findPreference(PREF_AUTO_FLATTR_PREFS)
                .setOnPreferenceClickListener(preference -> {
                    AutoFlattrPreferenceDialog.newAutoFlattrPreferenceDialog(getActivity(),
                            new AutoFlattrPreferenceDialog.AutoFlattrPreferenceDialogInterface() {
                                @Override
                                public void onCancelled() {

                                }

                                @Override
                                public void onConfirmed(boolean autoFlattrEnabled, float autoFlattrValue) {
                                    UserPreferences.setAutoFlattrSettings(autoFlattrEnabled, autoFlattrValue);
                                    checkFlattrItemVisibility();
                                }
                            });
                    return true;
                });
    }

    private void checkFlattrItemVisibility() {
        boolean hasFlattrToken = FlattrUtils.hasToken();
        findPreference(PREF_FLATTR_AUTH).setEnabled(!hasFlattrToken);
        findPreference(PREF_FLATTR_REVOKE).setEnabled(hasFlattrToken);
        findPreference(PREF_AUTO_FLATTR_PREFS).setEnabled(hasFlattrToken);
    }
}

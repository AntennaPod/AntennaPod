package de.danoeh.antennapod.fragment.preferences;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;

public class IntegrationsPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_SCREEN_FLATTR = "prefFlattrSettings";
    private static final String PREF_SCREEN_GPODDER = "prefGpodderSettings";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_integrations);
        setupIntegrationsScreen();
    }

    private void setupIntegrationsScreen() {
        findPreference(PREF_SCREEN_FLATTR).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_flattr);
            return true;
        });
        findPreference(PREF_SCREEN_GPODDER).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_gpodder);
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        findPreference(PREF_SCREEN_FLATTR).setEnabled(FlattrUtils.hasAPICredentials());
    }
}

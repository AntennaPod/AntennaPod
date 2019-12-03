package de.danoeh.antennapodSA.fragment.preferences;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.activity.PreferenceActivity;

public class IntegrationsPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_SCREEN_GPODDER = "prefGpodderSettings";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_integrations);
        setupIntegrationsScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.integrations_label);
    }

    private void setupIntegrationsScreen() {
        findPreference(PREF_SCREEN_GPODDER).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_gpodder);
            return true;
        });
    }
}

package de.danoeh.antennapod.ui.preferences.screen;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;
import de.danoeh.antennapod.ui.preferences.R;

public class NotificationPreferencesFragment extends AnimatedPreferenceFragment {

    private static final String PREF_GPODNET_NOTIFICATIONS = "pref_gpodnet_notifications";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_notifications);
        setUpScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.notification_pref_fragment);
    }

    private void setUpScreen() {
        findPreference(PREF_GPODNET_NOTIFICATIONS).setEnabled(SynchronizationSettings.isProviderConnected());
    }
}

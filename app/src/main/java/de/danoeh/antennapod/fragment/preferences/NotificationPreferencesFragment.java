package de.danoeh.antennapod.fragment.preferences;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;

public class NotificationPreferencesFragment extends PreferenceFragmentCompat {

    private static final String TAG = "NotificationPrefFragment";
    private static final String PREF_GPODNET_NOTIFICATIONS = "pref_gpodnet_notifications";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_notifications);
        setUpScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.notification_pref_fragment);
    }

    private void setUpScreen() {
        final boolean loggedIn = GpodnetPreferences.loggedIn();
        findPreference(PREF_GPODNET_NOTIFICATIONS).setEnabled(loggedIn);
    }
}

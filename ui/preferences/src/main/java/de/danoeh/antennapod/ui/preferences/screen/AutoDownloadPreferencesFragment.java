package de.danoeh.antennapod.ui.preferences.screen;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import de.danoeh.antennapod.ui.preferences.R;

public class AutoDownloadPreferencesFragment extends AnimatedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_autodownload);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_automatic_download_title);
    }
}

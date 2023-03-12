package de.danoeh.antennapod.fragment.preferences;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;
import de.danoeh.antennapod.dialog.ProxyDialog;
import de.danoeh.antennapod.storage.preferences.UserPreferences;


public class NetworkPreferencesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PREF_SCREEN_AUTODL = "prefAutoDownloadSettings";
    private static final String PREF_PROXY = "prefProxy";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_network);
        setupNetworkScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.network_pref);
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setParallelDownloadsText(UserPreferences.getParallelDownloads());
    }

    private void setupNetworkScreen() {
        findPreference(PREF_SCREEN_AUTODL).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_autodownload);
            return true;
        });
        findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS).setOnPreferenceChangeListener((preference, o) -> {
            if (o instanceof Integer) {
                setParallelDownloadsText((Integer) o);
            }
            return true;
        });
        // validate and set correct value: number of downloads between 1 and 50 (inclusive)
        findPreference(PREF_PROXY).setOnPreferenceClickListener(preference -> {
            ProxyDialog dialog = new ProxyDialog(getActivity());
            dialog.show();
            return true;
        });
    }

    private void setParallelDownloadsText(int downloads) {
        final Resources res = getActivity().getResources();
        String s = res.getString(R.string.parallel_downloads, downloads);
        findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS).setSummary(s);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (UserPreferences.PREF_UPDATE_INTERVAL.equals(key)) {
            FeedUpdateManager.restartUpdateAlarm(getContext());
        }
    }
}

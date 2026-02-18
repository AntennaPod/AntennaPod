package de.danoeh.antennapod.ui.screen.preferences;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import de.danoeh.antennapod.ui.screen.feed.preferences.SkipPreferenceDialog;
import de.danoeh.antennapod.ui.screen.playback.VariableSpeedDialog;

import java.util.Map;

public class PlaybackPreferencesFragment extends AnimatedPreferenceFragment {
    private static final String PREF_PLAYBACK_SPEED_LAUNCHER = "prefPlaybackSpeedLauncher";
    private static final String PREF_PLAYBACK_REWIND_DELTA_LAUNCHER = "prefPlaybackRewindDeltaLauncher";
    private static final String PREF_PLAYBACK_FAST_FORWARD_DELTA_LAUNCHER = "prefPlaybackFastForwardDeltaLauncher";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_playback);

        setupPlaybackScreen();
        buildSmartMarkAsPlayedPreference();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.playback_pref);
    }

    private void setupPlaybackScreen() {
        final Activity activity = getActivity();

        findPreference(PREF_PLAYBACK_SPEED_LAUNCHER).setOnPreferenceClickListener(preference -> {
            new VariableSpeedDialog().show(getChildFragmentManager(), null);
            return true;
        });
        findPreference(PREF_PLAYBACK_REWIND_DELTA_LAUNCHER).setOnPreferenceClickListener(preference -> {
            SkipPreferenceDialog.showSkipPreference(activity, SkipPreferenceDialog.SkipDirection.SKIP_REWIND, null);
            return true;
        });
        findPreference(PREF_PLAYBACK_FAST_FORWARD_DELTA_LAUNCHER).setOnPreferenceClickListener(preference -> {
            SkipPreferenceDialog.showSkipPreference(activity, SkipPreferenceDialog.SkipDirection.SKIP_FORWARD, null);
            return true;
        });
        // Keep the Bluetooth/car auto-resume toggle visible on all API levels.
        Preference btResume = findPreference(UserPreferences.PREF_UNPAUSE_ON_BLUETOOTH_RECONNECT);
        if (btResume == null) {
            // Fallback: if the preference was stripped by resource merging, add it programmatically.
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(requireContext());
            pref.setKey(UserPreferences.PREF_UNPAUSE_ON_BLUETOOTH_RECONNECT);
            pref.setTitle(R.string.pref_unpauseOnBluetoothReconnect_title);
            pref.setSummary(R.string.pref_unpauseOnBluetoothReconnect_sum);
            pref.setDefaultValue(false);
            pref.setDependency(UserPreferences.PREF_PAUSE_ON_HEADSET_DISCONNECT);

            PreferenceScreen screen = getPreferenceScreen();
            PreferenceCategory targetCategory = null;
            if (screen != null && screen.getPreferenceCount() > 0) {
                Preference maybeCategory = screen.getPreference(0);
                if (maybeCategory instanceof PreferenceCategory) {
                    targetCategory = (PreferenceCategory) maybeCategory;
                }
            }
            if (targetCategory != null) {
                targetCategory.addPreference(pref);
            } else if (screen != null) {
                screen.addPreference(pref);
            }
            btResume = pref;
        }
        if (btResume != null) {
            btResume.setVisible(true);
        }

        buildEnqueueLocationPreference();
    }

    private void buildEnqueueLocationPreference() {
        final Resources res = requireActivity().getResources();
        final Map<String, String> options = new ArrayMap<>();
        {
            String[] keys = res.getStringArray(R.array.enqueue_location_values);
            String[] values = res.getStringArray(R.array.enqueue_location_options);
            for (int i = 0; i < keys.length; i++) {
                options.put(keys[i], values[i]);
            }
        }

        ListPreference pref = requirePreference(UserPreferences.PREF_ENQUEUE_LOCATION);
        pref.setSummary(res.getString(R.string.pref_enqueue_location_sum, options.get(pref.getValue())));

        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(newValue instanceof String)) {
                return false;
            }
            String newValStr = (String) newValue;
            pref.setSummary(res.getString(R.string.pref_enqueue_location_sum, options.get(newValStr)));
            return true;
        });
    }

    @NonNull
    private <T extends Preference> T requirePreference(@NonNull CharSequence key) {
        // Possibly put it to a common method in abstract base class
        T result = findPreference(key);
        if (result == null) {
            throw new IllegalArgumentException("Preference with key '" + key + "' is not found");

        }
        return result;
    }

    private void buildSmartMarkAsPlayedPreference() {
        final Resources res = getActivity().getResources();

        ListPreference pref = findPreference(UserPreferences.PREF_SMART_MARK_AS_PLAYED_SECS);
        String[] values = res.getStringArray(R.array.smart_mark_as_played_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            if (x == 0) {
                entries[x] = res.getString(R.string.pref_smart_mark_as_played_disabled);
            } else {
                int v = Integer.parseInt(values[x]);
                if (v < 60) {
                    entries[x] = res.getQuantityString(R.plurals.time_seconds_quantified, v, v);
                } else {
                    v /= 60;
                    entries[x] = res.getQuantityString(R.plurals.time_minutes_quantified, v, v);
                }
            }
        }
        pref.setEntries(entries);
    }
}

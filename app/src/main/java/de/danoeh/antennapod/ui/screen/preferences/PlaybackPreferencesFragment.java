package de.danoeh.antennapod.ui.screen.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.settings.CompressorPreferenceChangedEvent;
import de.danoeh.antennapod.event.settings.EqualizerPreferenceChangedEvent;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.preference.NegativeSeekBarPreference;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import de.danoeh.antennapod.ui.screen.feed.preferences.SkipPreferenceDialog;
import de.danoeh.antennapod.ui.screen.playback.VariableSpeedDialog;

import java.util.Map;

import org.greenrobot.eventbus.EventBus;

public class PlaybackPreferencesFragment
            extends AnimatedPreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
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
        registerToPreferenceChanged();
    }

    @Override
    public void onStop() {
        unregisterFromPreferenceChanged();
        super.onStop();
    }

    private void registerToPreferenceChanged() {
        SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
        assert sharedPrefs != null;
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    private void unregisterFromPreferenceChanged()  {
        SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
        assert sharedPrefs != null;
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) {
            return;
        }

        if (key.contains("prefCompressor")) {
            if (key.equals(UserPreferences.PREF_COMPRESSOR_ENABLED)) {
                setCompressorPrefsVisibility(UserPreferences.isCompressorEnabled());
            }
            postCompressorPrefsChangedEvent();
        }

        if (key.contains("prefEqualizer")) {
            if (key.equals(UserPreferences.PREF_EQUALIZER_ENABLED)) {
                setEqualizerPrefsVisibility(UserPreferences.isEqualizerEnabled());
            }
            postEqualizerPrefsChangedEvent();
        }
    }

    private static void postCompressorPrefsChangedEvent() {
        EventBus.getDefault().post(new CompressorPreferenceChangedEvent(
                UserPreferences.isCompressorEnabled(),
                UserPreferences.getCompressorThreshold(),
                UserPreferences.getCompressorRatio(),
                UserPreferences.getCompressorAttackTime(),
                UserPreferences.getCompressorReleaseTime(),
                UserPreferences.getCompressorNoiseGateThreshold(),
                UserPreferences.getCompressorPostGain()
        ));
    }

    private static void postEqualizerPrefsChangedEvent() {
        EventBus.getDefault().post(new EqualizerPreferenceChangedEvent(
                UserPreferences.isEqualizerEnabled(),
                UserPreferences.getEqualizerGains()
        ));
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
        if (Build.VERSION.SDK_INT >= 31) {
            findPreference(UserPreferences.PREF_UNPAUSE_ON_HEADSET_RECONNECT).setVisible(false);
            findPreference(UserPreferences.PREF_UNPAUSE_ON_BLUETOOTH_RECONNECT).setVisible(false);
        }

        buildEnqueueLocationPreference();

        setupDynamicsProcessingEffectPreferences();
    }

    private void setupDynamicsProcessingEffectPreferences() {
        setCompressorPrefsVisibility(UserPreferences.isCompressorEnabled());
        setCompressorResetActivatedHandler();
        setEqualizerPrefsVisibility(UserPreferences.isEqualizerEnabled());
        setEqualizerResetActivatedHandler();
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

    private void setCompressorPrefsVisibility(boolean visible) {
        requirePreference(UserPreferences.PREF_COMPRESSOR_RESET).setVisible(visible);
        requirePreference(UserPreferences.PREF_COMPRESSOR_THRESHOLD).setVisible(visible);
        requirePreference(UserPreferences.PREF_COMPRESSOR_RATIO).setVisible(visible);
        requirePreference(UserPreferences.PREF_COMPRESSOR_ATTACK_TIME).setVisible(visible);
        requirePreference(UserPreferences.PREF_COMPRESSOR_RELEASE_TIME).setVisible(visible);
        requirePreference(UserPreferences.PREF_COMPRESSOR_NOISE_GATE_THRESHOLD).setVisible(visible);
        requirePreference(UserPreferences.PREF_COMPRESSOR_POST_GAIN).setVisible(visible);
    }

    private void setCompressorResetActivatedHandler() {
        requirePreference(UserPreferences.PREF_COMPRESSOR_RESET).setOnPreferenceClickListener(pref -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.pref_compressor_reset_question_title)
                    .setMessage(R.string.pref_compressor_reset_question_text)
                    .setPositiveButton(R.string.yes, (dialog, which) ->
                            resetCompressorUiAndPrefStoreThenPostEvent())
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        });
    }

    private void resetCompressorUiAndPrefStoreThenPostEvent() {
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_COMPRESSOR_THRESHOLD).setValue(-50);
        this.<SeekBarPreference>requirePreference(UserPreferences.PREF_COMPRESSOR_RATIO).setValue(5);
        this.<SeekBarPreference>requirePreference(UserPreferences.PREF_COMPRESSOR_ATTACK_TIME).setValue(5);
        this.<SeekBarPreference>requirePreference(UserPreferences.PREF_COMPRESSOR_RELEASE_TIME).setValue(40);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_COMPRESSOR_NOISE_GATE_THRESHOLD)
                .setValue(-90);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_COMPRESSOR_POST_GAIN).setValue(15);

        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        assert prefs != null;
        prefs.edit().putInt(UserPreferences.PREF_COMPRESSOR_THRESHOLD, -50).apply();
        prefs.edit().putInt(UserPreferences.PREF_COMPRESSOR_RATIO, 5).apply();
        prefs.edit().putInt(UserPreferences.PREF_COMPRESSOR_ATTACK_TIME, 5).apply();
        prefs.edit().putInt(UserPreferences.PREF_COMPRESSOR_RELEASE_TIME, 40).apply();
        prefs.edit().putInt(UserPreferences.PREF_COMPRESSOR_NOISE_GATE_THRESHOLD, -90).apply();
        prefs.edit().putInt(UserPreferences.PREF_COMPRESSOR_POST_GAIN, 15).apply();

        postCompressorPrefsChangedEvent();
    }

    private void setEqualizerPrefsVisibility(boolean visible) {
        requirePreference(UserPreferences.PREF_EQUALIZER_RESET).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_01).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_02).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_03).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_04).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_05).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_06).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_07).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_08).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_09).setVisible(visible);
        requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_10).setVisible(visible);
    }

    private void setEqualizerResetActivatedHandler() {
        requirePreference(UserPreferences.PREF_EQUALIZER_RESET).setOnPreferenceClickListener(pref -> {
            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.pref_equalizer_reset_question_title)
                .setMessage(R.string.pref_equalizer_reset_question_text)
                .setPositiveButton(R.string.yes, (dialog, which) ->
                        resetEqualizerGainsUiAndPrefStoreThenPostEvent())
                .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        });
    }

    private void resetEqualizerGainsUiAndPrefStoreThenPostEvent() {
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_01).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_02).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_03).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_04).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_05).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_06).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_07).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_08).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_09).setValue(0);
        this.<NegativeSeekBarPreference>requirePreference(UserPreferences.PREF_EQUALIZER_GAIN_BAND_10).setValue(0);

        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        assert prefs != null;
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_01, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_02, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_03, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_04, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_05, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_06, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_07, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_08, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_09, 0).apply();
        prefs.edit().putInt(UserPreferences.PREF_EQUALIZER_GAIN_BAND_10, 0).apply();

        postEqualizerPrefsChangedEvent();
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

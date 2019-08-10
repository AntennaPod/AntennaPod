package de.danoeh.antennapod.fragment.preferences;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MediaplayerActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.gui.PictureInPictureUtil;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;
import de.danoeh.antennapod.preferences.PreferenceControllerFlavorHelper;

public class PlaybackPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_PLAYBACK_SPEED_LAUNCHER = "prefPlaybackSpeedLauncher";
    private static final String PREF_PLAYBACK_REWIND_DELTA_LAUNCHER = "prefPlaybackRewindDeltaLauncher";
    private static final String PREF_PLAYBACK_FAST_FORWARD_DELTA_LAUNCHER = "prefPlaybackFastForwardDeltaLauncher";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_playback);

        setupPlaybackScreen();
        PreferenceControllerFlavorHelper.setupFlavoredUI(this);
        buildSmartMarkAsPlayedPreference();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkSonicItemVisibility();
    }

    private void setupPlaybackScreen() {
        final Activity activity = getActivity();

        findPreference(PREF_PLAYBACK_SPEED_LAUNCHER)
                .setOnPreferenceClickListener(preference -> {
                    VariableSpeedDialog.showDialog(activity);
                    return true;
                });
        findPreference(PREF_PLAYBACK_REWIND_DELTA_LAUNCHER)
                .setOnPreferenceClickListener(preference -> {
                    MediaplayerActivity.showSkipPreference(activity, MediaplayerActivity.SkipDirection.SKIP_REWIND);
                    return true;
                });
        findPreference(PREF_PLAYBACK_FAST_FORWARD_DELTA_LAUNCHER)
                .setOnPreferenceClickListener(preference -> {
                    MediaplayerActivity.showSkipPreference(activity, MediaplayerActivity.SkipDirection.SKIP_FORWARD);
                    return true;
                });
        if (!PictureInPictureUtil.supportsPictureInPicture(activity)) {
            ListPreference behaviour = (ListPreference) findPreference(UserPreferences.PREF_VIDEO_BEHAVIOR);
            behaviour.setEntries(R.array.video_background_behavior_options_without_pip);
            behaviour.setEntryValues(R.array.video_background_behavior_values_without_pip);
        }
    }

    private void buildSmartMarkAsPlayedPreference() {
        final Resources res = getActivity().getResources();

        ListPreference pref = (ListPreference) findPreference(UserPreferences.PREF_SMART_MARK_AS_PLAYED_SECS);
        String[] values = res.getStringArray(R.array.smart_mark_as_played_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            if(x == 0) {
                entries[x] = res.getString(R.string.pref_smart_mark_as_played_disabled);
            } else {
                Integer v = Integer.parseInt(values[x]);
                if(v < 60) {
                    entries[x] = res.getQuantityString(R.plurals.time_seconds_quantified, v, v);
                } else {
                    v /= 60;
                    entries[x] = res.getQuantityString(R.plurals.time_minutes_quantified, v, v);
                }
            }
        }
        pref.setEntries(entries);
    }



    private void checkSonicItemVisibility() {
        if (Build.VERSION.SDK_INT < 16) {
            ListPreference p = (ListPreference) findPreference(UserPreferences.PREF_MEDIA_PLAYER);
            p.setEntries(R.array.media_player_options_no_sonic);
            p.setEntryValues(R.array.media_player_values_no_sonic);
        }
    }
}

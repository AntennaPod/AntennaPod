package de.danoeh.antennapod.dialog;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.IntentUtils;

public class VariableSpeedDialog {

    private static final String TAG = VariableSpeedDialog.class.getSimpleName();

    private static final Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
        Uri.parse("market://details?id=com.falconware.prestissimo"));

    private VariableSpeedDialog() {
    }

    public static void showDialog(final Context context) {
        if (org.antennapod.audio.MediaPlayer.isPrestoLibraryInstalled(context)
                || UserPreferences.useSonic()
                || UserPreferences.useExoplayer()
                || Build.VERSION.SDK_INT >= 23) {
            showSpeedSelectorDialog(context);
        } else {
            showGetPluginDialog(context, true);
        }
    }

    public static void showGetPluginDialog(final Context context) {
        showGetPluginDialog(context, false);
    }

    private static void showGetPluginDialog(final Context context, boolean showSpeedSelector) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.no_playback_plugin_title);
        builder.setMessage(R.string.no_playback_plugin_or_sonic_msg);

        if (Build.VERSION.SDK_INT >= 16) {
            builder.setPositiveButton(R.string.enable_sonic, (dialog, which) -> {
                UserPreferences.enableSonic();
                if (showSpeedSelector) {
                    showSpeedSelectorDialog(context);
                }
            });
        }
        if (IntentUtils.isCallable(context.getApplicationContext(), playStoreIntent)) {
            builder.setNegativeButton(R.string.download_plugin_label, (dialog, which) -> {
                try {
                    context.startActivity(playStoreIntent);
                } catch (ActivityNotFoundException e) {
                    // this is usually thrown on an emulator if the Android market is not installed
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            });
        }
        builder.setNeutralButton(R.string.close_label, null);
        builder.show();
    }

    private static void showSpeedSelectorDialog(final Context context) {
        final String[] speedValues = context.getResources().getStringArray(
                R.array.playback_speed_values);
        // According to Java spec these get initialized to false on creation
        final boolean[] speedChecked = new boolean[speedValues.length];

        // Build the "isChecked" array so that multiChoice dialog is
        // populated correctly
        List<String> selectedSpeedList = Arrays.asList(UserPreferences
                .getPlaybackSpeedArray());
        for (int i = 0; i < speedValues.length; i++) {
            speedChecked[i] = selectedSpeedList.contains(speedValues[i]);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.set_playback_speed_label);
        builder.setMultiChoiceItems(R.array.playback_speed_values,
            speedChecked, (dialog, which, isChecked) -> speedChecked[which] = isChecked);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok,
            (dialog, which) -> {
                int choiceCount = 0;
                for (boolean checked : speedChecked) {
                    if (checked) {
                        choiceCount++;
                    }
                }
                String[] newSpeedValues = new String[choiceCount];
                int newSpeedIndex = 0;
                for (int i = 0; i < speedChecked.length; i++) {
                    if (speedChecked[i]) {
                        newSpeedValues[newSpeedIndex++] = speedValues[i];
                    }
                }

                UserPreferences.setPlaybackSpeedArray(newSpeedValues);

            });
        builder.create().show();
    }

}

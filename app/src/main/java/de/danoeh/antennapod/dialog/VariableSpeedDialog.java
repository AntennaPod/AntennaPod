package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class VariableSpeedDialog {

    private VariableSpeedDialog() {
    }

    public static void showDialog(final Context context) {
        if (UserPreferences.useSonic()
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
        builder.setPositiveButton(R.string.enable_sonic, (dialog, which) -> {
            UserPreferences.enableSonic();
            if (showSpeedSelector) {
                showSpeedSelectorDialog(context);
            }
        });
        builder.setNeutralButton(R.string.close_label, null);
        builder.show();
    }

    private static void showSpeedSelectorDialog(final Context context) {
        DecimalFormatSymbols format = new DecimalFormatSymbols(Locale.US);
        format.setDecimalSeparator('.');
        DecimalFormat speedFormat = new DecimalFormat("0.00", format);

        final String[] speedValues = context.getResources().getStringArray(
                R.array.playback_speed_values);
        // According to Java spec these get initialized to false on creation
        final boolean[] speedChecked = new boolean[speedValues.length];

        // Build the "isChecked" array so that multiChoice dialog is populated correctly
        List<String> selectedSpeedList = new ArrayList<>();
        float[] selectedSpeeds = UserPreferences.getPlaybackSpeedArray();
        for (float speed : selectedSpeeds) {
            selectedSpeedList.add(speedFormat.format(speed));
        }

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

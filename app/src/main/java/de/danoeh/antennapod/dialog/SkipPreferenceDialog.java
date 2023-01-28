package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Shows the dialog that allows setting the skip time.
 */
public class SkipPreferenceDialog {
    public static void showSkipPreference(Context context, SkipDirection direction, TextView textView) {
        int checked = 0;

        int skipSecs;
        if (direction == SkipDirection.SKIP_FORWARD) {
            skipSecs = UserPreferences.getFastForwardSecs();
        } else {
            skipSecs = UserPreferences.getRewindSecs();
        }

        final int[] values = context.getResources().getIntArray(R.array.seek_delta_values);
        final String[] choices = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (skipSecs == values[i]) {
                checked = i;
            }
            choices[i] = String.format(Locale.getDefault(),
                    "%d %s", values[i], context.getString(R.string.time_seconds));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(direction == SkipDirection.SKIP_FORWARD ? R.string.pref_fast_forward : R.string.pref_rewind);
        builder.setSingleChoiceItems(choices, checked, (dialog, which) -> {
            int choice = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            if (choice < 0 || choice >= values.length) {
                System.err.printf("Choice in showSkipPreference is out of bounds %d", choice);
            } else {
                int seconds = values[choice];
                if (direction == SkipDirection.SKIP_FORWARD) {
                    UserPreferences.setFastForwardSecs(seconds);
                } else {
                    UserPreferences.setRewindSecs(seconds);
                }
                if (textView != null) {
                    textView.setText(NumberFormat.getInstance().format(seconds));
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.show();
    }

    public enum SkipDirection {
        SKIP_FORWARD, SKIP_REWIND
    }
}

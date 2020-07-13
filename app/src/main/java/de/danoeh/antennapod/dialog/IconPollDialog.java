package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.util.IntentUtils;

public class IconPollDialog {

    private IconPollDialog() {

    }

    public static final String PREFS_NAME = "IconPollDialog";
    public static final String KEY_DIALOG_ALLOWED = "dialog_allowed";

    public static void showIfNeeded(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (!preferences.getBoolean(KEY_DIALOG_ALLOWED, true)) {
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.icon_poll_title)
                .setMessage(R.string.icon_poll_message)
                .setCancelable(false)
                .setPositiveButton(R.string.icon_poll_vote, (dialog, which) ->
                    IntentUtils.openInBrowser(context, "https://www.surveymonkey.com/r/NTS7Z7N"))
                .setNegativeButton(R.string.icon_poll_dont_vote, null)
                .setOnDismissListener(dialog -> preferences.edit().putBoolean(KEY_DIALOG_ALLOWED, false).apply())
                .show();
    }
}

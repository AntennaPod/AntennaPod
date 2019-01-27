package de.danoeh.antennapod.core.dialog;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import de.danoeh.antennapod.core.R;

/** Creates Alert Dialog for null value. */
public class NullValueErrorDialogCreator {
    private NullValueErrorDialogCreator() {
    }

    public static void newRequestErrorDialog(Context context,
                                             String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setNeutralButton(android.R.string.ok,
                (dialog, which) -> dialog.dismiss())
                .setTitle(R.string.null_value_error_error)
                .setMessage(
                        context.getString(R.string.null_value_error_dialog_message_prefix)
                                + errorMessage);
        builder.create().show();
    }
}

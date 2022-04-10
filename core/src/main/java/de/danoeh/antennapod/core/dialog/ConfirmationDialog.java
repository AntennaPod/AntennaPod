package de.danoeh.antennapod.core.dialog;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import de.danoeh.antennapod.core.R;

/**
 * Creates an AlertDialog which asks the user to confirm something. Other
 * classes can handle events like confirmation or cancellation.
 */
public abstract class ConfirmationDialog {

    private static final String TAG = ConfirmationDialog.class.getSimpleName();

    private final Context context;
    private final int titleId;
    private final String message;

    private int positiveText;

    public ConfirmationDialog(Context context, int titleId, int messageId) {
        this(context, titleId, context.getString(messageId));
    }

    public ConfirmationDialog(Context context, int titleId, String message) {
        this.context = context;
        this.titleId = titleId;
        this.message = message;
    }

    private void onCancelButtonPressed(DialogInterface dialog) {
        Log.d(TAG, "Dialog was cancelled");
        dialog.dismiss();
    }

    public void setPositiveText(int id) {
        this.positiveText = id;
    }

    public abstract void onConfirmButtonPressed(DialogInterface dialog);

    public final AlertDialog createNewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText != 0 ? positiveText : R.string.confirm_label,
                (dialog, which) -> onConfirmButtonPressed(dialog));
        builder.setNegativeButton(R.string.cancel_label, (dialog, which) -> onCancelButtonPressed(dialog));
        builder.setOnCancelListener(ConfirmationDialog.this::onCancelButtonPressed);
        return builder.create();
    }
}

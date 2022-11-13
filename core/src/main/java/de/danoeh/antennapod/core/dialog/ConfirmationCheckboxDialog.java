package de.danoeh.antennapod.core.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.LayoutInflaterCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.core.R;

/**
 * Creates an AlertDialog which asks the user to confirm something. Other
 * classes can handle events like confirmation or cancellation.
 */
public abstract class ConfirmationCheckboxDialog {

    private static final String TAG = ConfirmationCheckboxDialog.class.getSimpleName();

    private final Context context;
    private final String title;
    private final String message;
    private final String checkboxMsg;
    private boolean isCheckboxChecked;
    private final View checkBoxView;


    private int positiveText;

    public ConfirmationCheckboxDialog(Context context, int titleId, int messageId, int checkboxMsgId, boolean isCheckboxChecked) {
        this(context, context.getString(titleId), context.getString(messageId), context.getString(checkboxMsgId), isCheckboxChecked);
    }

    public ConfirmationCheckboxDialog(Context context, String title, String message, String checkboxMsg, boolean isCheckboxChecked) {
        this.context = context;
        this.title = title;
        this.message = message;
        this.checkboxMsg = checkboxMsg;
        this.isCheckboxChecked = isCheckboxChecked;
        checkBoxView = View.inflate(context, R.layout.dialog_checkbox_view, null);
        ((CheckBox) checkBoxView.findViewById(R.id.dialog_checkbox)).setOnCheckedChangeListener(((buttonView, isChecked) -> onCheckStateChanged(buttonView, isChecked)));

    }


    private void onCancelButtonPressed(DialogInterface dialog) {
        Log.d(TAG, "Dialog was cancelled");
        dialog.dismiss();
    }

    public void setPositiveText(int id) {
        this.positiveText = id;
    }

    public abstract void onConfirmButtonPressed(DialogInterface dialog);

    public abstract void onCheckStateChanged(CompoundButton checkBox, boolean isCheckboxChecked);

    public final AlertDialog createNewDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText != 0 ? positiveText : R.string.confirm_label,
                (dialog, which) -> onConfirmButtonPressed(dialog));
        builder.setNegativeButton(R.string.cancel_label, (dialog, which) -> onCancelButtonPressed(dialog));
        builder.setOnCancelListener(ConfirmationCheckboxDialog.this::onCancelButtonPressed);
        builder.setView(checkBoxView);
        return builder.create();
    }
}

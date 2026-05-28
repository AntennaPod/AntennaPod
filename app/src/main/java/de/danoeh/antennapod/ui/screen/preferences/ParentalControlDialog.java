package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.Keyboard;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class ParentalControlDialog {
    public static void show(Context context, Runnable onSuccess) {
        show(context, onSuccess, null);
    }

    public static void show(Context context, Runnable onSuccess, Runnable onCancel) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.pref_parental_control_title);
        final EditTextDialogBinding dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context));
        dialogBinding.textInput.setHint(R.string.password_label);
        dialogBinding.textInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(dialogBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label, null);
        if (onCancel != null) {
            builder.setNegativeButton(R.string.cancel_label, (d, w) -> onCancel.run());
            builder.setOnCancelListener(d -> onCancel.run());
        } else {
            builder.setNegativeButton(R.string.cancel_label, null);
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        dialogBinding.textInput.requestFocus();
        Keyboard.show(context, dialogBinding.textInput);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = dialogBinding.textInput.getText().toString();
            if (UserPreferences.verifyParentalControlPassword(entered)) {
                alertDialog.dismiss();
                onSuccess.run();
            } else {
                dialogBinding.textInputLayout.setError(context.getString(R.string.wrong_password));
            }
        });
    }
}

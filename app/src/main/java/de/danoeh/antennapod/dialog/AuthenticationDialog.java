package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.AuthenticationDialogBinding;

/**
 * Displays a dialog with a username and password text field and an optional checkbox to save username and preferences.
 */
public abstract class AuthenticationDialog extends AlertDialog.Builder {
    boolean passwordHidden = true;

    public AuthenticationDialog(Context context, int titleRes, boolean enableUsernameField,
                                String usernameInitialValue, String passwordInitialValue) {
        super(context);
        setTitle(titleRes);
        AuthenticationDialogBinding viewBinding = AuthenticationDialogBinding.inflate(LayoutInflater.from(context));
        setView(viewBinding.getRoot());

        viewBinding.usernameEditText.setEnabled(enableUsernameField);
        if (usernameInitialValue != null) {
            viewBinding.usernameEditText.setText(usernameInitialValue);
        }
        if (passwordInitialValue != null) {
            viewBinding.passwordEditText.setText(passwordInitialValue);
        }
        viewBinding.showPasswordButton.setOnClickListener(v -> {
            if (passwordHidden) {
                viewBinding.passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                viewBinding.showPasswordButton.setAlpha(1.0f);
            } else {
                viewBinding.passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                viewBinding.showPasswordButton.setAlpha(0.6f);
            }
            passwordHidden = !passwordHidden;
        });

        setOnCancelListener(dialog -> onCancelled());
        setOnDismissListener(dialog -> onCancelled());
        setNegativeButton(R.string.cancel_label, null);
        setPositiveButton(R.string.confirm_label, (dialog, which)
                -> onConfirmed(viewBinding.usernameEditText.getText().toString(),
                        viewBinding.passwordEditText.getText().toString()));
    }

    protected void onCancelled() {

    }

    protected abstract void onConfirmed(String username, String password);
}

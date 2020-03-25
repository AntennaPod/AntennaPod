package de.danoeh.antennapod.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import de.danoeh.antennapod.R;

/**
 * Displays a dialog with a username and password text field and an optional checkbox to save username and preferences.
 */
public abstract class AuthenticationDialog extends AlertDialog.Builder {

    public AuthenticationDialog(Context context, int titleRes, boolean enableUsernameField,
                                String usernameInitialValue, String passwordInitialValue) {
        super(context);
        setTitle(titleRes);
        View rootView = View.inflate(context, R.layout.authentication_dialog, null);
        setView(rootView);

        final EditText etxtUsername = rootView.findViewById(R.id.etxtUsername);
        final EditText etxtPassword = rootView.findViewById(R.id.etxtPassword);

        etxtUsername.setEnabled(enableUsernameField);
        if (usernameInitialValue != null) {
            etxtUsername.setText(usernameInitialValue);
        }
        if (passwordInitialValue != null) {
            etxtPassword.setText(passwordInitialValue);
        }
        setOnCancelListener(dialog -> onCancelled());
        setNegativeButton(R.string.cancel_label, null);
        setPositiveButton(R.string.confirm_label, (dialog, which)
                -> onConfirmed(etxtUsername.getText().toString(), etxtPassword.getText().toString()));
    }

    protected void onCancelled() {

    }

    protected abstract void onConfirmed(String username, String password);
}

package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import de.danoeh.antennapod.R;

/**
 * Displays a dialog with a username and password text field and an optional checkbox to save username and preferences.
 */
public abstract class AuthenticationDialog extends Dialog {

    private final int titleRes;
    private final boolean enableUsernameField;
    private final boolean showSaveCredentialsCheckbox;
    private final String usernameInitialValue;
    private final String passwordInitialValue;

    public AuthenticationDialog(Context context, int titleRes, boolean enableUsernameField, boolean showSaveCredentialsCheckbox, String usernameInitialValue, String passwordInitialValue) {
        super(context);
        this.titleRes = titleRes;
        this.enableUsernameField = enableUsernameField;
        this.showSaveCredentialsCheckbox = showSaveCredentialsCheckbox;
        this.usernameInitialValue = usernameInitialValue;
        this.passwordInitialValue = passwordInitialValue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authentication_dialog);
        final EditText etxtUsername = (EditText) findViewById(R.id.etxtUsername);
        final EditText etxtPassword = (EditText) findViewById(R.id.etxtPassword);
        final CheckBox saveUsernamePassword = (CheckBox) findViewById(R.id.chkSaveUsernamePassword);
        final Button butConfirm = (Button) findViewById(R.id.butConfirm);
        final Button butCancel = (Button) findViewById(R.id.butCancel);

        if (titleRes != 0) {
            setTitle(titleRes);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        etxtUsername.setEnabled(enableUsernameField);
        if (showSaveCredentialsCheckbox) {
            saveUsernamePassword.setVisibility(View.VISIBLE);
        } else {
            saveUsernamePassword.setVisibility(View.GONE);
        }
        if (usernameInitialValue != null) {
            etxtUsername.setText(usernameInitialValue);
        }
        if (passwordInitialValue != null) {
            etxtPassword.setText(passwordInitialValue);
        }
        setOnCancelListener(dialog -> onCancelled());
        butCancel.setOnClickListener(v -> cancel());
        butConfirm.setOnClickListener(v -> {
            onConfirmed(etxtUsername.getText().toString(),
                    etxtPassword.getText().toString(),
                    showSaveCredentialsCheckbox && saveUsernamePassword.isChecked());
            dismiss();
        });
    }

    protected void onCancelled() {

    }

    protected abstract void onConfirmed(String username, String password, boolean saveUsernamePassword);
}

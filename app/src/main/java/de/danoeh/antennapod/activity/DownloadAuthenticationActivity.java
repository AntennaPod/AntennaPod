package de.danoeh.antennapod.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.storage.DownloadRequester;

/**
 * Shows a username and a password text field.
 * The activity MUST be started with the ARG_DOWNlOAD_REQUEST argument set to a non-null value.
 * Other arguments are optional.
 * The activity's result will be the same DownloadRequest with the entered username and password.
 */
public class DownloadAuthenticationActivity extends AppCompatActivity {

    /**
     * The download request object that contains information about the resource that requires a username and a password
     */
    public static final String ARG_DOWNLOAD_REQUEST = "request";
    /**
     * True if the request should be sent to the DownloadRequester when this activity is finished, false otherwise.
     * The default value is false.
     */
    public static final String ARG_SEND_TO_DOWNLOAD_REQUESTER_BOOL = "send_to_downloadrequester";

    private static final String RESULT_REQUEST = "request";

    private EditText etxtUsername;
    private EditText etxtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        setContentView(R.layout.download_authentication_activity);
        TextView txtvDescription = (TextView) findViewById(R.id.txtvDescription);
        etxtUsername = (EditText) findViewById(R.id.etxtUsername);
        etxtPassword = (EditText) findViewById(R.id.etxtPassword);
        Button butConfirm = (Button) findViewById(R.id.butConfirm);
        Button butCancel = (Button) findViewById(R.id.butCancel);

        Validate.isTrue(getIntent().hasExtra(ARG_DOWNLOAD_REQUEST), "Download request missing");
        DownloadRequest request = getIntent().getParcelableExtra(ARG_DOWNLOAD_REQUEST);
        boolean sendToDownloadRequester = getIntent().getBooleanExtra(ARG_SEND_TO_DOWNLOAD_REQUESTER_BOOL, false);

        String newDescription = txtvDescription.getText() + ":\n\n" + request.getTitle();
        txtvDescription.setText(newDescription);

        if (savedInstanceState != null) {
            etxtUsername.setText(savedInstanceState.getString("username"));
            etxtPassword.setText(savedInstanceState.getString("password"));
        }

        butConfirm.setOnClickListener(v -> {
            String username = etxtUsername.getText().toString();
            String password = etxtPassword.getText().toString();
            request.setUsername(username);
            request.setPassword(password);
            Intent result = new Intent();
            result.putExtra(RESULT_REQUEST, request);
            setResult(Activity.RESULT_OK, result);

            if (sendToDownloadRequester) {
                DownloadRequester.getInstance().download(DownloadAuthenticationActivity.this, request);
            }
            finish();
        });

        butCancel.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("username", etxtUsername.getText().toString());
        outState.putString("password", etxtPassword.getText().toString());
    }
}

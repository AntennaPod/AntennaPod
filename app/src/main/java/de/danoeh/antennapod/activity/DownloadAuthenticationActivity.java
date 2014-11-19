package de.danoeh.antennapod.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.BuildConfig;
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
public class DownloadAuthenticationActivity extends ActionBarActivity {
    private static final String TAG = "DownloadAuthenticationActivity";

    /**
     * The download request object that contains information about the resource that requires a username and a password
     */
    public static final String ARG_DOWNLOAD_REQUEST = "request";
    /**
     * True if the request should be sent to the DownloadRequester when this activity is finished, false otherwise.
     * The default value is false.
     */
    public static final String ARG_SEND_TO_DOWNLOAD_REQUESTER_BOOL = "send_to_downloadrequester";

    public static final String RESULT_REQUEST = "request";

    private EditText etxtUsername;
    private EditText etxtPassword;
    private Button butConfirm;
    private Button butCancel;
    private TextView txtvDescription;

    private DownloadRequest request;
    private boolean sendToDownloadRequester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.download_authentication_activity);

        etxtUsername = (EditText) findViewById(R.id.etxtUsername);
        etxtPassword = (EditText) findViewById(R.id.etxtPassword);
        butConfirm = (Button) findViewById(R.id.butConfirm);
        butCancel = (Button) findViewById(R.id.butCancel);
        txtvDescription = (TextView) findViewById(R.id.txtvDescription);

        Validate.isTrue(getIntent().hasExtra(ARG_DOWNLOAD_REQUEST), "Download request missing");

        request = getIntent().getParcelableExtra(ARG_DOWNLOAD_REQUEST);
        sendToDownloadRequester = getIntent().getBooleanExtra(ARG_SEND_TO_DOWNLOAD_REQUESTER_BOOL, false);

        if (savedInstanceState != null) {
            etxtUsername.setText(savedInstanceState.getString("username"));
            etxtPassword.setText(savedInstanceState.getString("password"));
        }

        txtvDescription.setText(txtvDescription.getText() + ":\n\n" + request.getTitle());

        butCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });

        butConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etxtUsername.getText().toString();
                String password = etxtPassword.getText().toString();
                request.setUsername(username);
                request.setPassword(password);
                Intent result = new Intent();
                result.putExtra(RESULT_REQUEST, request);
                setResult(Activity.RESULT_OK, result);

                if (sendToDownloadRequester) {
                   if (BuildConfig.DEBUG) Log.d(TAG, "Sending request to DownloadRequester");
                    DownloadRequester.getInstance().download(DownloadAuthenticationActivity.this, request);
                }
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("username", etxtUsername.getText().toString());
        outState.putString("password", etxtPassword.getText().toString());
    }
}

package de.danoeh.antennapod.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import org.apache.commons.lang3.Validate;

/**
 * Shows a username and a password text field.
 * The activity MUST be started with the ARG_DOWNlOAD_REQUEST argument set to a non-null value.
 */
public class DownloadAuthenticationActivity extends AppCompatActivity {

    /**
     * The download request object that contains information about the resource that requires a username and a password.
     */
    public static final String ARG_DOWNLOAD_REQUEST = "request";

    private EditText etxtUsername;
    private EditText etxtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getNoTitleTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_authentication_activity);

        etxtUsername = findViewById(R.id.etxtUsername);
        etxtPassword = findViewById(R.id.etxtPassword);

        Validate.isTrue(getIntent().hasExtra(ARG_DOWNLOAD_REQUEST), "Download request missing");
        DownloadRequest request = getIntent().getParcelableExtra(ARG_DOWNLOAD_REQUEST);

        TextView txtvDescription = findViewById(R.id.txtvDescription);
        String newDescription = txtvDescription.getText() + ":\n\n" + request.getTitle();
        txtvDescription.setText(newDescription);

        if (savedInstanceState != null) {
            etxtUsername.setText(savedInstanceState.getString("username"));
            etxtPassword.setText(savedInstanceState.getString("password"));
        }

        findViewById(R.id.butConfirm).setOnClickListener(v -> {
            String username = etxtUsername.getText().toString();
            String password = etxtPassword.getText().toString();
            request.setUsername(username);
            request.setPassword(password);
            DownloadRequester.getInstance().download(DownloadAuthenticationActivity.this, request);
            finish();
        });

        findViewById(R.id.butCancel).setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("username", etxtUsername.getText().toString());
        outState.putString("password", etxtPassword.getText().toString());
    }
}
